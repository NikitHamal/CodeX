package com.codex.apk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DeepseekParser {
    private static final String TAG = "DeepseekParser";
    private final Context context;

    public DeepseekParser(Context context) {
        this.context = context;
    }

    public String parseDeepseekResponse(String responseBody) {
        try {
            // Parse the JSON response
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Extract the assistant's message content
            if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
                JsonObject firstChoice = jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject();
                    
                if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    
                    if (message.has("content")) {
                        String content = message.get("content").getAsString().trim();
                        
                        // The content contains our JSON response - extract it
                        try {
                            // Remove any non-JSON content before parsing
                            int jsonStart = content.indexOf("{");
                            int jsonEnd = content.lastIndexOf("}") + 1;
                            if (jsonStart != -1 && jsonEnd != -1) {
                                String jsonContent = content.substring(jsonStart, jsonEnd);
                                JsonObject parsedContent = JsonParser.parseString(jsonContent).getAsJsonObject();
                                return jsonContent;
                            }
                        } catch (JsonSyntaxException e) {
                            Log.e(TAG, "Failed to parse content JSON", e);
                        }
                        
                        // Fallback to returning the raw content if JSON parsing fails
                        return content;
                    }
                }
            }
            
            // If we couldn't find the expected structure, log and return raw response
            Log.w(TAG, "Unexpected Deepseek response format");
            copyToClipboard("Deepseek Raw Response", responseBody);
            return responseBody;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Deepseek response", e);
            copyToClipboard("Deepseek Error Response", responseBody);
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