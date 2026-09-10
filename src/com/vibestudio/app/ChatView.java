package com.vibestudio.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatView {

    private static final int MATCH_PARENT = -1;
    private static final int WRAP_CONTENT = -2;

    private final Context mContext;

    public ChatView(Context context) {
        mContext = context;
    }

    public View buildView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);

        final ScrollView chatScroll = new ScrollView(mContext);
        final LinearLayout chatContainer = new LinearLayout(mContext);
        chatContainer.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(chatContainer);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                MATCH_PARENT, 0, 1.0f);
        chatScroll.setLayoutParams(scrollParams);

        addChatMessage(chatContainer, "Assistant", "Hello! Welcome to VibeStudio. How can I assist with your project today?", false);
        addChatMessage(chatContainer, "User", "Can you build a Material Dark Android app?", true);
        addChatMessage(chatContainer, "Assistant", "Absolutely! VibeStudio is configured with a Material Dark theme and drawer navigation.", false);

        LinearLayout inputRow = new LinearLayout(mContext);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setPadding(0, 12, 0, 0);

        final EditText msgInput = new EditText(mContext);
        msgInput.setHint("Message VibeStudio...");
        msgInput.setHintTextColor(Color.parseColor("#666666"));
        msgInput.setTextColor(Color.parseColor("#FFFFFF"));
        msgInput.setBackgroundColor(Color.parseColor("#1E1E24"));
        msgInput.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams inParams = new LinearLayout.LayoutParams(
                0, WRAP_CONTENT, 1.0f);
        msgInput.setLayoutParams(inParams);

        Button btnSend = new Button(mContext);
        btnSend.setText("SEND");
        btnSend.setTextColor(Color.parseColor("#121212"));
        btnSend.setBackgroundColor(Color.parseColor("#03DAC6"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = msgInput.getText().toString();
                if (text.length() > 0) {
                    addChatMessage(chatContainer, "User", text, true);
                    msgInput.setText("");
                    addChatMessage(chatContainer, "Assistant", "Received: " + text, false);
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

    private void addChatMessage(LinearLayout container, String sender, String text, boolean isUser) {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 14, 18, 14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);

        if (isUser) {
            params.gravity = android.view.Gravity.RIGHT;
            card.setBackgroundColor(Color.parseColor("#3700B3"));
        } else {
            params.gravity = android.view.Gravity.LEFT;
            card.setBackgroundColor(Color.parseColor("#25252A"));
        }
        card.setLayoutParams(params);

        TextView tvSender = new TextView(mContext);
        tvSender.setText(sender);
        tvSender.setTextColor(isUser ? Color.parseColor("#03DAC6") : Color.parseColor("#BB86FC"));
        tvSender.setTextSize(12);
        tvSender.setTypeface(null, Typeface.BOLD);

        TextView tvText = new TextView(mContext);
        tvText.setText(text);
        tvText.setTextColor(Color.parseColor("#FFFFFF"));
        tvText.setTextSize(14);
        tvText.setPadding(0, 4, 0, 0);

        card.addView(tvSender);
        card.addView(tvText);
        container.addView(card);
    }
}
