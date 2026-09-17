package com.vibestudio.app.activity;

import com.vibestudio.app.R;
import com.vibestudio.app.db.DatabaseHelper;
import com.vibestudio.app.logging.CrashHandler;
import com.vibestudio.app.service.LogViewerService;

import com.libtermux.LibTermux;
import com.libtermux.TermuxConfig;
import com.libtermux.LogLevel;
import com.libtermux.bootstrap.InstallState;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class OnboardingActivity extends Activity {

    private static final String TAG = "OnboardingActivity";

    private int mCurrentStep = 1;
    private boolean mIsInstalling = false;
    private boolean mIsInstalled = false;

    private View mStep1Layout;
    private View mStep2Layout;
    private View mStep3Layout;

    private TextView mStep1Indicator;
    private TextView mStep2Indicator;
    private TextView mStep3Indicator;

    private TextView mStep1Title;
    private TextView mStep2Title;
    private TextView mStep3Title;

    private TextView mStatusMessage;
    private TextView mInstallLogText;
    private android.widget.ScrollView mInstallLogScroll;

    private Button mBtnBack;
    private Button mBtnNext;

    private Handler mHandler;
    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mDbHelper = new DatabaseHelper(this);

            if (mDbHelper.isEnvInitialized()) {
                String storedPrefix = mDbHelper.getSetting("env_prefix");
                File expectedUsrDir = new File(getFilesDir(), "libtermux/usr");
                if (storedPrefix != null && new File(storedPrefix).getAbsolutePath().equals(expectedUsrDir.getAbsolutePath())) {
                    navigateToMain();
                    return;
                }
                // Invalid or legacy path (e.g., files/usr), invalidate readiness flag so onboarding completes migration
                mDbHelper.setEnvInitialized(false);
            }

            setContentView(R.layout.activity_onboarding);

            mHandler = new Handler(Looper.getMainLooper());

            mStep1Layout = findViewById(R.id.step1_layout);
            mStep2Layout = findViewById(R.id.step2_layout);
            mStep3Layout = findViewById(R.id.step3_layout);

            mStep1Indicator = (TextView) findViewById(R.id.step1_indicator);
            mStep2Indicator = (TextView) findViewById(R.id.step2_indicator);
            mStep3Indicator = (TextView) findViewById(R.id.step3_indicator);

            mStep1Title = (TextView) findViewById(R.id.step1_title);
            mStep2Title = (TextView) findViewById(R.id.step2_title);
            mStep3Title = (TextView) findViewById(R.id.step3_title);

            mStatusMessage = (TextView) findViewById(R.id.status_message);
            mInstallLogText = (TextView) findViewById(R.id.install_log_text);
            mInstallLogScroll = (android.widget.ScrollView) findViewById(R.id.install_log_scroll);

            mBtnBack = (Button) findViewById(R.id.btn_back);
            mBtnNext = (Button) findViewById(R.id.btn_next);

            updateStepUi();

            mBtnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentStep < 3) {
                        mCurrentStep++;
                        updateStepUi();
                        if (mCurrentStep == 3 && !mIsInstalled && !mIsInstalling) {
                            startEnvironmentInstallation();
                        }
                    } else {
                        if (mIsInstalled) {
                            navigateToMain();
                        }
                    }
                }
            });

            mBtnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrentStep > 1 && !mIsInstalling) {
                        mCurrentStep--;
                        updateStepUi();
                    }
                }
            });
        } catch (Throwable t) {
            CrashHandler.getInstance().logError("OnboardingActivity", "Error in onCreate", t);
            throw t;
        }
    }

    private void updateStepUi() {
        mStep1Layout.setVisibility(mCurrentStep == 1 ? View.VISIBLE : View.GONE);
        mStep2Layout.setVisibility(mCurrentStep == 2 ? View.VISIBLE : View.GONE);
        mStep3Layout.setVisibility(mCurrentStep == 3 ? View.VISIBLE : View.GONE);

        updateIndicator(mStep1Indicator, mStep1Title, mCurrentStep == 1, mCurrentStep > 1);
        updateIndicator(mStep2Indicator, mStep2Title, mCurrentStep == 2, mCurrentStep > 2);
        updateIndicator(mStep3Indicator, mStep3Title, mCurrentStep == 3, mIsInstalled);

        mBtnBack.setVisibility(mCurrentStep > 1 && !mIsInstalling ? View.VISIBLE : View.INVISIBLE);

        if (mCurrentStep < 3) {
            mBtnNext.setText("Next");
            mBtnNext.setEnabled(true);
        } else {
            if (mIsInstalled) {
                mBtnNext.setText("Finish & Launch");
                mBtnNext.setEnabled(true);
            } else {
                mBtnNext.setText("Installing...");
                mBtnNext.setEnabled(false);
            }
        }
    }

    private void updateIndicator(TextView indicator, TextView title, boolean isActive, boolean isDone) {
        if (isActive) {
            indicator.setBackgroundResource(R.drawable.bg_step_circle_active);
            indicator.setTextColor(Color.WHITE);
            title.setTextColor(Color.WHITE);
            title.setTypeface(null, Typeface.BOLD);
        } else if (isDone) {
            indicator.setBackgroundResource(R.drawable.bg_step_circle_active);
            indicator.setTextColor(Color.WHITE);
            title.setTextColor(Color.parseColor("#AAAAAA"));
            title.setTypeface(null, Typeface.NORMAL);
        } else {
            indicator.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            indicator.setTextColor(Color.parseColor("#888888"));
            title.setTextColor(Color.parseColor("#666666"));
            title.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void appendLog(final String text) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInstallLogText.append(text + "\n");
                if (mInstallLogScroll != null) {
                    mInstallLogScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            mInstallLogScroll.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }

    private void setStatusMessage(final String message, final String colorHex) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mStatusMessage.setText(message);
                mStatusMessage.setTextColor(Color.parseColor(colorHex));
            }
        });
    }

    private void startEnvironmentInstallation() {
        mIsInstalling = true;
        mBtnBack.setVisibility(View.INVISIBLE);
        mBtnNext.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File filesDir = getFilesDir();
                    File usrDir = new File(filesDir, "libtermux/usr");
                    File homeDir = new File(filesDir, "libtermux/home");
                    File usrBin = new File(usrDir, "bin");
                    File bashFile = new File(usrBin, "bash");
                    File pkgFile = new File(usrBin, "pkg");

                    appendLog("[libtermux] Preparing APT environment structure...");
                    setupAptEnvironment(usrDir);
                    File aptConfFile = new File(usrDir, "etc/apt/apt.conf");

                    TermuxConfig config = TermuxConfig.Companion.builder()
                            .autoInstall(true)
                            .logLevel(LogLevel.DEBUG)
                            .addEnv("TERMUX_APP_PACKAGE_MANAGER", "apt")
                            .addEnv("TERMUX_MAIN_PACKAGE_FORMAT", "debian")
                            .addEnv("TERMUX_PKG_NO_MIRROR_SELECT", "1")
                            .addEnv("APT_CONFIG", aptConfFile.getAbsolutePath())
                            .build();

                    LibTermux libTermux = LibTermux.Companion.init(getApplicationContext(), config);

                    boolean isInstalled = libTermux.isInstalled();
                    boolean binariesExist = usrBin.exists() && bashFile.exists() && pkgFile.exists();

                    appendLog("[libtermux] Initializing installation (isInstalled=" + isInstalled + ", binariesExist=" + binariesExist + ")...");
                    boolean forceReinstall = !binariesExist;
                    Flow<InstallState> flow = libTermux.install(forceReinstall);

                    kotlinx.coroutines.BuildersKt.runBlocking(
                        Dispatchers.getIO(),
                        (scope, continuation) -> flow.collect(new FlowCollector<InstallState>() {
                            @Nullable
                            @Override
                            public Object emit(InstallState state, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
                                if (state instanceof InstallState.Downloading) {
                                    InstallState.Downloading d = (InstallState.Downloading) state;
                                    int pct = (int) (d.getProgress() * 100);
                                    appendLog("[libtermux] Downloading bootstrap: " + pct + "%");
                                    setStatusMessage("Downloading bootstrap: " + pct + "%", "#3B82F6");
                                } else if (state instanceof InstallState.Extracting) {
                                    InstallState.Extracting e = (InstallState.Extracting) state;
                                    int pct = (int) (e.getProgress() * 100);
                                    appendLog("[libtermux] Extracting bootstrap: " + pct + "%");
                                    setStatusMessage("Extracting bootstrap: " + pct + "%", "#3B82F6");
                                } else if (state instanceof InstallState.Completed) {
                                    appendLog("[libtermux] Bootstrap extraction completed.");
                                } else if (state instanceof InstallState.Failed) {
                                    InstallState.Failed f = (InstallState.Failed) state;
                                    appendLog("[error] Bootstrap installation failed: " + f.getError());
                                }
                                return kotlin.Unit.INSTANCE;
                            }
                        }, continuation)
                    );

                    appendLog("[libtermux] Overriding Termux hardcoded paths...");
                    overrideSTermuxPaths(usrDir, homeDir);

                    appendLog("[libtermux] Storing LibTermux settings in database...");
                    mDbHelper.setSetting("env_prefix", usrDir.getAbsolutePath());
                    mDbHelper.setSetting("env_home", homeDir.getAbsolutePath());
                    mDbHelper.setSetting("env_shell", bashFile.getAbsolutePath());
                    mDbHelper.setEnvInitialized(true);

                    appendLog("[libtermux] LibTermux Environment setup complete!");

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mIsInstalling = false;
                            mIsInstalled = true;
                            mStatusMessage.setText("LibTermux Linux Environment installed!");
                            mStatusMessage.setTextColor(Color.parseColor("#10B981"));
                            updateStepUi();
                        }
                    });

                } catch (Exception e) {
                    final String err = e.getMessage();
                    CrashHandler.getInstance().logError("OnboardingActivity", "Error during environment installation", e);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mIsInstalling = false;
                            mStatusMessage.setText("Installation error: " + err);
                            mStatusMessage.setTextColor(Color.parseColor("#EF4444"));
                            appendLog("[error] " + err);
                        }
                    });
                }
            }
        }).start();
    }

    private void overrideSTermuxPaths(File usrDir, File homeDir) {
        if (usrDir == null || !usrDir.exists() || !usrDir.isDirectory()) return;

        String targetUsrPrefix = usrDir.getAbsolutePath();
        String targetHomePrefix = (homeDir != null) ? homeDir.getAbsolutePath() : targetUsrPrefix.replace("/usr", "/home");

        String defaultTermuxUsr = "/data/data/com.termux/files/usr";
        String defaultTermuxHome = "/data/data/com.termux/files/home";

        int count = processDirectoryForTermuxPaths(usrDir, targetUsrPrefix, defaultTermuxUsr, targetHomePrefix, defaultTermuxHome, 0);
        LogViewerService.getInstance().i(TAG, "overrideSTermuxPaths completed. Overrode hardcoded termux paths in " + count + " files.");

        setupAptEnvironment(usrDir);
    }

    private void setupAptEnvironment(File usrDir) {
        try {
            File dpkgDir = new File(usrDir, "var/lib/dpkg");
            if (!dpkgDir.exists()) dpkgDir.mkdirs();

            new File(dpkgDir, "updates").mkdirs();
            new File(dpkgDir, "info").mkdirs();
            new File(dpkgDir, "triggers").mkdirs();
            new File(dpkgDir, "alternatives").mkdirs();

            File statusFile = new File(dpkgDir, "status");
            if (!statusFile.exists()) {
                statusFile.createNewFile();
            }

            File availableFile = new File(dpkgDir, "available");
            if (!availableFile.exists()) {
                availableFile.createNewFile();
            }

            File aptListsDir = new File(usrDir, "var/lib/apt/lists/partial");
            if (!aptListsDir.exists()) aptListsDir.mkdirs();

            File aptArchivesDir = new File(usrDir, "var/cache/apt/archives/partial");
            if (!aptArchivesDir.exists()) aptArchivesDir.mkdirs();

            File aptLogDir = new File(usrDir, "var/log/apt");
            if (!aptLogDir.exists()) aptLogDir.mkdirs();

            File aptEtcDir = new File(usrDir, "etc/apt");
            if (!aptEtcDir.exists()) aptEtcDir.mkdirs();

            new File(aptEtcDir, "apt.conf.d").mkdirs();
            new File(aptEtcDir, "preferences.d").mkdirs();
            new File(aptEtcDir, "sources.list.d").mkdirs();
            new File(aptEtcDir, "trusted.gpg.d").mkdirs();

            File aptConfFile = new File(aptEtcDir, "apt.conf");
            String aptConfContent = "Dir \"" + usrDir.getAbsolutePath() + "\";\n" +
                    "Dir::State \"" + new File(usrDir, "var/lib/apt").getAbsolutePath() + "\";\n" +
                    "Dir::State::status \"" + statusFile.getAbsolutePath() + "\";\n" +
                    "Dir::Cache \"" + new File(usrDir, "var/cache/apt").getAbsolutePath() + "\";\n" +
                    "Dir::Etc \"" + aptEtcDir.getAbsolutePath() + "\";\n" +
                    "Dir::Log \"" + aptLogDir.getAbsolutePath() + "\";\n" +
                    "Dir::Bin::methods \"" + new File(usrDir, "lib/apt/methods").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::solvers \"" + new File(usrDir, "lib/apt/solvers").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::solvers:: \"" + new File(usrDir, "lib/apt/solvers").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::planners \"" + new File(usrDir, "lib/apt/planners").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::planners:: \"" + new File(usrDir, "lib/apt/planners").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::dpkg \"" + new File(usrDir, "bin/dpkg").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::gzip \"" + new File(usrDir, "bin/gzip").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::bzip2 \"" + new File(usrDir, "bin/bzip2").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::xz \"" + new File(usrDir, "bin/xz").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::lz4 \"" + new File(usrDir, "bin/lz4").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::zstd \"" + new File(usrDir, "bin/zstd").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::lzma \"" + new File(usrDir, "bin/xz").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::apt-key \"" + new File(usrDir, "bin/apt-key").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::gpg \"" + new File(usrDir, "bin/gpg").getAbsolutePath() + "\";\n" +
                    "Dir::Bin::gpgv \"" + new File(usrDir, "bin/gpgv").getAbsolutePath() + "\";\n" +
                    "APT::System \"Debian dpkg interface\";\n" +
                    "APT::Get::AllowUnauthenticated \"true\";\n" +
                    "Acquire::AllowInsecureRepositories \"true\";\n" +
                    "Acquire::AllowDowngradeToInsecureRepositories \"true\";\n" +
                    "Acquire::https::Verify-Peer \"false\";\n" +
                    "Acquire::ssl::Verify-Peer \"false\";\n";

            java.nio.file.Files.write(aptConfFile.toPath(), aptConfContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            LogViewerService.getInstance().i(TAG, "Configured apt.conf at " + aptConfFile.getAbsolutePath());

            fixSourcesListFiles(usrDir);
        } catch (Exception e) {
            LogViewerService.getInstance().w(TAG, "Failed to setup APT environment", e);
        }
    }

    private void fixSourcesListFiles(File usrDir) {
        try {
            File aptEtcDir = new File(usrDir, "etc/apt");
            java.util.List<File> sourcesFiles = new java.util.ArrayList<>();
            File mainSources = new File(aptEtcDir, "sources.list");
            if (mainSources.exists()) sourcesFiles.add(mainSources);

            File sourcesListDir = new File(aptEtcDir, "sources.list.d");
            if (sourcesListDir.exists() && sourcesListDir.isDirectory()) {
                File[] listFiles = sourcesListDir.listFiles();
                if (listFiles != null) {
                    for (File f : listFiles) {
                        if (f.isFile() && f.getName().endsWith(".list")) {
                            sourcesFiles.add(f);
                        }
                    }
                }
            }

            for (File f : sourcesFiles) {
                String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                String[] lines = content.split("\n");
                StringBuilder sb = new StringBuilder();
                boolean modified = false;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("deb ") && !trimmed.contains("[trusted=yes]")) {
                        line = line.replaceFirst("deb\\s+", "deb [trusted=yes] ");
                        modified = true;
                    }
                    sb.append(line).append("\n");
                }
                if (modified) {
                    java.nio.file.Files.write(f.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    LogViewerService.getInstance().i(TAG, "Updated sources file with trusted=yes: " + f.getName());
                }
            }
        } catch (Exception e) {
            LogViewerService.getInstance().w(TAG, "Failed to fix sources list files", e);
        }
    }

    private int processDirectoryForTermuxPaths(File dir, String targetUsr, String defaultUsr, String targetHome, String defaultHome, int depth) {
        if (depth > 6) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            try {
                if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                    java.nio.file.Path targetPath = java.nio.file.Files.readSymbolicLink(file.toPath());
                    String targetStr = targetPath.toString();
                    boolean modified = false;
                    if (targetStr.contains(defaultUsr)) {
                        targetStr = targetStr.replace(defaultUsr, targetUsr);
                        modified = true;
                    }
                    if (targetStr.contains(defaultHome)) {
                        targetStr = targetStr.replace(defaultHome, targetHome);
                        modified = true;
                    }
                    if (modified) {
                        java.nio.file.Files.delete(file.toPath());
                        java.nio.file.Files.createSymbolicLink(file.toPath(), java.nio.file.Paths.get(targetStr));
                        count++;
                    }
                    continue;
                }
            } catch (Exception e) {
                LogViewerService.getInstance().w(TAG, "Failed to update symlink for " + file.getName(), e);
                continue;
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

    private void navigateToMain() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
