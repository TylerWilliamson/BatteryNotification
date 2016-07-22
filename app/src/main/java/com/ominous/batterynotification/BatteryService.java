package com.ominous.batterynotification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class BatteryService extends Service {
    public final static IntentFilter UPDATE_FILTER = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    final static String STARTING_MESSAGE = "Starting Battery Notification...";

    private final BroadcastReceiver bbr = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            NotificationUtil.updateBatteryNotification(context, intent);
            setAlarm();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(BatteryBroadcastReceiver.UPDATE_ACTION), 0);
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        this.startForeground(12345,
                NotificationUtil.makeBatteryNotification(this,
                        this.registerReceiver(bbr, UPDATE_FILTER)));

        setAlarm();
        return Service.START_STICKY;
    }

    private void setAlarm() {
        alarmManager.cancel(pendingIntent);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, 60000, 60000, pendingIntent);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        this.unregisterReceiver(bbr);
    }
}
