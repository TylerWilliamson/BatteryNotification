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
import java.util.Locale;

import androidx.annotation.NonNull;

class BatteryUtils {
    @NonNull
    private static String getBatteryStatus(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

        switch (status) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return context.getString(R.string.state_charging);
            case BatteryManager.BATTERY_PLUGGED_USB:
                return context.getString(R.string.state_usb_charging);
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return context.getString(R.string.state_wireless_charging);
            default:
                return context.getString(R.string.status_unknown);
        }
    }

    @NonNull
    private static String getBatteryState(Context context, Intent intent) {
        int state = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        switch (state) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return context.getString(R.string.state_charging);
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return context.getString(R.string.status_discharging);
            case BatteryManager.BATTERY_STATUS_FULL:
                return context.getString(R.string.status_full);
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return context.getString(R.string.status_not_charging);
            //case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return context.getString(R.string.status_unknown);
        }
    }

    @NonNull
    private static String getBatteryHealth(Context context, Intent intent) {
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
                return context.getString(R.string.status_unknown);
        }
    }

    @NonNull
    static String getTemperature(Context context, Intent intent, boolean useFahrenheit) {
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) * 10;

        if (useFahrenheit) {
            temperature = temperature / 5 * 9 + 3200;
        }

        return context.getString(
                R.string.formatted_temperature,
                divideAndRound(temperature),
                useFahrenheit ? context.getString(R.string.char_f) : context.getString(R.string.char_c));
    }

    static int getBatteryLevel(Intent intent) {
        return intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
    }

    @NonNull
    static String getAmperage(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

            int batteryCurrent = (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING ? -1 : 1)
                    * Math.abs(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));

            return Math.abs(batteryCurrent) > 1000000 ?
                    context.getString(R.string.formatted_a, divideAndRound(batteryCurrent / 10000)) :
                    (Math.abs(batteryCurrent) > 1000 ?
                            context.getString(R.string.formatted_ma, divideAndRound(batteryCurrent / 10)) :
                            context.getString(R.string.formatted_ua, batteryCurrent)
                    );
        } else {
            return "";
        }
    }

    @NonNull
    static String getState(Context context, Intent intent) {
        String state = getBatteryState(context, intent);

        return state.equals(context.getString(R.string.state_charging)) ? getBatteryStatus(context, intent) : state;
    }

    @NonNull
    static String getHealth(Context context, Intent intent) {
        return getBatteryHealth(context, intent);
    }

    @NonNull
    static String getVoltage(Context context, Intent intent) {
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

        return context.getString(R.string.formatted_v,
                voltage > 1000 ?
                        divideAndRound(voltage / 10) :
                        Integer.toString(voltage));
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
                        return computeTimeString(context, batteryTimeRemaining / 1000);
                    } else if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
                            && chargeTimeRemaining != null) {
                        return computeTimeString(context, chargeTimeRemaining / 1000);
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
                                    return computeTimeString(context, batteryTimeRemaining / 1000000);
                                } else if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
                                        && chargeTimeRemaining != null) {
                                    return computeTimeString(context, chargeTimeRemaining / 1000000);
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
    private static String computeTimeString(Context context, long secsRemaining) {
        if (secsRemaining < 1)
            return "";

        long min = (secsRemaining / 60) % 60,
                hour = (secsRemaining / (60 * 60)) % 24,
                day = (secsRemaining / (60 * 60 * 24));

        StringBuilder timeString = new StringBuilder();

        if (day > 0) {
            timeString.append(day).append(context.getString(R.string.char_d));
        }

        if (hour > 0) {
            timeString.append(hour).append(context.getString(R.string.char_h));
        }

        return timeString.append(min).append(context.getString(R.string.char_m_left)).toString();
    }

    @NonNull
    private static String divideAndRound(int i) {
        return String.format(Locale.getDefault(), "%.2f", i / 100f);
    }
}
