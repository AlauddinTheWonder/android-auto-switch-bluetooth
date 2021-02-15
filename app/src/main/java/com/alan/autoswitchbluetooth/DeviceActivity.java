package com.alan.autoswitchbluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.alan.autoswitchbluetooth.adapters.DeviceListAdapter;
import com.alan.autoswitchbluetooth.bluetooth.SerialSocket;
import com.alan.autoswitchbluetooth.dialogs.ConfirmDialog;
import com.alan.autoswitchbluetooth.dialogs.ProgressDialog;
import com.alan.autoswitchbluetooth.dialogs.SimpleAlertDialog;
import com.alan.autoswitchbluetooth.extras.Command;
import com.alan.autoswitchbluetooth.extras.Constants;
import com.alan.autoswitchbluetooth.interfaces.ConfirmDialogInterface;
import com.alan.autoswitchbluetooth.interfaces.DeviceListListener;
import com.alan.autoswitchbluetooth.interfaces.SerialListener;
import com.alan.autoswitchbluetooth.models.DeviceModel;

import java.util.Set;

public class DeviceActivity extends AppCompatActivity implements SerialListener {

    private final static int LESCAN_DURATION = 10 * 1000;
    Context context;

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private SerialSocket btSocket;
    private SerialListener serialListener;

    private DeviceListAdapter listAdapter;
    private Menu menu;

    private ProgressDialog progressDialog;

    private final Handler scanStopHandler = new Handler();
    final Handler commandHandler = new Handler();
    int commandRetry = 0;
    boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        serialListener = this;
        context = this;

        listAdapter = new DeviceListAdapter(this);
        progressDialog = new ProgressDialog(this);

