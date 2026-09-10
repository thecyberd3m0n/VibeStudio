package com.vibestudio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.DrawerLayout;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

public class MainActivity extends Activity {

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

    private DatabaseHelper mDbHelper;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHelper = new DatabaseHelper(this);
        mHandler = new Handler(Looper.getMainLooper());

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
        final LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        final String provider = "Gemini";
        final String savedKey = mDbHelper.getApiKey(provider);

        LinearLayout card = createCard();

        TextView name = new TextView(this);
        name.setText("Google Gemini 1.5 Pro");
        name.setTextColor(Color.parseColor("#BB86FC"));
        name.setTextSize(18);
        name.setTypeface(null, Typeface.BOLD);

        TextView desc = new TextView(this);
        desc.setText("Google • High performance multimodal reasoning and long-context AI");
        desc.setTextColor(Color.parseColor("#B0B0B0"));
        desc.setTextSize(14);
        desc.setPadding(0, 8, 0, 8);

        final TextView status = new TextView(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configure " + provider + " API Key");

        final EditText input = new EditText(this);
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
                    Toast.makeText(MainActivity.this, provider + " API Key saved to database!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
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

        final ScrollView outputScroll = new ScrollView(this);
        final EditText consoleOutput = new EditText(this);
        consoleOutput.setText("vibestudio@android:~$ ");
        consoleOutput.setTextColor(Color.parseColor("#00FF66"));
        consoleOutput.setBackgroundColor(Color.parseColor("#0D0D11"));
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(20, 20, 20, 20);
        consoleOutput.setKeyListener(null); // Read-only: selection allowed, keyboard editing disabled

        outputScroll.addView(consoleOutput);
        LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1.0f);
        outputScroll.setLayoutParams(outParams);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText cmdInput = new EditText(this);
        cmdInput.setHint("Type command (e.g. pwd, ls, echo hello)...");
        cmdInput.setHintTextColor(Color.parseColor("#666666"));
        cmdInput.setTextColor(Color.parseColor("#FFFFFF"));
        cmdInput.setBackgroundColor(Color.parseColor("#1E1E24"));
        cmdInput.setPadding(16, 16, 16, 16);
        cmdInput.setSingleLine(true);
        cmdInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1.0f);
        cmdInput.setLayoutParams(inParams);

        final Button btnSend = new Button(this);
        btnSend.setText("RUN");
        btnSend.setTextColor(Color.parseColor("#121212"));
        btnSend.setBackgroundColor(Color.parseColor("#BB86FC"));
        btnSend.setFocusable(false);

        final Runnable triggerRun = new Runnable() {
            @Override
            public void run() {
                final String cmd = cmdInput.getText().toString().trim();
                if (cmd.length() == 0) return;

                consoleOutput.append(cmd + "\n");
                cmdInput.setText("");
                restoreInputFocus(cmdInput);
                btnSend.setEnabled(false);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        executeCommandInBash(cmd, consoleOutput, outputScroll, btnSend, cmdInput);
                    }
                }).start();
            }
        };

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerRun.run();
            }
        });

        cmdInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEND ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    triggerRun.run();
                    return true;
                }
                return false;
            }
        });

        inputRow.addView(cmdInput);
        inputRow.addView(btnSend);

        layout.addView(outputScroll);
        layout.addView(inputRow);
        return layout;
    }

    private void restoreInputFocus(final EditText cmdInput) {
        cmdInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(cmdInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void executeCommandInBash(String cmd, final EditText consoleOutput, final ScrollView outputScroll, final Button btnSend, final EditText cmdInput) {
        String envHome = mDbHelper.getSetting("env_home");
        String envPrefix = mDbHelper.getSetting("env_prefix");

        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", cmd);

            Map<String, String> env = pb.environment();
            if (!TextUtils.isEmpty(envHome)) env.put("HOME", envHome);
            if (!TextUtils.isEmpty(envPrefix)) env.put("PREFIX", envPrefix);
            env.put("PATH", (envPrefix != null ? envPrefix + "/bin:" : "") + "/system/bin:/system/xbin");

            if (!TextUtils.isEmpty(envHome) && new File(envHome).exists()) {
                pb.directory(new File(envHome));
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            final StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
            reader.close();

            final String resultText = output.toString();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    consoleOutput.append(resultText);
                    consoleOutput.append("vibestudio@android:~$ ");
                    btnSend.setEnabled(true);
                    restoreInputFocus(cmdInput);
                    outputScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            outputScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });

        } catch (final Exception e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    consoleOutput.append("[Error]: " + e.getMessage() + "\nvibestudio@android:~$ ");
                    btnSend.setEnabled(true);
                    restoreInputFocus(cmdInput);
                }
            });
        }
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

        EditText tvText = new EditText(this);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#FFFFFF"));
        tvText.setTextSize(14);
        tvText.setPadding(0, 4, 0, 0);
        tvText.setBackgroundColor(Color.TRANSPARENT);
        tvText.setKeyListener(null); // Read-only: selection allowed, keyboard editing disabled

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
