package com.alan.autoswitchbluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

public interface Serial {
    String getName();
    BluetoothDevice getDevice();
    boolean isConnected();
    void connect(SerialListener listener);
    void disconnect();
    void write(String data);
    void write(byte[] data);
}
