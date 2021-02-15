package com.alan.autoswitchbluetooth.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitchbluetooth.interfaces.PromptDialogInterface;

public class PromptDialog {

    private AlertDialog alertDialog;
    private EditText input;

    private PromptDialogInterface.OnClickListener clickListener;

    public PromptDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setCancelable(true);

        RelativeLayout layout = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int pads = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, context.getResources().getDisplayMetrics());
        layout.setLayoutParams(layoutParams);
        layout.setPadding(pads, pads, pads, pads);

        input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(layoutParams);
        input.setText("");

        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clickListener.onClick(input.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog = builder.create();
    }

    public void setInputText(String text) {
        input.setText(text);
    }

    public void show(PromptDialogInterface.OnClickListener listener) {
        clickListener = listener;
        alertDialog.show();
    }
}
