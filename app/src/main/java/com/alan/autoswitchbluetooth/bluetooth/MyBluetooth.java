package com.alan.autoswitchbluetooth.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.alan.autoswitchbluetooth.interfaces.Serial;

public class MyBluetooth {

    public static Serial getSocket(Context context, BluetoothDevice device) {
        if (BluetoothUtils.isBLE(device)) {
            return new SerialGattService(context, device);
        }
        return new SerialSocket(device);
    }
}
