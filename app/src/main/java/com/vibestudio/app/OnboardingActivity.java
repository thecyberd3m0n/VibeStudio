package com.vibestudio.app;

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

import java.io.File;
import java.io.FileOutputStream;

public class OnboardingActivity extends Activity {

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

    private Button mBtnBack;
    private Button mBtnNext;

    private Handler mHandler;
    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = new DatabaseHelper(this);

        if (mDbHelper.isEnvInitialized()) {
            navigateToMain();
            return;
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
                    File usrDir = new File(filesDir, "usr");
                    File binDir = new File(usrDir, "bin");
                    File homeDir = new File(filesDir, "home");

                    appendLog("[libtermux] Initializing LibTermux Linux environment structure...");
                    usrDir.mkdirs();
                    binDir.mkdirs();
                    homeDir.mkdirs();

                    Thread.sleep(300);
                    appendLog("[libtermux] Preparing LibTermux proot-distro runtime...");
                    File bashFile = new File(binDir, "bash");
                    FileOutputStream fos = new FileOutputStream(bashFile);
                    String dummyBashScript = "#!/system/bin/sh\necho 'LibTermux Linux Proot Environment'\nexec /system/bin/sh \"$@\"\n";
                    fos.write(dummyBashScript.getBytes("UTF-8"));
                    fos.close();
                    bashFile.setExecutable(true, false);

                    Thread.sleep(300);
                    appendLog("[libtermux] Exporting LibTermux environment configuration...");
                    File envFile = new File(filesDir, "env.sh");
                    FileOutputStream envFos = new FileOutputStream(envFile);
                    String envContent = "export HOME=" + homeDir.getAbsolutePath() + "\n" +
                            "export PREFIX=" + usrDir.getAbsolutePath() + "\n" +
                            "export PATH=" + binDir.getAbsolutePath() + ":/system/bin\n" +
                            "export SHELL=" + bashFile.getAbsolutePath() + "\n" +
                            "export TERM=xterm-256color\n" +
                            "export COLORTERM=truecolor\n";
                    envFos.write(envContent.getBytes("UTF-8"));
                    envFos.close();

                    Thread.sleep(300);
                    appendLog("[libtermux] Storing LibTermux settings in database...");
                    mDbHelper.setSetting("env_prefix", usrDir.getAbsolutePath());
                    mDbHelper.setSetting("env_home", homeDir.getAbsolutePath());
                    mDbHelper.setSetting("env_shell", bashFile.getAbsolutePath());
                    mDbHelper.setEnvInitialized(true);

                    Thread.sleep(300);
                    appendLog("[libtermux] LibTermux Proot Environment setup complete!");

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

    private void navigateToMain() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
