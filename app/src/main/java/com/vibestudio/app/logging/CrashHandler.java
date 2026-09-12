package com.vibestudio.app.logging;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.vibestudio.app.CrashActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;

    private Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {}

    public static synchronized CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String logText = saveCrashLog(thread, throwable);
        launchCrashActivity(logText);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    public void logError(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        writeLogToFile("ERROR", tag, message, throwable);
    }

    private String saveCrashLog(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String fullLog = "Thread: " + thread.getName() + " (ID: " + thread.getId() + ")\n" +
                         "Exception: " + throwable.toString() + "\n\n" +
                         "Stack Trace:\n" + stackTrace;

        writeLogToFile("CRASH", "UncaughtException", fullLog, null);
        return fullLog;
    }

    private void launchCrashActivity(String crashLog) {
        try {
            if (context != null) {
                Intent intent = new Intent(context, CrashActivity.class);
                intent.putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch CrashActivity", e);
        }
    }

    private void writeLogToFile(String level, String tag, String message, Throwable throwable) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            String timestamp = dateFormat.format(new Date());

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("[").append(timestamp).append("] [").append(level).append("] [").append(tag).append("]: ")
                      .append(message).append("\n");

            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                logBuilder.append(sw.toString()).append("\n");
            }

            // 1. Internal app storage file error.log
            if (context != null) {
                File internalLogFile = new File(context.getFilesDir(), "error.log");
                writeToFile(internalLogFile, logBuilder.toString(), true);
            }

            // 2. External/Shared Storage
            if (context != null) {
                File externalFilesDir = context.getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    File externalLogFile = new File(externalFilesDir, "error.log");
                    writeToFile(externalLogFile, logBuilder.toString(), true);
                }
            }

            // 3. Public Downloads directory
            try {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir != null && (downloadDir.exists() || downloadDir.mkdirs())) {
                    File publicLogFile = new File(downloadDir, "vibestudio_error.log");
                    writeToFile(publicLogFile, logBuilder.toString(), true);
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Log.e(TAG, "Error writing crash log to file", e);
        }
    }

    private synchronized void writeToFile(File file, String text, boolean append) {
        try (FileWriter writer = new FileWriter(file, append)) {
            writer.write(text);
            writer.flush();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write to file: " + file.getAbsolutePath(), e);
        }
    }
}
