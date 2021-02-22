package com.alan.autoswitchbluetooth.interfaces;

import com.alan.autoswitchbluetooth.models.DeviceModel;

public interface DeviceListListener {
    void onClick(int position, DeviceModel deviceModel, boolean isSaved);
    void onDelete(int position, DeviceModel deviceModel);
    void onRename(int position, DeviceModel deviceModel);
}
