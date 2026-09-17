package com.chris.uberratebanner;

import android.content.Context;
import android.graphics.Insets;
import android.os.Build;
import android.view.WindowInsets;
import android.view.WindowManager;

public final class SafeArea {
    private SafeArea() {}

    public static int topInset(Context c) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                WindowManager wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
                WindowInsets wi = wm.getCurrentWindowMetrics().getWindowInsets();
                Insets insets = wi.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars() | WindowInsets.Type.displayCutout());
                return Math.max(0, insets.top);
            } catch (Exception ignored) {}
        }
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) return c.getResources().getDimensionPixelSize(id);
        return UiKit.dp(c, 24);
    }

    public static int bannerTop(Context c) {
        return topInset(c) + UiKit.dp(c, 6);
    }
}
