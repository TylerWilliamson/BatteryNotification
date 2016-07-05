package com.ominous.batterynotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

class NotificationUtil {
    private static final int BATTERY_NOTIFICATION = 12345;
    private static NotificationManager notificationManager;
    private static SharedPreferences sharedPreferences;

    private static PendingIntent batterySummaryPendingIntent;
    public static Intent savedIntent;

    @SuppressWarnings("ResourceAsColor")
    public static Notification makeBatteryNotification(Context context, Intent intent) {
        if (notificationManager == null) init(context);

        savedIntent = intent;

        String timeRemaining = ((sharedPreferences.getBoolean(SettingsActivity.PREFERENCE_TIME_REMAINING, false)) ? Battery.getTimeRemaining(intent) : EMPTY);
        if (!timeRemaining.equals(EMPTY))
            timeRemaining = SPACER + timeRemaining;

        int level = Battery.getBatteryLevel(intent);
        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentIntent(batterySummaryPendingIntent)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentTitle(level + PERCENT + SPACER +
                        Battery.getTemperature(intent, sharedPreferences.getBoolean(SettingsActivity.PREFERENCE_FAHRENHEIT, false)) +
                        timeRemaining)
                .setContentText((Build.VERSION.SDK_INT >= 21 ? Battery.getAmp() + SPACER : EMPTY) +
                        Battery.getState(intent) + SPACER +
                        Battery.getHealth(intent))
                .setSmallIcon(Battery.getState(intent).equals(CHARGING)
                        ? R.drawable.ic_battery_charging_full_white_24dp : R.drawable.ic_battery_full_white_24dp);

        if (Build.VERSION.SDK_INT >= 21)
            notificationBuilder.setColor(level > 50 ? blendColorWithYellow(GREEN, 200 - 2 * level) : blendColorWithYellow(RED, 2 * level));

        return notificationBuilder.build();
    }

    public static void updateBatteryNotification(Context context, Intent intent) {
        notificationManager.notify(BATTERY_NOTIFICATION, makeBatteryNotification(context, intent));
    }

    private static void init(Context context) {
        Battery.init(context);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        sharedPreferences = context.getSharedPreferences(SettingsActivity.PREFERENCES_FILE, Context.MODE_PRIVATE);

        batterySummaryPendingIntent = PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0);
    }

    //Super specific
    private static int blendColorWithYellow(int c1, int percent) {
        int finalColor = 0;
        for (int i = 0; i < 4; i++)
            finalColor += (int) (((YELLOW >> (8 * i)) & 0xFF) * ((double) percent / 100.0) + ((c1 >> (8 * i)) & 0xFF) * ((double) (100 - percent) / 100.0)) << (8 * i);
        return finalColor;
    }

    private static final String
            CHARGING = "Charging",
            EMPTY = "",
            SPACER = " \u2022 ",
            PERCENT = "%";

    private static final int
            RED = 0xF44336,
            YELLOW = 0xFFEB3B,
            GREEN = 0x4CAF50;


}
