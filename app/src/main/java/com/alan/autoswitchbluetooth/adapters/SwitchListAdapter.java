package com.alan.autoswitchbluetooth.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alan.autoswitchbluetooth.R;
import com.alan.autoswitchbluetooth.interfaces.SwitchListListener;
import com.alan.autoswitchbluetooth.models.SwitchModel;

import java.util.ArrayList;

public class SwitchListAdapter extends RecyclerView.Adapter<SwitchListAdapter.ViewHolder> {

    private final ArrayList<SwitchModel> list;
    private SwitchListListener listListener;

    public SwitchListAdapter() {
        this.list = new ArrayList<>();
    }

    public SwitchListAdapter(ArrayList<SwitchModel> defaultList) {
        this.list = defaultList;
    }

    public void setListener(SwitchListListener listener) {
        this.listListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout infoView;
        TextView addView, switchNum, onTxt, offTxt;
        ImageButton addBtn, editBtn, delBtn;

        ViewHolder(View itemView) {
            super(itemView);

            infoView = itemView.findViewById(R.id.info_view);
            addView = itemView.findViewById(R.id.add_view);

            switchNum = itemView.findViewById(R.id.switch_txt);
            onTxt = itemView.findViewById(R.id.on_txt);
            offTxt = itemView.findViewById(R.id.off_txt);

            addBtn = itemView.findViewById(R.id.add_btn);
            editBtn = itemView.findViewById(R.id.edit_btn);
            delBtn = itemView.findViewById(R.id.del_btn);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.switch_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final SwitchModel item = list.get(position);

        if (item.getPin() == 0) { // empty slot
            holder.addBtn.setVisibility(View.VISIBLE);
            holder.editBtn.setVisibility(View.GONE);
            holder.delBtn.setVisibility(View.GONE);

            holder.addView.setVisibility(View.VISIBLE);
            holder.infoView.setVisibility(View.GONE);

            holder.addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listListener != null)
                        listListener.onEdit(position, item);
                }
            });
        }
        else {
            holder.addBtn.setVisibility(View.GONE);
            holder.editBtn.setVisibility(View.VISIBLE);
            holder.delBtn.setVisibility(View.VISIBLE);

            holder.addView.setVisibility(View.GONE);
            holder.infoView.setVisibility(View.VISIBLE);

            holder.switchNum.setText(String.valueOf(item.getPin()));
            holder.onTxt.setText(String.valueOf(item.getOn()));
            holder.offTxt.setText(String.valueOf(item.getOff()));

            holder.delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listListener != null)
                        listListener.onDelete(position, item);
                }
            });

            holder.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listListener != null)
                        listListener.onEdit(position, item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public int size() {
        return list.size();
    }

    public ArrayList<SwitchModel> getList() {
        return new ArrayList<>(list);
    }

    public SwitchModel get(int position) {
        return list.get(position);
    }

    public void add(SwitchModel model) {
        list.add(model);
        notifyItemInserted(list.size() - 1);
    }

    public void update(int position, SwitchModel switchModel) {
        list.set(position, switchModel);
        notifyItemChanged(position, switchModel);
    }

    public void remove(int position) {
        list.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, list.size());
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }

}
