package com.codex.apk.util;

import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtils {
    private static final String TAG = "JsonUtils";

    public static String extractJsonFromCodeBlock(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // Look for ```json ... ``` pattern
        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
        Pattern pattern = Pattern.compile(jsonPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Also check for ``` ... ``` pattern (without json specifier)
        String genericPattern = "```\\s*([\\s\\S]*?)```";
        pattern = Pattern.compile(genericPattern);
        matcher = pattern.matcher(content);

        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            // Check if the extracted content looks like JSON
            if (looksLikeJson(extracted)) {
                return extracted;
            }
        }

        return null;
    }

    public static boolean looksLikeJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            Log.d(TAG, "looksLikeJson: response is null or empty");
            return false;
        }

        String trimmed = response.trim();
        boolean isJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                        (trimmed.startsWith("[") && trimmed.endsWith("]"));

        Log.d(TAG, "looksLikeJson: checking '" + trimmed.substring(0, Math.min(50, trimmed.length())) + "...'");
        return isJson;
    }
}