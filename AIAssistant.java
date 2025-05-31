package com.codex.apk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.codex.apk.codebase.FileChangeListener;
import com.codex.apk.codebase.FileMetadata;
import com.codex.apk.codebase.FileWatcher;
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

public class AIAssistant implements FileChangeListener { // Implement FileChangeListener
	public enum AIModel {
		GEMINI_2_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash"),
		DEEPSEEK_R1("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "Deepseek R1"),
		DEEPSEEK_V3("deepseek-v3-0324", "Deepseek V3 (Free)");
		
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
	}
	
	
	private AIModel currentModel = AIModel.GEMINI_2_FLASH;
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
	private final DeepseekV3Parser deepseekV3Parser;
	private final CodebaseIndexer codebaseIndexer;
	private final FileManager fileManager;
	private FileWatcher fileWatcher; // New FileWatcher instance
	
	// Store current file info used for the prompt to pass to AiProcessor
	private String currentFileNameForPromptContext;
	private String currentFileContentForPromptContext;
	
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
		// New methods for indexing progress
		void onIndexingStarted(int totalFiles);
		void onIndexingProgress(int indexedCount, int totalFiles, String currentFile);
		void onIndexingCompleted();
		void onIndexingError(String errorMessage);
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
		this.deepseekV3Parser = new DeepseekV3Parser(context);
		
		// Initialize CodebaseIndexer with a listener to report progress
		this.codebaseIndexer = new CodebaseIndexer(projectDir, fileManager, new CodebaseIndexer.IndexingListener() {
			@Override
			public void onIndexingStarted(int totalFiles) {
				if (AIAssistant.this.listener != null) {
					AIAssistant.this.listener.onIndexingStarted(totalFiles);
				}
			}

			@Override
			public void onIndexingProgress(int indexedCount, int totalFiles, String currentFile) {
				if (AIAssistant.this.listener != null) {
					AIAssistant.this.listener.onIndexingProgress(indexedCount, totalFiles, currentFile);
				}
			}

			@Override
			public void onIndexingCompleted() {
				if (AIAssistant.this.listener != null) {
					AIAssistant.this.listener.onIndexingCompleted();
				}
			}

			@Override
			public void onIndexingError(String errorMessage) {
				if (AIAssistant.this.listener != null) {
					AIAssistant.this.listener.onIndexingError(errorMessage);
				}
			}
		});
		
		// Start initial indexing
		executorService.execute(() -> {
			codebaseIndexer.indexProject();
			Log.d(TAG, "Initial project indexing completed");
		});

		// Initialize and start FileWatcher
		try {
			this.fileWatcher = new FileWatcher(projectDir, this); // 'this' refers to AIAssistant implementing FileChangeListener
			this.fileWatcher.start();
			Log.d(TAG, "FileWatcher initialized and started.");
		} catch (IOException e) {
			Log.e(TAG, "Failed to initialize FileWatcher: " + e.getMessage(), e);
			// Optionally notify listener about file watcher error
		}
	}

	/**
	 * Cleans up resources when the assistant is no longer needed.
	 */
	public void shutdown() {
		if (fileWatcher != null) {
			fileWatcher.stop();
			Log.d(TAG, "FileWatcher stopped during shutdown.");
		}
		// ExecutorService shutdown is handled by EditorActivity
	}
	
	private String getApiUrl() {
		switch (currentModel) {
			case GEMINI_2_FLASH:
			return "https://generativelanguage.googleapis.com/v1beta/models/" + 
			currentModel.getModelId() + ":generateContent?key=" + apiKey;
			case DEEPSEEK_R1:
			return "https://router.huggingface.co/nscale/v1/chat/completions";
			case DEEPSEEK_V3:
			return "https://api.paxsenix.biz.id/v1/chat/completions";
			default:
			throw new IllegalStateException("Unknown model: " + currentModel);
		}
	}
	
