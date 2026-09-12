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

public class McpFragment extends Fragment {

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

        String[][] servers = {
            {"FileSystem Server", "Status: Connected • 12 tools active", "#03DAC6"},
            {"Puppeteer Server", "Status: Connected • Browser automation enabled", "#03DAC6"},
            {"SQLite Server", "Status: Idle • Local database access", "#FFB74D"},
            {"GitHub API Server", "Status: Disconnected", "#CF6679"}
        };

        for (String[] s : servers) {
            LinearLayout card = createCard(context);

            TextView name = new TextView(context);
            name.setText("MCP: " + s[0]);
            name.setTextColor(Color.parseColor("#FFFFFF"));
            name.setTextSize(16);
            name.setTypeface(null, Typeface.BOLD);

            TextView info = new TextView(context);
            info.setText(s[1]);
            info.setTextColor(Color.parseColor(s[2]));
            info.setTextSize(14);
            info.setPadding(0, 8, 0, 0);

            card.addView(name);
            card.addView(info);
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
