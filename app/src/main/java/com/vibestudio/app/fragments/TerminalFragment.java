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
import com.vibestudio.app.service.LogViewerService;

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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startSession();
    }

    private boolean mIsPrepared = false;

    public void prepare() {
        if (mIsPrepared) {
            LogViewerService.getInstance().i(TAG, "Terminal environment already prepared");
            return;
        }

        Context context = getContext();
        if (context == null) return;

        LogViewerService.getInstance().i(TAG, "Preparing LibTermux environment handle...");

        try {
            File filesDir = context.getFilesDir();
            File usrDir = new File(filesDir, "libtermux/usr");
            File aptConfFile = new File(usrDir, "etc/apt/apt.conf");

            TermuxConfig config = TermuxConfig.Companion.builder()
                    .autoInstall(true)
                    .logLevel(LogLevel.DEBUG)
                    .addEnv("TERMUX_APP_PACKAGE_MANAGER", "apt")
                    .addEnv("TERMUX_MAIN_PACKAGE_FORMAT", "debian")
                    .addEnv("TERMUX_PKG_NO_MIRROR_SELECT", "1")
                    .addEnv("APT_CONFIG", aptConfFile.getAbsolutePath())
                    .build();
            mLibTermux = LibTermux.Companion.init(context.getApplicationContext(), config);
            mIsPrepared = true;
            LogViewerService.getInstance().i(TAG, "Terminal environment prepare completed.");
        } catch (Throwable t) {
            LogViewerService.getInstance().e(TAG, "LibTermux preparation failed", t);
            CrashHandler.getInstance().handleException(TAG, "LibTermux preparation failed", t);
        }
    }

    public void startSession() {
        Context context = getContext();
        if (context == null) return;

        new Thread(() -> {
            try {
                if (!mIsPrepared) {
                    prepare();
                }

                if (mSessionHandle == null && mLibTermux != null) {
                    Session session = new Session(UUID.randomUUID().toString(), "main", System.currentTimeMillis(), true);
                    MutableSharedFlow<SessionEvent> events = SharedFlowKt.MutableSharedFlow(0, 64, BufferOverflow.DROP_OLDEST);
                    CoroutineScope scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getMain().plus(SupervisorKt.SupervisorJob(null)));
                    mSessionHandle = new SessionHandle(session, scope, mLibTermux.getExecutor(), events);
                }

                mHandler.post(() -> {
                    if (mTerminalView != null && mSessionHandle != null) {
                        mTerminalView.attachSession(mSessionHandle);
                        mTerminalView.appendText("LibTermux environment ready\n", false);
                        LogViewerService.getInstance().i(TAG, "Session attached to TerminalView");
                    }
                });
            } catch (Throwable t) {
                LogViewerService.getInstance().e(TAG, "LibTermux startSession failed", t);
                CrashHandler.getInstance().handleException(TAG, "LibTermux startSession failed", t);
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
