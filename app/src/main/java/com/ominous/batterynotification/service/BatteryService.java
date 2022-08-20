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

package com.ominous.batterynotification.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.ominous.batterynotification.util.NotificationUtils;

//Updates immediately, or after a min
public class BatteryService extends Service {
    public final static IntentFilter UPDATE_FILTER = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private final static String TAG = "BatteryService";
    private final BroadcastReceiver bbr = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Updating Battery Notification in foreground");
            NotificationUtils.updateBatteryNotification(context, intent);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Starting Foreground Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startForeground(NotificationUtils.NOTIFICATION_ID,
                NotificationUtils.makeBatteryNotification(this,
                        this.registerReceiver(bbr, UPDATE_FILTER)));

        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        try {
            this.unregisterReceiver(bbr);
        } catch (IllegalArgumentException e) {
            //
        }
    }
}
