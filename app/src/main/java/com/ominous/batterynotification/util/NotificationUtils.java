/*
 * Copyright 2016 - 2024 Tyler Williamson
 *
 * This file is part of BatteryNotification.
 *
 * BatteryNotification is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BatteryNotification is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BatteryNotification.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.batterynotification.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;

import com.ominous.batterynotification.R;
import com.ominous.batterynotification.service.BatteryService;
import com.ominous.batterynotification.work.BatteryWorkManager;

import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

public class NotificationUtils {
    public final static int NOTIFICATION_ID = 12345;
    public final static int FLAG_IMMUTABLE = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    private static final IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

    public static Notification makeBatteryNotification(Context context, Intent intent) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (Build.VERSION.SDK_INT >= 26 &&
                notificationManager != null &&
                notificationManager.getNotificationChannel(context.getString(R.string.app_name)) == null) {
            createNotificationChannel(context);
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_filename), Context.MODE_PRIVATE);
        String spacer = context.getString(R.string.notification_spacer);

        int level = BatteryUtils.getLevel(intent);
        int batteryIconRes = BatteryUtils.isCharging(intent) ? R.drawable.ic_battery_charging_full_white_24dp : R.drawable.ic_battery_full_white_24dp;
        String timeRemaining = sharedPreferences.getBoolean(context.getString(R.string.preference_time_remaining), false) ? BatteryUtils.getTimeRemaining(context, intent) : "";

        StringBuilder notificationTitleBuilder = new StringBuilder(context.getString(R.string.format_percent, level))
                .append(spacer)
                .append(BatteryUtils.getTemperature(context, intent, sharedPreferences.getBoolean(context.getString(R.string.preference_fahrenheit), false)));

        if (!timeRemaining.isEmpty()) {
            notificationTitleBuilder
                    .append(spacer)
                    .append(timeRemaining);
        }

        StringBuilder notificationContentBuilder = new StringBuilder();

        if (Build.VERSION.SDK_INT >= 21) {
            notificationContentBuilder
                    .append(BatteryUtils.getAmperage(context))
                    .append(spacer);
        }

        notificationContentBuilder
                .append(BatteryUtils.getVoltage(context, intent))
                .append(spacer)
                .append(BatteryUtils.getHealth(context, intent));

        Notification.Builder notificationBuilder;

        if (Build.VERSION.SDK_INT >= 26) {
            notificationBuilder = new Notification.Builder(context, context.getString(R.string.app_name));
        } else {
            notificationBuilder = new Notification.Builder(context)
                    .setPriority(Notification.PRIORITY_MIN);
        }

        notificationBuilder
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), FLAG_IMMUTABLE))
                .setOngoing(true)
                .setShowWhen(false)
                .setContentTitle(notificationTitleBuilder.toString())
                .setContentText(notificationContentBuilder.toString());

        if (Build.VERSION.SDK_INT >= 31 &&
                sharedPreferences.getBoolean(context.getString(R.string.preference_immediate), false)) {
            notificationBuilder
                    .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            notificationBuilder
                    .setColor(blendColorWithYellow(context, ContextCompat.getColor(context, level > 50 ? R.color.green : R.color.red), 100 - 2 * Math.abs(level - 50)))
                    .setSmallIcon(batteryIconRes);
        } else {
            VectorDrawableCompat drawable = VectorDrawableCompat.create(context.getResources(), batteryIconRes, null);

            if (drawable != null) {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);
                drawable.setColorFilter(blendColorWithYellow(context, ContextCompat.getColor(context, level > 50 ? R.color.green : R.color.red), 100 - 2 * Math.abs(level - 50)), PorterDuff.Mode.SRC_IN);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);

                notificationBuilder.setLargeIcon(bitmap);
            }
        }

        return notificationBuilder.build();
    }

    public static void startBatteryNotification(Context context) {
        updateBatteryNotification(context);

        BatteryWorkManager.setRepeatingAlarm(context);
    }

    public static void updateBatteryNotification(Context context) {
        updateBatteryNotification(context, context.registerReceiver(null, batteryIntentFilter));
    }

    public static void updateBatteryNotification(Context context, Intent intent) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, makeBatteryNotification(context, intent));
        }
    }

    public static void cancelBatteryNotification(Context context) {
        BatteryWorkManager.stopRepeatingAlarm(context);
        context.stopService(new Intent(context, BatteryService.class));

        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel(context.getString(R.string.app_name), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription(context.getString(R.string.notification_channel_description));
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    //Super specific
    private static int blendColorWithYellow(Context context, int otherColor, int percent) {
        int yellow = ContextCompat.getColor(context, R.color.yellow);

        return Color.argb(
                255,
                ((Color.red(otherColor) * (100 - percent) / 100) + (Color.red(yellow) * percent / 100)),
                ((Color.green(otherColor) * (100 - percent) / 100) + (Color.green(yellow) * percent / 100)),
                ((Color.blue(otherColor) * (100 - percent) / 100) + (Color.blue(yellow) * percent / 100)));
    }

    public static boolean canShowNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
