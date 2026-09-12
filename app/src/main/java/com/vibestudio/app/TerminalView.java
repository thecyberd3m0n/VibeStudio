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
import com.libtermux.LogLevel;

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
        mLibTermux = LibTermux.builder(mContext)
            .autoInstall(true)
            .logLevel(LogLevel.DEBUG)
            .build();

        // Blocking init (run on background thread!)
        new Thread(() -> {
            try {
                mLibTermux.initializeBlocking();
                String output = mLibTermux.getBridge().runOrThrow("echo Hello Linux!");
                Log.d("TAG", output); // Hello Linux!
                appendOutputToTerminal(output + "\n");
            } catch (Exception e) {
                Log.e("TAG", "Failed", e);
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
