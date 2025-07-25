package com.codex.apk;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class GeminiParser {
	private static final String TAG = "GeminiParser";
	private final Context context;
	
	public GeminiParser(Context context) {
		this.context = context;
	}
	
	public String parseGeminiResponse(String responseBodyString) throws JsonParseException {
		try {
			JsonObject jsonResponse = JsonParser.parseString(responseBodyString).getAsJsonObject();
			
			if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
				JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
				if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
					JsonArray responseParts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
					if (responseParts.size() > 0 && responseParts.get(0).getAsJsonObject().has("text")) {
						String responseText = responseParts.get(0).getAsJsonObject().get("text").getAsString();
						
						// Enhanced JSON extraction
						if (responseText.contains("\"actions\"") && responseText.contains("\"explanation\"")) {
							int start = responseText.indexOf("{");
							int end = responseText.lastIndexOf("}");
							if (start != -1 && end != -1 && end > start) {
								return responseText.substring(start, end + 1);
							}
						}
						return responseText;
					}
				}
			}
			throw new JsonParseException("AI response format is invalid or missing required fields.");
		} catch (JsonParseException e) {
			Log.e(TAG, "Failed to parse Gemini response", e);
			throw e;
		}
	}
}
