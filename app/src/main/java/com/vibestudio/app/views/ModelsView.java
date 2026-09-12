package com.vibestudio.app.views;
import com.vibestudio.app.db.DatabaseHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ModelsView {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private final Context mContext;
    private final DatabaseHelper mDbHelper;

    public ModelsView(Context context, DatabaseHelper dbHelper) {
        mContext = context;
        mDbHelper = dbHelper;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E24"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);
        return card;
    }

    public View buildView() {
        ScrollView scrollView = new ScrollView(mContext);
        final LinearLayout container = new LinearLayout(mContext);
        container.setOrientation(LinearLayout.VERTICAL);

        final String provider = "Gemini";
        final String savedKey = mDbHelper.getApiKey(provider);

        LinearLayout card = createCard();

        TextView name = new TextView(mContext);
        name.setText("Google Gemini 1.5 Pro");
        name.setTextColor(Color.parseColor("#BB86FC"));
        name.setTextSize(18);
        name.setTypeface(null, Typeface.BOLD);

        TextView desc = new TextView(mContext);
        desc.setText("Google • High performance multimodal reasoning and long-context AI");
        desc.setTextColor(Color.parseColor("#B0B0B0"));
        desc.setTextSize(14);
        desc.setPadding(0, 8, 0, 8);

        final TextView status = new TextView(mContext);
        if (!TextUtils.isEmpty(savedKey)) {
            status.setText("Status: Configured (API Key set) • Click to edit");
            status.setTextColor(Color.parseColor("#03DAC6"));
        } else {
            status.setText("Status: Not configured (Click to set Gemini API Key)");
            status.setTextColor(Color.parseColor("#FFB74D"));
        }
        status.setTextSize(12);

        card.addView(name);
        card.addView(desc);
        card.addView(status);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiKeyDialog(provider, status);
            }
        });

        container.addView(card);
        scrollView.addView(container);
        return scrollView;
    }

    private void showApiKeyDialog(final String provider, final TextView statusTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Configure " + provider + " API Key");

        final EditText input = new EditText(mContext);
        input.setHint("Enter " + provider + " API Key");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String currentKey = mDbHelper.getApiKey(provider);
        if (!TextUtils.isEmpty(currentKey)) {
            input.setText(currentKey);
        }

        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                if (!TextUtils.isEmpty(key)) {
                    mDbHelper.saveApiKey(provider, key);
                    statusTextView.setText("Status: Configured (API Key set) • Click to edit");
                    statusTextView.setTextColor(Color.parseColor("#03DAC6"));
                    Toast.makeText(mContext, provider + " API Key saved to database!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
