package com.alan.autoswitchbluetooth;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.alan.autoswitchbluetooth.adapters.DeviceListAdapter;
import com.alan.autoswitchbluetooth.dialogs.ConfirmDialog;
import com.alan.autoswitchbluetooth.dialogs.PromptDialog;
import com.alan.autoswitchbluetooth.dialogs.SimpleAlertDialog;
import com.alan.autoswitchbluetooth.extras.Constants;
import com.alan.autoswitchbluetooth.extras.Utils;
import com.alan.autoswitchbluetooth.interfaces.ConfirmDialogInterface;
import com.alan.autoswitchbluetooth.interfaces.DeviceListListener;
import com.alan.autoswitchbluetooth.interfaces.PromptDialogInterface;
import com.alan.autoswitchbluetooth.models.DeviceModel;


public class LaunchActivity extends AppCompatActivity {

    Context context;
    private BluetoothAdapter btAdapter;
    private DeviceListAdapter listAdapter;

    LinearLayout addNewView, listDevicesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        context = this;

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        addNewView = findViewById(R.id.view_add_new);
        addNewView.setVisibility(View.VISIBLE);

        listDevicesView = findViewById(R.id.view_devices);
        listDevicesView.setVisibility(View.GONE);

        findViewById(R.id.btn_add_device).setOnClickListener(v -> onAddNewDevice());

        listAdapter = new DeviceListAdapter(this)
                .setShowControls(true)
                .makePersistable();

        if (listAdapter.size() > 0) {
            addNewView.setVisibility(View.GONE);
            listDevicesView.setVisibility(View.VISIBLE);
        }

        RecyclerView deviceListView = findViewById(R.id.saved_devices);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        deviceListView.setLayoutManager(layoutManager);
        deviceListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        deviceListView.setAdapter(listAdapter);

        listAdapter.setListener(deviceListListener);

        enableBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                ConfirmDialog confirmDialog = new ConfirmDialog(this, getText(R.string.bluetooth_enable_title), getText(R.string.bluetooth_enable_message));
                confirmDialog.show(new ConfirmDialogInterface.OnClickListener() {
                    @Override
                    public void onClick(boolean isTrue) {
                        if (isTrue) {
                            enableBluetooth();
                        }
                    }
                });
            }
        }

        if (requestCode == Constants.REQUEST_ADD_DEVICE && data != null) {
            BluetoothDevice device = data.getParcelableExtra(Constants.EXTRA_DEVICE_ADD);
            if (device != null) {
                onDeviceAdded(device);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bt_home, menu);
        return true;
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem item = menu.findItem(R.id.action_add_device);
//
//        Drawable icon = item.getIcon();
//        icon.setTint(getResources().getColor(R.color.menuIconTintActive));
//        item.setIcon(icon);
//
//        return super.onPrepareOptionsMenu(menu);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_device) {
            onAddNewDevice();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void log(String text) {
        log(text, false);
    }

    private void log(final String text, boolean showToast) {
        Utils.log(text);

        if (showToast) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void exitApp(String msg) {
        if (!msg.isEmpty()) {
            log(msg, true);
        }
        finish();
    }

    private void enableBluetooth() {
        if (btAdapter == null) {
            SimpleAlertDialog alertDialog = new SimpleAlertDialog(this, "Alert!", " Bluetooth Adapter not found!");
            alertDialog.show(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exitApp("Bluetooth not found!");
                }
            });
        }
        else if (!btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_ENABLE_BT);
        }
    }

    private void onAddNewDevice() {
        Intent intent = new Intent(this, DeviceActivity.class);
        startActivityForResult(intent, Constants.REQUEST_ADD_DEVICE);
    }

    private void onDeviceAdded(BluetoothDevice device) {
        if (listAdapter.contains(device)) {
            log("Device already added", true);
        }
        else {
            addNewView.setVisibility(View.GONE);
            listDevicesView.setVisibility(View.VISIBLE);

            listAdapter.add(device);

            log(device.getName() + " added successfully", true);
        }
    }


    // Device List Listener
    private final DeviceListListener deviceListListener = new DeviceListListener() {
        @Override
        public void onClick(int position, DeviceModel device, boolean isSaved) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.EXTRA_DEVICE_ADDRESS, device.getAddress());
            context.startActivity(intent);
        }

        @Override
        public void onRename(int position, DeviceModel device) {
            PromptDialog dialog = new PromptDialog(context, "Rename this device");
            dialog.setInputText(device.getLabel());
            dialog.show(new PromptDialogInterface.OnClickListener() {
                @Override
                public void onClick(String text) {
                    if (!text.equals(device.getLabel())) {
                        device.setLabel(text);
                        listAdapter.update(position, device);
                    }
                }
            });
        }

        @Override
        public void onDelete(int position, DeviceModel device) {
            ConfirmDialog confirmDialog = new ConfirmDialog(context, "Are you sure to remove this device?");
            confirmDialog.show(new ConfirmDialogInterface.OnClickListener() {
                @Override
                public void onClick(boolean isTrue) {
                    if (isTrue) {
                        listAdapter.remove(position);
                        if (listAdapter.size() == 0) {
                            addNewView.setVisibility(View.VISIBLE);
                            listDevicesView.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    };

}
