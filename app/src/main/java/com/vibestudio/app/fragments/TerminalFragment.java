package com.vibestudio.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.libtermux.LibTermux;
import com.libtermux.TermuxConfig;
import com.libtermux.LogLevel;
import com.libtermux.executor.SessionHandle;
import com.vibestudio.app.R;
import com.vibestudio.app.logging.CrashHandler;

import java.io.File;
import java.io.FileOutputStream;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class TerminalFragment extends Fragment {

    private static final String TAG = "TerminalFragment";

    private com.libtermux.view.TerminalView mTerminalView;
    private LibTermux mLibTermux;
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

                mLibTermux.getSessions().createSession("main", new Continuation<SessionHandle>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NonNull Object result) {
                        if (result instanceof SessionHandle) {
                            final SessionHandle session = (SessionHandle) result;
                            mHandler.post(() -> {
                                try {
                                    if (mTerminalView != null) {
                                        mTerminalView.attachSession(session);
                                        session.run("bash");
                                    }
                                } catch (Throwable t) {
                                    CrashHandler.getInstance().handleException(TAG, "Error attaching session", t);
                                }
                            });
                        } else if (result instanceof Throwable) {
                            Throwable t = (Throwable) result;
                            CrashHandler.getInstance().handleException(TAG, "Failed to create LibTermux session", t);
                        }
                    }
                });

            } catch (Throwable t) {
                CrashHandler.getInstance().handleException(TAG, "LibTermux initialization failed", t);
            }
        }).start();
    }

    public com.libtermux.view.TerminalView getTerminalView() {
        return mTerminalView;
    }
}
