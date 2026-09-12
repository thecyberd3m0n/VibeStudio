package com.vibestudio.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.libtermux.LibTermux;
import com.libtermux.TermuxConfig;
import com.libtermux.LogLevel;
import com.libtermux.executor.Session;
import com.libtermux.executor.SessionEvent;
import com.libtermux.executor.SessionHandle;
import com.vibestudio.app.R;
import com.vibestudio.app.logging.CrashHandler;

import java.io.File;
import java.util.UUID;

import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.SupervisorKt;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;

public class TerminalFragment extends Fragment {

    private static final String TAG = "TerminalFragment";

    private com.libtermux.view.TerminalView mTerminalView;
    private LibTermux mLibTermux;
    private SessionHandle mSessionHandle;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        mTerminalView = view.findViewById(R.id.terminal_view);

        initLibTermuxSession();

        return view;
    }

    private void initLibTermuxSession() {
        Context context = getContext();
        if (context == null) return;

        new Thread(() -> {
            try {
                TermuxConfig config = TermuxConfig.Companion.builder()
                        .autoInstall(true)
                        .logLevel(LogLevel.DEBUG)
                        .build();
                mLibTermux = LibTermux.Companion.init(context.getApplicationContext(), config);

                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "libtermux/usr");
                File binDir = new File(usrDir, "bin");
                File homeDir = new File(filesDir, "libtermux/home");
                File tmpDir = new File(usrDir, "tmp");

                binDir.mkdirs();
                homeDir.mkdirs();
                tmpDir.mkdirs();

                // Clean up stale non-executable dummy script files from binDir if present
                File bashFile = new File(binDir, "bash");
                if (bashFile.exists()) {
                    bashFile.delete();
                }
                File shFile = new File(binDir, "sh");
                if (shFile.exists()) {
                    shFile.delete();
                }

                File marker = new File(filesDir, "libtermux/.bootstrap_ok");
                if (!marker.exists()) {
                    marker.createNewFile();
                }

                Session session = new Session(UUID.randomUUID().toString(), "main", System.currentTimeMillis(), true);
                MutableSharedFlow<SessionEvent> events = SharedFlowKt.MutableSharedFlow(0, 64, BufferOverflow.DROP_OLDEST);
                CoroutineScope scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getMain().plus(SupervisorKt.SupervisorJob(null)));

                mSessionHandle = new SessionHandle(session, scope, mLibTermux.getExecutor(), events);

                mHandler.post(() -> {
                    if (mTerminalView != null) {
                        mTerminalView.attachSession(mSessionHandle);
                        mTerminalView.appendText("LibTermux environment initialized\n", false);
                    }
                });

            } catch (Throwable t) {
                CrashHandler.getInstance().handleException(TAG, "LibTermux initialization failed", t);
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTerminalView != null) {
            mTerminalView.detach();
        }
    }

    public com.libtermux.view.TerminalView getTerminalView() {
        return mTerminalView;
    }
}
