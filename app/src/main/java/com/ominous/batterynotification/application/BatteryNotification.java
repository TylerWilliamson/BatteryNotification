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

package com.ominous.batterynotification.application;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import me.weishu.reflection.Reflection;

public class BatteryNotification extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (Build.VERSION.SDK_INT >= 28) {
            //The nuclear option
            Reflection.unseal(base);
        }
    }
}
