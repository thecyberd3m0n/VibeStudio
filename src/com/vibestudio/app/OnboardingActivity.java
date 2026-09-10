package com.vibestudio.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class OnboardingActivity extends Activity {

    private int mCurrentStep = 1;

    private View mStep1Layout;
    private View mStep2Layout;
    private TextView mStep1Indicator;
    private TextView mStep2Indicator;
    private TextView mStep1Title;
    private TextView mStep2Title;
    private Button mBtnBack;
    private Button mBtnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        mStep1Layout = findViewById(R.id.step1_layout);
        mStep2Layout = findViewById(R.id.step2_layout);
        mStep1Indicator = (TextView) findViewById(R.id.step1_indicator);
        mStep2Indicator = (TextView) findViewById(R.id.step2_indicator);
        mStep1Title = (TextView) findViewById(R.id.step1_title);
        mStep2Title = (TextView) findViewById(R.id.step2_title);
        mBtnBack = (Button) findViewById(R.id.btn_back);
        mBtnNext = (Button) findViewById(R.id.btn_next);

        updateStepUi();

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentStep == 1) {
                    mCurrentStep = 2;
                    updateStepUi();
                } else {
                    navigateToMain();
                }
            }
        });

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentStep > 1) {
                    mCurrentStep--;
                    updateStepUi();
                }
            }
        });
    }

    private void updateStepUi() {
        if (mCurrentStep == 1) {
            mStep1Layout.setVisibility(View.VISIBLE);
            mStep2Layout.setVisibility(View.GONE);

            mStep1Indicator.setBackgroundResource(R.drawable.bg_step_circle_active);
            mStep1Indicator.setTextColor(Color.WHITE);
            mStep1Title.setTextColor(Color.WHITE);
            mStep1Title.setTypeface(null, Typeface.BOLD);

            mStep2Indicator.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            mStep2Indicator.setTextColor(Color.parseColor("#888888"));
            mStep2Title.setTextColor(Color.parseColor("#666666"));
            mStep2Title.setTypeface(null, Typeface.NORMAL);

            mBtnBack.setVisibility(View.INVISIBLE);
            mBtnNext.setText("Next");
        } else if (mCurrentStep == 2) {
            mStep1Layout.setVisibility(View.GONE);
            mStep2Layout.setVisibility(View.VISIBLE);

            mStep1Indicator.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            mStep1Indicator.setTextColor(Color.parseColor("#888888"));
            mStep1Title.setTextColor(Color.parseColor("#666666"));
            mStep1Title.setTypeface(null, Typeface.NORMAL);

            mStep2Indicator.setBackgroundResource(R.drawable.bg_step_circle_active);
            mStep2Indicator.setTextColor(Color.WHITE);
            mStep2Title.setTextColor(Color.WHITE);
            mStep2Title.setTypeface(null, Typeface.BOLD);

            mBtnBack.setVisibility(View.VISIBLE);
            mBtnNext.setText("Get Started");
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
