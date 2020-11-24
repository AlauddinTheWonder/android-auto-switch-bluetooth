package com.alan.autoswitchbluetooth;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.alan.autoswitchbluetooth.adapters.MyBluetoothAdapter;
import com.alan.autoswitchbluetooth.dialogs.ConfirmDialog;
import com.alan.autoswitchbluetooth.dialogs.ProgressDialog;
import com.alan.autoswitchbluetooth.extras.Command;
import com.alan.autoswitchbluetooth.extras.Constants;
import com.alan.autoswitchbluetooth.interfaces.DeviceListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LaunchActivity extends AppCompatActivity implements DeviceListener {

    Context context;

    private MyBluetoothAdapter myDevice;
    private BluetoothDevice btDevice;

    private ProgressDialog progressDialog;
    private SharedPreferences sharedPref;

    final Handler commandHandler = new Handler();
    int commandRetry = 0;

    LinearLayout addNewView, verifyDeviceView, listDevicesView;

    private ArrayAdapter<String> arrayAdapter;
    private Set<String> prefsSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        context = this;

        addNewView = findViewById(R.id.view_add_new);
        addNewView.setVisibility(View.VISIBLE);

        verifyDeviceView = findViewById(R.id.view_verify);
        verifyDeviceView.setVisibility(View.GONE);

        listDevicesView = findViewById(R.id.view_devices);
        listDevicesView.setVisibility(View.GONE);

        findViewById(R.id.btn_add_device).setOnClickListener(v -> onAddNewDevice());

        progressDialog = new ProgressDialog(this);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        prefsSet = sharedPref.getStringSet(Constants.SHARED_PREF_NAME, new HashSet<String>());

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        if (prefsSet.size() > 0) {
            List<String> list = new ArrayList<String>();

            for (String sName: prefsSet) {
                list.add(sName.replace("|", "\n"));
            }
            arrayAdapter.addAll(list);

            addNewView.setVisibility(View.GONE);
            listDevicesView.setVisibility(View.VISIBLE);
        }

        ListView deviceListView = findViewById(R.id.saved_devices);
        deviceListView.setAdapter(arrayAdapter);

        myDevice = new MyBluetoothAdapter(this);
        myDevice.setListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                myDevice.connect();
            }
            else if (resultCode == RESULT_CANCELED) {
                ConfirmDialog confirmDialog = new ConfirmDialog(this, "Bluetooth is required for this App\nEnable it now?");
                confirmDialog.show(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1) {
                            myDevice.connect();
                        }
                        else {
                            onExitRequest();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressDialog.dismiss();
        myDevice.onExit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bt_home, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_add_device);

        Drawable icon = item.getIcon();
        icon.setTint(getResources().getColor(R.color.menuIconTintActive));
        item.setIcon(icon);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_device) {
            onAddNewDevice();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInfo(String text) {
        log(text);
    }

    @Override
    public void onReceivedFromDevice(String data) {
        onDataReceived(data);
    }

    @Override
    public void onDeviceConnect(BluetoothDevice device) {
        addNewView.setVisibility(View.GONE);
        verifyDeviceView.setVisibility(View.VISIBLE);

        log("Connected to: " + device.getName() + " (" + device.getAddress() + ")", true);

        processSelectedDevice(device);
    }

    @Override
    public void onDeviceConnectError(String msg) {
        log(msg);
        onInvalidDevice();
    }

    @Override
    public void onDeviceDisconnect() {

    }

    @Override
    public void onExitRequest() {
        exitScreen("");
    }

    @Override
    public void onProgressStart() {
        progressDialog.show();
    }

    @Override
    public void onProgressStop() {
        progressDialog.hide();
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

    private void exitScreen(String msg) {
        progressDialog.hide();
        if (!msg.isEmpty()) {
            log(msg, true);
        }
        finish();
    }

    private void onAddNewDevice() {
        myDevice.connect();
    }

    private void processSelectedDevice(BluetoothDevice device) {
        btDevice = device;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runCommand(Command.PING_BACK, Command.NO_COMMAND_VALUE);
            }
        }, 3000);
    }

    private void onDeviceAdded() {
        addNewView.setVisibility(View.GONE);
        verifyDeviceView.setVisibility(View.GONE);
        listDevicesView.setVisibility(View.VISIBLE);

        prefsSet.add(btDevice.getName() + "|" + btDevice.getAddress());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(Constants.SHARED_PREF_NAME, prefsSet);
        editor.apply();

        arrayAdapter.add(btDevice.getName() + "\n" + btDevice.getAddress());
        arrayAdapter.notifyDataSetChanged();

        log(btDevice.getName() + " added successfully", true);
    }

    private void onInvalidDevice() {
        if (prefsSet.size() == 0) {
            addNewView.setVisibility(View.VISIBLE);
        }
        verifyDeviceView.setVisibility(View.GONE);

        log("Invalid device", true);
    }

    private void runCommand(final int command, final long param) {
        progressDialog.show();

        String commandStr = command + ":" + param;

        myDevice.send(commandStr);
        log(">> " + command + ":" + param);

        commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    commandRetry = 0;
                    progressDialog.hide();

                    log("Command timeout. Please try again.", true);
                    onInvalidDevice();
                }
                else {
                    log("Retrying...");
                    commandRetry++;
                    runCommand(command, param);
                }
            }
        }, Constants.COMMAND_TIMEOUT);
    }

    private void onDataReceived(String data) {
        if (data.isEmpty()) {
            return;
        }

        if (data.equals(String.valueOf(Command.PING_BACK))) {
            onDeviceAdded();
        } else {
            onInvalidDevice();
        }

        progressDialog.hide();
    }

}