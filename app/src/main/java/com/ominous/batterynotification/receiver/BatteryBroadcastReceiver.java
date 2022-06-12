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

package com.ominous.batterynotification.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ominous.batterynotification.R;
import com.ominous.batterynotification.service.BatteryService;
import com.ominous.batterynotification.util.NotificationUtils;

public class BatteryBroadcastReceiver extends BroadcastReceiver {
    public final static String ACTION_UPDATE = "com.ominous.batterynotification.UPDATE_ACTION";
    private final static String TAG = "BatBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && isNotificationEnabled(context)) {
            switch (intent.getAction()) {
                case ACTION_UPDATE:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.v(TAG, "Updating Battery Notification in background");
                    NotificationUtils.updateBatteryNotification(context);

                    break;
                case Intent.ACTION_MY_PACKAGE_REPLACED:
                case Intent.ACTION_BOOT_COMPLETED:
                    Toast.makeText(context, context.getString(R.string.message_starting), Toast.LENGTH_SHORT).show();

                    NotificationUtils.startBatteryNotification(context);

                    if (context.getSharedPreferences(context.getString(R.string.preference_filename), Context.MODE_PRIVATE)
                            .getBoolean(context.getString(R.string.preference_immediate), false)) {
                        context.startService(new Intent(context, BatteryService.class));
                    }
                    break;
                default:
                    Log.e(TAG, context.getString(R.string.message_received_strange_intent, intent.getAction()));
            }
        }
    }

    private boolean isNotificationEnabled(Context context) {
        return context.getSharedPreferences(context.getString(R.string.preference_filename), Context.MODE_PRIVATE).getBoolean(context.getString(R.string.preference_notification), false);
    }
}
