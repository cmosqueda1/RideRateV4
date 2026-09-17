package com.chris.uberratebanner;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Manual, tap-to-analyze capture service.
 *
 * RideRate intentionally does NOT monitor accessibility events or keep a MediaProjection session
 * open. The accessibility service is used only for a movable accessibility overlay and a single
 * on-demand screenshot when the user taps the bubble. This avoids Android treating RideRate as an
 * active screen-sharing/recording session, which can minimize/redact notifications and create an
 * unavoidable ongoing foreground-service notification.
 */
public class UberAccessibilityService extends AccessibilityService {
    private static final String PREFS = "riderate_service";
    private static final String KEY_ACTIVE = "bubble_active";
    private static volatile UberAccessibilityService instance;
    public static volatile boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private ImageView bubble;
    private WindowManager.LayoutParams bubbleLp;
    private OfferAnalyzer analyzer;
    private boolean busy = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        analyzer = new OfferAnalyzer(this);
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)) {
            showBubble();
        }
    }

    public static boolean isConnected() { return instance != null; }
    public static boolean isRunning() { return running && instance != null; }

    public static void setRideRateActive(android.content.Context context, boolean active) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, active).apply();
        MonitorState.active = active;
        UberAccessibilityService s = instance;
        if (s != null) {
            if (active) s.showBubble(); else s.hideBubble();
        } else if (!active) {
            running = false;
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately empty. RideRate never continuously scans screen content.
    }
    @Override public void onInterrupt() { }

    private void showBubble() {
        if (windowManager == null || bubble != null) {
            if (bubble != null) { running = true; MonitorState.active = true; }
            return;
        }
        bubble = new ImageView(this);
        bubble.setImageResource(R.drawable.ic_bubble);
        bubble.setBackground(UiKit.rounded(Color.argb(220, 7, 17, 21), 28, this));
        bubble.setPadding(dp(8), dp(8), dp(8), dp(8));

        int size = dp(54);
        bubbleLp = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        bubbleLp.gravity = Gravity.TOP | Gravity.START;
        bubbleLp.x = screenWidth() - size - dp(14);
        bubbleLp.y = Math.max(SafeArea.topInset(this) + dp(18), dp(60));

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startX, startY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX=e.getRawX(); downY=e.getRawY(); startX=bubbleLp.x; startY=bubbleLp.y; moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx=Math.round(e.getRawX()-downX), dy=Math.round(e.getRawY()-downY);
                        if(Math.abs(dx)+Math.abs(dy)>8) moved=true;
                        bubbleLp.x=clamp(startX+dx,dp(10),screenWidth()-size-dp(10));
                        bubbleLp.y=clamp(startY+dy,SafeArea.topInset(UberAccessibilityService.this)+dp(10),screenHeight()-size-bottomInset()-dp(10));
                        try{windowManager.updateViewLayout(bubble,bubbleLp);}catch(Exception ignored){}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(!moved) analyzeNow();
                        else {
                            bubbleLp.x=bubbleLp.x<screenWidth()/2?dp(12):screenWidth()-size-dp(12);
                            try{windowManager.updateViewLayout(bubble,bubbleLp);}catch(Exception ignored){}
                        }
                        return true;
                }
                return false;
            }
        });
        try {
            windowManager.addView(bubble, bubbleLp);
            running = true;
            MonitorState.active = true;
        } catch (Exception e) {
            bubble = null;
            running = false;
            MonitorState.active = false;
        }
    }

    private void hideBubble() {
        running = false;
        MonitorState.active = false;
        busy = false;
        if (bubble != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
    }

    private void analyzeNow() {
        if (busy || Build.VERSION.SDK_INT < 30 || bubble == null) return;
        busy = true;
        bubble.setVisibility(View.INVISIBLE);
        // Give Android one frame to remove our bubble before taking the evidence screenshot.
        handler.postDelayed(this::captureBottomHalf, 55);
    }

    private void captureBottomHalf() {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult screenshot) {
                    HardwareBuffer buffer = screenshot.getHardwareBuffer();
                    Bitmap software = null;
                    try {
                        Bitmap hardware = Bitmap.wrapHardwareBuffer(buffer, screenshot.getColorSpace());
                        if (hardware == null) { finishBusy(); return; }
                        software = hardware.copy(Bitmap.Config.ARGB_8888, false);
                        if (software == null) { finishBusy(); return; }
                        int top = Math.round(software.getHeight() * 0.30f);
                        Bitmap lower = Bitmap.createBitmap(
                                software,
                                0,
                                top,
                                software.getWidth(),
                                software.getHeight() - top
                        );
                        software.recycle(); software = null;
                        analyzer.analyze(lower, new OfferAnalyzer.Callback() {
                            @Override public void onSuccess(OfferParser.ParsedOffer offer) { saveResult(offer, lower); }
                            @Override public void onFailure(String message) {
                                lower.recycle();
                                showTransient("No valid Uber offer detected", UiKit.SURFACE_2, UiKit.TEXT);
                                finishBusy();
                            }
                        });
                    } catch (Exception e) {
                        finishBusy();
                    } finally {
                        if (software != null) software.recycle();
                        try { buffer.close(); } catch (Exception ignored) {}
                    }
                }
                @Override public void onFailure(int errorCode) {
                    showTransient("Screen capture failed", UiKit.SURFACE_2, UiKit.TEXT);
                    finishBusy();
                }
            });
        } catch (Exception e) {
            showTransient("Screen capture failed", UiKit.SURFACE_2, UiKit.TEXT);
            finishBusy();
        }
    }

    private void saveResult(OfferParser.ParsedOffer o, Bitmap lower) {
        long now=System.currentTimeMillis();
        RideRecord r=new RideRecord();
        r.createdAt=now; r.payout=o.payout; r.pickupMinutes=o.pickupMinutes; r.tripMinutes=o.tripMinutes;
        r.baseTotalMinutes=o.baseTotalMinutes; r.deliveryBufferMinutes=o.deliveryBufferMinutes;
        r.stopCount=o.stopCount; r.stopBufferMinutes=o.stopBufferMinutes;
        r.totalMinutes=o.totalMinutes; r.hourly=o.hourly; r.type=o.type; r.source="Floating OCR"; r.ocrText=o.rawText;
        RideDatabase db=new RideDatabase(this);
        r.id=db.insert(r);
        try { String p=ImageStorage.saveVerificationCrop(this,lower,o.cropRect,now); db.updateThumb(r.id,p); } catch(Exception ignored) {}
        db.close(); lower.recycle();
        Notifications.notifyRide(this,r);
        showResultBanner(r);
        finishBusy();
    }

    private void showResultBanner(RideRecord r) {
        int bg=BannerUi.translucent(RateColors.forHourly(this,r.hourly));
        showTransient(String.format(Locale.US,"$%.2f/hr  •  $%.2f  •  %d min",r.hourly,r.payout,r.totalMinutes), bg, RateColors.textForHourly(this,r.hourly));
    }

    private void showTransient(String text,int bg,int fg) {
        TextView v=new TextView(this);
        v.setText(text); v.setTextSize(15); v.setTypeface(null,android.graphics.Typeface.BOLD); v.setTextColor(fg); v.setGravity(Gravity.CENTER);
        v.setBackground(UiKit.rounded(bg,11,this)); v.setPadding(dp(10),0,dp(10),0);
        int height=Math.max(dp(28),Math.round(getResources().getDisplayMetrics().densityDpi*.25f));
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
                screenWidth()-dp(32),height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL; lp.y=SafeArea.bannerTop(this);
        try { windowManager.addView(v,lp); handler.postDelayed(()->{try{windowManager.removeView(v);}catch(Exception ignored){}},AppSettings.bannerDurationMs(this)); } catch(Exception ignored) {}
    }

    private void finishBusy() {
        busy=false;
        if (bubble != null && getSharedPreferences(PREFS,MODE_PRIVATE).getBoolean(KEY_ACTIVE,false)) bubble.setVisibility(View.VISIBLE);
    }

    private int screenWidth(){if(Build.VERSION.SDK_INT>=30)return windowManager.getCurrentWindowMetrics().getBounds().width();return getResources().getDisplayMetrics().widthPixels;}
    private int screenHeight(){if(Build.VERSION.SDK_INT>=30)return windowManager.getCurrentWindowMetrics().getBounds().height();return getResources().getDisplayMetrics().heightPixels;}
    private int bottomInset(){if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=windowManager.getCurrentWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars());return x.bottom;}return 0;}
    private int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private int dp(int v){return UiKit.dp(this,v);}

    @Override public void onDestroy() {
        hideBubble();
        if (analyzer != null) analyzer.close();
        analyzer=null; instance=null; running=false; MonitorState.active=false;
        super.onDestroy();
    }
}
