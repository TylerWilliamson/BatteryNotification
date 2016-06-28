package com.ominous.batterynotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BatteryBroadcastReceiver extends BroadcastReceiver {
    public final static String UPDATE_ACTION = "com.ominous.batterynotification.UPDATE_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(UPDATE_ACTION))
            context.registerReceiver(null, BatteryService.UPDATE_FILTER);
        else if (context.getSharedPreferences(SettingsActivity.PREFERENCES_FILE, Context.MODE_PRIVATE).getBoolean(SettingsActivity.PREFERENCE_NOTIFICATION, false))
            context.startService(new Intent(context, BatteryService.class));
    }
}
