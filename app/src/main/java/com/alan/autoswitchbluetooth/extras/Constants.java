package com.alan.autoswitchbluetooth.extras;

import com.alan.autoswitchbluetooth.BuildConfig;

import java.util.UUID;

public class Constants {

    public final static String TAG = "ALAN_AUTO_SWITCH";

    public static final String EXTRA_DEVICE_ADDRESS = "com.alan.extra.device.address";
    public static final String EXTRA_DEVICE_ADD = "com.alan.extra.device";

    public static final byte DEFAULT_DELIMITER = '\n';

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_LOCATION = 2;
    public static final int REQUEST_DISCOVERABLE_BT = 3;
    public static final int REQUEST_ADD_DEVICE = 99;
    public static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    public static final UUID BT_DEV_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final int COMMAND_TIMEOUT = 5000;
    public static final int COMMAND_GAP_TIME = 1000;
    public static final int COMMAND_MAX_RETRY = 2;
    public static final int SWITCH_SINGLE_ROW_CNT = 3;
}
