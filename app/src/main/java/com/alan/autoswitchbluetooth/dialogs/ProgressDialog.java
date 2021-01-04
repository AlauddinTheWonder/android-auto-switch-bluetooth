package com.alan.autoswitchbluetooth.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitchbluetooth.R;

import java.util.Objects;

public class ProgressDialog {

    private AlertDialog dialog;
    private boolean showing = false;
    private TextView viewMessage;

    public ProgressDialog(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        assert inflater != null;
        View progressLayout = inflater.inflate(R.layout.progress_layout, null);
        viewMessage = progressLayout.findViewById(R.id.progressMsg);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle("Please wait...");
        builder.setCancelable(false);
        builder.setView(progressLayout);
        dialog = builder.create();

        showing = false;
    }

    public void show() {
        show("Please wait...");
    }

    public void show(String msg) {
        viewMessage.setText(msg);
        if (showing) return;
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(700, 500);
        showing = true;
    }

    public void hide() {
        if (!showing) return;
        dialog.hide();
        viewMessage.setText("");
        showing = false;
    }

    public void dismiss() {
        dialog.dismiss();
        viewMessage.setText("");
        showing = false;
    }

    public boolean isShowing() {
        return showing;
    }
}
