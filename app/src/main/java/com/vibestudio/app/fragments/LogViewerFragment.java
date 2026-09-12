package com.vibestudio.app.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vibestudio.app.R;
import com.vibestudio.app.service.LogViewerService;

public class LogViewerFragment extends Fragment implements LogViewerService.OnLogListener {

    private TextView mTvLogContent;
    private ScrollView mScrollView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private LogViewerService mLogService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_viewer, container, false);

        mTvLogContent = view.findViewById(R.id.tv_log_content);
        mScrollView = view.findViewById(R.id.log_scroll_view);
        Button btnCopy = view.findViewById(R.id.btn_copy_logs);
        Button btnClear = view.findViewById(R.id.btn_clear_logs);

        mLogService = LogViewerService.getInstance();
        mTvLogContent.setText(mLogService.getAllLogs());

        btnCopy.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("VibeStudio Logs", mTvLogContent.getText().toString());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClear.setOnClickListener(v -> mLogService.clear());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLogService != null) {
            mLogService.addListener(this);
            mTvLogContent.setText(mLogService.getAllLogs());
            scrollToBottom();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLogService != null) {
            mLogService.removeListener(this);
        }
    }

    @Override
    public void onLogAdded(String logEntry) {
        mHandler.post(() -> {
            if (mTvLogContent != null) {
                mTvLogContent.append(logEntry + "\n");
                scrollToBottom();
            }
        });
    }

    @Override
    public void onLogsCleared() {
        mHandler.post(() -> {
            if (mTvLogContent != null) {
                mTvLogContent.setText("");
            }
        });
    }

    private void scrollToBottom() {
        if (mScrollView != null) {
            mScrollView.post(() -> mScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
}
