package com.chris.uberratebanner;

import android.content.Context;

public final class RateColors {
    public static final int BOTTOM = 0;
    public static final int MID = 1;
    public static final int TOP = 2;

    private RateColors() {}

    public static int rangeFor(Context context, double hourly) {
        int low = AppSettings.lowCutoff(context);
        int high = AppSettings.highCutoff(context);
        if (hourly < low) return BOTTOM;
        if (hourly <= high) return MID;
        return TOP;
    }

    public static int forHourly(Context context, double hourly) {
        switch (rangeFor(context, hourly)) {
            case BOTTOM: return AppSettings.bottomBannerColor(context);
            case MID: return AppSettings.midBannerColor(context);
            default: return AppSettings.topBannerColor(context);
        }
    }

    public static int textForHourly(Context context, double hourly) {
        switch (rangeFor(context, hourly)) {
            case BOTTOM: return AppSettings.bottomTextColor(context);
            case MID: return AppSettings.midTextColor(context);
            default: return AppSettings.topTextColor(context);
        }
    }

    public static String label(Context context, double hourly) {
        switch (rangeFor(context, hourly)) {
            case BOTTOM: return "BOTTOM";
            case MID: return "MID";
            default: return "TOP";
        }
    }

    public static String description(Context context) {
        return "Bottom below $" + AppSettings.lowCutoff(context) + "/hr • Mid $" +
                AppSettings.lowCutoff(context) + "–$" + AppSettings.highCutoff(context) +
                "/hr • Top above $" + AppSettings.highCutoff(context) + "/hr";
    }
}
