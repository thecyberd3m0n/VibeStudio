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
import com.libtermux.bootstrap.InstallState;
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
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;
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


        private void overrideSTermuxPaths(File usrDir, File homeDir) {
        if (usrDir == null || !usrDir.exists() || !usrDir.isDirectory()) return;

        String targetUsrPrefix = usrDir.getAbsolutePath();
        String targetHomePrefix = (homeDir != null) ? homeDir.getAbsolutePath() : targetUsrPrefix.replace("/usr", "/home");

        String defaultTermuxUsr = "/data/data/com.termux/files/usr";
        String defaultTermuxHome = "/data/data/com.termux/files/home";

        int count = processDirectoryForTermuxPaths(usrDir, targetUsrPrefix, defaultTermuxUsr, targetHomePrefix, defaultTermuxHome, 0);
        LogViewerService.getInstance().i(TAG, "overrideSTermuxPaths completed. Overrode hardcoded termux paths in " + count + " files.");
    }

    private int processDirectoryForTermuxPaths(File dir, String targetUsr, String defaultUsr, String targetHome, String defaultHome, int depth) {
        if (depth > 6) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            try {
                if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                    continue;
                }
            } catch (Exception ignored) {
            }

            if (file.isDirectory()) {
                count += processDirectoryForTermuxPaths(file, targetUsr, defaultUsr, targetHome, defaultHome, depth + 1);
            } else if (file.isFile() && file.canRead() && file.length() < 2 * 1024 * 1024) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    boolean isCandidate = false;
                    if (bytes.length > 2 && bytes[0] == '#' && bytes[1] == '!') {
                        isCandidate = true;
                    } else if (bytes.length > 0) {
                        int checkLen = Math.min(bytes.length, 128);
                        isCandidate = true;
                        for (int i = 0; i < checkLen; i++) {
                            if (bytes[i] == 0) {
                                isCandidate = false;
                                break;
                            }
                        }
                    }

                    if (isCandidate) {
                        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                        boolean modified = false;
                        if (content.contains(defaultUsr)) {
                            content = content.replace(defaultUsr, targetUsr);
                            modified = true;
                        }
                        if (content.contains(defaultHome)) {
                            content = content.replace(defaultHome, targetHome);
                            modified = true;
                        }

                        if (modified) {
                            java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            if (file.getParentFile() != null) {
                                String parentName = file.getParentFile().getName();
                                if ("bin".equals(parentName) || "libexec".equals(parentName) || (bytes.length > 2 && bytes[0] == '#' && bytes[1] == '!')) {
                                    file.setExecutable(true, false);
                                }
                            }
                            count++;
                        }
                    }
                } catch (Exception e) {
                    LogViewerService.getInstance().w(TAG, "Failed to process path for " + file.getName(), e);
                }
            }
        }
        return count;
    }

    private void initLibTermuxSession() {
        Context context = getContext();
        if (context == null) return;

        LogViewerService.getInstance().i(TAG, "Initializing LibTermux session...");

        new Thread(() -> {
            try {
                TermuxConfig config = TermuxConfig.Companion.builder()
                        .autoInstall(true)
                        .logLevel(LogLevel.DEBUG)
                        .build();
                mLibTermux = LibTermux.Companion.init(context.getApplicationContext(), config);

                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "libtermux/usr");
                File usrBin = new File(usrDir, "bin");

                File bashFile = new File(usrBin, "bash");
                File pkgFile = new File(usrBin, "pkg");

                boolean isInstalled = mLibTermux.isInstalled();
                boolean binariesExist = usrBin.exists() && bashFile.exists() && pkgFile.exists();

                LogViewerService.getInstance().i(TAG, "LibTermux status - isInstalled: " + isInstalled +
                        ", binariesExist: " + binariesExist + " (bash: " + bashFile.exists() + ", pkg: " + pkgFile.exists() + ")");

                if (!isInstalled || !binariesExist) {
                    LogViewerService.getInstance().i(TAG, "Triggering bootstrap installation (forceReinstall=" + (!binariesExist) + ")...");
                    mHandler.post(() -> {
                        if (mTerminalView != null) {
                            mTerminalView.appendText("Installing Termux bootstrap environment...\n", false);
                        }
                    });

                    // Force reinstall if marker exists but binaries are missing
                    boolean forceReinstall = !binariesExist;
                    Flow<InstallState> flow = mLibTermux.install(forceReinstall);

                    kotlinx.coroutines.BuildersKt.runBlocking(
                        Dispatchers.getIO(),
                        (scope, continuation) -> flow.collect(new FlowCollector<InstallState>() {
                            @Nullable
                            @Override
                            public Object emit(InstallState state, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
                                LogViewerService.getInstance().d(TAG, "InstallState: " + state.getClass().getSimpleName());
                                mHandler.post(() -> {
                                    if (mTerminalView != null) {
                                        if (state instanceof InstallState.Downloading) {
                                            InstallState.Downloading d = (InstallState.Downloading) state;
                                            int pct = (int) (d.getProgress() * 100);
                                            mTerminalView.appendText("Downloading bootstrap: " + pct + "%\n", false);
                                        } else if (state instanceof InstallState.Extracting) {
                                            InstallState.Extracting e = (InstallState.Extracting) state;
                                            int pct = (int) (e.getProgress() * 100);
                                            mTerminalView.appendText("Extracting bootstrap: " + pct + "%\n", false);
                                        } else if (state instanceof InstallState.Completed) {
                                            mTerminalView.appendText("Bootstrap installation completed.\n", false);
                                            LogViewerService.getInstance().i(TAG, "Bootstrap installation completed successfully.");
                                        } else if (state instanceof InstallState.Failed) {
                                            InstallState.Failed f = (InstallState.Failed) state;
                                            mTerminalView.appendText("Bootstrap installation failed: " + f.getError() + "\n", false);
                                            LogViewerService.getInstance().e(TAG, "Bootstrap installation failed: " + f.getError(), f.getCause());
                                        }
                                    }
                                });
                                return kotlin.Unit.INSTANCE;
                            }
                        }, continuation)
                    );
                }

                File homeDir = new File(filesDir, "libtermux/home");
                overrideSTermuxPaths(usrDir, homeDir);

                Session session = new Session(UUID.randomUUID().toString(), "main", System.currentTimeMillis(), true);
                MutableSharedFlow<SessionEvent> events = SharedFlowKt.MutableSharedFlow(0, 64, BufferOverflow.DROP_OLDEST);
                CoroutineScope scope = CoroutineScopeKt.CoroutineScope(Dispatchers.getMain().plus(SupervisorKt.SupervisorJob(null)));

                mSessionHandle = new SessionHandle(session, scope, mLibTermux.getExecutor(), events);

                mHandler.post(() -> {
                    if (mTerminalView != null) {
                        mTerminalView.attachSession(mSessionHandle);
                        mTerminalView.appendText("LibTermux environment ready\n", false);
                        LogViewerService.getInstance().i(TAG, "Session attached to TerminalView");
                    }
                });

            } catch (Throwable t) {
                LogViewerService.getInstance().e(TAG, "LibTermux initialization failed", t);
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
