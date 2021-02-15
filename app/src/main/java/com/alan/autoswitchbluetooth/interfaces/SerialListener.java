package com.alan.autoswitchbluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

public interface SerialListener {
    void onSerialConnect      (BluetoothDevice device);
    void onSerialConnectError (Exception e);
    void onSerialRead         (String data);
    void onSerialIoError      (Exception e);
}
