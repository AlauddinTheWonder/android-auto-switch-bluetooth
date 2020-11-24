package com.alan.autoswitchbluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

public interface DeviceListener {
    void onInfo(String text);
    void onReceivedFromDevice(String data);
    void onDeviceConnect(BluetoothDevice device);
    void onDeviceConnectError(String msg);
    void onDeviceDisconnect();
    void onExitRequest();
    void onProgressStart();
    void onProgressStop();
}
