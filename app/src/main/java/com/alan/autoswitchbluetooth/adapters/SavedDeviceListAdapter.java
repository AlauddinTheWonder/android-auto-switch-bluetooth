package com.alan.autoswitchbluetooth.adapters;

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
import com.alan.autoswitchbluetooth.interfaces.SavedDeviceListListener;
import com.alan.autoswitchbluetooth.models.DeviceModel;

import java.util.ArrayList;

public class SavedDeviceListAdapter extends RecyclerView.Adapter<SavedDeviceListAdapter.ViewHolder> {

    private final String BT_NAME_SEPARATOR = "~";
    private final String DEVICE_LIST_SEPARATOR = ",";
    private final String SHARED_PREF_KEY = "DevicesList";

    private SharedPreferences sharedPref;
    private ArrayList<DeviceModel> list = new ArrayList<>();
    private SavedDeviceListListener listListener;
    private boolean isPersist = false;


    public SavedDeviceListAdapter() {}

    public SavedDeviceListAdapter(ArrayList<DeviceModel> defaultList) {
        this.list = defaultList;
    }

    public void makePersistable(Context context, String persistFile) {
        sharedPref = context.getSharedPreferences(persistFile, Context.MODE_PRIVATE);

        String listString = sharedPref.getString(SHARED_PREF_KEY, "");
        list = parseToList(listString);

        isPersist = true;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout viewItem;
        TextView labelView, nameView, addressView;
        ImageButton editBtn, deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);

            viewItem = itemView.findViewById(R.id.view_item);
            labelView = itemView.findViewById(R.id.label);
            nameView = itemView.findViewById(R.id.name);
            addressView = itemView.findViewById(R.id.address);
            deleteBtn = itemView.findViewById(R.id.btn_delete);
            editBtn = itemView.findViewById(R.id.btn_edit);
        }
    }

    public void setListener(SavedDeviceListListener listener) {
        this.listListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.saved_device_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel item = list.get(position);

        holder.labelView.setText(item.getLabel());
        holder.nameView.setText(item.getName());
        holder.addressView.setText(item.getAddress());

        holder.viewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listListener != null) {
                    listListener.onClick(position, item);
                }
            }
        });

        holder.editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listListener != null) {
                    listListener.onRename(position, item);
                }
            }
        });

        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listListener != null) {
                    listListener.onDelete(position, item);
                }
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
        ArrayList<DeviceModel> newList = new ArrayList<DeviceModel>();
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