package com.vibestudio.app;

import android.app.Application;
import com.vibestudio.app.logging.CrashHandler;

public class VibeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
