package com.ominous.batterynotification;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;

public class SettingsActivity extends Activity {
    public static final String PREFERENCES_FILE = "Settings",
            PREFERENCE_NOTIFICATION = "NotificationEnabled",
            PREFERENCE_FAHRENHEIT = "FahrenheitEnabled",
            PREFERENCE_TIME_REMAINING = "TimeRemainingEnabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private SharedPreferences preferences;
        private SwitchPreference notificationPreference,
                fahrenheitPreference,
                timeRemainingPreference;

        @SuppressWarnings("deprecation")
        @TargetApi(23)
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.addPreferencesFromResource(R.xml.settings);

            Bitmap launcherIcon = null;
            try {
                BitmapDrawable bd = ((BitmapDrawable) this.getResources().getDrawable(R.mipmap.ic_launcher, null));
                launcherIcon = (bd != null) ? bd.getBitmap() : null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.getActivity().setTaskDescription(
                    new ActivityManager.TaskDescription(
                            this.getString(R.string.app_name),
                            launcherIcon,
                            (Build.VERSION.SDK_INT > 21) ?
                                    this.getResources().getColor(R.color.colorPrimary, null) :
                                    this.getResources().getColor(R.color.colorPrimary)));


            preferences = this.getActivity().getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
            if (preferences.getBoolean(PREFERENCE_NOTIFICATION, false))
                this.getActivity().startService(new Intent(this.getActivity(), BatteryService.class));

            notificationPreference = (SwitchPreference) findPreference(getResources().getString(R.string.key_notification_enable));
            notificationPreference.setOnPreferenceChangeListener(this);
            notificationPreference.setDefaultValue(preferences.getBoolean(PREFERENCE_NOTIFICATION, false));

            fahrenheitPreference = (SwitchPreference) findPreference(getResources().getString(R.string.key_use_fahrenheit));
            fahrenheitPreference.setOnPreferenceChangeListener(this);
            fahrenheitPreference.setDefaultValue(preferences.getBoolean(PREFERENCE_FAHRENHEIT, false));

            timeRemainingPreference = (SwitchPreference) findPreference(getResources().getString(R.string.key_show_time_remaining));
            timeRemainingPreference.setOnPreferenceChangeListener(this);
            timeRemainingPreference.setDefaultValue(preferences.getBoolean(PREFERENCE_TIME_REMAINING, false));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            switch (preference.getTitleRes()) {
                case R.string.desc_fahrenheit:
                    preferences.edit().putBoolean(PREFERENCE_FAHRENHEIT, enabled).apply();
                    break;
                case R.string.desc_notification:
                    if (enabled)
                        this.getActivity().startService(new Intent(this.getActivity(), BatteryService.class));
                    else {
                        this.getActivity().stopService(new Intent(this.getActivity(), BatteryService.class));
                        ((NotificationManager) this.getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
                    }
                    preferences.edit().putBoolean(PREFERENCE_NOTIFICATION, enabled).apply();
                    break;
                case R.string.desc_time_remaining:
                    if (enabled && this.getActivity().getPackageManager()
                            .checkPermission(BATTERY_STATS, this.getActivity().getPackageName()) != PackageManager.PERMISSION_GRANTED)
                        try {
                            executeSuCommand(PM_GRANT + this.getActivity().getPackageName() + SPACE + BATTERY_STATS);

                            if (this.getActivity().getPackageManager()
                                    .checkPermission(BATTERY_STATS, this.getActivity().getPackageName()) != PackageManager.PERMISSION_GRANTED)
                                throw new Exception();

                            preferences.edit().putBoolean(PREFERENCE_TIME_REMAINING, true).apply();
                        } catch (Exception e) {
                            Toast.makeText(this.getActivity(), PERMISSION_FAILURE_MESSAGE, Toast.LENGTH_SHORT).show();
                            Log.e(this.getActivity().getResources().getString(R.string.app_name), PERMISSION_FAILURE_MESSAGE);

                            preferences.edit().putBoolean(PREFERENCE_TIME_REMAINING, false).apply();

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    timeRemainingPreference.setChecked(false);
                                }
                            }, 1000);
                        }
                    else
                        preferences.edit().putBoolean(PREFERENCE_TIME_REMAINING, enabled).apply();
            }
            NotificationUtil.updateBatteryNotification(this.getActivity(), NotificationUtil.savedIntent);
            return true;
        }
    }

    private static void executeSuCommand(String command) throws Exception {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(SU);
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + END_LINE);
            os.writeBytes(EXIT + END_LINE);
            os.flush();
            os.close();
            process.waitFor();
            if (process.exitValue() == 255)
                throw new Exception();
        } finally {
            if (process != null)
                process.destroy();
        }
    }

    private static final String
            BATTERY_STATS = "android.permission.BATTERY_STATS",
            SPACE = " ",
            EXIT = "exit",
            END_LINE = "\n",
            SU = "su",
            PM_GRANT = "pm grant ",
            PERMISSION_FAILURE_MESSAGE = "Failed to grant permission";
}