        RecyclerView deviceListView = findViewById(R.id.device_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        deviceListView.setLayoutManager(layoutManager);
        deviceListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        deviceListView.setAdapter(listAdapter);

        listAdapter.setListener(new DeviceListListener() {
            @Override
            public void onClick(int position, DeviceModel deviceModel) {
                BluetoothDevice device = btAdapter.getRemoteDevice(deviceModel.getAddress());
                logDevice("Selected", device);

                stopScan();
                progressDialog.show("Connecting...");
                btSocket = new SerialSocket(device);
                btSocket.connect(serialListener);
            }

            @Override
            public void onDelete(int position, DeviceModel deviceModel) {}

            @Override
            public void onRename(int position, DeviceModel deviceModel) {}
        });

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager != null) {
            btAdapter = btManager.getAdapter();
            bleScanner = btAdapter.getBluetoothLeScanner();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        enableBluetooth();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressDialog.dismiss();
        if (btSocket != null) {
            btSocket.disconnect();
        }
        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_device, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_scan) {
            attemptStartScanning();
            return true;
        }
        else if (id == R.id.action_scan_stop) {
            stopScan();
            return true;
        } else if (id == R.id.action_scan_settings) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                enableBluetooth();
            }
            else if (resultCode == RESULT_CANCELED) {
                ConfirmDialog confirmDialog = new ConfirmDialog(this, getText(R.string.bluetooth_enable_title), getText(R.string.bluetooth_enable_message));
                confirmDialog.show(new ConfirmDialogInterface.OnClickListener() {
                    @Override
                    public void onClick(boolean isTrue) {
                        if (isTrue) {
                            enableBluetooth();
                        }
                        else {
                            finish();
                        }
                    }
                });
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Constants.REQUEST_ENABLE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attemptStartScanning();
            } else {
                SimpleAlertDialog alertDialog = new SimpleAlertDialog(this, getText(R.string.location_denied_title), getText(R.string.location_denied_message));
                alertDialog.show(null);
            }
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) {
                        logDevice("Discovery", device);
                        addDeviceToList(device);
                    }
                }
            }
        }
    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            logDevice("LE", device);
            addDeviceToList(device);
        }
    };

    private void addDeviceToList(BluetoothDevice device) {
        if (!listAdapter.contains(device)) {
            listAdapter.add(device);
        }
    }

    private void log(String text) {
        log(text, false);
    }

    private void log(final String text, boolean showToast) {
        Log.i(Constants.TAG, text);

        if (showToast) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void logDevice(String tag, BluetoothDevice device) {
        log("From: " + tag + ", Device Name: " + device.getName() + ", address: " + device.getAddress());
    }

    private void enableBluetooth() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
        else if (!btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_ENABLE_BT);
        } else {
            listAdapter.clear();
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice device : devices) {
                logDevice("Paired", device);
                addDeviceToList(device);
            }
        }
    }

    private void attemptStartScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                SimpleAlertDialog alertDialog = new SimpleAlertDialog(this, getText(R.string.location_permission_title), getText(R.string.location_permission_message));
                alertDialog.show(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_ENABLE_LOCATION);
                    }
                });
                return;
            }

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            boolean locationEnabled = false;

            if (locationManager != null) {
                try {
                    locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch (Exception ignored) {}

                try {
                    locationEnabled |= locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                } catch (Exception ignored) {}
            }
            if(!locationEnabled) {
                log("location not enabled");

                SimpleAlertDialog alertDialog = new SimpleAlertDialog(this, "Location Required!", "Enable location to Scan devices");
                alertDialog.show(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                return;
            }
        }
        startScan();
    }

    private void startScan() {
        if(isScanning)
            return;

        isScanning = true;

        menu.findItem(R.id.action_scan).setVisible(false);
        menu.findItem(R.id.action_scan_stop).setVisible(true);

        log("Scanning started...");

        scanStopHandler.postDelayed(this::stopScan, LESCAN_DURATION);

        listAdapter.clear();
        showPairedDevices();

        btAdapter.startDiscovery();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bleScanner.startScan(leScanCallback);
            }
        });

    }

    private void stopScan() {
        if(!isScanning)
            return;

        scanStopHandler.removeCallbacks(this::stopScan);

        btAdapter.cancelDiscovery();
        bleScanner.stopScan(leScanCallback);

        isScanning = false;

        menu.findItem(R.id.action_scan).setVisible(true);
        menu.findItem(R.id.action_scan_stop).setVisible(false);

        log("Scanning stopped.");
    }

    private void runCommand(final int command, final long param) {
        String commandStr = command + ":" + param;

        btSocket.write(commandStr);
        log(">> " + command + ":" + param);

        commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    commandRetry = 0;
                    progressDialog.hide();
                    onInvalidDevice();
                    log("Command timeout. Please try again.", true);
                }
                else {
                    log("Retrying...");
                    commandRetry++;
                    runCommand(command, param);
                }
            }
        }, Constants.COMMAND_TIMEOUT);
    }

    private void onDeviceValidated(BluetoothDevice device) {
        log(btSocket.getName() + " validated successfully", true);

        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_DEVICE_ADD, device);
        setResult(Constants.REQUEST_ADD_DEVICE, intent);
        finish();
    }

    private void onInvalidDevice() {
        log("Invalid device", true);
        btSocket.disconnect();
    }

    @Override
    public void onSerialConnect(BluetoothDevice device) {
        log("Device connected: " + device.getName());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show("Validating device...");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        runCommand(Command.PING_BACK, Command.NO_COMMAND_VALUE);
                    }
                }, 3000);
            }
        });
    }

    @Override
    public void onSerialConnectError(Exception e) {
        log(e.getMessage());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Serial Connection Error", true);
                progressDialog.hide();
            }
        });
    }

    @Override
    public void onSerialRead(String data) {
        log("<< " + data);

        if (data.isEmpty()) {
            return;
        }

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (data.equals(String.valueOf(Command.PING_BACK))) {
                    onDeviceValidated(btSocket.getDevice());
                } else {
                    onInvalidDevice();
                }
                progressDialog.hide();
            }
        });
    }

    @Override
    public void onSerialIoError(Exception e) {
        log(e.getMessage());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log("Serial IO Error", true);
                progressDialog.hide();
            }
        });
    }
}