	public void sendPrompt(String userPrompt, String currentFileName, String currentFileContent) {
		this.currentFileNameForPromptContext = currentFileName;
		this.currentFileContentForPromptContext = currentFileContent;
		
		if ((currentModel == AIModel.GEMINI_2_FLASH || currentModel == AIModel.DEEPSEEK_R1) && 
		(apiKey == null || apiKey.isEmpty())) {
			String errorMsg = currentModel.getDisplayName() + " API key/token not configured";
			if (listener != null) listener.onAiError(errorMsg);
			// Removed Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
			return;
		}
		
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
					case GEMINI_2_FLASH:
					requestBody = buildGeminiRequestBody(contextPrompt);
					break;
					case DEEPSEEK_R1:
					requestBody = buildDeepseekRequestBody(contextPrompt);
					break;
					case DEEPSEEK_V3:
					requestBody = buildDeepseekV3RequestBody(contextPrompt);
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
				
				if (currentModel == AIModel.DEEPSEEK_R1) {
					requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
				}
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
				case GEMINI_2_FLASH:
				aiJsonResponseText = geminiParser.parseGeminiResponse(responseBodyString);
				break;
				case DEEPSEEK_R1:
				aiJsonResponseText = deepseekParser.parseDeepseekResponse(responseBodyString);
				break;
				case DEEPSEEK_V3:
				aiJsonResponseText = deepseekV3Parser.parseDeepseekV3Response(responseBodyString);
				break;
				default:
				throw new IllegalStateException("Unknown model for response processing: " + currentModel);
			}
			
