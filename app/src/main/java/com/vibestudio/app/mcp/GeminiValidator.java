package com.vibestudio.app.mcp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vibestudio.app.service.LogViewerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiValidator {

    private static final String TAG = "GeminiValidator";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public interface ValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public static void validateKey(final String apiKey, final ValidationCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(GEMINI_API_URL + apiKey.trim());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setDoOutput(true);

                    JSONObject textPart = new JSONObject();
                    textPart.put("text", "ping");

                    JSONArray parts = new JSONArray();
                    parts.put(textPart);

                    JSONObject content = new JSONObject();
                    content.put("parts", parts);

                    JSONArray contents = new JSONArray();
                    contents.put(content);

                    JSONObject payload = new JSONObject();
                    payload.put("contents", contents);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    LogViewerService.getInstance().d(TAG, "Gemini API validation HTTP response code: " + responseCode);

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

                    String responseStr = responseSb.toString();

                    if (responseCode >= 200 && responseCode < 300) {
                        LogViewerService.getInstance().i(TAG, "Gemini API Key validated successfully!");
                        mainHandler.post(() -> callback.onSuccess());
                    } else {
                        String errorMsg = parseErrorMessage(responseStr, responseCode);
                        LogViewerService.getInstance().w(TAG, "Gemini API Key validation failed: " + errorMsg);
                        mainHandler.post(() -> callback.onError(errorMsg));
                    }

                } catch (Exception e) {
                    LogViewerService.getInstance().e(TAG, "Exception during Gemini API Key validation", e);
                    final String msg = "Network error: " + (e.getMessage() != null ? e.getMessage() : "Unable to reach Gemini API");
                    mainHandler.post(() -> callback.onError(msg));
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private static String parseErrorMessage(String rawResponse, int statusCode) {
        if (rawResponse != null && !rawResponse.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(rawResponse);
                if (json.has("error")) {
                    JSONObject errorObj = json.getJSONObject("error");
                    String message = errorObj.optString("message", null);
                    String status = errorObj.optString("status", null);

                    if ("API_KEY_INVALID".equals(status) || (message != null && message.contains("API key not valid"))) {
                        return "Invalid Gemini API Key. Please check your token.";
                    }
                    if ("RESOURCE_EXHAUSTED".equals(status) || (message != null && message.contains("quota"))) {
                        return "Quota exceeded or API not billed. Please check your Google AI account quota.";
                    }
                    if (message != null && !message.isEmpty()) {
                        return message;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        switch (statusCode) {
            case 400:
                return "Invalid API Key or request parameters (HTTP 400).";
            case 401:
            case 403:
                return "API Key unauthorized or permission denied (HTTP " + statusCode + "). Check billing/quota.";
            case 429:
                return "Rate limit or quota exceeded (HTTP 429).";
            default:
                return "Validation failed (HTTP " + statusCode + "). Check network or API Key.";
        }
    }
}
