package com.vibestudio.app.mcp;

import android.os.Handler;
import android.os.Looper;

import com.vibestudio.app.service.LogViewerService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiValidator {

    private static final String TAG = "GeminiValidator";

    public interface ValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private static String censorToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***CENSORED***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    public static void validateKey(final String apiKey, final ValidationCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                String censored = censorToken(apiKey);
                LogViewerService.getInstance().i(TAG, "Starting key validation (GET /v1beta/models) for token [" + censored + "]...");

                String responseStr = executeGetModelsRequest(apiKey.trim());
                int statusCode = getHttpStatusCode(responseStr);

                if (statusCode >= 200 && statusCode < 300) {
                    LogViewerService.getInstance().i(TAG, "Gemini API Key [" + censored + "] validated successfully!");
                    mainHandler.post(() -> callback.onSuccess());
                } else {
                    int bodyIdx = responseStr.indexOf("\n");
                    String body = (bodyIdx != -1) ? responseStr.substring(bodyIdx + 1) : responseStr;
                    String errorMsg = parseErrorMessage(body, statusCode);
                    LogViewerService.getInstance().w(TAG, "Gemini API Key [" + censored + "] validation failed: " + errorMsg);
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            }
        }).start();
    }

    private static int getHttpStatusCode(String rawResult) {
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

    private static String executeGetModelsRequest(String apiKey) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
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
            return "HTTP_CODE:500\nNetwork error: " + (e.getMessage() != null ? e.getMessage() : "Unable to reach Gemini API");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String parseErrorMessage(String rawResponse, int statusCode) {
        if (rawResponse != null && !rawResponse.trim().isEmpty()) {
            try {
                org.json.JSONObject json = new org.json.JSONObject(rawResponse);
                if (json.has("error")) {
                    org.json.JSONObject errorObj = json.getJSONObject("error");
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
