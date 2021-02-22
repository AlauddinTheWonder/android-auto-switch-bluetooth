package com.alan.autoswitchbluetooth.extras;

import android.util.Log;

import java.util.Random;
import java.util.TimeZone;

public class Utils {

    public static void log(String msg) {
        Log.d(Constants.TAG, msg);
    }

    public static int getIndexByDelim(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == Constants.DEFAULT_DELIMITER) {
                return i;
            }
        }
        return -1;
    }

    // Get system's current time in UTC
    public static long getCurrentTimeUTC() {
        long timestamp = System.currentTimeMillis() / 1000;
        long offset = TimeZone.getDefault().getRawOffset() / 1000;
        return timestamp + offset;
    }

    public static int getRandomNumberInRange(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
