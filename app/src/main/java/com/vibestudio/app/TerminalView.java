package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;

public class TerminalView implements TerminalService.OutputListener {

    private static final int MATCH_PARENT = -1;

    private final Context mContext;
    private ScrollView mScrollView;
    private EditText mTerminalBuffer;

    private TerminalService mTerminalService;
    private boolean mIsWritingFromProcess = false;

    public TerminalView(Context context) {
        mContext = context;
    }

    public void setTerminalService(TerminalService service) {
        mTerminalService = service;
        if (mTerminalService != null) {
            mTerminalService.setOutputListener(this);
        }
    }

    public View buildView() {
        FrameLayout container = new FrameLayout(mContext);
        container.setBackgroundColor(Color.parseColor("#1E1E2E"));

        mScrollView = new ScrollView(mContext);
        mScrollView.setFillViewport(true);

        mTerminalBuffer = new EditText(mContext);
        mTerminalBuffer.setTextColor(Color.parseColor("#CDD6F4"));
        mTerminalBuffer.setBackgroundColor(Color.TRANSPARENT);
        mTerminalBuffer.setTypeface(Typeface.MONOSPACE);
        mTerminalBuffer.setTextSize(13);
        mTerminalBuffer.setPadding(24, 24, 24, 24);
        mTerminalBuffer.setGravity(Gravity.TOP | Gravity.LEFT);

        mTerminalBuffer.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mTerminalBuffer.setImeOptions(EditorInfo.IME_ACTION_NONE);

        mTerminalBuffer.setFocusable(true);
        mTerminalBuffer.setFocusableInTouchMode(true);

        mScrollView.addView(mTerminalBuffer, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        container.addView(mScrollView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        mTerminalBuffer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mIsWritingFromProcess) return;

                if (count > 0 && mTerminalService != null) {
                    CharSequence added = s.subSequence(start, start + count);
                    mTerminalService.writeInput(added.toString());
                } else if (before > 0 && count == 0 && mTerminalService != null) {
                    mTerminalService.writeInput("\b");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mTerminalBuffer.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (mTerminalService != null) {
                        mTerminalService.writeInput("\n");
                    }
                    return true;
                }
                return false;
            }
        });

        return container;
    }

    @Override
    public void onOutput(String text) {
        appendOutputToTerminal(text);
    }

    private void appendOutputToTerminal(String text) {
        mIsWritingFromProcess = true;
        mTerminalBuffer.append(text);
        mTerminalBuffer.setSelection(mTerminalBuffer.getText().length());
        mIsWritingFromProcess = false;

        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}
