package com.vibestudio.app.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.libtermux.LibTermux;
import com.libtermux.TermuxConfig;
import com.libtermux.LogLevel;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.vibestudio.app.R;
import com.vibestudio.app.logging.CrashHandler;
import com.vibestudio.app.service.LogViewerService;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class TerminalFragment extends Fragment {

    private static final String TAG = "TerminalFragment";

    private TerminalView mTerminalView;
    private LibTermux mLibTermux;
    private TerminalSession mTerminalSession;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsPrepared = false;

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
                    .autoInstall(false)
                    .logLevel(LogLevel.DEBUG)
                    .addEnv("TERMUX_APP_PACKAGE_MANAGER", "apt")
                    .addEnv("TERMUX_MAIN_PACKAGE_FORMAT", "debian")
                    .addEnv("TERMUX_PKG_NO_MIRROR_SELECT", "1")
                    .addEnv("APT_CONFIG", aptConfFile.getAbsolutePath())
                    .addEnv("DPKG_ADMINDIR", new File(usrDir, "var/lib/dpkg").getAbsolutePath())
                    .build();
            mLibTermux = LibTermux.Companion.init(context.getApplicationContext(), config);
            mIsPrepared = true;
            LogViewerService.getInstance().i(TAG, "Terminal environment prepare completed.");
        } catch (Throwable t) {
            LogViewerService.getInstance().e(TAG, "LibTermux preparation failed", t);
            CrashHandler.getInstance().handleException(TAG, "LibTermux preparation failed", t);
        }
    }

    private void fixPermissionsRecursively(File file) {
        if (file == null || !file.exists()) return;
        try {
            boolean isDir = file.isDirectory();
            boolean isExec = isDir || file.canExecute() ||
                    (file.getParentFile() != null && ("bin".equals(file.getParentFile().getName()) || "lib".equals(file.getParentFile().getName())));
            Os.chmod(file.getAbsolutePath(), isExec ? 0755 : 0644);

            if (isDir) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        fixPermissionsRecursively(child);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void showSoftKeyboard() {
        if (mTerminalView != null) {
            mTerminalView.requestFocus();
            mTerminalView.post(() -> {
                Context ctx = getContext();
                if (ctx != null) {
                    InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(mTerminalView, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
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

                // Recursively fix file/directory permissions for LibTermux USR tree so bash can read profile/etc files
                if (mLibTermux != null) {
                    File usrDir = mLibTermux.getVfs().getPrefixDir();
                    if (usrDir.exists()) {
                        fixPermissionsRecursively(usrDir);
                    }
                }

                if (mTerminalSession == null && mLibTermux != null) {
                    File bashFile = mLibTermux.getExecutor().resolveBinary("bash");
                    String shellPath = bashFile.exists() ? bashFile.getAbsolutePath() : "/system/bin/sh";

                    Map<String, String> envMap = mLibTermux.getVfs().buildEnv(Collections.emptyMap());
                    String[] envVars = new String[envMap.size()];
                    int idx = 0;
                    for (Map.Entry<String, String> entry : envMap.entrySet()) {
                        envVars[idx++] = entry.getKey() + "=" + entry.getValue();
                    }

                    String cwd = mLibTermux.getVfs().getHomeDir().getAbsolutePath();
                    String[] args = bashFile.exists() ? new String[]{"-bash"} : new String[]{shellPath};

                    TerminalSessionClient sessionClient = new TerminalSessionClient() {
                        @Override
                        public void setTerminalShellPid(TerminalSession session, int pid) {}

                        @Override
                        public void onTextChanged(TerminalSession changedSession) {
                            if (mTerminalView != null) mTerminalView.onScreenUpdated();
                        }

                        @Override
                        public void onTitleChanged(TerminalSession updatedSession) {}

                        @Override
                        public void onSessionFinished(TerminalSession finishedSession) {}

                        @Override
                        public void onCopyTextToClipboard(TerminalSession session, String text) {
                            if (text == null || text.isEmpty()) return;
                            Context context = getContext();
                            if (context != null) {
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text));
                                }
                            }
                        }

                        @Override
                        public void onPasteTextFromClipboard(TerminalSession session) {
                            Context context = getContext();
                            if (context != null && session != null) {
                                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (clipboard != null && clipboard.hasPrimaryClip()) {
                                    ClipData clip = clipboard.getPrimaryClip();
                                    if (clip != null && clip.getItemCount() > 0) {
                                        CharSequence pasteText = clip.getItemAt(0).coerceToText(context);
                                        if (pasteText != null) {
                                            session.getEmulator().paste(pasteText.toString());
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onBell(TerminalSession session) {}

                        @Override
                        public void onColorsChanged(TerminalSession session) {}

                        @Override
                        public void onTerminalCursorStateChange(boolean state) {}

                        @Override
                        public Integer getTerminalCursorStyle() { return null; }

                        @Override
                        public void logVerbose(String tag, String message) {
                            LogViewerService.getInstance().d(tag, message);
                        }

                        @Override
                        public void logDebug(String tag, String message) {
                            LogViewerService.getInstance().d(tag, message);
                        }

                        @Override
                        public void logInfo(String tag, String message) {
                            LogViewerService.getInstance().i(tag, message);
                        }

                        @Override
                        public void logWarn(String tag, String message) {
                            LogViewerService.getInstance().w(tag, message);
                        }

                        @Override
                        public void logError(String tag, String message) {
                            LogViewerService.getInstance().e(tag, message);
                        }

                        @Override
                        public void logStackTraceWithMessage(String tag, String message, Exception e) {
                            LogViewerService.getInstance().e(tag, message, e);
                        }

                        @Override
                        public void logStackTrace(String tag, Exception e) {
                            LogViewerService.getInstance().e(tag, "Terminal error", e);
                        }
                    };

                                        LogViewerService.getInstance().i(TAG, "Starting TerminalSession - Shell: " + shellPath + ", CWD: " + cwd);
                    mTerminalSession = new TerminalSession(
                            shellPath,
                            cwd,
                            args,
                            envVars,
                            10000,
                            sessionClient
                    );
                }

                mHandler.post(() -> {
                    if (mTerminalView != null && mTerminalSession != null) {
                        mTerminalView.setBackgroundColor(Color.parseColor("#1E1E2E"));
                        mTerminalView.setTerminalViewClient(new TerminalViewClient() {
                            @Override
                            public float onScale(float scale) { return 1.0f; }

                            @Override
                            public void onSingleTapUp(MotionEvent e) {
                                showSoftKeyboard();
                            }

                            @Override
                            public boolean shouldBackButtonBeMappedToEscape() { return false; }

                            @Override
                            public boolean shouldEnforceCharBasedInput() { return true; }

                            @Override
                            public boolean shouldUseCtrlSpaceWorkaround() { return false; }

                            @Override
                            public boolean isTerminalViewSelected() { return true; }

                            @Override
                            public void copyModeChanged(boolean copyMode) {}

                            @Override
                            public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) { return false; }

                            @Override
                            public boolean onKeyUp(int keyCode, KeyEvent e) { return false; }

                            @Override
                            public boolean onLongPress(MotionEvent event) { return false; }

                            @Override
                            public boolean readControlKey() { return false; }

                            @Override
                            public boolean readAltKey() { return false; }

                            @Override
                            public boolean readShiftKey() { return false; }

                            @Override
                            public boolean readFnKey() { return false; }

                            @Override
                            public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) { return false; }

                            @Override
                            public void onEmulatorSet() {}

                            @Override
                            public void logVerbose(String tag, String message) {
                                LogViewerService.getInstance().d(tag, message);
                            }

                            @Override
                            public void logDebug(String tag, String message) {
                                LogViewerService.getInstance().d(tag, message);
                            }

                            @Override
                            public void logInfo(String tag, String message) {
                                LogViewerService.getInstance().i(tag, message);
                            }

                            @Override
                            public void logWarn(String tag, String message) {
                                LogViewerService.getInstance().w(tag, message);
                            }

                            @Override
                            public void logError(String tag, String message) {
                                LogViewerService.getInstance().e(tag, message);
                            }

                            @Override
                            public void logStackTraceWithMessage(String tag, String message, Exception e) {
                                LogViewerService.getInstance().e(tag, message, e);
                            }

                            @Override
                            public void logStackTrace(String tag, Exception e) {
                                LogViewerService.getInstance().e(tag, "TerminalView error", e);
                            }
                        });

                        mTerminalView.attachSession(mTerminalSession);
                        showSoftKeyboard();
                        LogViewerService.getInstance().i(TAG, "Official TerminalSession attached to TerminalView");
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
        if (mTerminalSession != null) {
            mTerminalSession.finishIfRunning();
            mTerminalSession = null;
        }
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }
}
