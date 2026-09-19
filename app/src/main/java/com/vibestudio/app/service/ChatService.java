package com.vibestudio.app.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vibestudio.app.db.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    private static final String TAG = "ChatService";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String SYSTEM_INSTRUCTION = "You are VibeStudio Assistant, an intelligent AI helper built into the VibeStudio Android IDE application. Assist the user with coding, project guidance, and general inquiries clearly and concisely.";

    private static ChatService sInstance;

    public static class ChatMessage {
        private final String sender;
        private final String text;
        private final boolean isUser;

        public ChatMessage(String sender, String text, boolean isUser) {
            this.sender = sender;
            this.text = text;
            this.isUser = isUser;
        }

        public String getSender() { return sender; }
        public String getText() { return text; }
        public boolean isUser() { return isUser; }
    }

    public interface OnChatMessageListener {
        void onMessageAdded(ChatMessage message);
        void onResponseLoading(boolean isLoading);
    }

    private final List<ChatMessage> mMessages = new ArrayList<>();
    private final List<OnChatMessageListener> mListeners = new ArrayList<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean mIsLoading = false;

    private ChatService() {
        // Initial static welcome message
        mMessages.add(new ChatMessage("Assistant", "Hello! Welcome to VibeStudio. How can I help you?", false));
    }

    public static synchronized ChatService getInstance() {
        if (sInstance == null) {
            sInstance = new ChatService();
        }
        return sInstance;
    }

    public synchronized List<ChatMessage> getMessages() {
        return new ArrayList<>(mMessages);
    }

    public synchronized boolean isLoading() {
        return mIsLoading;
    }

    public synchronized void addListener(OnChatMessageListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void removeListener(OnChatMessageListener listener) {
        mListeners.remove(listener);
    }

    public void sendMessage(final Context context, final String userText) {
        if (userText == null || userText.trim().isEmpty()) return;

        final String trimmedText = userText.trim();
        final ChatMessage userMsg = new ChatMessage("User", trimmedText, true);

        synchronized (this) {
            mMessages.add(userMsg);
            notifyMessageAdded(userMsg);
            mIsLoading = true;
            notifyLoading(true);
        }

        LogViewerService.getInstance().i(TAG, "User sent message: " + trimmedText);

        new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseHelper dbHelper = new DatabaseHelper(context.getApplicationContext());
                String apiKey = dbHelper.getApiKey("Gemini");

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    LogViewerService.getInstance().w(TAG, "No Gemini API key found in SQLite database");
                    postAssistantResponse("Error: Gemini API Key is not configured. Please set your API Key in the Models tab.");
                    return;
                }

                String[] candidateModels = new String[] {
                    DEFAULT_MODEL,
                    "gemini-2.5-pro",
                    "gemini-flash-latest",
                    "gemini-pro-latest"
                };

                String responseStr = "";
                int statusCode = 500;

                for (String model : candidateModels) {
                    responseStr = executeGeminiRequest(apiKey.trim(), model);
                    statusCode = getHttpStatusCode(responseStr);

                    if (statusCode != 404) {
                        break;
                    }
                    LogViewerService.getInstance().w(TAG, "Model " + model + " returned 404, attempting next fallback model...");
                }

                if (statusCode >= 200 && statusCode < 300) {
                    String reply = parseGeminiResponse(responseStr);
                    LogViewerService.getInstance().i(TAG, "Gemini reply received successfully.");
                    postAssistantResponse(reply);
                } else {
                    LogViewerService.getInstance().w(TAG, "Gemini Chat API error response: " + responseStr);
                    String errorReply = "Error (" + statusCode + "): " + parseErrorResponse(responseStr);
                    postAssistantResponse(errorReply);
                }
            }
        }).start();
    }

    private int getHttpStatusCode(String rawResult) {
        if (rawResult != null && rawResult.startsWith("HTTP_CODE:")) {
            try {
                int index = rawResult.indexOf("\n");
                if (index != -1) {
                    return Integer.parseInt(rawResult.substring(10, index).trim());
                }
            } catch (Exception ignored) {}
        }
        return 500;
    }

    private String executeGeminiRequest(String apiKey, String modelName) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            JSONArray contentsArray = new JSONArray();
            List<ChatMessage> currentHistory;
            synchronized (ChatService.this) {
                currentHistory = new ArrayList<>(mMessages);
            }

            for (ChatMessage msg : currentHistory) {
                String role = msg.isUser() ? "user" : "model";

                JSONObject textPart = new JSONObject();
                textPart.put("text", msg.getText());

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject contentObj = new JSONObject();
                contentObj.put("role", role);
                contentObj.put("parts", parts);

                contentsArray.put(contentObj);
            }

            JSONObject payload = new JSONObject();

            // System Instruction applied when sending conversation
            JSONObject sysTextPart = new JSONObject();
            sysTextPart.put("text", SYSTEM_INSTRUCTION);

            JSONArray sysParts = new JSONArray();
            sysParts.put(sysTextPart);

            JSONObject systemInstructionObj = new JSONObject();
            systemInstructionObj.put("parts", sysParts);

            payload.put("system_instruction", systemInstructionObj);
            payload.put("contents", contentsArray);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            LogViewerService.getInstance().d(TAG, "Gemini Chat API [" + modelName + "] HTTP response code: " + responseCode);

            InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder responseSb = new StringBuilder();
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseSb.append(line);
                    }
                }
            }

            return "HTTP_CODE:" + responseCode + "\n" + responseSb.toString();

        } catch (Exception e) {
            LogViewerService.getInstance().e(TAG, "Exception during Gemini Chat request for model " + modelName, e);
            return "HTTP_CODE:500\nNetwork error: " + (e.getMessage() != null ? e.getMessage() : "Unable to reach Gemini API");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void postAssistantResponse(final String reply) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ChatMessage assistantMsg = new ChatMessage("Assistant", reply, false);
                synchronized (ChatService.this) {
                    mMessages.add(assistantMsg);
                    mIsLoading = false;
                    notifyMessageAdded(assistantMsg);
                    notifyLoading(false);
                }
            }
        });
    }

    private String parseGeminiResponse(String rawResult) {
        try {
            int bodyIndex = rawResult.indexOf("\n");
            String rawJson = (bodyIndex != -1) ? rawResult.substring(bodyIndex + 1) : rawResult;
            JSONObject json = new JSONObject(rawJson);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCand = candidates.getJSONObject(0);
                JSONObject content = firstCand.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    JSONObject firstPart = parts.getJSONObject(0);
                    return firstPart.getString("text");
                }
            }
        } catch (Exception e) {
            LogViewerService.getInstance().e(TAG, "Error parsing Gemini response JSON", e);
        }
        return "No response text generated.";
    }

    private String parseErrorResponse(String rawResult) {
        try {
            int bodyIndex = rawResult.indexOf("\n");
            String rawJson = (bodyIndex != -1) ? rawResult.substring(bodyIndex + 1) : rawResult;
            JSONObject json = new JSONObject(rawJson);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                return err.optString("message", "Unknown error");
            }
        } catch (Exception ignored) {}
        return "Failed to process request.";
    }

    private void notifyMessageAdded(ChatMessage message) {
        for (OnChatMessageListener listener : new ArrayList<>(mListeners)) {
            listener.onMessageAdded(message);
        }
    }

    private void notifyLoading(boolean isLoading) {
        for (OnChatMessageListener listener : new ArrayList<>(mListeners)) {
            listener.onResponseLoading(isLoading);
        }
    }
}
