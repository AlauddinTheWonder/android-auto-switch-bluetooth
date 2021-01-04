package com.alan.autoswitchbluetooth.interfaces;

import com.alan.autoswitchbluetooth.models.SwitchModel;

public interface SwitchListListener {
    void onEdit(int position, SwitchModel switchModel);
    void onDelete(int position, SwitchModel switchModel);
}
