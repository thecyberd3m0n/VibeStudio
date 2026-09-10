package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class TerminalView {

    private static final int MATCH_PARENT = -1;

    private final Context mContext;
    private final DatabaseHelper mDbHelper;
    private final Handler mHandler;

    private Process mProcess;
    private OutputStream mProcessInput;
    private InputStream mProcessOutput;

    private ScrollView mScrollView;
    private EditText mTerminalBuffer;
    private boolean mIsWritingFromProcess = false;
    private int mLastBufferLength = 0;

    public TerminalView(Context context, DatabaseHelper dbHelper) {
        mContext = context;
        mDbHelper = dbHelper;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public View buildView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        mScrollView = new ScrollView(mContext);
        mTerminalBuffer = new EditText(mContext);

        mTerminalBuffer.setTextColor(Color.parseColor("#00FF66"));
        mTerminalBuffer.setBackgroundColor(Color.parseColor("#0D0D11"));
        mTerminalBuffer.setTypeface(Typeface.MONOSPACE);
        mTerminalBuffer.setTextSize(13);
        mTerminalBuffer.setPadding(24, 24, 24, 24);

        // Configure as a full-screen terminal canvas
        mTerminalBuffer.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mTerminalBuffer.setImeOptions(EditorInfo.IME_ACTION_NONE);

        mTerminalBuffer.setFocusable(true);
        mTerminalBuffer.setFocusableInTouchMode(true);

        mScrollView.addView(mTerminalBuffer, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        layout.addView(mScrollView, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // Send typed text directly to process stdin
        mTerminalBuffer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mIsWritingFromProcess) return;

                if (count > 0 && mProcessInput != null) {
                    CharSequence added = s.subSequence(start, start + count);
                    sendToProcess(added.toString());
                } else if (before > 0 && count == 0 && mProcessInput != null) {
                    // Send backspace / delete ASCII byte
                    sendToProcess("\b");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Capture Enter key press on soft/hardware keyboards
        mTerminalBuffer.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    sendToProcess("\n");
                    return true;
                }
                return false;
            }
        });

        startShellProcess();

        return layout;
    }

    private void sendToProcess(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mProcessInput != null) {
                        mProcessInput.write(text.getBytes("UTF-8"));
                        mProcessInput.flush();
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void startShellProcess() {
        String envHome = mDbHelper.getSetting("env_home");
        String envPrefix = mDbHelper.getSetting("env_prefix");

        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-i");

            Map<String, String> env = pb.environment();
            if (!TextUtils.isEmpty(envHome)) env.put("HOME", envHome);
            if (!TextUtils.isEmpty(envPrefix)) env.put("PREFIX", envPrefix);
            env.put("PATH", (envPrefix != null ? envPrefix + "/bin:" : "") + "/system/bin:/system/xbin");
            env.put("TERM", "xterm-256color");

            if (!TextUtils.isEmpty(envHome) && new File(envHome).exists()) {
                pb.directory(new File(envHome));
            }

            pb.redirectErrorStream(true);
            mProcess = pb.start();

            mProcessInput = mProcess.getOutputStream();
            mProcessOutput = mProcess.getInputStream();

            // Background reader thread for stdout/stderr
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int length;
                    try {
                        while ((length = mProcessOutput.read(buffer)) != -1) {
                            final String text = new String(buffer, 0, length, "UTF-8");
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    appendOutputToTerminal(text);
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }).start();

        } catch (Exception e) {
            appendOutputToTerminal("[Error starting terminal session]: " + e.getMessage() + "\n");
        }
    }

    private void appendOutputToTerminal(String text) {
        mIsWritingFromProcess = true;
        mTerminalBuffer.append(text);
        mTerminalBuffer.setSelection(mTerminalBuffer.getText().length());
        mLastBufferLength = mTerminalBuffer.getText().length();
        mIsWritingFromProcess = false;

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}
