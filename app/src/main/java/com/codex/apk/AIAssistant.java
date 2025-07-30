package com.codex.apk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAssistant {
	public enum AIModel {
		// Latest Gemini 2.x Models
		GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash"),
		GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite"),
		GEMINI_2_5_PRO("gemini-2.5-pro", "Gemini 2.5 Pro"),
		GEMINI_2_0_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash"),
		GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental"),
		GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite"),
		GEMINI_2_0_FLASH_THINKING("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking"),
		
		// Gemini 1.5 Models
		GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash"),
		GEMINI_1_5_FLASH_8B("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B"),
		GEMINI_1_5_FLASH_002("gemini-1.5-flash-002", "Gemini 1.5 Flash 002"),
		GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro"),
		GEMINI_1_5_PRO_002("gemini-1.5-pro-002", "Gemini 1.5 Pro 002"),
		
		// Gemini 1.0 Models
		GEMINI_1_0_PRO("gemini-1.0-pro", "Gemini 1.0 Pro"),
		GEMINI_1_0_PRO_VISION("gemini-1.0-pro-vision", "Gemini 1.0 Pro Vision"),
		
		// DeepSeek Models for comparison
		DEEPSEEK_R1("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "Deepseek R1");

		private final String modelId;
		private final String displayName;

		AIModel(String modelId, String displayName) {
			this.modelId = modelId;
			this.displayName = displayName;
		}

		public String getModelId() {
			return modelId;
		}

		public String getDisplayName() {
			return displayName;
		}

		/**
		 * Returns a list of all AI model display names.
		 * @return A List of String containing all display names.
		 */
		public static List<String> getAllDisplayNames() {
			List<String> displayNames = new ArrayList<>();
			for (AIModel model : AIModel.values()) {
				displayNames.add(model.getDisplayName());
			}
			return displayNames;
		}

		/**
		 * Returns the AIModel enum constant corresponding to the given display name.
		 * @param displayName The display name of the AI model.
		 * @return The AIModel enum constant, or null if no match is found.
		 */
		public static AIModel fromDisplayName(String displayName) {
			for (AIModel model : AIModel.values()) {
				if (model.getDisplayName().equals(displayName)) {
					return model;
				}
			}
			return null;
		}
		
		/**
		 * Get model capabilities and features description
		 * @return String describing the model's key capabilities
		 */
		public String getCapabilities() {
			switch (this) {
				case GEMINI_2_5_FLASH:
					return "Latest & fastest • Thinking mode • Code execution • 1M context";
				case GEMINI_2_5_FLASH_LITE:
					return "Ultra-fast • Lightweight • Optimized for speed";
				case GEMINI_2_5_PRO:
					return "Most capable • Advanced reasoning • Multimodal excellence";
				case GEMINI_2_0_FLASH:
					return "Fast & efficient • Multimodal • Function calling";
				case GEMINI_2_0_FLASH_EXP:
					return "Experimental features • Latest improvements • Beta access";
				case GEMINI_2_0_FLASH_LITE:
					return "Lightweight • Fast responses • Mobile optimized";
				case GEMINI_2_0_FLASH_THINKING:
					return "Chain-of-thought • Reasoning transparency • Problem solving";
				case GEMINI_1_5_FLASH:
					return "Balanced • Reliable • Production ready";
				case GEMINI_1_5_FLASH_8B:
					return "Compact • Efficient • Lower resource usage";
				case GEMINI_1_5_FLASH_002:
					return "Enhanced version • Improved accuracy • Stable";
				case GEMINI_1_5_PRO:
					return "Professional • Complex tasks • High accuracy";
				case GEMINI_1_5_PRO_002:
					return "Enhanced Pro • Better performance • Latest fixes";
				case GEMINI_1_0_PRO:
					return "Stable • Text generation • Code assistance";
				case GEMINI_1_0_PRO_VISION:
					return "Vision capabilities • Image understanding • Multimodal";
				case DEEPSEEK_R1:
					return "Reasoning focused • Mathematical • Alternative choice";
				default:
					return "Advanced AI capabilities";
			}
		}
		
		/**
		 * Check if model supports vision/image understanding
		 * @return true if model supports vision
		 */
		public boolean supportsVision() {
			switch (this) {
				case GEMINI_2_5_FLASH:
				case GEMINI_2_5_PRO:
				case GEMINI_2_0_FLASH:
				case GEMINI_2_0_FLASH_EXP:
				case GEMINI_1_5_FLASH:
				case GEMINI_1_5_FLASH_002:
				case GEMINI_1_5_PRO:
				case GEMINI_1_5_PRO_002:
				case GEMINI_1_0_PRO_VISION:
					return true;
				default:
					return false;
			}
		}
		
		/**
		 * Check if model supports function calling
		 * @return true if model supports function calling
		 */
		public boolean supportsFunctionCalling() {
			switch (this) {
				case GEMINI_2_5_FLASH:
				case GEMINI_2_5_PRO:
				case GEMINI_2_0_FLASH:
				case GEMINI_2_0_FLASH_EXP:
				case GEMINI_1_5_FLASH:
				case GEMINI_1_5_FLASH_002:
				case GEMINI_1_5_PRO:
				case GEMINI_1_5_PRO_002:
					return true;
				default:
					return false;
			}
		}
	}


	private AIModel currentModel = AIModel.GEMINI_2_5_FLASH;
	private String apiKey;

	private static final String TAG = "AIAssistant";
	private final OkHttpClient httpClient;
	private final ExecutorService executorService;
	private final Context context;
	private final File projectDir;
	private final String projectName;
	private final AIActionListener listener;
	private final GeminiParser geminiParser;
	private final DeepseekParser deepseekParser;
	
	private final FileManager fileManager;

	// Store current file info used for the prompt to pass to AiProcessor
	private String currentFileNameForPromptContext;
	private String currentFileContentForPromptContext;
    private boolean isProcessing = false;

    public boolean isProcessing() {
        return isProcessing;
    }

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public AIModel getCurrentModel() {
		return currentModel;
	}

	public void setCurrentModel(AIModel model) {
		this.currentModel = model;
	}

	public interface AIActionListener {
		// Updated signature to pass raw JSON and proposedFileChanges
		void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName);
		void onAiError(String errorMessage);
		void onAiRequestStarted();
		void onAiRequestCompleted();
		void onAiContextBuildingStarted();
		void onAiContextBuildingCompleted();
	}

	public AIAssistant(Context context, String initialApiKey, File projectDir, String projectName,
					   ExecutorService executorService, AIActionListener listener) {
		this.context = context;
		this.apiKey = initialApiKey;
		this.httpClient = new OkHttpClient.Builder()
				.connectTimeout(300, TimeUnit.SECONDS)
				.readTimeout(1200, TimeUnit.SECONDS)
				.writeTimeout(300, TimeUnit.SECONDS)
				.build();
		this.executorService = executorService;
		this.projectDir = projectDir;
		this.projectName = projectName;
		this.listener = listener;
		this.fileManager = new FileManager(context, projectDir);
		this.geminiParser = new GeminiParser(context);
		this.deepseekParser = new DeepseekParser(context);
	}

	/**
	 * Cleans up resources when the assistant is no longer needed.
	 */
	public void shutdown() {
		// ExecutorService shutdown is handled by EditorActivity
	}

	private String getApiUrl() {
		switch (currentModel) {
			case GEMINI_2_5_FLASH:
			case GEMINI_2_5_FLASH_LITE:
			case GEMINI_2_5_PRO:
			case GEMINI_2_0_FLASH:
			case GEMINI_2_0_FLASH_EXP:
			case GEMINI_2_0_FLASH_LITE:
			case GEMINI_2_0_FLASH_THINKING:
			case GEMINI_1_5_FLASH:
			case GEMINI_1_5_FLASH_8B:
			case GEMINI_1_5_FLASH_002:
			case GEMINI_1_5_PRO:
			case GEMINI_1_5_PRO_002:
			case GEMINI_1_0_PRO:
			case GEMINI_1_0_PRO_VISION:
				return "https://generativelanguage.googleapis.com/v1beta/models/" +
						currentModel.getModelId() + ":generateContent?key=" + apiKey;
			case DEEPSEEK_R1:
				return "https://router.huggingface.co/nscale/v1/chat/completions";
			
			default:
				throw new IllegalStateException("Unknown model: " + currentModel);
		}
	}

	public void sendPrompt(String userPrompt, String currentFileName, String currentFileContent) {
		this.currentFileNameForPromptContext = currentFileName;
		this.currentFileContentForPromptContext = currentFileContent;

		if ((currentModel == AIModel.GEMINI_2_5_FLASH || currentModel == AIModel.GEMINI_2_5_FLASH_LITE || currentModel == AIModel.GEMINI_2_5_PRO || currentModel == AIModel.GEMINI_2_0_FLASH || currentModel == AIModel.GEMINI_2_0_FLASH_EXP || currentModel == AIModel.GEMINI_2_0_FLASH_LITE || currentModel == AIModel.GEMINI_2_0_FLASH_THINKING || currentModel == AIModel.GEMINI_1_5_FLASH || currentModel == AIModel.GEMINI_1_5_FLASH_8B || currentModel == AIModel.GEMINI_1_5_FLASH_002 || currentModel == AIModel.GEMINI_1_5_PRO || currentModel == AIModel.GEMINI_1_5_PRO_002 || currentModel == AIModel.GEMINI_1_0_PRO || currentModel == AIModel.GEMINI_1_0_PRO_VISION || currentModel == AIModel.DEEPSEEK_R1) &&
				(apiKey == null || apiKey.isEmpty())) {
			String errorMsg = currentModel.getDisplayName() + " API key/token not configured";
			if (listener != null) listener.onAiError(errorMsg);
			return;
		}

		isProcessing = true;
		if (listener != null) {
			listener.onAiRequestStarted();
			listener.onAiContextBuildingStarted();
		}

		executorService.execute(() -> {
			try {
				String contextPrompt = buildEnhancedContext(userPrompt, this.currentFileNameForPromptContext, this.currentFileContentForPromptContext);
				if (listener != null) listener.onAiContextBuildingCompleted();

				JsonObject requestBody;
				switch (currentModel) {
					case GEMINI_2_5_FLASH:
					case GEMINI_2_5_FLASH_LITE:
					case GEMINI_2_5_PRO:
					case GEMINI_2_0_FLASH:
					case GEMINI_2_0_FLASH_EXP:
					case GEMINI_2_0_FLASH_LITE:
					case GEMINI_2_0_FLASH_THINKING:
					case GEMINI_1_5_FLASH:
					case GEMINI_1_5_FLASH_8B:
					case GEMINI_1_5_FLASH_002:
					case GEMINI_1_5_PRO:
					case GEMINI_1_5_PRO_002:
					case GEMINI_1_0_PRO:
					case GEMINI_1_0_PRO_VISION:
						requestBody = buildGeminiRequestBody(contextPrompt);
						break;
					case DEEPSEEK_R1:
						requestBody = buildDeepseekRequestBody(context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE).getString("prompt_prefix", "") + contextPrompt);
						break;
					
					default:
						throw new IllegalStateException("Unknown model: " + currentModel);
				}

				RequestBody body = RequestBody.create(
						requestBody.toString(),
						MediaType.get("application/json; charset=utf-8")
				);

				Request.Builder requestBuilder = new Request.Builder()
						.url(getApiUrl())
						.post(body);

				
				Request request = requestBuilder.build();

				try (Response response = httpClient.newCall(request).execute()) {
					String responseBodyString = response.body() != null ?
							response.body().string() : "";

					if (!response.isSuccessful()) {
						handleErrorResponse(response, responseBodyString);
						return;
					}

					processSuccessfulResponse(responseBodyString);
				}
			} catch (Exception e) { // Catch generic Exception for broader error handling
				handleException(e);
			} finally {
				isProcessing = false;
				if (listener != null) listener.onAiRequestCompleted();
			}
		});
	}

	private void handleErrorResponse(Response response, String responseBody) {
		String errorMsg = "AI request failed: " + response.code();
		if (response.code() == 401) {
			errorMsg += " - Invalid API key/token for " + currentModel.getDisplayName() + ".";
		} else if (response.code() == 429) {
			errorMsg += " - Rate limit exceeded for " + currentModel.getDisplayName() + ".";
		} else if (response.code() == 400) {
			errorMsg += " - Bad request to " + currentModel.getDisplayName() + ". Details: " + response.message();
		} else {
			errorMsg += " - " + response.message();
		}

		Log.e(TAG, errorMsg + "\nResponse Body: " + responseBody);
		if (listener != null) listener.onAiError(errorMsg);
	}

	private void processSuccessfulResponse(String responseBodyString) {
		try {
			String aiJsonResponseText;

			switch (currentModel) {
				case GEMINI_2_5_FLASH:
				case GEMINI_2_5_FLASH_LITE:
				case GEMINI_2_5_PRO:
				case GEMINI_2_0_FLASH:
				case GEMINI_2_0_FLASH_EXP:
				case GEMINI_2_0_FLASH_LITE:
				case GEMINI_2_0_FLASH_THINKING:
				case GEMINI_1_5_FLASH:
				case GEMINI_1_5_FLASH_8B:
				case GEMINI_1_5_FLASH_002:
				case GEMINI_1_5_PRO:
				case GEMINI_1_5_PRO_002:
				case GEMINI_1_0_PRO:
				case GEMINI_1_0_PRO_VISION:
					aiJsonResponseText = geminiParser.parseGeminiResponse(responseBodyString);
					break;
				case DEEPSEEK_R1:
					aiJsonResponseText = deepseekParser.parseDeepseekResponse(responseBodyString);
					break;
				
				default:
					throw new IllegalStateException("Unknown model for response processing: " + currentModel);
			}

			JsonObject aiResponseJson;
			try {
				aiResponseJson = JsonParser.parseString(aiJsonResponseText).getAsJsonObject();
			} catch (JsonSyntaxException | IllegalStateException e) {
				Log.e(TAG, currentModel.getDisplayName() + " parser did not return a valid JSON object string. Response was: " + aiJsonResponseText, e);
				throw new JsonParseException(currentModel.getDisplayName() + " response after parsing was not a valid JSON object. " + e.getMessage());
			}

			// Extract explanation and suggestions
			String explanation = aiResponseJson.has("explanation") ? aiResponseJson.get("explanation").getAsString() : "No explanation provided.";
			List<String> suggestions = new ArrayList<>();
			if (aiResponseJson.has("suggestions")) {
				JsonArray suggestionsArray = aiResponseJson.getAsJsonArray("suggestions");
				for (int i = 0; i < suggestionsArray.size(); i++) {
					suggestions.add(suggestionsArray.get(i).getAsString());
				}
			}

			// --- Extract detailed proposed file changes ---
			List<ChatMessage.FileActionDetail> proposedFileChanges = new ArrayList<>();
			if (aiResponseJson.has("actions")) {
				JsonArray actionsArray = aiResponseJson.getAsJsonArray("actions");
				for (int i = 0; i < actionsArray.size(); i++) {
					JsonObject action = actionsArray.get(i).getAsJsonObject();
					String type = action.has("type") ? action.get("type").getAsString() : "unknown";
					String path = action.has("path") ? action.get("path").getAsString() : null;
					String oldPath = action.has("oldPath") ? action.get("oldPath").getAsString() : null;
					String newPath = action.has("newPath") ? action.get("newPath").getAsString() : null;
					String content = action.has("content") ? action.get("content").getAsString() : null; // For createFile, updateFile

					String oldContent = null;
					String newContent = null;
					File targetFile = null;

					// Determine oldContent and newContent for diffing
					if (path != null) {
						targetFile = new File(projectDir, path);
					} else if (oldPath != null) { // For rename
						targetFile = new File(projectDir, oldPath);
					}

					try {
						if (type.equals("createFile")) {
							oldContent = "";
							newContent = content;
						} else if (type.equals("deleteFile")) {
							// Check if file exists before reading
							if (targetFile != null && targetFile.exists()) {
								oldContent = fileManager.readFileContent(targetFile);
							} else {
								Log.w(TAG, "File to delete not found for diff generation: " + path);
								oldContent = "// File not found for deletion.";
							}
							newContent = "";
						} else if (type.equals("updateFile")) {
							// Check if file exists before reading
							if (targetFile != null && targetFile.exists()) {
								oldContent = fileManager.readFileContent(targetFile);
							} else {
								Log.w(TAG, "File to update not found for diff generation: " + path);
								oldContent = "// File not found for update.";
							}
							newContent = content;
						} else if (type.equals("renameFile")) {
							File oldFile = new File(projectDir, oldPath);
							if (oldFile.exists()) {
								oldContent = fileManager.readFileContent(oldFile);
							} else {
								Log.w(TAG, "Old file for rename not found for diff generation: " + oldPath);
								oldContent = "// Old file not found for rename.";
							}
							// For rename, content typically doesn't change, just the path.
							newContent = oldContent;
						}

						// Create FileActionDetail using the single comprehensive constructor
						// startLine, deleteCount, insertLines will be 0/null as modifyLines is removed.
						proposedFileChanges.add(new ChatMessage.FileActionDetail(
								type, path, oldPath, newPath, oldContent, newContent,
								0, 0, null
						));

					} catch (Exception e) {
						Log.e(TAG, "Error reading file content for diff generation: " + (path != null ? path : oldPath), e);
						// Add a placeholder detail if file content cannot be read
						proposedFileChanges.add(new ChatMessage.FileActionDetail(
								type, path != null ? path : (oldPath != null ? oldPath : "unknown_path"), null, null,
								"Error: Could not read file for diff.", "Error: Could not read file for diff.",
								0, 0, null
						));
					}
				}
			}

			// Notify listener with raw JSON and proposed file changes
			if (listener != null) {
				listener.onAiActionsProcessed(aiJsonResponseText, explanation, suggestions, proposedFileChanges, currentModel.getDisplayName());
			}

		} catch (JsonParseException e) {
			String errorMsg = "Failed to parse " + currentModel.getDisplayName() + " response: " + e.getMessage();
			Log.e(TAG, errorMsg + "\nOriginal Response Body: " + responseBodyString, e);
			if (listener != null) listener.onAiError(errorMsg);
		} catch (Exception e) {
			String errorMsg = "Unexpected error processing " + currentModel.getDisplayName() + " response: " + e.getMessage();
			Log.e(TAG, errorMsg + "\nOriginal Response Body: " + responseBodyString, e);
			if (listener != null) listener.onAiError(errorMsg);
		}
	}


	private JsonObject buildGeminiRequestBody(String prompt) {
		JsonObject requestBody = new JsonObject();

		JsonArray contents = new JsonArray();
		JsonObject contentItem = new JsonObject();

		JsonArray parts = new JsonArray();
		JsonObject textPart = new JsonObject();
		textPart.addProperty("text", prompt);
		parts.add(textPart);

		contentItem.add("parts", parts);
		contents.add(contentItem);
		requestBody.add("contents", contents);

		JsonObject generationConfig = new JsonObject();
		generationConfig.addProperty("temperature", 0.2);
		generationConfig.addProperty("maxOutputTokens", 8192);
		generationConfig.addProperty("topP", 0.9);
		generationConfig.addProperty("topK", 30);
		requestBody.add("generationConfig", generationConfig);

		JsonArray safetySettings = new JsonArray();
		addSafetySetting(safetySettings, "HARM_CATEGORY_HARASSMENT", "BLOCK_MEDIUM_AND_ABOVE");
		addSafetySetting(safetySettings, "HARM_CATEGORY_HATE_SPEECH", "BLOCK_MEDIUM_AND_ABOVE");
		addSafetySetting(safetySettings, "HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE");
		addSafetySetting(safetySettings, "HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE");
		requestBody.add("safetySettings", safetySettings);

		return requestBody;
	}

	private void addSafetySetting(JsonArray safetySettings, String category, String threshold) {
		JsonObject safetySetting = new JsonObject();
		safetySetting.addProperty("category", category);
		safetySetting.addProperty("threshold", threshold);
		safetySettings.add(safetySetting);
	}

	private JsonObject buildDeepseekRequestBody(String prompt) {
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", currentModel.getModelId());
		requestBody.addProperty("max_tokens", 8000);
		requestBody.addProperty("temperature", 0.1);
		requestBody.addProperty("top_p", 0.9);
		requestBody.addProperty("stream", false);

		JsonArray messages = new JsonArray();
		JsonObject message = new JsonObject();
		message.addProperty("role", "user");
		message.addProperty("content", prompt);
		messages.add(message);

		requestBody.add("messages", messages);
		return requestBody;
	}

	

	private String buildEnhancedContext(String userPrompt, String currentFileName, String currentFileContent) {
		StringBuilder contextBuilder = new StringBuilder();

		contextBuilder.append("ROLE: You are an expert AI coding assistant for a mobile web (HTML, CSS, JavaScript) development IDE.\n");
		contextBuilder.append("Your primary goal is to help the user by making precise code changes focused on HTML structure, CSS styling, and JavaScript functionality.\n");
		contextBuilder.append("PROJECT: '").append(projectName).append("'\n");
		contextBuilder.append("PROJECT_ROOT: ").append(projectDir.getAbsolutePath()).append("\n\n");

		if (currentFileName != null && !currentFileName.isEmpty()) {
			contextBuilder.append("=== CURRENT FILE CONTEXT (").append(currentFileName).append(") ===\n");
			contextBuilder.append("CONTENT:\n```\n")
					.append(currentFileContent != null ? currentFileContent : "// File content not available or file is empty.")
					.append("\n```\n\n");
		} else {
			contextBuilder.append("=== GENERAL PROJECT CONTEXT (NO SPECIFIC FILE OPEN) ===\n");
			contextBuilder.append("The user is likely referring to the project in general or the root directory.\n");
		}

		contextBuilder.append("=== USER REQUEST ===\n");
		contextBuilder.append(userPrompt).append("\n\n");

		contextBuilder.append("=== RESPONSE FORMAT AND ACTION GUIDELINES ===\n");
		contextBuilder.append("IMPORTANT: Respond with a single, valid JSON object. Do NOT include any text outside this JSON object.\n");
		contextBuilder.append("The JSON object MUST contain:\n");
		contextBuilder.append("- 'actions': An array of file operation objects. Can be empty if no file changes are needed.\n");
		contextBuilder.append("- 'explanation': A clear, concise explanation of the changes. This field is mandatory.\n");
		contextBuilder.append("- 'suggestions': (Optional) An array of strings with follow-up suggestions.\n\n");

		contextBuilder.append("GUIDELINES FOR ACTIONS:\n");
		contextBuilder.append("- Focus strictly on web development files: HTML, CSS, and JavaScript. Do not propose changes to other file types.\n");
		contextBuilder.append("- Paths: All 'path', 'oldPath', 'newPath' MUST be relative to project root (e.g., 'index.html', 'css/style.css', 'js/script.js') and NOT start with '/'.\n");
		contextBuilder.append("- Prefer 'updateFile' for all changes to existing files, providing the complete new content for the file.\n");
		contextBuilder.append("- If the request is general and no specific file is open, consider 'index.html' if it exists and is relevant.\n\n");


		contextBuilder.append("Supported action types in the 'actions' array:\n");
		contextBuilder.append("1. 'createFile': `{\"type\": \"createFile\", \"path\": \"path/to/newFile.ext\", \"content\": \"...\"}`\n");
		contextBuilder.append("2. 'updateFile': `{\"type\": \"updateFile\", \"path\": \"path/to/existingFile.ext\", \"content\": \"...\"}` (replaces entire file content)\n");
		contextBuilder.append("3. 'deleteFile': `{\"type\": \"deleteFile\", \"path\": \"path/to/fileOrDir\"}`\n");
		contextBuilder.append("4. 'renameFile': `{\"type\": \"renameFile\", \"oldPath\": \"old/path.ext\", \"newPath\": \"new/path.ext\"}`\n\n");

		contextBuilder.append("Example JSON response structure:\n");
		contextBuilder.append("{\n");
		contextBuilder.append("  \"actions\": [\n");
		contextBuilder.append("    {\"type\": \"updateFile\", \"path\": \"index.html\", \"content\": \"<!DOCTYPE html>\\n<html>\\n<head><title>New Title</title></head><body>Hello Web!</body></html>\"}\n");
		contextBuilder.append("  ],\n");
		contextBuilder.append("  \"explanation\": \"Updated 'index.html' with a new title and content.\",\n");
		contextBuilder.append("  \"suggestions\": [\"Review the changes in the preview.\", \"Add more CSS styling.\"]\n");
		contextBuilder.append("}\n");
		contextBuilder.append("Only output the JSON object itself.\n");

		return contextBuilder.toString();
	}

	private void handleException(Exception e) {
		Log.e(TAG, "Unhandled exception in AIAssistant: " + e.getMessage(), e);
		if (listener != null) listener.onAiError("Unexpected internal error: " + e.getMessage());
	}
}
