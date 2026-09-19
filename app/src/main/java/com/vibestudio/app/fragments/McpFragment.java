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

import com.vibestudio.app.mcp.McpClientManager;
import com.vibestudio.app.service.McpService;

import java.util.List;

public class McpFragment extends Fragment implements McpService.OnMcpServerChangeListener {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private LinearLayout mainContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        if (context == null) return null;

        ScrollView scrollView = new ScrollView(context);
        mainContainer = new LinearLayout(context);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mainContainer);

        renderServers();
        return scrollView;
    }

    @Override
    public void onStart() {
        super.onStart();
        McpService service = McpService.getInstance();
        if (service != null) {
            service.addListener(this);
            renderServers();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        McpService service = McpService.getInstance();
        if (service != null) {
            service.removeListener(this);
        }
    }

    @Override
    public void onServerListUpdated(List<McpClientManager.McpServerInfo> servers) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::renderServers);
        }
    }

    private void renderServers() {
        if (mainContainer == null) return;
        Context context = getContext();
        if (context == null) return;

        mainContainer.removeAllViews();

        McpService service = McpService.getInstance();
        List<McpClientManager.McpServerInfo> servers = service != null ? service.getConfiguredServers() : null;

        if (servers == null || servers.isEmpty()) {
            TextView emptyView = new TextView(context);
            emptyView.setText("No MCP servers available.");
            emptyView.setTextColor(Color.parseColor("#888888"));
            emptyView.setPadding(32, 32, 32, 32);
            mainContainer.addView(emptyView);
            return;
        }

        for (McpClientManager.McpServerInfo server : servers) {
            LinearLayout card = createCard(context);

            TextView name = new TextView(context);
            name.setText("MCP: " + server.getName());
            name.setTextColor(Color.parseColor("#FFFFFF"));
            name.setTextSize(16);
            name.setTypeface(null, Typeface.BOLD);

            TextView info = new TextView(context);
            info.setText(server.getStatus());
            info.setTextColor(Color.parseColor(server.getColor()));
            info.setTextSize(14);
            info.setPadding(0, 8, 0, 0);

            card.addView(name);
            card.addView(info);
            mainContainer.addView(card);
        }
    }

    private LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(16, 16, 16, 0);
        card.setLayoutParams(params);
        card.setPadding(24, 24, 24, 24);
        card.setBackgroundColor(Color.parseColor("#1E1E1E"));
        return card;
    }
}
