package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PermissionsView {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private final Context mContext;

    public PermissionsView(Context context) {
        mContext = context;
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
        LinearLayout container = new LinearLayout(mContext);
        container.setOrientation(LinearLayout.VERTICAL);

        String[][] permissions = {
            {"INTERNET", "Allows app to communicate with remote MCP servers and models", "Granted"},
            {"WRITE_EXTERNAL_STORAGE", "Required for file output and export", "Granted"},
            {"READ_EXTERNAL_STORAGE", "Required for loading local project files", "Granted"},
            {"RECORD_AUDIO", "Voice input capabilities for Chat mode", "Prompt on Use"}
        };

        for (String[] p : permissions) {
            LinearLayout card = createCard();

            TextView name = new TextView(mContext);
            name.setText("Permission: " + p[0]);
            name.setTextColor(Color.parseColor("#BB86FC"));
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);

            TextView desc = new TextView(mContext);
            desc.setText(p[1]);
            desc.setTextColor(Color.parseColor("#B0B0B0"));
            desc.setTextSize(13);
            desc.setPadding(0, 6, 0, 6);

            TextView status = new TextView(mContext);
            status.setText("Status: " + p[2]);
            status.setTextColor(Color.parseColor("#03DAC6"));
            status.setTextSize(12);

            card.addView(name);
            card.addView(desc);
            card.addView(status);
            container.addView(card);
        }

        scrollView.addView(container);
        return scrollView;
    }
}
