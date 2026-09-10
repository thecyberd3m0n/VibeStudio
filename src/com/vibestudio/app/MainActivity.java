package com.vibestudio.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.widget.DrawerLayout;

public class MainActivity extends Activity {

    // -1 represents MATCH_PARENT / FILL_PARENT in early Android API
    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                mContentFrame.addView(buildModelsView());
                break;
            case 1:
                mContentFrame.addView(buildMCPView());
                break;
            case 2:
                mContentFrame.addView(buildTerminalView());
                break;
            case 3:
                mContentFrame.addView(buildChatView());
                break;
            case 4:
                mContentFrame.addView(buildPermissionsView());
                break;
        }

        mDrawerLayout.closeDrawer(mDrawerContainer);
    }

    // Helper to build Material-styled Dark Card
    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E24"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);
        return card;
    }

    private View buildModelsView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        String[][] models = {
            {"Claude 3.5 Sonnet", "Anthropic • State-of-the-art coding and reasoning", "Active"},
            {"GPT-4o", "OpenAI • Multimodal high-speed model", "Ready"},
            {"DeepSeek R1", "DeepSeek • Advanced reasoning and open-weights", "Ready"},
            {"Llama 3.3 70B", "Meta • Open-source performant model", "Local"}
        };

        for (String[] m : models) {
            LinearLayout card = createCard();

            TextView name = new TextView(this);
            name.setText(m[0]);
            name.setTextColor(Color.parseColor("#BB86FC"));
            name.setTextSize(18);
            name.setTypeface(null, Typeface.BOLD);

            TextView desc = new TextView(this);
            desc.setText(m[1]);
            desc.setTextColor(Color.parseColor("#B0B0B0"));
            desc.setTextSize(14);
            desc.setPadding(0, 8, 0, 8);

            TextView status = new TextView(this);
            status.setText("Status: " + m[2]);
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

    private View buildMCPView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        String[][] servers = {
            {"FileSystem Server", "Status: Connected • 12 tools active", "#03DAC6"},
            {"Puppeteer Server", "Status: Connected • Browser automation enabled", "#03DAC6"},
            {"SQLite Server", "Status: Idle • Local database access", "#FFB74D"},
            {"GitHub API Server", "Status: Disconnected", "#CF6679"}
        };

        for (String[] s : servers) {
            LinearLayout card = createCard();

            TextView name = new TextView(this);
            name.setText("MCP: " + s[0]);
            name.setTextColor(Color.parseColor("#FFFFFF"));
            name.setTextSize(16);
            name.setTypeface(null, Typeface.BOLD);

            TextView info = new TextView(this);
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

    private View buildTerminalView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView consoleOutput = new TextView(this);
        consoleOutput.setText("vibestudio@termux:~$ uname -a\nLinux termux-android 5.10.0-vibe aarch64\nvibestudio@termux:~$ ");
        consoleOutput.setTextColor(Color.parseColor("#00FF66"));
        consoleOutput.setBackgroundColor(Color.parseColor("#0D0D11"));
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1.0f);
        consoleOutput.setLayoutParams(outParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText cmdInput = new EditText(this);
        cmdInput.setHint("Type command...");
        cmdInput.setHintTextColor(Color.parseColor("#666666"));
        cmdInput.setTextColor(Color.parseColor("#FFFFFF"));
        cmdInput.setBackgroundColor(Color.parseColor("#1E1E24"));
        cmdInput.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1.0f);
        cmdInput.setLayoutParams(inParams);

        Button btnSend = new Button(this);
        btnSend.setText("RUN");
        btnSend.setTextColor(Color.parseColor("#121212"));
        btnSend.setBackgroundColor(Color.parseColor("#BB86FC"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = cmdInput.getText().toString();
                if (cmd.length() > 0) {
                    consoleOutput.append(cmd + "\n[Exec]: " + cmd + " executed successfully.\nvibestudio@termux:~$ ");
                    cmdInput.setText("");
                }
            }
        });

        inputRow.addView(cmdInput);
        inputRow.addView(btnSend);

        layout.addView(consoleOutput);
        layout.addView(inputRow);
        return layout;
    }

    private View buildChatView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final ScrollView chatScroll = new ScrollView(this);
        final LinearLayout chatContainer = new LinearLayout(this);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1.0f);
        chatScroll.setLayoutParams(scrollParams);

        // Add sample messages
        addChatMessage(chatContainer, "Assistant", "Hello! Welcome to VibeStudio. How can I assist with your project today?", false);
        addChatMessage(chatContainer, "User", "Can you build a Material Dark Android app?", true);
        addChatMessage(chatContainer, "Assistant", "Absolutely! VibeStudio is configured with a Material Dark theme and drawer navigation.", false);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText msgInput = new EditText(this);
        msgInput.setHint("Message VibeStudio...");
        msgInput.setHintTextColor(Color.parseColor("#666666"));
        msgInput.setTextColor(Color.parseColor("#FFFFFF"));
        msgInput.setBackgroundColor(Color.parseColor("#1E1E24"));
        msgInput.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1.0f);
        msgInput.setLayoutParams(inParams);

        Button btnSend = new Button(this);
        btnSend.setText("SEND");
        btnSend.setTextColor(Color.parseColor("#121212"));
        btnSend.setBackgroundColor(Color.parseColor("#03DAC6"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = msgInput.getText().toString();
                if (text.length() > 0) {
                    addChatMessage(chatContainer, "User", text, true);
                    msgInput.setText("");
                    addChatMessage(chatContainer, "Assistant", "Received: " + text, false);
                    chatScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        inputRow.addView(msgInput);
        inputRow.addView(btnSend);

        layout.addView(chatScroll);
        layout.addView(inputRow);
        return layout;
    }

    private void addChatMessage(LinearLayout container, String sender, String text, boolean isUser) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 14, 18, 14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);

        if (isUser) {
            params.gravity = android.view.Gravity.RIGHT;
            card.setBackgroundColor(Color.parseColor("#3700B3"));
        } else {
            params.gravity = android.view.Gravity.LEFT;
            card.setBackgroundColor(Color.parseColor("#25252A"));
        }
        card.setLayoutParams(params);

        TextView tvSender = new TextView(this);
        tvSender.setText(sender);
        tvSender.setTextColor(isUser ? Color.parseColor("#03DAC6") : Color.parseColor("#BB86FC"));
        tvSender.setTextSize(12);
        tvSender.setTypeface(null, Typeface.BOLD);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#FFFFFF"));
        tvText.setTextSize(14);
        tvText.setPadding(0, 4, 0, 0);

        card.addView(tvSender);
        card.addView(tvText);
        container.addView(card);
    }

    private View buildPermissionsView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        String[][] permissions = {
            {"INTERNET", "Allows app to communicate with remote MCP servers and models", "Granted"},
            {"WRITE_EXTERNAL_STORAGE", "Required for file output and export", "Granted"},
            {"READ_EXTERNAL_STORAGE", "Required for loading local project files", "Granted"},
            {"RECORD_AUDIO", "Voice input capabilities for Chat mode", "Prompt on Use"}
        };

        for (String[] p : permissions) {
            LinearLayout card = createCard();

            TextView name = new TextView(this);
            name.setText("Permission: " + p[0]);
            name.setTextColor(Color.parseColor("#BB86FC"));
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);

            TextView desc = new TextView(this);
            desc.setText(p[1]);
            desc.setTextColor(Color.parseColor("#B0B0B0"));
            desc.setTextSize(13);
            desc.setPadding(0, 6, 0, 6);

            TextView status = new TextView(this);
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
