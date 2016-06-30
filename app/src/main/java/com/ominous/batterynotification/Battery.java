package com.ominous.batterynotification;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.SparseArray;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;

class Battery {
    private static BatteryManager batteryManager;

    private static SparseArray<String> batteryStatusMap;
    private static SparseArray<String> batteryStateMap;
    private static SparseArray<String> batteryHealthMap;

    public static void init(Context context) {
        batteryStatusMap = new SparseArray<>();
        batteryStatusMap.append(BatteryManager.BATTERY_PLUGGED_AC, CHARGING);
        batteryStatusMap.append(BatteryManager.BATTERY_PLUGGED_USB, USB_CHARGING);
        batteryStatusMap.append(BatteryManager.BATTERY_PLUGGED_WIRELESS, WIRELESS_CHARGING);

        batteryStateMap = new SparseArray<>();
        batteryStateMap.append(BatteryManager.BATTERY_STATUS_CHARGING, CHARGING);
        batteryStateMap.append(BatteryManager.BATTERY_STATUS_DISCHARGING, DISCHARGING);
        batteryStateMap.append(BatteryManager.BATTERY_STATUS_FULL, FULL);
        batteryStateMap.append(BatteryManager.BATTERY_STATUS_NOT_CHARGING, NOT_CHARGING);
        batteryStateMap.append(BatteryManager.BATTERY_STATUS_UNKNOWN, UNKNOWN);

        batteryHealthMap = new SparseArray<>();
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_COLD, COLD);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_DEAD, DEAD);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_OVERHEAT, OVERHEATING);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE, OVER_VOLTAGE);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_GOOD, HEALTHY);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_UNKNOWN, UNKNOWN);
        batteryHealthMap.append(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE, FAILED);

        if (Build.VERSION.SDK_INT >= 21)
            batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
    }

    public static String getTemperature(Intent intent, boolean useFahrenheit) {
        int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) * 10;
        if (useFahrenheit)
            temperature = temperature / 5 * 9 + 3200;

        return divideAndRound(temperature) + DEGREE + (useFahrenheit ? F : C);
    }

    public static int getBatteryLevel(Intent intent) {
        return (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
    }

    public static String getAmp() {
        if (Build.VERSION.SDK_INT >= 21) {
            int batteryCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

            return (Math.abs(batteryCurrent) > 1000000) ?
                    divideAndRound(batteryCurrent / 10000) + A :
                    ((Math.abs(batteryCurrent) > 1000) ?
                            divideAndRound(batteryCurrent / 10) + mA :
                            batteryCurrent + uA);
        } else
            return EMPTY;
    }

    public static String getState(Intent intent) {
        String state = batteryStateMap.get(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1), UNKNOWN);
        if (state.equals(CHARGING))
            state = batteryStatusMap.get(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1), CHARGING);
        return state;
    }

    public static String getHealth(Intent intent) {
        return batteryHealthMap.get(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1));
    }

    public static String getTimeRemaining(Intent intent) {
        if (Build.VERSION.SDK_INT >= 19)
            try {
                byte[] data = IBatteryStats.Stub.asInterface(
                        ServiceManager.getService(BatteryStats.SERVICE_NAME)).getStatistics();
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);
                BatteryStatsImpl batteryStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                        .createFromParcel(parcel);
                parcel.recycle();

                if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    return computeTimeString(batteryStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000) / 1000000);
                } else if (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING) {
                    return computeTimeString(batteryStats.computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000) / 1000000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return EMPTY;
    }

    private static String computeTimeString(long secsRemaining) {
        if (secsRemaining < 1)
            return EMPTY;

        long min = (secsRemaining / 60) % 60,
                hour = (secsRemaining / (60 * 60)) % 24,
                day = (secsRemaining / (60 * 60 * 24));

        StringBuilder timeString = new StringBuilder();

        if (day > 0)
            timeString.append(day).append(DAYS);
        if (hour > 0)
            timeString.append(hour).append(HOURS);

        return timeString.append(min).append(MINS).toString();
    }

    private static String divideAndRound(int i) {
        return (i / 100) + DECIMAL + Math.abs(i % 100);
    }

    private static final String
            CHARGING = "Charging",
            USB_CHARGING = "USB Charging",
            WIRELESS_CHARGING = "Wireless Charging",
            DISCHARGING = "Discharging",
            FULL = "Full",
            NOT_CHARGING = "Not Charging",
            UNKNOWN = "Unknown",
            COLD = "Cold",
            DEAD = "Dead",
            OVERHEATING = "Overheating",
            OVER_VOLTAGE = "Over Voltage",
            HEALTHY = "Healthy",
            FAILED = "Failed",
            uA = ".0\u03BCA",
            mA = "mA",
            A = "A",
            DEGREE = "\u00B0",
            F = "F",
            C = "C",
            DAYS = "d ",
            HOURS = "h ",
            MINS = "m left",
            EMPTY = "",
            DECIMAL = ".";
}
