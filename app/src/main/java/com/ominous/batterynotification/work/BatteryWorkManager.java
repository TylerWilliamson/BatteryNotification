/*
 *     Copyright 2016 - 2022 Tyler Williamson
 *
 *     This file is part of BatteryNotification.
 *
 *     BatteryNotification is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     BatteryNotification is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with BatteryNotification.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.batterynotification.work;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.ominous.batterynotification.R;
import com.ominous.batterynotification.receiver.BatteryBroadcastReceiver;
import com.ominous.batterynotification.util.NotificationUtils;

import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BatteryWorkManager {
    private final static int ONE_MIN = 60000, REQUEST_CODE = 123;

    public static void setRepeatingAlarm(Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);

        if (alarmManager != null) {
            stopRepeatingAlarm(context);

            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + ONE_MIN,
                    ONE_MIN,
                    getPendingIntent(context));
        }
    }

    public static void stopRepeatingAlarm(Context context) {
        AlarmManager alarmManager = ContextCompat.getSystemService(context, AlarmManager.class);

        if (alarmManager != null) {
            alarmManager.cancel(getPendingIntent(context));
        }

    }

    private static PendingIntent getPendingIntent(Context context) {
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                new Intent(context, BatteryBroadcastReceiver.class)
                        .setAction(context.getString(R.string.intent_update_action)),
                NotificationUtils.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static void enqueueBackgroundJob(Context context) {
        WorkManager.getInstance(context).enqueue(
                new OneTimeWorkRequest.Builder(BatteryWorker.class)
                        .addTag("batteryWork")
                        .build()
        );
    }
}
