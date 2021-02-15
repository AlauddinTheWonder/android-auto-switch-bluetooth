package com.alan.autoswitchbluetooth.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitchbluetooth.interfaces.ConfirmDialogInterface;

public class ConfirmDialog {

    private AlertDialog alertDialog;

    private ConfirmDialogInterface.OnClickListener clickListener;

    public ConfirmDialog(Context context, CharSequence message) {
        make(context, "Confirmation", message.toString());
    }

    public ConfirmDialog(Context context, String message) {
        make(context, "Confirmation", message);
    }

    public ConfirmDialog(Context context, CharSequence title, CharSequence message) {
        make(context, title.toString(), message.toString());
    }

    public ConfirmDialog(Context context, String title, String message) {
        make(context, title, message);
    }

    private void make(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickListener.onClick(true);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickListener.onClick(false);
                dialog.dismiss();
            }
        });
        alertDialog = builder.create();
    }

    public void show(ConfirmDialogInterface.OnClickListener listener) {
        clickListener = listener;
        alertDialog.show();
    }
}
