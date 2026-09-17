package com.chris.uberratebanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Lightweight two-thumb integer-only range control. No Material dependency is required.
 * Values snap to whole dollars per hour.
 */
public class DualRangeSlider extends View {
    public interface Listener {
        void onChanged(int lowValue, int highValue);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int min = 20;
    private int max = 40;
    private int low = 24;
    private int high = 27;
    private int activeThumb = 0;
    private Listener listener;

    private float leftPad;
    private float rightPad;
    private float trackY;
    private float thumbRadius;

    public DualRangeSlider(Context context) { super(context); init(); }
    public DualRangeSlider(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public DualRangeSlider(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setFocusable(true);
        setContentDescription("Hourly rate range boundaries");
        float d = getResources().getDisplayMetrics().density;
        leftPad = 22f * d;
        rightPad = 22f * d;
        thumbRadius = 10f * d;
        setMinimumHeight(Math.round(92f * d));
    }

    public void setBounds(int minValue, int maxValue) {
        min = minValue;
        max = Math.max(minValue + 1, maxValue);
        setValues(low, high);
    }

    public void setValues(int lowValue, int highValue) {
        low = clamp(lowValue, min, max);
        high = clamp(highValue, low, max);
        invalidate();
        notifyListener();
    }

    public int getLowValue() { return low; }
    public int getHighValue() { return high; }
    public void setListener(Listener l) { listener = l; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float d = getResources().getDisplayMetrics().density;
        trackY = 42f * d;
        float startX = leftPad;
        float endX = Math.max(startX + 1f, width - rightPad);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(4f * d);
        paint.setColor(0xFF4F626B);
        canvas.drawLine(startX, trackY, endX, trackY, paint);

        float lowX = xFor(low, startX, endX);
        float highX = xFor(high, startX, endX);
        paint.setColor(0xFF2D7EF7);
        canvas.drawLine(lowX, trackY, highX, trackY, paint);

        paint.setStrokeWidth(1f * d);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10f * d);
        for (int v = min; v <= max; v++) {
            float x = xFor(v, startX, endX);
            boolean major = (v % 5 == 0) || v == min || v == max;
            paint.setColor(major ? 0xFF90A4AE : 0xFF4F626B);
            float tick = (major ? 8f : 4f) * d;
            canvas.drawLine(x, trackY + 12f * d, x, trackY + 12f * d + tick, paint);
            if (major) {
                paint.setColor(0xFFA4B4BB);
                canvas.drawText(String.valueOf(v), x, trackY + 34f * d, paint);
            }
        }

        drawThumb(canvas, lowX, low);
        drawThumb(canvas, highX, high);
    }

    private void drawThumb(Canvas canvas, float x, int value) {
        float d = getResources().getDisplayMetrics().density;
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x, trackY, thumbRadius + 2f * d, paint);
        paint.setColor(0xFF2D7EF7);
        canvas.drawCircle(x, trackY, thumbRadius, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(12f * d);
        paint.setColor(0xFFF5F9FA);
        canvas.drawText("$" + value, x, trackY - 18f * d, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        float startX = leftPad;
        float endX = Math.max(startX + 1f, getWidth() - rightPad);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                float lowX = xFor(low, startX, endX);
                float highX = xFor(high, startX, endX);
                activeThumb = Math.abs(event.getX() - lowX) <= Math.abs(event.getX() - highX) ? 1 : 2;
                updateFromX(event.getX(), startX, endX);
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromX(event.getX(), startX, endX);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateFromX(event.getX(), startX, endX);
                activeThumb = 0;
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFromX(float x, float startX, float endX) {
        float ratio = (x - startX) / (endX - startX);
        ratio = Math.max(0f, Math.min(1f, ratio));
        int value = Math.round(min + ratio * (max - min));
        if (activeThumb == 1) {
            low = Math.min(value, high);
        } else if (activeThumb == 2) {
            high = Math.max(value, low);
        }
        invalidate();
        notifyListener();
    }

    private float xFor(int value, float startX, float endX) {
        return startX + ((value - min) / (float)(max - min)) * (endX - startX);
    }

    private void notifyListener() {
        if (listener != null) listener.onChanged(low, high);
    }

    private int clamp(int value, int minValue, int maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }
}
