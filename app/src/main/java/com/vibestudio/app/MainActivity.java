package com.vibestudio.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static class MenuItem {
        String title;
        String icon;

        MenuItem(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private final MenuItem[] mMenuItems = new MenuItem[] {
        new MenuItem("Models", "🧠"),
        new MenuItem("MCP", "🔌"),
        new MenuItem("Terminal", "💻"),
        new MenuItem("Chat", "💬"),
        new MenuItem("Permissions", "🔒")
    };

    private DrawerLayout mDrawerLayout;
    private View mDrawerContainer;
    private ListView mDrawerList;
    private FrameLayout mContentFrame;
    private TextView mToolbarTitle;

    private DatabaseHelper mDbHelper;
    private ModelsView mModelsView;
    private McpView mMcpView;
    private TerminalView mTerminalView;
    private ChatView mChatView;
    private PermissionsView mPermissionsView;

    private TerminalService mTerminalService;
    private boolean mIsBound = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TerminalService.LocalBinder binder = (TerminalService.LocalBinder) service;
            mTerminalService = binder.getService();
            mIsBound = true;
            if (mTerminalView != null) {
                mTerminalView.setTerminalService(mTerminalService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTerminalService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new DatabaseHelper(this);
        mModelsView = new ModelsView(this, mDbHelper);
        mMcpView = new McpView(this);
        mTerminalView = new TerminalView(this);
        mChatView = new ChatView(this);
        mPermissionsView = new PermissionsView(this);

        // Bind to TerminalService
        Intent serviceIntent = new Intent(this, TerminalService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerContainer = findViewById(R.id.left_drawer_container);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mContentFrame = (FrameLayout) findViewById(R.id.content_frame);
        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);

        ArrayAdapter<MenuItem> adapter = new ArrayAdapter<MenuItem>(this, R.layout.drawer_list_item, mMenuItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.drawer_list_item, parent, false);
                }
                MenuItem item = getItem(position);
                TextView tvIcon = (TextView) convertView.findViewById(R.id.item_icon);
                TextView tvTitle = (TextView) convertView.findViewById(R.id.item_title);

                if (item != null) {
                    tvIcon.setText(item.icon);
                    tvTitle.setText(item.title);
                }
                return convertView;
            }
        };

        mDrawerList.setAdapter(adapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        findViewById(R.id.btn_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDrawerLayout.isDrawerOpen(mDrawerContainer)) {
                    mDrawerLayout.closeDrawer(mDrawerContainer);
                } else {
                    mDrawerLayout.openDrawer(mDrawerContainer);
                }
            }
        });

        if (savedInstanceState == null) {
            selectItem(0);
        }
    }

    private void selectItem(int position) {
        MenuItem item = mMenuItems[position];
        mToolbarTitle.setText(item.title);
        mDrawerList.setItemChecked(position, true);

        mContentFrame.removeAllViews();

        switch (position) {
            case 0:
                mContentFrame.addView(mModelsView.buildView());
                break;
            case 1:
                mContentFrame.addView(mMcpView.buildView());
                break;
            case 2:
                mContentFrame.addView(mTerminalView.buildView());
                break;
            case 3:
                mContentFrame.addView(mChatView.buildView());
                break;
            case 4:
                mContentFrame.addView(mPermissionsView.buildView());
                break;
        }

        mDrawerLayout.closeDrawer(mDrawerContainer);
    }

    @Override
    protected void onDestroy() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }
}
