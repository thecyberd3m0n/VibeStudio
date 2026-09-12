package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.libtermux.LibTermux;
import com.libtermux.TermuxConfig;
import com.libtermux.LogLevel;
import com.libtermux.executor.ExecutionResult;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

public class TerminalView {

    private static final String TAG = "TerminalView";
    private static final int MATCH_PARENT = -1;

    private final Context mContext;
    private ScrollView mScrollView;
    private TextView mTerminalBuffer;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private LibTermux mLibTermux;

    public TerminalView(Context context) {
        mContext = context;
        initLibTermux();
    }

    private void initLibTermux() {
        TermuxConfig config = TermuxConfig.Companion.builder().build();

        mLibTermux = LibTermux.Companion.init(mContext, config);

        // Run initialization & execution on background thread
        new Thread(() -> {
            try {
                // Ensure directory structure is in place
                File filesDir = mContext.getFilesDir();
                File usrDir = new File(filesDir, "libtermux/usr");
                File binDir = new File(usrDir, "bin");
                File homeDir = new File(filesDir, "libtermux/home");
                File tmpDir = new File(usrDir, "tmp");

                binDir.mkdirs();
                homeDir.mkdirs();
                tmpDir.mkdirs();

                File bashFile = new File(binDir, "bash");
                if (!bashFile.exists()) {
                    String dummyBashScript = "#!/system/bin/sh\nexec /system/bin/sh \"$@\"\n";
                    FileOutputStream fos = new FileOutputStream(bashFile);
                    fos.write(dummyBashScript.getBytes("UTF-8"));
                    fos.close();
                    bashFile.setExecutable(true, false);
                }

                File shFile = new File(binDir, "sh");
                if (!shFile.exists()) {
                    String dummyShScript = "#!/system/bin/sh\nexec /system/bin/sh \"$@\"\n";
                    FileOutputStream fos = new FileOutputStream(shFile);
                    fos.write(dummyShScript.getBytes("UTF-8"));
                    fos.close();
                    shFile.setExecutable(true, false);
                }

                File marker = new File(filesDir, "libtermux/.bootstrap_ok");
                if (!marker.exists()) {
                    marker.createNewFile();
                }

                // Execute command using system sh via reflection on access$runProcess
                Method runProcessMethod = com.libtermux.executor.CommandExecutor.class.getDeclaredMethod(
                    "access$runProcess",
                    com.libtermux.executor.CommandExecutor.class,
                    String.class,
                    File.class,
                    Map.class,
                    String.class
                );
                runProcessMethod.setAccessible(true);
                ExecutionResult result = (ExecutionResult) runProcessMethod.invoke(
                    null,
                    mLibTermux.getExecutor(),
                    "echo Hello Linux from LibTermux!",
                    null,
                    Collections.emptyMap(),
                    "/system/bin/sh"
                );

                String output = result.getStdout();
                Log.d(TAG, "Output: " + output);
                appendOutputToTerminal(output + "\n");
            } catch (Exception e) {
                Log.e(TAG, "Failed", e);
                appendOutputToTerminal("\nFailed: " + e.getMessage() + "\n");
            }
        }).start();
    }

    public View buildView() {
        FrameLayout container = new FrameLayout(mContext);
        container.setBackgroundColor(Color.parseColor("#1E1E2E"));

        mScrollView = new ScrollView(mContext);
        mScrollView.setFillViewport(true);

        mTerminalBuffer = new TextView(mContext);
        mTerminalBuffer.setTextColor(Color.parseColor("#CDD6F4"));
        mTerminalBuffer.setBackgroundColor(Color.TRANSPARENT);
        mTerminalBuffer.setTypeface(Typeface.MONOSPACE);
        mTerminalBuffer.setTextSize(13);
        mTerminalBuffer.setPadding(24, 24, 24, 24);
        mTerminalBuffer.setGravity(Gravity.TOP | Gravity.LEFT);
        mTerminalBuffer.setTextIsSelectable(true);

        mScrollView.addView(mTerminalBuffer, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        container.addView(mScrollView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        return container;
    }

    private void appendOutputToTerminal(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mTerminalBuffer != null) {
                    mTerminalBuffer.append(text);
                    mScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mScrollView != null) {
                                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        }
                    });
                }
            }
        });
    }
}
