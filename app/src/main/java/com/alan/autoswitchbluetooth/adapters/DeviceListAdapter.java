package com.alan.autoswitchbluetooth.adapters;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alan.autoswitchbluetooth.R;
import com.alan.autoswitchbluetooth.interfaces.DeviceListListener;

import java.util.ArrayList;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    private ArrayList<BluetoothDevice> list = new ArrayList<>();
    private DeviceListListener listListener;
    private Context context;

    public DeviceListAdapter(Context context) {
        this.context = context;
    }

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> defaultList) {
        this.context = context;
        this.list = defaultList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout ViewListItem;
        TextView ViewListLabel, ViewListDescription;
        View ViewListStatus;

        ViewHolder(View itemView) {
            super(itemView);

            ViewListItem = itemView.findViewById(R.id.list_item);
            ViewListLabel = itemView.findViewById(R.id.list_label);
            ViewListDescription = itemView.findViewById(R.id.list_description);
            ViewListStatus = itemView.findViewById(R.id.list_status);
        }
    }

    public void setListener(DeviceListListener listener) {
        this.listListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.device_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice item = list.get(position);

        holder.ViewListLabel.setText(item.getName());
        holder.ViewListDescription.setText(item.getAddress());

        int colorPrimary = context.getResources().getColor(R.color.primary);
        int colorDisable = context.getResources().getColor(R.color.disabled);
        holder.ViewListStatus.setBackgroundColor(item.getBondState() == BluetoothDevice.BOND_BONDED ? colorPrimary : colorDisable);

        holder.ViewListItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listListener != null) {
                    listListener.onClick(position, item);
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

    public ArrayList<BluetoothDevice> getList() {
        return new ArrayList<>(list);
    }

    public void add(BluetoothDevice device) {
        list.add(device);
        notifyItemInserted(list.size() - 1);
    }

    public void update(int position, BluetoothDevice device) {
        list.set(position, device);
        notifyItemChanged(position);
    }

    public void remove(int position) {
        list.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, list.size());
    }

    public boolean contains(BluetoothDevice device) {
        return list.contains(device);
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }
}