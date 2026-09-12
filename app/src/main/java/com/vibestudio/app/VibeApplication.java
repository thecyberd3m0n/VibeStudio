package com.vibestudio.app;

import android.app.Application;

import com.libtermux.utils.TermuxLogger;
import com.vibestudio.app.logging.CrashHandler;
import com.vibestudio.app.service.LogViewerService;

public class VibeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);

        LogViewerService logService = LogViewerService.getInstance();
        logService.startLogcatCapture();

        TermuxLogger.INSTANCE.setCustomLogger((level, tag, message, throwable) -> {
            if (throwable != null) {
                logService.e(tag, message, throwable);
            } else {
                logService.log(level, tag, message);
            }
            return null;
        });

        logService.i("VibeApplication", "VibeApplication initialized with full logcat capture & LibTermux logging");
    }
}
