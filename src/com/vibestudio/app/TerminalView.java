package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

public class TerminalView {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private final Context mContext;
    private final DatabaseHelper mDbHelper;
    private final Handler mHandler;

    public TerminalView(Context context, DatabaseHelper dbHelper) {
        mContext = context;
        mDbHelper = dbHelper;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public View buildView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        final ScrollView outputScroll = new ScrollView(mContext);
        final EditText consoleOutput = new EditText(mContext);
        consoleOutput.setText("vibestudio@android:~$ ");
        consoleOutput.setTextColor(Color.parseColor("#00FF66"));
        consoleOutput.setBackgroundColor(Color.parseColor("#0D0D11"));
        consoleOutput.setTypeface(Typeface.MONOSPACE);
        consoleOutput.setPadding(20, 20, 20, 20);

        // Read-only EditText enables text selection handles and copy context menu without bringing up the keyboard
        consoleOutput.setRawInputType(InputType.TYPE_NULL);
        consoleOutput.setFocusable(true);
        consoleOutput.setFocusableInTouchMode(true);
        consoleOutput.setClickable(true);
        consoleOutput.setLongClickable(true);

        outputScroll.addView(consoleOutput);
        LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1.0f);
        outputScroll.setLayoutParams(outParams);

        LinearLayout inputRow = new LinearLayout(mContext);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText cmdInput = new EditText(mContext);
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

        final Button btnSend = new Button(mContext);
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
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
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
}
