package com.vibestudio.app.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ChatFragment extends Fragment {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        if (context == null) return null;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final ScrollView chatScroll = new ScrollView(context);
        final LinearLayout chatContainer = new LinearLayout(context);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1.0f);
        chatScroll.setLayoutParams(scrollParams);

        addChatMessage(context, chatContainer, "Assistant", "Hello! Welcome to VibeStudio. How can I assist with your project today?", false);
        addChatMessage(context, chatContainer, "User", "Can you build a Material Dark Android app?", true);
        addChatMessage(context, chatContainer, "Assistant", "Absolutely! VibeStudio is configured with a Material Dark theme and drawer navigation.", false);

        LinearLayout inputRow = new LinearLayout(context);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText msgInput = new EditText(context);
        msgInput.setHint("Message VibeStudio...");
        msgInput.setHintTextColor(Color.parseColor("#666666"));
        msgInput.setTextColor(Color.parseColor("#FFFFFF"));
        msgInput.setBackgroundColor(Color.parseColor("#1E1E24"));
        msgInput.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.0f);
        msgInput.setLayoutParams(inParams);

        Button btnSend = new Button(context);
        btnSend.setText("SEND");
        btnSend.setTextColor(Color.parseColor("#121212"));
        btnSend.setBackgroundColor(Color.parseColor("#03DAC6"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = msgInput.getText().toString();
                if (text.length() > 0) {
                    addChatMessage(context, chatContainer, "User", text, true);
                    msgInput.setText("");
                    addChatMessage(context, chatContainer, "Assistant", "Received: " + text, false);
                    chatScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        });

        inputRow.addView(msgInput);
        inputRow.addView(btnSend);

        layout.addView(chatScroll);
        layout.addView(inputRow);
        return layout;
    }

    private void addChatMessage(Context context, LinearLayout container, String sender, String text, boolean isUser) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 14, 18, 14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);

        if (isUser) {
            params.gravity = android.view.Gravity.RIGHT;
            card.setBackgroundColor(Color.parseColor("#3700B3"));
        } else {
            params.gravity = android.view.Gravity.LEFT;
            card.setBackgroundColor(Color.parseColor("#25252A"));
        }
        card.setLayoutParams(params);

        TextView tvSender = new TextView(context);
        tvSender.setText(sender);
        tvSender.setTextColor(isUser ? Color.parseColor("#03DAC6") : Color.parseColor("#BB86FC"));
        tvSender.setTextSize(12);
        tvSender.setTypeface(null, Typeface.BOLD);

        TextView tvText = new TextView(context);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#FFFFFF"));
        tvText.setTextSize(14);
        tvText.setPadding(0, 4, 0, 0);

        card.addView(tvSender);
        card.addView(tvText);
        container.addView(card);
    }
}
