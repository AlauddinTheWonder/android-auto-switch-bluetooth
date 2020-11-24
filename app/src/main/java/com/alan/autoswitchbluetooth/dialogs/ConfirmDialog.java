package com.alan.autoswitchbluetooth.dialogs;

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class ConfirmDialog {

    private AlertDialog alertDialog;

    private DialogInterface.OnClickListener clickListener;

    public ConfirmDialog(Context context, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirmation!");
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickListener.onClick(dialog, 1);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickListener.onClick(dialog, 0);
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
