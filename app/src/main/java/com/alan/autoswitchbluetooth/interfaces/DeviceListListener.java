package com.alan.autoswitchbluetooth.interfaces;


import android.bluetooth.BluetoothDevice;

public interface DeviceListListener {
    void onClick(int position, BluetoothDevice device);
}