			JsonObject aiResponseJson;
			try {
				aiResponseJson = JsonParser.parseString(aiJsonResponseText).getAsJsonObject();
			} catch (JsonSyntaxException | IllegalStateException e) {
				Log.e(TAG, currentModel.getDisplayName() + " parser did not return a valid JSON object string. Response was: " + aiJsonResponseText, e);
				copyToClipboard(currentModel.getDisplayName() + " - Invalid Parsed JSON", "Original API Response:\n" + responseBodyString + "\n\nPareser Output (Problematic):\n" + aiJsonResponseText);
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
					int startLine = action.has("startLine") ? action.get("startLine").getAsInt() : 0;
					int deleteCount = action.has("deleteCount") ? action.get("deleteCount").getAsInt() : 0;
					JsonArray insertLinesArray = action.has("insertLines") ? action.getAsJsonArray("insertLines") : null;
					
					List<String> insertLines = new ArrayList<>();
					if (insertLinesArray != null) {
						for (int j = 0; j < insertLinesArray.size(); j++) {
							insertLines.add(insertLinesArray.get(j).getAsString());
						}
					}
					
					String oldContent = null;
					String newContent = null;
					File targetFile = null;
					
					// Determine oldContent and newContent for diffing
					if (path != null) {
						targetFile = new File(projectDir, path);
					} else if (oldPath != null) { // For rename
						targetFile = new File(projectDir, oldPath);
					}
					
					try { // FIX: Changed to generic Exception to avoid unreachable catch
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
							// If new file exists (e.g., target of rename already exists, though it shouldn't be allowed by AI)
							// or if it's a rename of a directory, its content is effectively the old content.
							// For simplicity, for rename, we'll assume the content itself doesn't change, just the path.
							newContent = oldContent; // Content typically doesn't change on rename
						} else if (type.equals("modifyLines")) {
							String originalFileContent;
							if (targetFile != null && targetFile.exists()) {
								originalFileContent = fileManager.readFileContent(targetFile);
							} else {
								Log.w(TAG, "File to modify not found for diff generation: " + path);
								originalFileContent = "// File not found for modification.";
							}
							oldContent = originalFileContent;
							
							// Simulate the modification to get the new content
							List<String> lines = new ArrayList<>(Arrays.asList(originalFileContent.split("\\r?\\n", -1)));
							int actualStartLineIndex = startLine - 1;
							
							if (actualStartLineIndex >= 0 && actualStartLineIndex <= lines.size()) {
								// Remove lines
								for (int k = 0; k < deleteCount; k++) {
									if (actualStartLineIndex < lines.size()) {
										lines.remove(actualStartLineIndex);
									} else {
										Log.w(TAG, "Attempted to delete line beyond file end during diff simulation.");
										break;
									}
								}
								// Insert lines
								for (int k = 0; k < insertLines.size(); k++) {
									lines.add(actualStartLineIndex + k, insertLines.get(k));
								}
							}
							newContent = String.join("\n", lines);
						}
						
						// Create FileActionDetail using the single comprehensive constructor
						proposedFileChanges.add(new ChatMessage.FileActionDetail(
						type, path, oldPath, newPath, oldContent, newContent,
						startLine, deleteCount, insertLines
						));
						
					} catch (Exception e) { // FIX: Changed to generic Exception to avoid unreachable catch
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
			copyToClipboard(currentModel.getDisplayName() + " - JSON Parse Error", responseBodyString);
		} catch (Exception e) {
			String errorMsg = "Unexpected error processing " + currentModel.getDisplayName() + " response: " + e.getMessage();
			Log.e(TAG, errorMsg + "\nOriginal Response Body: " + responseBodyString, e);
			if (listener != null) listener.onAiError(errorMsg);
			copyToClipboard(currentModel.getDisplayName() + " - Processing Error", responseBodyString);
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
	
	private JsonObject buildDeepseekV3RequestBody(String prompt) {
		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("model", currentModel.getModelId());
		requestBody.addProperty("temperature", 0.1);
		requestBody.addProperty("max_tokens", 8000);
		
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
		
		contextBuilder.append("ROLE: You are an expert AI coding assistant for a mobile web development IDE.\n");
		contextBuilder.append("Your primary goal is to help the user by making precise code changes as requested.\n");
		contextBuilder.append("PROJECT: '").append(projectName).append("'\n");
		contextBuilder.append("PROJECT_ROOT: ").append(projectDir.getAbsolutePath()).append("\n\n");
		
		contextBuilder.append("=== CODEBASE OVERVIEW ===\n");
		contextBuilder.append(codebaseIndexer.getCodebaseSummary()).append("\n");
		
		boolean isRootDirectoryContext = (currentFileName == null || currentFileName.isEmpty() || currentFileName.equals(projectDir.getName()));
		
		if (currentFileName != null && !currentFileName.isEmpty() && !isRootDirectoryContext) {
			String relativePath = "";
			try {
				relativePath = fileManager.getRelativePath(new File(projectDir, currentFileName), projectDir);
			} catch (Exception e) { // FIX: Changed to generic Exception to avoid unreachable catch
				Log.w(TAG, "Could not get relative path for current file: " + currentFileName, e);
			}

			contextBuilder.append("=== CURRENT FILE CONTEXT (").append(currentFileName).append(") ===\n");
			contextBuilder.append("IMPORTANT: All line numbers and modifications for '").append(currentFileName).append("' MUST be based on the 'CONTENT' block below.\n");
			
			if (!relativePath.isEmpty()) {
				contextBuilder.append(codebaseIndexer.getFileSummary(relativePath)).append("\n");

				// Add symbol context for the current file
				List<String> symbolsDefinedInCurrentFile = codebaseIndexer.getSymbolsDefinedInFile(relativePath);
				if (!symbolsDefinedInCurrentFile.isEmpty()) {
					contextBuilder.append("\nSymbols Defined in This File:\n");
					for (String symbol : symbolsDefinedInCurrentFile) {
						contextBuilder.append("  - ").append(symbol).append("\n");
					}
				}

				List<String> symbolsUsedInCurrentFile = codebaseIndexer.getSymbolsUsedInFile(relativePath);
				if (!symbolsUsedInCurrentFile.isEmpty()) {
					contextBuilder.append("\nSymbols Used in This File:\n");
					for (String symbol : symbolsUsedInCurrentFile) {
						contextBuilder.append("  - ").append(symbol);
						List<String> definitions = codebaseIndexer.getSymbolDefinitions(symbol);
						if (!definitions.isEmpty()) {
							contextBuilder.append(" (Defined in: ").append(String.join(", ", definitions)).append(")");
						}
						contextBuilder.append("\n");
					}
				}

			} else {
				contextBuilder.append("Summary not available for current file path.\n");
			}
			
			contextBuilder.append("CONTENT:\n```\n")
			.append(currentFileContent != null ? currentFileContent : "// File content not available or file is empty.")
			.append("\n```\n\n");
			
			contextBuilder.append("=== RELATED FILES (to current file) ===\n");
			if (!relativePath.isEmpty()) {
				contextBuilder.append(codebaseIndexer.getRelatedFilesSummary(relativePath)).append("\n");
			} else {
				contextBuilder.append("Related files summary not available for current file path.\n");
			}
		} else {
			contextBuilder.append("=== GENERAL PROJECT CONTEXT (NO SPECIFIC FILE OPEN) ===\n");
			contextBuilder.append("The user is likely referring to the project in general or the root directory.\n");
			File potentialIndexHtml = new File(projectDir, "index.html");
			if (potentialIndexHtml.exists() && potentialIndexHtml.isFile()) {
				contextBuilder.append("An 'index.html' file exists in the project root. For general requests related to the main page or project entry point, consider targeting 'index.html'.\n");
			}
			contextBuilder.append("\n");
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
		contextBuilder.append("- Paths: All 'path', 'oldPath', 'newPath' MUST be relative to project root (e.g., 'index.html', 'src/file.js') and NOT start with '/'.\n");
		contextBuilder.append("- Prefer 'modifyLines' for small, targeted changes to existing files. Use 'updateFile' for substantial rewrites.\n");
		contextBuilder.append("- For 'modifyLines' on the currently open file (if provided in 'CURRENT FILE CONTEXT'), base ALL line number calculations (`startLine`, `deleteCount`) strictly on the `CONTENT:` block shown for that file.\n");
		contextBuilder.append("- When replacing content (e.g., an HTML tag like `<title>Old</title>`), ensure `startLine` is the line where the old content begins, `deleteCount` accurately covers all lines of the old content, and `insertLines` provides the complete new content.\n");
		contextBuilder.append("  Example - Replacing `<title>Old Title</title>` (on line 5) with `<title>New Title</title>`:\n");
		contextBuilder.append("  `{\"type\": \"modifyLines\", \"path\": \"index.html\", \"startLine\": 5, \"deleteCount\": 1, \"insertLines\": [\"<title>New Title</title>\"]}`\n");
		contextBuilder.append("- If the request is general and no specific file is open, consider 'index.html' if it exists and is relevant.\n\n");
		
		
		contextBuilder.append("Supported action types in the 'actions' array:\n");
		contextBuilder.append("1. 'createFile': `{\"type\": \"createFile\", \"path\": \"path/to/newFile.ext\", \"content\": \"...\"}`\n");
		contextBuilder.append("2. 'updateFile': `{\"type\": \"updateFile\", \"path\": \"path/to/existingFile.ext\", \"content\": \"...\"}` (replaces entire file)\n");
		contextBuilder.append("3. 'deleteFile': `{\"type\": \"deleteFile\", \"path\": \"path/to/fileOrDir\"}`\n");
		contextBuilder.append("4. 'renameFile': `{\"type\": \"renameFile\", \"oldPath\": \"old/path.ext\", \"newPath\": \"new/path.ext\"}`\n");
		contextBuilder.append("5. 'modifyLines': `{\"type\": \"modifyLines\", \"path\": \"path/to/file.ext\", \"startLine\": N, \"deleteCount\": M, \"insertLines\": [\"line1\", \"line2\"]}`\n");
		contextBuilder.append("   - `startLine`: 1-indexed. Line where modification begins.\n");
		contextBuilder.append("   - `deleteCount`: Number of lines to delete starting from `startLine`. 0 if only inserting.\n");
		contextBuilder.append("   - `insertLines`: Array of new lines to insert. Omit or `[]` if only deleting.\n\n");
		
		contextBuilder.append("Example JSON response structure:\n");
		contextBuilder.append("{\n");
		contextBuilder.append("  \"actions\": [\n");
		contextBuilder.append("    {\"type\": \"modifyLines\", \"path\": \"src/app.js\", \"startLine\": 10, \"deleteCount\": 1, \"insertLines\": [\"const newVersion = 2;\"]}\n");
		contextBuilder.append("  ],\n");
		contextBuilder.append("  \"explanation\": \"Updated 'app.js' to version 2.\",\n");
		contextBuilder.append("  \"suggestions\": [\"Consider testing the new version.\"]\n");
		contextBuilder.append("}\n");
		contextBuilder.append("Only output the JSON object itself.\n");
		
		return contextBuilder.toString();
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
	
	public void refreshCodebaseIndex() {
		executorService.execute(() -> {
			codebaseIndexer.indexProject(); // This now performs incremental indexing
			Log.d(TAG, "Codebase index refreshed (incrementally)");
		});
	}
	
	private void handleException(Exception e) {
		Log.e(TAG, "Unhandled exception in AIAssistant: " + e.getMessage(), e);
		if (listener != null) listener.onAiError("Unexpected internal error: " + e.getMessage());
	}

	// --- FileChangeListener implementations ---
	@Override
	public void onFileCreated(File file) {
		Log.d(TAG, "File created event: " + file.getAbsolutePath());
		refreshCodebaseIndex(); // Trigger incremental indexing
	}

	@Override
	public void onFileModified(File file) {
		Log.d(TAG, "File modified event: " + file.getAbsolutePath());
		refreshCodebaseIndex(); // Trigger incremental indexing
	}

	@Override
	public void onFileDeleted(File file) {
		Log.d(TAG, "File deleted event: " + file.getAbsolutePath());
		refreshCodebaseIndex(); // Trigger incremental indexing
	}
}
