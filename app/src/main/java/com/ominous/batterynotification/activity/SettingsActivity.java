/*
 * Copyright 2016 - 2025 Tyler Williamson
 *
 * This file is part of BatteryNotification.
 *
 * BatteryNotification is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BatteryNotification is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BatteryNotification.  If not, see <https://www.gnu.org/licenses/>.
 */        //todo only needed for api 21-23

package com.ominous.batterynotification.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;

import com.ominous.batterynotification.R;
import com.ominous.batterynotification.dialog.TextDialog;
import com.ominous.batterynotification.service.BatteryService;
import com.ominous.batterynotification.util.NotificationUtils;

import java.io.DataOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

public class SettingsActivity extends AppCompatActivity {
    private final static String TAG = "SettingsActivity";
    private final static String PERMISSION_BATTERY_STATS = "android.permission.BATTERY_STATS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        int colorPrimaryDark = ContextCompat.getColor(this, R.color.colorPrimaryDark);
        SystemBarStyle barStyle = SystemBarStyle.dark(colorPrimaryDark);

        EdgeToEdge.enable(this, barStyle, barStyle);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT >= 21) {
            if (Build.VERSION.SDK_INT < 23) {
                getWindow().setStatusBarColor(colorPrimaryDark);
                getWindow().setNavigationBarColor(colorPrimaryDark);
            }

            setTaskDescription(
                    Build.VERSION.SDK_INT >= 28 ?
                            new ActivityManager.TaskDescription(
                                    getString(R.string.app_name),
                                    R.mipmap.ic_launcher_round,
                                    ContextCompat.getColor(this, R.color.colorPrimary)) :
                            new ActivityManager.TaskDescription(
                                    getString(R.string.app_name),
                                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round),
                                    ContextCompat.getColor(this, R.color.colorPrimary))
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.toolbar),
                (v, windowInsetsCompat) -> {
                    Insets insets = windowInsetsCompat.getInsets(
                            WindowInsetsCompat.Type.statusBars() |
                                    WindowInsetsCompat.Type.navigationBars());

                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

                    mlp.setMargins(
                            0,
                            insets.top,
                            0,
                            insets.bottom);
                    v.setLayoutParams(mlp);

                    return windowInsetsCompat;
                }
        );
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private SwitchPreference timeRemainingPreference;
        private SwitchPreference notificationPreference;
        private SwitchPreference updateImmediatelyPreference;
        private SwitchPreference fahrenheitPreference;

        private TextDialog timeRemainingFailureDialog;
        private TextDialog adbInstructionsDialog;
        private TextDialog foregroundServiceDialog;

        private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), r -> {
                    if (r) {
                        startNotification(getContext());

                        updateImmediatelyPreference.setEnabled(true);
                        fahrenheitPreference.setEnabled(true);
                        timeRemainingPreference.setEnabled(true);
                    } else {
                        notificationPreference.setChecked(false);
                    }
                });

        private SwitchPreference setUpSwitchPreference(String key) {
            SwitchPreference preference = findPreference(key);

            if (preference != null) {
                preference.setOnPreferenceChangeListener(this);
            }

            return preference;
        }

        private Preference setUpPreference(String key) {
            Preference preference = findPreference(key);

            if (preference != null) {
                preference.setOnPreferenceClickListener(this);
            }

            return preference;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(getString(R.string.preference_filename));
            addPreferencesFromResource(R.xml.settings);

            notificationPreference = setUpSwitchPreference(getString(R.string.preference_notification));
            fahrenheitPreference = setUpSwitchPreference(getString(R.string.preference_fahrenheit));
            updateImmediatelyPreference = setUpSwitchPreference(getString(R.string.preference_immediate));
            timeRemainingPreference = setUpSwitchPreference(getString(R.string.preference_time_remaining));
            Preference openNotificationSettings = setUpPreference(getString(R.string.preference_notification_settings));

            if (Build.VERSION.SDK_INT < 21) {
                openNotificationSettings.setEnabled(false);
                timeRemainingPreference.setEnabled(false);
                timeRemainingPreference.setChecked(false);
            }

            if (!notificationPreference.isChecked()) {
                updateImmediatelyPreference.setEnabled(false);
                fahrenheitPreference.setEnabled(false);
                timeRemainingPreference.setEnabled(false);
            }

            Context context = getContext();

            if (context != null) {
                if (NotificationUtils.canShowNotifications(context)) {
                    if (notificationPreference.isChecked()) {
                        NotificationUtils.startBatteryNotification(context);
                    }
                } else {
                    notificationPreference.setChecked(false);
                }

                if (updateImmediatelyPreference.isChecked()) {
                    context.startService(new Intent(context, BatteryService.class));
                }
            }
        }

        private void startNotification(Context context) {
            NotificationUtils.startBatteryNotification(context);

            if (context.getSharedPreferences(getString(R.string.preference_filename), Context.MODE_PRIVATE)
                    .getBoolean(getString(R.string.preference_immediate), false)) {
                context.startService(new Intent(context, BatteryService.class));
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (Boolean) newValue;
            String preferenceKey = preference.getKey();
            Context context = getContext();

            if (context != null) {
                if (preferenceKey.equals(getString(R.string.preference_notification))) {
                    if (enabled) {
                        if (NotificationUtils.canShowNotifications(getContext())) {
                            startNotification(context);

                            updateImmediatelyPreference.setEnabled(true);
                            fahrenheitPreference.setEnabled(true);
                            timeRemainingPreference.setEnabled(true);
                        } else {
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    } else {
                        NotificationUtils.cancelBatteryNotification(context);
                        updateImmediatelyPreference.setEnabled(false);
                        fahrenheitPreference.setEnabled(false);
                        timeRemainingPreference.setEnabled(false);
                    }
                } else if (preferenceKey.equals(getString(R.string.preference_immediate))) {
                    Intent batteryServiceIntent = new Intent(context, BatteryService.class);

                    if (enabled) {
                        context.startService(batteryServiceIntent);

                        if (Build.VERSION.SDK_INT >= 26) {
                            if (foregroundServiceDialog == null) {
                                foregroundServiceDialog = new TextDialog(context)
                                        .setContent(getString(R.string.dialog_foreground_content))
                                        .setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.dialog_button_notification_settings), this::openNotificationSettings)
                                        .setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_button_close), null);
                            }

                            foregroundServiceDialog.show();
                        }
                    } else {
                        context.stopService(batteryServiceIntent);
                    }
                } else if (preferenceKey.equals(getString(R.string.preference_time_remaining))) {
                    if (enabled) {
                        obtainPermission();
                    }
                }
            }

            if (enabled || !preferenceKey.equals(getString(R.string.preference_notification))) {
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        NotificationUtils.updateBatteryNotification(context), 1000);
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            if (Build.VERSION.SDK_INT > 21
                    && preference.getKey().equals(getString(R.string.preference_notification_settings))) {
                openNotificationSettings();
            }

            return true;
        }

        private void openNotificationSettings() {
            Context context = getContext();

            if (context != null && Build.VERSION.SDK_INT > 21) {
                Intent intent = new Intent();

                if (Build.VERSION.SDK_INT > 26) {
                    intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                            .putExtra(Settings.EXTRA_CHANNEL_ID, getString(R.string.app_name));
                } else {
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra("app_package", context.getPackageName())
                            .putExtra("app_uid", context.getApplicationInfo().uid);
                }

                startActivity(intent);
            }
        }

        private void obtainPermission() {
            final FragmentActivity activity = getActivity();

            if (activity != null) {
                if (hasPermission(activity)) {
                    timeRemainingPreference.setChecked(true);
                } else {
                    try (ExecutorService pool = Executors.newCachedThreadPool()) {
                        pool.submit(() -> {
                            final boolean successful;
                            final int messageRes = switch (executeSuCommand(getString(R.string.format_command, activity.getPackageName(), PERMISSION_BATTERY_STATS))) {
                                case 0 -> {
                                    successful = hasPermission(activity);
                                    yield successful ? R.string.message_permission_granted : R.string.message_permission_failure;
                                }
                                case 1 -> {
                                    successful = false;
                                    yield R.string.message_root_failure;
                                }
                                case 255 -> {
                                    successful = false;
                                    yield R.string.message_permission_failure;
                                }
                                default -> {
                                    successful = false;
                                    yield R.string.message_unknown_error;
                                }
                            };

                            activity.runOnUiThread(() -> {
                                if (successful) {
                                    Log.v(TAG, getString(messageRes));

                                    timeRemainingPreference.setChecked(true);
                                } else {
                                    Log.e(TAG, getString(messageRes));

                                    if (timeRemainingFailureDialog == null) {
                                        timeRemainingFailureDialog = new TextDialog(activity)
                                                .setTitle(getString(R.string.dialog_time_remaining_title))
                                                .setContent(getString(R.string.dialog_time_remaining_content))
                                                .setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_button_tryagain), this::obtainPermission)
                                                .setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.dialog_adb_title), () -> adbInstructionsDialog.show())
                                                .setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_button_close), null);
                                    }

                                    if (adbInstructionsDialog == null) {
                                        adbInstructionsDialog = new TextDialog(activity)
                                                .setTitle(getString(R.string.dialog_adb_title))
                                                .setContent(getString(R.string.dialog_adb_content) +
                                                        "\n\n" +
                                                        getString(R.string.format_command, activity.getPackageName(), PERMISSION_BATTERY_STATS))
                                                .setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_button_tryagain), this::obtainPermission)
                                                .setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.dialog_button_close), null);
                                    }

                                    timeRemainingPreference.setChecked(false);
                                    timeRemainingFailureDialog.show();
                                }
                            });
                        });
                    }
                }
            }
        }

        private boolean hasPermission(Context context) {
            return context.getPackageManager()
                    .checkPermission(PERMISSION_BATTERY_STATS, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        }

        private int executeSuCommand(String command) {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(command + "\n");
                os.writeBytes("exit\n");
                os.flush();
                os.close();
                process.waitFor();
                return process.exitValue();
            } catch (Exception e) {
                return -1;
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }
    }
}