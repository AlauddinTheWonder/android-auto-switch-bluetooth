package com.alan.autoswitchbluetooth.models;

import android.bluetooth.BluetoothDevice;

public class DeviceModel {
    private String address;
    private String name;
    private String label;

    public DeviceModel(String name, String address) {
        this.name = name;
        this.address = address;
        this.label = name;
    }

    public DeviceModel(String name, String address, String label) {
        this.name = name;
        this.address = address;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!name.isEmpty()) {
            this.name = name;
        }
    }

    public String getAddress() {
        return address;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label.isEmpty() ? getName() : label;
    }

    public static DeviceModel bluetoothDeviceToModel(BluetoothDevice device) {
        return new DeviceModel(device.getName(), device.getAddress());
    }
}
