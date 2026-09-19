package com.vibestudio.app.mcp;

import android.util.Log;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.List;

public class McpClientManager {
    private static final String TAG = "McpClientManager";

    public static class McpServerInfo {
        private String name;
        private String status;
        private String color;
        private int toolsCount;

        public McpServerInfo(String name, String status, String color, int toolsCount) {
            this.name = name;
            this.status = status;
            this.color = color;
            this.toolsCount = toolsCount;
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getColor() { return color; }
        public int getToolsCount() { return toolsCount; }
    }

    private final List<McpServerInfo> configuredServers = new ArrayList<>();

    public McpClientManager() {
        Log.i(TAG, "Initializing McpClientManager and loading servers...");
        initDefaultServers();
    }

    private void initDefaultServers() {
        configuredServers.add(new McpServerInfo("FileSystem Server", "Status: Connected • 12 tools active", "#03DAC6", 12));
        configuredServers.add(new McpServerInfo("Puppeteer Server", "Status: Connected • Browser automation enabled", "#03DAC6", 8));
        configuredServers.add(new McpServerInfo("SQLite Server", "Status: Idle • Local database access", "#FFB74D", 5));
        configuredServers.add(new McpServerInfo("GitHub API Server", "Status: Disconnected", "#CF6679", 0));

        for (McpServerInfo s : configuredServers) {
            Log.d(TAG, "Loaded MCP Server: " + s.getName() + " [" + s.getStatus() + "]");
        }
    }

    public List<McpServerInfo> getConfiguredServers() {
        return configuredServers;
    }

    public McpSchema.ClientCapabilities getClientCapabilities() {
        Log.d(TAG, "Retrieving MCP client capabilities from SDK...");
        return new McpSchema.ClientCapabilities(null, null, null, null);
    }
}
