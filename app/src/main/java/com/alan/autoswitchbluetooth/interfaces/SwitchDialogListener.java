package com.alan.autoswitchbluetooth.interfaces;

import android.content.DialogInterface;

import com.alan.autoswitchbluetooth.models.SwitchModel;

public interface SwitchDialogListener {
    interface OnSaveListener {
        void onClick(DialogInterface dialog, SwitchModel model, int position);
    }
}
