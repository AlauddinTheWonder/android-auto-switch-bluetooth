package com.alan.autoswitchbluetooth.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.alan.autoswitchbluetooth.extras.Constants;
import com.alan.autoswitchbluetooth.interfaces.SerialListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;


public class SerialSocket implements Runnable {

    private SerialListener listener;
    private BluetoothDevice device;
    private BluetoothSocket socket;


    public SerialSocket(BluetoothDevice device) {
        this.device = device;
    }

    public String getName() {
        return device.getName() != null ? device.getName() : device.getAddress();
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void connect(SerialListener listener) {
        this.listener = listener;

        Executors.newSingleThreadExecutor().submit(this);
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public void disconnect() {
        if(socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        listener = null;
        device = null;
    }

    public void write(String data) {
        write(data.getBytes());
    }

    private void write(byte[] data) {
        if (isConnected()) {
            try {
                socket.getOutputStream().write(data);
            } catch (IOException ignore) {}
        }
    }

    @Override
    public void run() {
        try {
            socket = device.createRfcommSocketToServiceRecord(Constants.BT_DEV_UUID);
            socket.connect();

            if(listener != null) {
                listener.onSerialConnect(device);
            }

        } catch (Exception e) {
            if(listener != null) {
                listener.onSerialConnectError(e);
            }

            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
            return;
        }
        if (listener != null) {
            try {
                int len;
                byte[] readBuffer = new byte[1024];
                int readBufferPosition = 0;
                final byte delimiter = 10; //This is the ASCII code for a newline character

                //noinspection InfiniteLoopStatement
                while (true) {
                    len = socket.getInputStream().available();
                    if (len > 0) {
                        byte[] buffer = new byte[len];
                        socket.getInputStream().read(buffer);

                        for (int i = 0; i < len; i++) {
                            if (buffer[i] == delimiter) {
                                String data = new String(readBuffer, 0, readBufferPosition, StandardCharsets.US_ASCII).trim();

                                readBufferPosition = 0;
                                listener.onSerialRead(data);
                            }
                            else {
                                readBuffer[readBufferPosition++] = buffer[i];
                            }
                        }
                    }
                }
            } catch (Exception e) {
                listener.onSerialIoError(e);
                disconnect();
            }
        }
    }
}
