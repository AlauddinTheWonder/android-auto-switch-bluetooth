package com.alan.autoswitchbluetooth.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import com.alan.autoswitchbluetooth.extras.Utils;
import com.alan.autoswitchbluetooth.interfaces.SerialListener;
import com.alan.autoswitchbluetooth.interfaces.Serial;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SerialGattService implements Serial {

    private static final UUID BLUETOOTH_LE_CC254X_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"); // HM-10 Custom (Primary) Service
    private static final UUID BLUETOOTH_LE_CC254X_CHAR_RW = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"); // HM-10 Custom Characteristic
    private static final UUID BLUETOOTH_LE_CCCD           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); // Client Characteristic Configuration Descriptor
    private static final int MAX_MTU = 512; // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
    private static final int DEFAULT_MTU = 23;
    private int payloadSize = DEFAULT_MTU - 3;

    private final Context context;

    private SerialListener listener;
    private BluetoothDevice device;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic readCharacteristic, writeCharacteristic;
    private boolean isLeConnected;

    private final ArrayList<byte[]> writeBuffer;

    byte[] readBuffer = new byte[1024];
    int readBufferPosition = 0;
    final byte delimiter = 10; //This is the ASCII code for a newline character

    public SerialGattService(Context context, BluetoothDevice device) {
        this.context = context;
        this.device = device;

        writeBuffer = new ArrayList<>();
    }

    public String getName() {
        return device.getName() != null ? device.getName() : device.getAddress();
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public boolean isConnected() {
        return isLeConnected;
    }

    public void connect(SerialListener listener) {
        this.listener = listener;

        if (BluetoothUtils.isBLE(device)) {
            if (Build.VERSION.SDK_INT < 23) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback);
            } else {
                bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
            }
        } else {
            onSerialConnectError(new Exception("Not a BLE device"));
        }
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            try {
                bluetoothGatt.close();
            } catch (Exception ignored) {}
        }
        synchronized (writeBuffer) {
            writeBuffer.clear();
        }
        bluetoothGatt = null;
        readCharacteristic = null;
        writeCharacteristic = null;
        listener = null;
        device = null;
        isLeConnected = false;
    }

    public void write(String data) {
        write(data.getBytes());
    }

    public void write(byte[] data) {
        if (!isConnected() || writeCharacteristic == null) {
            onSerialIoError(new Exception("No device connected"));
            return;
        }

        if (data.length > 0) {
            byte[] data0;

            if (data.length <= payloadSize) {
                data0 = data;
            } else {
                data0 = Arrays.copyOfRange(data, 0, payloadSize);
            }

            if (!writeBuffer.isEmpty()) {
                writeBuffer.add(data0);
                data0 = null;
            }

            if (data.length > payloadSize) {
                for(int i = 1; i < (data.length + payloadSize - 1) / payloadSize; i++) {
                    int from = i * payloadSize;
                    int to = Math.min(from + payloadSize, data.length);
                    writeBuffer.add(Arrays.copyOfRange(data, from, to));
                }
            }

            writeCharacteristic(data0);
        }
        // continues asynchronously in onCharacteristicWrite()
    }

    private void writeNext() {
        final byte[] data;

        synchronized (writeBuffer) {
            if (!writeBuffer.isEmpty()) {
                data = writeBuffer.remove(0);
            } else {
                data = null;
            }
        }

        writeCharacteristic(data);
    }

    private void writeCharacteristic(byte[] data) {
        if (data != null) {
            writeCharacteristic.setValue(data);
            if (!bluetoothGatt.writeCharacteristic(writeCharacteristic)) {
                onSerialIoError(new Exception("Write failed!"));
            } else {
                Utils.log("Write started");
            }
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (isConnected()) {
                    onSerialIoError(new Exception("GATT disconnected: " + status));
                } else {
                    onSerialConnectError(new Exception("GATT disconnected: " + status));
                }
                disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Utils.log("Services discovered" + status);

            for (BluetoothGattService gattService : gatt.getServices()) {
                Utils.log("Service: " + gattService.getType() + ", " + gattService.getUuid());

                if (gattService.getUuid().equals(BLUETOOTH_LE_CC254X_SERVICE)) {
                    Utils.log("service found");

                    readCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);
                    writeCharacteristic = gattService.getCharacteristic(BLUETOOTH_LE_CC254X_CHAR_RW);

                    break;
                }
            }

            if (readCharacteristic == null || writeCharacteristic == null) {
                onSerialConnectError(new Exception("No Serial profile found!"));
                return;
            }
            this.checkMtuAndConnect(gatt);
        }

        private void checkMtuAndConnect(BluetoothGatt gatt) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Utils.log("Request max MTU");
                if (!gatt.requestMtu(MAX_MTU)) {
                    onSerialConnectError(new Exception("Request MTU failed!"));
                }
                // continues asynchronously in onMtuChanged
            } else {
                this.connectCharacteristic(gatt);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Utils.log("MTU changed. Size: " + mtu + ", status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                payloadSize = mtu - 3;
                Utils.log("New payload size: " + payloadSize);
            }
            this.connectCharacteristic(gatt);
        }

        private void connectCharacteristic(BluetoothGatt gatt) {
            int writeProperty = writeCharacteristic.getProperties();
            if ((writeProperty & (BluetoothGattCharacteristic.PROPERTY_WRITE + BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                onSerialConnectError(new Exception("Device not writable"));
                return;
            }

            if (!gatt.setCharacteristicNotification(readCharacteristic, true)) {
                onSerialConnectError(new Exception("No notification for read characteristic"));
                return;
            }

            BluetoothGattDescriptor readDescriptor = readCharacteristic.getDescriptor(BLUETOOTH_LE_CCCD);
            if (readDescriptor == null) {
                onSerialConnectError(new Exception("No CCCD descriptor for read characteristic"));
            }

            int readProperties = readCharacteristic.getProperties();
            if ((readProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                Utils.log("Enable read indication");
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            else if ((readProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                Utils.log("Enable read notification");
                readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            }
            else {
                onSerialConnectError(new Exception("No indication/notification for read characteristic (" + readProperties + ")"));
                return;
            }

            Utils.log("Writing read characteristic descriptor");

            if (!gatt.writeDescriptor(readDescriptor)) {
                onSerialConnectError(new Exception("Read characteristic CCCD descriptor not writable"));
            }

            // continues asynchronously in onDescriptorWrite()
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic() == readCharacteristic) {
                Utils.log("Writing read characteristic descriptor finished, status: " + status);

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onSerialConnectError(new Exception("Write descriptor failed"));
                } else {
                    isLeConnected = true;
                    onSerialConnect(device);
                    Utils.log("Connected!");
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();
            Utils.log("on char Read: " + Arrays.toString(data));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();
            Utils.log("on char write: " + Arrays.toString(data));

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Utils.log("Write failed!");
                onSerialIoError(new Exception("Write failed!"));
                return;
            }
            if (characteristic == writeCharacteristic) {
                Utils.log("Write Finished");
                writeNext();
            }
        }

        // Read from serial device
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Utils.log("on char changed, length: " + characteristic.getValue().length);

            if (characteristic == readCharacteristic) {
                byte[] buffer = readCharacteristic.getValue();
                int len = buffer.length;

                for (byte b : buffer) {
                    if (b == delimiter) {
                        String data = new String(readBuffer, 0, readBufferPosition, StandardCharsets.US_ASCII).trim();

                        readBufferPosition = 0;
                        readBuffer = new byte[1024];
                        onSerialRead(data);
                    }else {
                        readBuffer[readBufferPosition++] = b;
                    }
                }
            }
        }
    };


    /**
     * SerialListener
     */
    private void onSerialConnect(BluetoothDevice device) {
        if (listener != null)
            listener.onSerialConnect(device);
    }

    private void onSerialConnectError(Exception e) {
        if (listener != null)
            listener.onSerialConnectError(e);
    }

    private void onSerialRead(String data) {
        if (listener != null) {
            listener.onSerialRead(data);
        }
    }

    private void onSerialIoError(Exception e) {
        if (listener != null)
            listener.onSerialIoError(e);
    }
}
