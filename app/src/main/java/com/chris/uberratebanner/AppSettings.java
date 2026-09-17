package com.chris.uberratebanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class AppSettings {
    private static final String PREFS = "ride_rate_settings";
    public static final int DEFAULT_LOW_CUTOFF = 24;
    public static final int DEFAULT_HIGH_CUTOFF = 27;
    public static final int DEFAULT_HISTORY_LIMIT = 200;
    public static final int DEFAULT_BANNER_DURATION_MS = 5000;
    public static final int DEFAULT_DELIVERY_WAIT_BUFFER = 2;

    private AppSettings() {}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static boolean autoDeleteEnabled(Context c){return prefs(c).getBoolean("auto_delete_enabled",true);}
    public static int historyLimit(Context c){return clamp(prefs(c).getInt("history_limit",DEFAULT_HISTORY_LIMIT),1,5000);}
    public static int bannerDurationMs(Context c){return clamp(prefs(c).getInt("banner_duration_ms",DEFAULT_BANNER_DURATION_MS),2500,15000);}
    public static void setBannerDurationMs(Context c,int ms){prefs(c).edit().putInt("banner_duration_ms",clamp(ms,2500,15000)).apply();}
    public static int lowCutoff(Context c){return clamp(prefs(c).getInt("low_cutoff",DEFAULT_LOW_CUTOFF),20,40);}
    public static int highCutoff(Context c){return clamp(prefs(c).getInt("high_cutoff",DEFAULT_HIGH_CUTOFF),lowCutoff(c),40);}
    public static int deliveryWaitBuffer(Context c){return clamp(prefs(c).getInt("delivery_wait_buffer",DEFAULT_DELIVERY_WAIT_BUFFER),0,15);}
    public static int bottomBannerColor(Context c){return prefs(c).getInt("bottom_banner_color",Color.rgb(227,38,54));}
    public static int midBannerColor(Context c){return prefs(c).getInt("mid_banner_color",Color.rgb(255,140,0));}
    public static int topBannerColor(Context c){return prefs(c).getInt("top_banner_color",Color.rgb(10,159,61));}
    public static int bottomTextColor(Context c){return prefs(c).getInt("bottom_text_color",Color.WHITE);}
    public static int midTextColor(Context c){return prefs(c).getInt("mid_text_color",Color.WHITE);}
    public static int topTextColor(Context c){return prefs(c).getInt("top_text_color",Color.WHITE);}

    public static void save(Context c,boolean autoDelete,int historyLimit,int deliveryBuffer,int low,int high,
                            int bottomBanner,int bottomText,int midBanner,int midText,int topBanner,int topText){
        if(high<low) high=low;
        prefs(c).edit().putBoolean("auto_delete_enabled",autoDelete)
                .putInt("history_limit",clamp(historyLimit,1,5000))
                .putInt("delivery_wait_buffer",clamp(deliveryBuffer,0,15))
                .putInt("low_cutoff",clamp(low,20,40)).putInt("high_cutoff",clamp(high,20,40))
                .putInt("bottom_banner_color",bottomBanner).putInt("bottom_text_color",bottomText)
                .putInt("mid_banner_color",midBanner).putInt("mid_text_color",midText)
                .putInt("top_banner_color",topBanner).putInt("top_text_color",topText).apply();
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
