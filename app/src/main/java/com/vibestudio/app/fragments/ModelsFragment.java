package com.vibestudio.app.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vibestudio.app.db.DatabaseHelper;

public class ModelsFragment extends Fragment {

    private static final String TAG = "ModelsFragment";
    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private DatabaseHelper mDbHelper;
    private TextView mStatusTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            mDbHelper = new DatabaseHelper(getActivity().getApplicationContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        if (context == null) return null;

        ScrollView scrollView = new ScrollView(context);
        final LinearLayout mainContainer = new LinearLayout(context);
        mainContainer.setOrientation(LinearLayout.VERTICAL);

        final String provider = "Gemini";

        LinearLayout card = createCard(context);

        TextView name = new TextView(context);
        name.setText("Google Gemini 1.5 Pro");
        name.setTextColor(Color.parseColor("#BB86FC"));
        name.setTextSize(18);
        name.setTypeface(null, Typeface.BOLD);

        TextView desc = new TextView(context);
        desc.setText("Google • High performance multimodal reasoning and long-context AI");
        desc.setTextColor(Color.parseColor("#B0B0B0"));
        desc.setTextSize(14);
        desc.setPadding(0, 8, 0, 8);

        mStatusTextView = new TextView(context);
        mStatusTextView.setTextSize(12);

        card.addView(name);
        card.addView(desc);
        card.addView(mStatusTextView);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiKeyDialog(context, provider);
            }
        });

        mainContainer.addView(card);
        scrollView.addView(mainContainer);

        updateStatusView(provider);

        return scrollView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatusView("Gemini");
    }

    private void updateStatusView(String provider) {
        if (mStatusTextView == null) return;

        String savedKey = mDbHelper != null ? mDbHelper.getApiKey(provider) : null;
        if (!TextUtils.isEmpty(savedKey)) {
            mStatusTextView.setText("Status: Configured (API Key set) • Click to edit");
            mStatusTextView.setTextColor(Color.parseColor("#03DAC6"));
            Log.d(TAG, provider + " API Key loaded from SQLite db");
        } else {
            mStatusTextView.setText("Status: Not configured (Click to set Gemini API Key)");
            mStatusTextView.setTextColor(Color.parseColor("#FFB74D"));
            Log.d(TAG, provider + " API Key is not set in SQLite db");
        }
    }

    private LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E1E"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(16, 16, 16, 0);
        card.setLayoutParams(params);
        return card;
    }

    private void showApiKeyDialog(final Context context, final String provider) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Configure " + provider + " API Key");

        final EditText input = new EditText(context);
        input.setHint("Enter " + provider + " API Key");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String currentKey = mDbHelper != null ? mDbHelper.getApiKey(provider) : "";
        if (!TextUtils.isEmpty(currentKey)) {
            input.setText(currentKey);
        }

        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                if (!TextUtils.isEmpty(key)) {
                    if (mDbHelper != null) {
                        mDbHelper.saveApiKey(provider, key);
                        Log.i(TAG, provider + " API Key successfully saved/updated in SQLite DB");
                    }
                    updateStatusView(provider);
                    Toast.makeText(context, provider + " API Key saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "API Key cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
