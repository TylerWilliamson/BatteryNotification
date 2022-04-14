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

import android.content.Context;
import android.util.Log;

import com.ominous.batterynotification.util.NotificationUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BatteryWorker extends Worker {
    public static final String TAG = "BatteryWorker";

    public BatteryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.v(TAG,"Updating Battery Notification in background");

        NotificationUtils.updateBatteryNotification(getApplicationContext());

        return Result.success(Data.EMPTY);
    }
}
