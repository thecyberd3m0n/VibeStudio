package com.vibestudio.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.vibestudio.app.mcp.McpClientManager;

import java.util.ArrayList;
import java.util.List;

public class McpService extends Service {

    private static final String TAG = "McpService";
    private static McpService sInstance;

    private final IBinder binder = new LocalBinder();
    private McpClientManager mcpClientManager;
    private final List<OnMcpServerChangeListener> listeners = new ArrayList<>();

    public interface OnMcpServerChangeListener {
        void onServerListUpdated(List<McpClientManager.McpServerInfo> servers);
    }

    public class LocalBinder extends Binder {
        public McpService getService() {
            return McpService.this;
        }
    }

    public static synchronized McpService getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Log.i(TAG, "McpService onCreate: Initializing MCP subsystem...");
        mcpClientManager = new McpClientManager();
        Log.i(TAG, "McpService created. Configured servers count: " + mcpClientManager.getConfiguredServers().size());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "McpService onStartCommand triggered (startId=" + startId + ")");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "McpService client bound via Intent: " + intent);
        return binder;
    }

    public List<McpClientManager.McpServerInfo> getConfiguredServers() {
        if (mcpClientManager == null) {
            Log.w(TAG, "getConfiguredServers called but McpClientManager is null");
            return new ArrayList<>();
        }
        List<McpClientManager.McpServerInfo> servers = mcpClientManager.getConfiguredServers();
        Log.d(TAG, "getConfiguredServers returned " + servers.size() + " server(s)");
        return servers;
    }

    public McpClientManager getMcpClientManager() {
        return mcpClientManager;
    }

    public void addListener(OnMcpServerChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Listener added. Active listeners: " + listeners.size());
        }
    }

    public void removeListener(OnMcpServerChangeListener listener) {
        if (listeners.remove(listener)) {
            Log.d(TAG, "Listener removed. Active listeners: " + listeners.size());
        }
    }

    public void notifyServersChanged() {
        List<McpClientManager.McpServerInfo> servers = getConfiguredServers();
        Log.i(TAG, "Notifying " + listeners.size() + " listener(s) of server changes");
        for (OnMcpServerChangeListener listener : new ArrayList<>(listeners)) {
            listener.onServerListUpdated(servers);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "McpService onDestroy: Shutting down MCP Service");
        sInstance = null;
    }
}
