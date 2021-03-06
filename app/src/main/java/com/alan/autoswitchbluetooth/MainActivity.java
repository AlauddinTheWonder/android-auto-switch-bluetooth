package com.alan.autoswitchbluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alan.autoswitchbluetooth.adapters.SwitchListAdapter;
import com.alan.autoswitchbluetooth.bluetooth.BluetoothUtils;
import com.alan.autoswitchbluetooth.bluetooth.MyBluetooth;
import com.alan.autoswitchbluetooth.bluetooth.SerialSocket;
import com.alan.autoswitchbluetooth.dialogs.ConfirmDialog;
import com.alan.autoswitchbluetooth.dialogs.ProgressDialog;
import com.alan.autoswitchbluetooth.dialogs.SwitchDialog;
import com.alan.autoswitchbluetooth.extras.Command;
import com.alan.autoswitchbluetooth.extras.Constants;
import com.alan.autoswitchbluetooth.extras.Utils;
import com.alan.autoswitchbluetooth.interfaces.ConfirmDialogInterface;
import com.alan.autoswitchbluetooth.interfaces.Serial;
import com.alan.autoswitchbluetooth.interfaces.SerialListener;
import com.alan.autoswitchbluetooth.interfaces.SwitchListListener;
import com.alan.autoswitchbluetooth.models.CommandList;
import com.alan.autoswitchbluetooth.models.CommandType;
import com.alan.autoswitchbluetooth.models.SwitchModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    Context context;

    private BluetoothAdapter btAdapter;
    private Serial btSocket;
    private BluetoothUtils btUtils;

    private ProgressDialog progressDialog;
    private SwitchDialog switchDialog;
    private SwitchListAdapter listAdapter;

    private SimpleDateFormat utcDF, localDF;

    private int NumOfSwitches = 0;
    private int MaxSettingsCount = 0;
    private int[] PinSettingsArray;
    private long DeviceTimestamp = 0;

    final Handler commandHandler = new Handler();
    Thread counterThread = null;
    int commandRetry = 0;
    CommandType currentCommand;

    // Views
    private TextView currentTimeView, deviceTimeView;
    private LinearLayout timeView;
    private RecyclerView switchListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Connecting...");

        context = this;

        progressDialog = new ProgressDialog(this);
        switchDialog = new SwitchDialog(this);

        localDF = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        utcDF = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        utcDF.setTimeZone(TimeZone.getTimeZone("GMT"));

        timeView = findViewById(R.id.time_view);
        currentTimeView = findViewById(R.id.current_time);
        deviceTimeView = findViewById(R.id.device_time);

        String deviceAddress = getIntent().getStringExtra(Constants.EXTRA_DEVICE_ADDRESS);

        if (deviceAddress == null || deviceAddress.isEmpty()) {
            exitScreen("Invalid device selection");
        } else {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            btUtils = new BluetoothUtils(btAdapter);
        }

        if (btAdapter.isEnabled()) {
            BluetoothDevice device = btUtils.getDeviceByAddress(deviceAddress);
            if (device != null) {
                progressDialog.show("Connecting...");

                btSocket = MyBluetooth.getSocket(this, device);
                btSocket.connect(serialListener);
            }
        } else {
            exitScreen("Bluetooth is disabled");
        }

        listAdapter = new SwitchListAdapter();

        switchListView = findViewById(R.id.switch_list_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        switchListView.setLayoutManager(layoutManager);
        switchListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        switchListView.setAdapter(listAdapter);

        listAdapter.setListener(switchListListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_bt_disconnect) {
            exitScreen("Disconnecting...");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressDialog.dismiss();
        if (btSocket != null) {
            btSocket.disconnect();
        }
    }

    public void onSyncDeviceTimeClick(View view) {
        long timestampToUTC = Utils.getCurrentTimeUTC() + 1;
        log("Syncing device time with: " + timestampToUTC);
        runCommand(Command.SET_TIME, timestampToUTC);
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

    private void exitScreen(final String msg) {
        progressDialog.dismiss();
        if (!msg.isEmpty()) {
            log(msg, true);
        }
        finish();
    }

    private void startCounter() {
        counterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        displayDateTime();
                        displayDeviceDateTime();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        counterThread.start();
    }

    private void displayDateTime() {
        String currentDateTimeString = localDF.format(new Date());
        String str = getString(R.string.current_time_str, currentDateTimeString);
        currentTimeView.setText(str);
    }

    private void displayDeviceDateTime() {
        String currentDateTimeString = "Not connected";
        if (DeviceTimestamp > 100000) {
            DeviceTimestamp++;

            Date dt = new Date(DeviceTimestamp * 1000);
            currentDateTimeString = utcDF.format(new Date(DeviceTimestamp * 1000));
        }

        String str = getString(R.string.dev_time_str, currentDateTimeString);
        deviceTimeView.setText(str);
    }

    private void onDataLoadedFromDevice() {
        timeView.setVisibility(View.VISIBLE);
        switchListView.setVisibility(View.VISIBLE);
        switchDialog.setPinSize(NumOfSwitches);

        startCounter();
    }

    private void hideDetailView() {
        timeView.setVisibility(View.GONE);
        switchListView.setVisibility(View.GONE);
    }

    private void getInfoFromDevice() {
        progressDialog.show("Fetching info...");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                log("Getting info from device");
                runCommand(Command.GET_ALL_DATA, Command.NO_COMMAND_VALUE);
            }
        }, 3000);
    }

    private void runCommandList() {
        if (CommandList.size() == 0) {
            return;
        }
        CommandType command = CommandList.get(0);
        CommandList.remove(0);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runCommand(command.getCommand(), command.getParam());
            }
        }, Constants.COMMAND_GAP_TIME);
    }

    private void runCommand(final int command, final long param) {
        progressDialog.show("Fetching info...");

        currentCommand = new CommandType(command, param);

        String commandStr = command + ":" + param;

        btSocket.write(commandStr);
        log(">> " + command + ":" + param);

        commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    currentCommand = null;
                    commandRetry = 0;
                    progressDialog.hide();

                    exitScreen("Command timeout. Please try again.");
                }
                else {
                    log("Retrying...");
                    commandRetry++;
                    long newParam = param;

                    if (command == Command.SET_TIME) {
                        newParam = Utils.getCurrentTimeUTC() + 1;
                    }
                    runCommand(command, newParam);
                }
            }
        }, Constants.COMMAND_TIMEOUT);
    }

    private void onDataReceived(String data) {
        if (data.isEmpty()) {
            log("Empty data received");
            return;
        }

        log("<< " + data);

        int command = currentCommand.getCommand();
        long param = currentCommand.getParam();

        currentCommand = null;

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;

        switch (command) {

            case Command.PING_BACK:
                if (data.equals(String.valueOf(Command.PING_BACK))) {
                    runCommand(Command.GET_ALL_DATA, Command.NO_COMMAND_VALUE);
                } else {
                    exitScreen("Invalid device.");
                }
                break;

            case Command.GET_ALL_DATA:
                parseInitialConfigData(data);
                break;

            case Command.GET_TIME:
            case Command.SET_TIME:
                DeviceTimestamp = Long.parseLong(data);
                break;

            case Command.GET_SWITCH_VALUE:
                PinSettingsArray[(int) param] = Integer.parseInt(data);
                updateSwitchListByIndex((int) param, Integer.parseInt(data));
                break;

            default:
                if (command > 0 && MaxSettingsCount > 0 && command <= (MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT)) {
                    PinSettingsArray[command] = Integer.parseInt(data);
                    updateSwitchListByIndex(command, Integer.parseInt(data));
                }
                break;
        }

        if (CommandList.size() > 0) {
            runCommandList();
        } else {
            progressDialog.hide();
        }
    }

    private void parseInitialConfigData(String data) {
        String[] chunks = data.split("\\|");
        if (chunks.length > 1)
        {
            for (String chunk : chunks) {
                if (!chunk.isEmpty()) {
                    String[] commands = chunk.split(":");

                    if (commands.length < 2) {
                        return;
                    }
                    int command = Integer.parseInt(commands[0]);

                    if (command > 0) {
                        switch (command) {
                            case Command.GET_TIME:
                                DeviceTimestamp = Long.parseLong(commands[1]);
                                break;

                            case Command.GET_SWITCH_NUM:
                                NumOfSwitches = Integer.parseInt(commands[1]);
                                break;

                            case Command.GET_MAX_SETTINGS:
                                MaxSettingsCount = Integer.parseInt(commands[1]);

                                if (MaxSettingsCount > 0) {
                                    PinSettingsArray = new int[MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT + 1];
                                    PinSettingsArray[0] = NumOfSwitches; // Pin setting starts from 1 index.
                                }
                                break;

                            default:
                                if (MaxSettingsCount > 0 && command <= (MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT)) {
                                    PinSettingsArray[command] = Integer.parseInt(commands[1]);
                                }
                                break;
                        }
                    }
                }
            }

            parseSettingsToSwitchList();
            onDataLoadedFromDevice();
        } else {
            exitScreen("Invalid data received. Please try again!");
        }
    }

    private void parseSettingsToSwitchList() {
        if (listAdapter.size() > 0) {
            listAdapter.clear();
        }
        for (int i = 0; i < MaxSettingsCount; i++) {
            int index = (i * Constants.SWITCH_SINGLE_ROW_CNT) + 1;
            int pin = PinSettingsArray[index];
            int on = PinSettingsArray[index + 1];
            int off = PinSettingsArray[index + 2];

            SwitchModel switchModel = new SwitchModel(pin, on, off, index);
            listAdapter.add(switchModel);
        }
    }

    private void updateSwitchListByIndex(int command, int value) {
        int target = command - 1;
        int position = (int) Math.floor(target / (float) Constants.SWITCH_SINGLE_ROW_CNT);
        int sequence = target - (position * Constants.SWITCH_SINGLE_ROW_CNT); // 0 = pin, 1 = on, 2 = off

        SwitchModel switchModel = listAdapter.get(position);
        if (sequence == 0) {
            switchModel.setPin(value);
        }
        else if (sequence == 1) {
            switchModel.setOn(value);
        }
        else if (sequence == 2) {
            switchModel.setOff(value);
        }
        listAdapter.update(position, switchModel);
    }


    // Serial Listener
    private final SerialListener serialListener = new SerialListener() {
        @Override
        public void onSerialConnect(BluetoothDevice device) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String str = getString(R.string.connected_str, device.getName());
                    log(str, true);

                    setTitle(device.getName());

                    getInfoFromDevice();
                }
            });
        }

        @Override
        public void onSerialConnectError(Exception e) {
            log(e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    exitScreen("Error in Connecting Device");
                }
            });
        }

        @Override
        public void onSerialRead(String data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onDataReceived(data);
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
    };

    // Switch List Listener
    private final SwitchListListener switchListListener = new SwitchListListener() {
        @Override
        public void onEdit(int position, SwitchModel switchModel) {
            switchDialog.show(switchModel, position, (dialog, model, position1) -> {
                log("Updating list " + (position + 1));

                int idx = model.getIndex();
                if (switchModel.getPin() != model.getPin()) {
                    CommandList.add(idx, model.getPin());
                }
                if (switchModel.getOn() != model.getOn()) {
                    CommandList.add(idx + 1, model.getOn());
                }
                if (switchModel.getOff() != model.getOff()) {
                    CommandList.add(idx + 2, model.getOff());
                }

                runCommandList();
            });
        }

        @Override
        public void onDelete(int position, SwitchModel switchModel) {
            ConfirmDialog dialog = new ConfirmDialog(context, "Are you sure to delete this?");
            dialog.show(new ConfirmDialogInterface.OnClickListener() {
                @Override
                public void onClick(boolean isTrue) {
                    if (isTrue) {
                        log("Deleting list " + (position + 1));

                        int idx = switchModel.getIndex();

                        CommandList.add(idx, 0);
                        CommandList.add(idx + 1, 0);
                        CommandList.add(idx + 2, 0);

                        runCommandList();
                    }
                }
            });
        }
    };
}
