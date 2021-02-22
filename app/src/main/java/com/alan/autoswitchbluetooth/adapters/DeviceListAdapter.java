package com.alan.autoswitchbluetooth.adapters;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alan.autoswitchbluetooth.R;
import com.alan.autoswitchbluetooth.interfaces.DeviceListListener;
import com.alan.autoswitchbluetooth.models.DeviceModel;

import java.util.ArrayList;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    private final String BT_NAME_SEPARATOR = "~";
    private final String DEVICE_LIST_SEPARATOR = ",";
    private final String SHARED_PREF_KEY = "DevicesList";

    private SharedPreferences sharedPref;
    private BluetoothAdapter btAdapter;

    private DeviceListListener listListener;

    final private Context context;
    private ArrayList<DeviceModel> list = new ArrayList<>();
    private boolean showControls = false;
    private boolean isPersist = false;

    public DeviceListAdapter(Context context) {
        this.context = context;

        BluetoothManager mgr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mgr != null) {
            btAdapter = mgr.getAdapter();
        }
    }

    public DeviceListAdapter setList(ArrayList<DeviceModel> defaultList) {
        this.list = defaultList;
        return this;
    }

    public DeviceListAdapter setShowControls(boolean show) {
        showControls = show;
        return this;
    }

    public DeviceListAdapter makePersistable(String persistFile) {
        sharedPref = context.getSharedPreferences(persistFile, Context.MODE_PRIVATE);
        String listString = sharedPref.getString(SHARED_PREF_KEY, "");
        this.list = parseToList(listString);

        isPersist = true;
        return this;
    }

    public void setListener(DeviceListListener listener) {
        this.listListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout ViewListItem, ViewListDescription2View, ViewListControls;
        TextView ViewListLabel, ViewListDescription, ViewListDescription2, ViewBleLabel;
        View ViewListStatus;
        ImageButton ViewListBtnEdit, ViewListBtnDelete;

        ViewHolder(View itemView) {
            super(itemView);

            ViewListItem = itemView.findViewById(R.id.list_item);
            ViewListDescription2View = itemView.findViewById(R.id.list_description2_view);
            ViewListControls = itemView.findViewById(R.id.list_controls);

            ViewListLabel = itemView.findViewById(R.id.list_label);
            ViewListDescription = itemView.findViewById(R.id.list_description);
            ViewListDescription2 = itemView.findViewById(R.id.list_description2);
            ViewBleLabel = itemView.findViewById(R.id.ble_label);

            ViewListStatus = itemView.findViewById(R.id.list_status);
            ViewListBtnEdit = itemView.findViewById(R.id.list_btn_edit);
            ViewListBtnDelete = itemView.findViewById(R.id.list_btn_delete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.device_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel deviceModel = list.get(position);
        BluetoothDevice item = getBluetoothDevice(deviceModel.getAddress());

        holder.ViewListLabel.setText(deviceModel.getLabel());
        holder.ViewListDescription.setText(deviceModel.getAddress());

        if (showControls) {
            holder.ViewListControls.setVisibility(View.VISIBLE);
        } else {
            holder.ViewListControls.setVisibility(View.GONE);
        }

        if (!deviceModel.getName().equals(deviceModel.getLabel())) {
            holder.ViewListDescription2View.setVisibility(View.VISIBLE);
            holder.ViewListDescription2.setText(deviceModel.getName());
        }

        if (item != null && item.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            holder.ViewBleLabel.setVisibility(View.VISIBLE);
        }

        int colorPrimary = context.getResources().getColor(R.color.primary);
        int colorDisable = context.getResources().getColor(R.color.disabled);
        holder.ViewListStatus.setBackgroundColor((item != null && item.getBondState() == BluetoothDevice.BOND_BONDED) ? colorPrimary : colorDisable);

        holder.ViewListItem.setOnClickListener(v -> {
            if (listListener != null) {
                listListener.onClick(position, deviceModel);
            }
        });

        holder.ViewListBtnEdit.setOnClickListener(v -> {
            if (listListener != null) {
                listListener.onRename(position, deviceModel);
            }
        });

        holder.ViewListBtnDelete.setOnClickListener(v -> {
            if (listListener != null) {
                listListener.onDelete(position, deviceModel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int size() {
        return list.size();
    }

    public ArrayList<DeviceModel> getList() {
        return new ArrayList<>(list);
    }

    public void add(BluetoothDevice device) {
        add(DeviceModel.bluetoothDeviceToModel(device));
    }

    public void add(DeviceModel device) {
        list.add(device);
        notifyItemInserted(list.size() - 1);

        if (isPersist) {
            syncPrefs();
        }
    }

    public void update(int position, DeviceModel device) {
        list.set(position, device);
        notifyItemChanged(position);

        if (isPersist) {
            syncPrefs();
        }
    }

    public void remove(int position) {
        list.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, list.size());

        if (isPersist) {
            syncPrefs();
        }
    }

    public boolean contains(BluetoothDevice device) {
        return contains(DeviceModel.bluetoothDeviceToModel(device));
    }

    public boolean contains(DeviceModel device) {
        for (DeviceModel deviceModel: list) {
            if (deviceModel.getAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged();

        if (isPersist) {
            syncPrefs();
        }
    }

    private BluetoothDevice getBluetoothDevice(String macAddress) {
        if (btAdapter != null) {
            return btAdapter.getRemoteDevice(macAddress);
        }
        return null;
    }

    private void syncPrefs() {
        String prefsString = parseToString();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SHARED_PREF_KEY, prefsString);
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
                        .append(BT_NAME_SEPARATOR)
                        .append(deviceModel.getLabel())
                        .append(DEVICE_LIST_SEPARATOR);
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }

        return "";
    }

    private ArrayList<DeviceModel> parseToList(String prefsString) {
        ArrayList<DeviceModel> newList = new ArrayList<>();
        if (!prefsString.isEmpty()) {
            String[] devices = prefsString.split(DEVICE_LIST_SEPARATOR);
            for (String dev : devices) {
                String[] chunks = dev.split(BT_NAME_SEPARATOR);
                if (chunks.length >= 3) {
                    newList.add(new DeviceModel(chunks[0], chunks[1], chunks[2]));
                }
            }
        }
        return newList;
    }
}