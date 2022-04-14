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

package com.ominous.batterynotification.dialog;

import android.content.Context;
import android.text.SpannableString;
import android.widget.Button;
import android.widget.TextView;

import com.ominous.batterynotification.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class TextDialog {
    private final AlertDialog dialog;
    private final TextView textView;

    public TextDialog(Context context) {
        textView = new TextView(context);

        int padding = context.getResources().getDimensionPixelSize(R.dimen.margin_standard);
        textView.setPadding(padding * 3 / 2, padding, padding * 3 / 2, padding);

        dialog = new AlertDialog.Builder(context)
                .setView(textView)
                .create();

        dialog.setOnShowListener(d -> {
            for (Button button : new Button[]{
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE),
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL),
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            }) {
                button.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            }
        });
    }

    public TextDialog setButton(int which, String text, @Nullable Runnable onAcceptRunnable) {
        dialog.setButton(which, text, null, (d, w) -> {
            if (onAcceptRunnable != null) {
                onAcceptRunnable.run();
            }

            d.dismiss();
        });

        return this;
    }

    public TextDialog setContent(CharSequence content) {
        textView.setText(new SpannableString(content));

        return this;
    }

    public TextDialog setTitle(CharSequence title) {
        dialog.setTitle(title);

        return this;
    }

    public void show() {
        dialog.show();
    }
}
