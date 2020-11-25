package com.alan.autoswitchbluetooth.models;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.alan.autoswitchbluetooth.extras.Constants;

import java.util.ArrayList;
import java.util.List;


public class DevicePrefs {
    private final String BT_NAME_SEPARATOR = "~";
    private final String DEVICE_LIST_SEPARATOR = ",";

    private SharedPreferences sharedPref;
    private List<DeviceModel> list;

    public DevicePrefs(Context context) {
        sharedPref = context.getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE);

        String listString = sharedPref.getString(Constants.SHARED_PREF_KEY, "");
        list = parseToList(listString);
    }

    public void add(BluetoothDevice device) {
        list.add(new DeviceModel(device.getName(), device.getAddress()));
        syncPrefs();
    }

    public DeviceModel get(int position) {
        return list.get(position);
    }

    public List<DeviceModel> getList() {
        return list;
    }

    public int size() {
        return list.size();
    }

    private void syncPrefs() {
        String prefsString = parseToString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.SHARED_PREF_KEY, prefsString);
        editor.apply();
    }

    private String parseToString() {
        if (list.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (DeviceModel deviceModel: list) {
                sb
                        .append(deviceModel.getName())
                        .append(BT_NAME_SEPARATOR)
                        .append(deviceModel.getAddress())
                        .append(DEVICE_LIST_SEPARATOR);
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        return "";
    }

    private List<DeviceModel> parseToList(String prefsString) {
        List<DeviceModel> newList = new ArrayList<DeviceModel>();
        if (!prefsString.isEmpty()) {
            String[] devices = prefsString.split(DEVICE_LIST_SEPARATOR);
            for (String dev : devices) {
                String[] chunks = dev.split(BT_NAME_SEPARATOR);
                if (chunks.length >= 2) {
                    newList.add(new DeviceModel(chunks[0], chunks[1]));
                }
            }
        }
        return newList;
    }

}
