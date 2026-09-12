package com.vibestudio.app.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
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

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private DatabaseHelper mDbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            mDbHelper = new DatabaseHelper(getActivity());
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
        final String savedKey = mDbHelper != null ? mDbHelper.getApiKey(provider) : null;

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

        final TextView status = new TextView(context);
        if (!TextUtils.isEmpty(savedKey)) {
            status.setText("Status: Configured (API Key set) • Click to edit");
            status.setTextColor(Color.parseColor("#03DAC6"));
        } else {
            status.setText("Status: Not configured (Click to set Gemini API Key)");
            status.setTextColor(Color.parseColor("#FFB74D"));
        }
        status.setTextSize(12);

        card.addView(name);
        card.addView(desc);
        card.addView(status);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiKeyDialog(context, provider, status);
            }
        });

        mainContainer.addView(card);
        scrollView.addView(mainContainer);
        return scrollView;
    }

    private LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1E1E24"));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);
        return card;
    }

    private void showApiKeyDialog(final Context context, final String provider, final TextView statusTextView) {
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
                    }
                    statusTextView.setText("Status: Configured (API Key set) • Click to edit");
                    statusTextView.setTextColor(Color.parseColor("#03DAC6"));
                    Toast.makeText(context, provider + " API Key saved to database!", Toast.LENGTH_SHORT).show();
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
