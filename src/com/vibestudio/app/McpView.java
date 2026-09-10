package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class McpView {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private final Context mContext;

    public McpView(Context context) {
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

        String[][] servers = {
            {"FileSystem Server", "Status: Connected • 12 tools active", "#03DAC6"},
            {"Puppeteer Server", "Status: Connected • Browser automation enabled", "#03DAC6"},
            {"SQLite Server", "Status: Idle • Local database access", "#FFB74D"},
            {"GitHub API Server", "Status: Disconnected", "#CF6679"}
        };

        for (String[] s : servers) {
            LinearLayout card = createCard();

            TextView name = new TextView(mContext);
            name.setText("MCP: " + s[0]);
            name.setTextColor(Color.parseColor("#FFFFFF"));
            name.setTextSize(16);
            name.setTypeface(null, Typeface.BOLD);

            TextView info = new TextView(mContext);
            info.setText(s[1]);
            info.setTextColor(Color.parseColor(s[2]));
            info.setTextSize(14);
            info.setPadding(0, 8, 0, 0);

            card.addView(name);
            card.addView(info);
            container.addView(card);
        }

        scrollView.addView(container);
        return scrollView;
    }
}
