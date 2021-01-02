package com.alan.autoswitchbluetooth.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BluetoothUtils {

    private BluetoothAdapter btAdapter;

    public BluetoothUtils(BluetoothAdapter adapter) {
        this.btAdapter = adapter;
    }

    public BluetoothDevice getDeviceByAddress(String address) {
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            return btAdapter.getRemoteDevice(address);
        }
        return null;
    }

    public static String getDeviceType(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "Classic";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "BLE";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "Dual";
            default:
                return "";
        }
    }
}
