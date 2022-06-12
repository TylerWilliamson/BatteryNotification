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

package com.ominous.batterynotification.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;

import com.ominous.batterynotification.R;

import java.lang.reflect.Field;

import androidx.annotation.NonNull;

class BatteryUtils {
    @NonNull
    static String getTemperature(Context context, Intent intent, boolean useFahrenheit) {
        double temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.;

        return context.getString(
                useFahrenheit ? R.string.format_temperature_f : R.string.format_temperature_c,
                useFahrenheit ? temperature / 5 * 9 + 32 : temperature);
    }

    static int getLevel(Intent intent) {
        return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
    }

    @NonNull
    static String getAmperage(Context context) {
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

            int batteryCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

            return Math.abs(batteryCurrent) > 1000000 ?
                    context.getString(R.string.format_amperage_a, batteryCurrent / 1000000.) :
                    (Math.abs(batteryCurrent) > 1000 ?
                            context.getString(R.string.format_amperage_ma, batteryCurrent / 1000.) :
                            context.getString(R.string.format_amperage_ua, batteryCurrent)
                    );
        } else {
            return "";
        }
    }

    @NonNull
    static String getHealth(Context context, Intent intent) {
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                return context.getString(R.string.health_cold);
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return context.getString(R.string.health_dead);
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return context.getString(R.string.health_overheating);
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return context.getString(R.string.health_overvoltage);
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return context.getString(R.string.health_healthy);
            //case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            //case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
            default:
                return context.getString(R.string.health_unknown);
        }
    }

    @NonNull
    static String getVoltage(Context context, Intent intent) {
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

        return context.getString(R.string.format_voltage,
                voltage > 1000 ?
                        voltage / 1000. :
                        voltage);
    }

    static boolean isCharging(Intent intent) {
        return intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    @SuppressLint("PrivateApi")
    @NonNull
    static String getTimeRemaining(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                //Somehow Android made things easier instead of harder
                BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

                //noinspection JavaReflectionMemberAccess
                Field fieldBatteryStats = BatteryManager.class.getDeclaredField("mBatteryStats");
                fieldBatteryStats.setAccessible(true);
                Object batteryStats = fieldBatteryStats.get(bm);

                if (batteryStats != null) {
                    Long batteryTimeRemaining = (Long) batteryStats
                            .getClass()
                            .getMethod("computeBatteryTimeRemaining")
                            .invoke(batteryStats);
                    Long chargeTimeRemaining = (Long) batteryStats
                            .getClass()
                            .getMethod("computeChargeTimeRemaining")
                            .invoke(batteryStats);

                    if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_DISCHARGING
                            && batteryTimeRemaining != null) {
                        return computeTimeString(context, (int) (batteryTimeRemaining / 1000));
                    } else if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
                            && chargeTimeRemaining != null) {
                        return computeTimeString(context, (int) (chargeTimeRemaining / 1000));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT >= 21) {
            try {
                //IBatteryStats iBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats");

                IBinder batteryStatsIBinder = (IBinder) Class.forName("android.os.ServiceManager")
                        .getMethod("getService", String.class)
                        .invoke(null, "batterystats");

                Object iBatteryStats = Class
                        .forName("com.android.internal.app.IBatteryStats$Stub")
                        .getMethod("asInterface", IBinder.class)
                        .invoke(null, batteryStatsIBinder);

                if (iBatteryStats != null) {
                    //byte[] data = iBatteryStats.getStatistics();
                    byte[] data = (byte[]) iBatteryStats
                            .getClass()
                            .getMethod("getStatistics")
                            .invoke(iBatteryStats);

                    if (data != null) {
                        Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(data, 0, data.length);
                        parcel.setDataPosition(0);

                        Object creator = Class
                                .forName("com.android.internal.os.BatteryStatsImpl")
                                .getField("CREATOR").get(null);

                        if (creator != null) {
                            //BatteryStats batteryStats = BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                            Object batteryStats = creator
                                    .getClass()
                                    .getMethod("createFromParcel", Parcel.class)
                                    .invoke(creator, parcel);

                            parcel.recycle();

                            if (batteryStats != null) {
                                long now = SystemClock.elapsedRealtime() * 1000;
                                Long batteryTimeRemaining = (Long) batteryStats
                                        .getClass()
                                        .getMethod("computeBatteryTimeRemaining", long.class)
                                        .invoke(batteryStats, now);
                                Long chargeTimeRemaining = (Long) batteryStats
                                        .getClass()
                                        .getMethod("computeChargeTimeRemaining", long.class)
                                        .invoke(batteryStats, now);

                                if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_DISCHARGING
                                        && batteryTimeRemaining != null) {
                                    return computeTimeString(context, (int) (batteryTimeRemaining / 1000000));
                                } else if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
                                        && chargeTimeRemaining != null) {
                                    return computeTimeString(context, (int) (chargeTimeRemaining / 1000000));
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "";
    }

    @NonNull
    private static String computeTimeString(Context context, int secsRemaining) {
        if (secsRemaining < 1) {
            return "";
        }

        int min  = (secsRemaining / 60) % 60;
        int hour = (secsRemaining / (60 * 60)) % 24;
        int day  = (secsRemaining / (60 * 60 * 24));

        if (day > 0) {
            return context.getString(R.string.format_time_remaining_days, day, hour, min);
        } else if (hour > 0) {
            return context.getString(R.string.format_time_remaining_hours, hour, min);
        } else {
            return context.getString(R.string.format_time_remaining_mins, min);
        }
    }
}
