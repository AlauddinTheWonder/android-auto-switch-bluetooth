package com.alan.autoswitchbluetooth.interfaces;

import com.alan.autoswitchbluetooth.models.DeviceModel;

public interface SavedDeviceListListener {
    void onClick(int position, DeviceModel device);
    void onDelete(int position, DeviceModel device);
    void onRename(int position, DeviceModel device);
}
