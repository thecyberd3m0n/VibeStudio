package com.vibestudio.app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class TerminalService extends Service {

    public interface OutputListener {
        void onOutput(String text);
    }

    public class LocalBinder extends Binder {
        public TerminalService getService() {
            return TerminalService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Process mProcess;
    private OutputStream mProcessInput;
    private InputStream mProcessOutput;
    private OutputListener mOutputListener;

    private DatabaseHelper mDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mDbHelper = new DatabaseHelper(this);
        startShellProcess();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setOutputListener(OutputListener listener) {
        mOutputListener = listener;
    }

    public void writeInput(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mProcessInput != null) {
                        mProcessInput.write(text.getBytes("UTF-8"));
                        if ("\n".equals(text)) {
                            mProcessInput.write("\n".getBytes("UTF-8"));
                        }
                        mProcessInput.flush();
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void startShellProcess() {
        String envHome = mDbHelper.getSetting("env_home");
        String envPrefix = mDbHelper.getSetting("env_prefix");

        try {
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");

            Map<String, String> env = pb.environment();
            if (!TextUtils.isEmpty(envHome)) env.put("HOME", envHome);
            if (!TextUtils.isEmpty(envPrefix)) env.put("PREFIX", envPrefix);
            env.put("PATH", (envPrefix != null ? envPrefix + "/bin:" : "") + "/system/bin:/system/xbin");
            env.put("TERM", "xterm-256color");

            if (!TextUtils.isEmpty(envHome) && new File(envHome).exists()) {
                pb.directory(new File(envHome));
            }

            pb.redirectErrorStream(true);
            mProcess = pb.start();

            mProcessInput = mProcess.getOutputStream();
            mProcessOutput = mProcess.getInputStream();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int length;
                    try {
                        while ((length = mProcessOutput.read(buffer)) != -1) {
                            final String text = new String(buffer, 0, length, "UTF-8");
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mOutputListener != null) {
                                        mOutputListener.onOutput(text);
                                    }
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }).start();

        } catch (Exception e) {
            if (mOutputListener != null) {
                mOutputListener.onOutput("[Error starting terminal service]: " + e.getMessage() + "\n");
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mProcess != null) {
            mProcess.destroy();
        }
        super.onDestroy();
    }
}
