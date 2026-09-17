package com.chris.uberratebanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Locale;

public final class Notifications {
    private static final String CHANNEL_ID = "ride_rate_offers";
    private Notifications() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Ride calculations", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Local notifications for rides detected by Ride Rate");
            nm.createNotificationChannel(channel);
        }
    }

    public static void notifyRide(Context context, RideRecord ride) {
        ensureChannel(context);
        Intent intent = new Intent(context, RideDetailActivity.class);
        intent.putExtra("ride_id", ride.id);
        PendingIntent pi = PendingIntent.getActivity(context, (int)(ride.id & 0x7fffffff), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = String.format(Locale.US, "$%.2f/hr • $%.2f • %d min", ride.hourly, ride.payout, ride.totalMinutes);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText("Tap to review this ride")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setColor(RateColors.forHourly(context, ride.hourly));
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try { nm.notify((int)(ride.id & 0x7fffffff), b.build()); } catch (SecurityException ignored) {}
    }
}
