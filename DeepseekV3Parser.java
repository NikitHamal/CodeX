package com.codex.apk;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DeepseekV3Parser {
    private static final String TAG = "DeepseekV3Parser";
    private final Context context;

    public DeepseekV3Parser(Context context) {
        this.context = context;
    }

    public String parseDeepseekV3Response(String responseBody) {
        try {
            // Parse the JSON response
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Check if response is valid
            if (!jsonResponse.has("choices") || !jsonResponse.get("choices").isJsonArray()) {
                Log.w(TAG, "Unexpected Deepseek V3 response format - missing choices array");
                copyToClipboard("Deepseek V3 Raw Response", responseBody);
                return responseBody;
            }

            // Extract the assistant's message content
            JsonObject firstChoice = jsonResponse.getAsJsonArray("choices")
                .get(0).getAsJsonObject();
                
            if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
                Log.w(TAG, "Unexpected Deepseek V3 response format - missing message object");
                copyToClipboard("Deepseek V3 Raw Response", responseBody);
                return responseBody;
            }

            JsonObject message = firstChoice.getAsJsonObject("message");
            
            if (!message.has("content")) {
                Log.w(TAG, "Unexpected Deepseek V3 response format - missing content");
                copyToClipboard("Deepseek V3 Raw Response", responseBody);
                return responseBody;
            }

            String content = message.get("content").getAsString().trim();
            
            // The content may contain our JSON response - try to extract it
            try {
                // Look for JSON structure in the content
                int jsonStart = content.indexOf("{");
                int jsonEnd = content.lastIndexOf("}") + 1;
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    String jsonContent = content.substring(jsonStart, jsonEnd);
                    // Validate it's proper JSON
                    JsonParser.parseString(jsonContent);
                    return jsonContent;
                }
            } catch (JsonSyntaxException e) {
                Log.d(TAG, "Content doesn't contain valid JSON, returning full content");
            }
            
            // Fallback to returning the raw content if JSON parsing fails
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Deepseek V3 response", e);
            copyToClipboard("Deepseek V3 Error Response", responseBody);
            return responseBody;
        }
    }

    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Copied to clipboard: " + label);
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy to clipboard", e);
        }
    }
}