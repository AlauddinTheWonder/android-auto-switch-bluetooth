package com.alan.autoswitchbluetooth.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class SimpleAlertDialog {

    private AlertDialog alertDialog;

    private DialogInterface.OnClickListener clickListener;

    public SimpleAlertDialog(Context context, CharSequence message) {
        make(context, "", message.toString());
    }

    public SimpleAlertDialog(Context context, String message) {
        make(context, "", message);
    }

    public SimpleAlertDialog(Context context, CharSequence title, CharSequence message) {
        make(context, title.toString(), message.toString());
    }

    public SimpleAlertDialog(Context context, String title, String message) {
        make(context, title, message);
    }

    private void make(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!title.isEmpty()) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (clickListener != null) {
                    clickListener.onClick(dialog, which);
                }
                dialog.dismiss();
            }
        });
        alertDialog = builder.create();
    }

    public void show(DialogInterface.OnClickListener listener) {
        clickListener = listener;
        alertDialog.show();
    }
}
