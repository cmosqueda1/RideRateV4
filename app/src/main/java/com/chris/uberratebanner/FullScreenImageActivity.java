package com.chris.uberratebanner;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class FullScreenImageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF000000);
        getWindow().setNavigationBarColor(0xFF000000);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        UiKit.applySafeInsets(root, 8, 8, 8, 8);
        String path = getIntent().getStringExtra("image_path");
        File file = path == null ? null : new File(path);
        Bitmap bitmap = file != null && file.exists() ? BitmapFactory.decodeFile(path) : null;

        if (bitmap == null) {
            TextView error = new TextView(this);
            error.setText("Verification image unavailable");
            error.setTextColor(0xFFFFFFFF);
            error.setTextSize(18f);
            error.setGravity(Gravity.CENTER);
            root.addView(error, new FrameLayout.LayoutParams(-1, -1));
        } else {
            ZoomImageView image = new ZoomImageView(bitmap);
            root.addView(image, new FrameLayout.LayoutParams(-1, -1));
        }

        TextView close = new TextView(this);
        close.setText("✕");
        close.setTextColor(0xFFFFFFFF);
        close.setTextSize(28f);
        close.setGravity(Gravity.CENTER);
        close.setBackground(UiKit.rounded(0xAA20242A, 24, this));
        close.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(UiKit.dp(this, 52), UiKit.dp(this, 52));
        cp.gravity = Gravity.TOP | Gravity.END;
        cp.topMargin = UiKit.dp(this, 8);
        cp.rightMargin = UiKit.dp(this, 8);
        root.addView(close, cp);
        setContentView(root);
    }

    private class ZoomImageView extends ImageView {
        private final ScaleGestureDetector scaleDetector;
        private float scale = 1f;
        private float lastX;
        private float lastY;

        ZoomImageView(Bitmap bitmap) {
            super(FullScreenImageActivity.this);
            setImageBitmap(bitmap);
            setScaleType(ScaleType.FIT_CENTER);
            scaleDetector = new ScaleGestureDetector(FullScreenImageActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override public boolean onScale(ScaleGestureDetector detector) {
                            scale *= detector.getScaleFactor();
                            scale = Math.max(1f, Math.min(scale, 5f));
                            setScaleX(scale);
                            setScaleY(scale);
                            return true;
                        }
                    });
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE && scale > 1f) {
                    float dx = event.getRawX() - lastX;
                    float dy = event.getRawY() - lastY;
                    setTranslationX(getTranslationX() + dx);
                    setTranslationY(getTranslationY() + dy);
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                } else if (event.getAction() == MotionEvent.ACTION_UP && scale <= 1f) {
                    animate().translationX(0).translationY(0).setDuration(120).start();
                }
            }
            return true;
        }
    }
}
