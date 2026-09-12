package com.vibestudio.app.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PermissionsFragment extends Fragment {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        if (context == null) return null;

        ScrollView scrollView = new ScrollView(context);
        LinearLayout mainContainer = new LinearLayout(context);
        mainContainer.setOrientation(LinearLayout.VERTICAL);

        String[][] permissions = {
            {"INTERNET", "Allows app to communicate with remote MCP servers and models", "Granted"},
            {"WRITE_EXTERNAL_STORAGE", "Required for file output and export", "Granted"},
            {"READ_EXTERNAL_STORAGE", "Required for loading local project files", "Granted"},
            {"RECORD_AUDIO", "Voice input capabilities for Chat mode", "Prompt on Use"}
        };

        for (String[] p : permissions) {
            LinearLayout card = createCard(context);

            TextView name = new TextView(context);
            name.setText("Permission: " + p[0]);
            name.setTextColor(Color.parseColor("#BB86FC"));
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);

            TextView desc = new TextView(context);
            desc.setText(p[1]);
            desc.setTextColor(Color.parseColor("#B0B0B0"));
            desc.setTextSize(13);
            desc.setPadding(0, 6, 0, 6);

            TextView status = new TextView(context);
            status.setText("Status: " + p[2]);
            status.setTextColor(Color.parseColor("#03DAC6"));
            status.setTextSize(12);

            card.addView(name);
            card.addView(desc);
            card.addView(status);
            mainContainer.addView(card);
        }

        scrollView.addView(mainContainer);
        return scrollView;
    }

    private LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E24"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);
        return card;
    }
}
