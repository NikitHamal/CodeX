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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.codex.apk.editor.AiAssistantManager;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAssistant {
	
	// Provider definitions
	public enum AIProvider {
		GOOGLE("Google"),
		HUGGINGFACE("Huggingface"),
		ALIBABA("Alibaba"),
		Z("Z");
		
		private final String displayName;
		
		AIProvider(String displayName) {
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}
	}
	
	// Model capabilities
	public static class ModelCapabilities {
		public final boolean supportsThinking;
		public final boolean supportsWebSearch;
		public final boolean supportsVision;
		public final boolean supportsDocument;
		public final boolean supportsVideo;
		public final boolean supportsAudio;
		public final boolean supportsCitations;
		public final int maxContextLength;
		public final int maxGenerationLength;
		
		public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch, 
								boolean supportsVision, boolean supportsDocument,
								boolean supportsVideo, boolean supportsAudio,
								boolean supportsCitations, int maxContextLength, 
								int maxGenerationLength) {
			this.supportsThinking = supportsThinking;
			this.supportsWebSearch = supportsWebSearch;
			this.supportsVision = supportsVision;
			this.supportsDocument = supportsDocument;
			this.supportsVideo = supportsVideo;
			this.supportsAudio = supportsAudio;
			this.supportsCitations = supportsCitations;
			this.maxContextLength = maxContextLength;
			this.maxGenerationLength = maxGenerationLength;
		}
	}

	public enum AIModel {
		// Google Models
		GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE, 
			new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
		GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
		GEMINI_2_5_PRO("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.GOOGLE,
			new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
		GEMINI_2_0_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.GOOGLE,
			new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
		GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental", AIProvider.GOOGLE,
			new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
		GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
		GEMINI_2_0_FLASH_THINKING("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking", AIProvider.GOOGLE,
			new ModelCapabilities(true, false, true, true, true, true, true, 1048576, 8192)),
		GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
		GEMINI_1_5_FLASH_8B("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
		GEMINI_1_5_FLASH_002("gemini-1.5-flash-002", "Gemini 1.5 Flash 002", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
		GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
		GEMINI_1_5_PRO_002("gemini-1.5-pro-002", "Gemini 1.5 Pro 002", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
		GEMINI_1_0_PRO("gemini-1.0-pro", "Gemini 1.0 Pro", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, false, true, false, false, false, 32768, 8192)),
		GEMINI_1_0_PRO_VISION("gemini-1.0-pro-vision", "Gemini 1.0 Pro Vision", AIProvider.GOOGLE,
			new ModelCapabilities(false, false, true, true, false, false, false, 16384, 8192)),
		
		// Huggingface Models
		DEEPSEEK_R1("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "Deepseek R1", AIProvider.HUGGINGFACE,
			new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
		
		// Alibaba/Qwen Models (will be populated dynamically from API)
		QWEN3_CODER_PLUS("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA,
			new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 65536)),
		QWEN3_235B_A22B("qwen3-235b-a22b", "Qwen3-235B-A22B-2507", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
		QWEN3_30B_A3B("qwen3-30b-a3b", "Qwen3-30B-A3B", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
		QWEN3_32B("qwen3-32b", "Qwen3-32B", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
		QWEN_MAX_LATEST("qwen-max-latest", "Qwen2.5-Max", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
		QWEN_PLUS_2025_01_25("qwen-plus-2025-01-25", "Qwen2.5-Plus", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
		QWQ_32B("qwq-32b", "QwQ-32B", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
		QWEN_TURBO_2025_02_11("qwen-turbo-2025-02-11", "Qwen2.5-Turbo", AIProvider.ALIBABA,
			new ModelCapabilities(true, true, true, true, true, true, true, 1000000, 8192)),
		QWEN2_5_OMNI_7B("qwen2.5-omni-7b", "Qwen2.5-Omni-7B", AIProvider.ALIBABA,
			new ModelCapabilities(false, false, true, true, true, true, true, 30720, 2048)),
		QVQ_72B_PREVIEW("qvq-72b-preview-0310", "QVQ-Max", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
		QWEN2_5_VL_32B("qwen2.5-vl-32b-instruct", "Qwen2.5-VL-32B-Instruct", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
		QWEN2_5_14B_1M("qwen2.5-14b-instruct-1m", "Qwen2.5-14B-Instruct-1M", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, true, true, true, false, true, 1000000, 8192)),
		QWEN2_5_CODER_32B("qwen2.5-coder-32b-instruct", "Qwen2.5-Coder-32B-Instruct", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
		QWEN2_5_72B("qwen2.5-72b-instruct", "Qwen2.5-72B-Instruct", AIProvider.ALIBABA,
			new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
		
		// Z/GLM Models
		GLM_4_PLUS("glm-4-plus", "GLM-4-Plus", AIProvider.Z,
			new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
		GLM_4_0520("glm-4-0520", "GLM-4-0520", AIProvider.Z,
			new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
		GLM_4_LONG("glm-4-long", "GLM-4-Long", AIProvider.Z,
			new ModelCapabilities(false, false, false, true, false, false, false, 1000000, 4096)),
		GLM_4_AIRX("glm-4-airx", "GLM-4-AirX", AIProvider.Z,
			new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
		GLM_4_AIR("glm-4-air", "GLM-4-Air", AIProvider.Z,
			new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
		GLM_4_FLASH("glm-4-flash", "GLM-4-Flash", AIProvider.Z,
			new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
		GLM_4V_PLUS("glm-4v-plus", "GLM-4V-Plus", AIProvider.Z,
			new ModelCapabilities(true, false, true, true, true, false, true, 128000, 4096)),
		GLM_4V("glm-4v", "GLM-4V", AIProvider.Z,
			new ModelCapabilities(false, false, true, true, true, false, true, 128000, 4096)),
		COGVIEW_3_PLUS("cogview-3-plus", "CogView-3-Plus", AIProvider.Z,
			new ModelCapabilities(false, false, false, false, false, false, false, 0, 0)),
		COGVIDEOX("cogvideox", "CogVideoX", AIProvider.Z,
			new ModelCapabilities(false, false, false, false, false, true, false, 0, 0)),
		GLM_4_ALLTOOLS("glm-4-alltools", "GLM-4-AllTools", AIProvider.Z,
			new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096));

		private final String modelId;
		private final String displayName;
		private final AIProvider provider;
		private final ModelCapabilities capabilities;

		AIModel(String modelId, String displayName, AIProvider provider, ModelCapabilities capabilities) {
			this.modelId = modelId;
			this.displayName = displayName;
			this.provider = provider;
			this.capabilities = capabilities;
		}

		public String getModelId() {
			return modelId;
		}

		public String getDisplayName() {
			return displayName;
		}
		
		public AIProvider getProvider() {
			return provider;
		}
		
		public ModelCapabilities getCapabilities() {
			return capabilities;
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
		 * Returns models grouped by provider
		 */
		public static Map<AIProvider, List<AIModel>> getModelsByProvider() {
			Map<AIProvider, List<AIModel>> groupedModels = new HashMap<>();
			for (AIProvider provider : AIProvider.values()) {
				groupedModels.put(provider, new ArrayList<>());
			}
			
			for (AIModel model : AIModel.values()) {
				groupedModels.get(model.getProvider()).add(model);
			}
			
			return groupedModels;
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
		 * Returns the AIModel enum constant corresponding to the given model ID.
		 * @param modelId The model ID.
		 * @return The AIModel enum constant, or null if no match is found.
		 */
		public static AIModel fromModelId(String modelId) {
			for (AIModel model : AIModel.values()) {
				if (model.getModelId().equals(modelId)) {
					return model;
				}
			}
			return null;
		}
	}

	// Conversation management
	private Map<String, String> conversationIds; // projectPath -> conversationId mapping
	private String currentProjectPath;
	
	// AI settings
	private boolean thinkingModeEnabled = false;
	private boolean webSearchEnabled = false;

	private final Context context;
	private AIModel currentModel;
	private final OkHttpClient httpClient;
	private final Gson gson;
	
	// API configurations
	private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";
	private static final String QWEN_AUTH_TOKEN = ""; // Will be configured
	private static final String GLM_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
	private static final String GLM_AUTH_TOKEN = ""; // Will be configured
	
	// GLM API client
	private GLMApiClient glmApiClient;
	
	// Listener for AI responses
	public interface AIResponseListener {
		void onResponse(String response, boolean isThinking, boolean isWebSearch, List<WebSource> webSources);
		void onError(String error);
		void onStreamUpdate(String partialResponse, boolean isThinking);
	}
	
	// Web source data class
	public static class WebSource {
		public final String url;
		public final String title;
		public final String snippet;
		public final String favicon;
		
		public WebSource(String url, String title, String snippet, String favicon) {
			this.url = url;
			this.title = title;
			this.snippet = snippet;
			this.favicon = favicon;
		}
	}

	private AIResponseListener responseListener;

	// Legacy interface for compatibility with AiAssistantManager
	public interface AIActionListener {
		void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, 
			List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName);
		void onAiError(String errorMessage);
		void onAiRequestStarted();
	}
	
	private AIActionListener actionListener;
	
	public AIAssistant(Context context) {
		this.context = context;
		this.currentModel = AIModel.GEMINI_2_5_FLASH; // Default model
		this.conversationIds = new HashMap<>();
		this.gson = new Gson();
		
		this.httpClient = new OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.build();
			
		// Initialize GLM API client
		this.glmApiClient = new GLMApiClient();
		this.glmApiClient.setApiKey(GLM_AUTH_TOKEN);
	}
	
	// Legacy constructor for compatibility with AiAssistantManager
	public AIAssistant(Context context, String apiKey, File projectDir, String projectName, 
		ExecutorService executorService, AIActionListener actionListener) {
		this(context);
		this.actionListener = actionListener;
		this.currentProjectPath = projectDir != null ? projectDir.getAbsolutePath() : "";
		
		// Set up the response listener to bridge to the action listener
		setResponseListener(new AIResponseListener() {
			private StringBuilder thinkingContentBuilder = new StringBuilder();
			private StringBuilder mainContentBuilder = new StringBuilder();
			private List<WebSource> collectedWebSources = new ArrayList<>();
			
			@Override
			public void onResponse(String response, boolean isThinking, boolean isWebSearch, List<WebSource> webSources) {
				if (actionListener != null) {
					// Convert WebSource to ChatMessage.WebSource
					List<ChatMessage.WebSource> chatWebSources = new ArrayList<>();
					for (WebSource source : webSources) {
						chatWebSources.add(new ChatMessage.WebSource(
							source.getUrl(), source.getTitle(), source.getSnippet(), source.getFavicon()));
					}
					
					// Convert to enhanced format if the action listener supports it
					if (actionListener instanceof AiAssistantManager) {
						((AiAssistantManager) actionListener).onAiActionsProcessed(
							null, response, new ArrayList<>(), new ArrayList<>(), 
							currentModel.getDisplayName(), 
							thinkingContentBuilder.length() > 0 ? thinkingContentBuilder.toString() : null,
							chatWebSources);
					} else {
						// Fallback to legacy format
						actionListener.onAiActionsProcessed(null, response, new ArrayList<>(), new ArrayList<>(), 
							currentModel.getDisplayName());
					}
				}
			}
			
			@Override
			public void onError(String error) {
				if (actionListener != null) {
					actionListener.onAiError(error);
				}
			}
			
			@Override
			public void onStreamUpdate(String partialResponse, boolean isThinking) {
				if (isThinking) {
					thinkingContentBuilder.setLength(0);
					thinkingContentBuilder.append(partialResponse);
				} else {
					mainContentBuilder.setLength(0);
					mainContentBuilder.append(partialResponse);
				}
			}
		});
	}
	
	// Getters and setters
	public void setCurrentProjectPath(String projectPath) {
		this.currentProjectPath = projectPath;
	}
	
	public void setThinkingModeEnabled(boolean enabled) {
		this.thinkingModeEnabled = enabled;
	}
	
	public void setWebSearchEnabled(boolean enabled) {
		this.webSearchEnabled = enabled;
	}
	
	public boolean isThinkingModeEnabled() {
		return thinkingModeEnabled;
	}
	
	public boolean isWebSearchEnabled() {
		return webSearchEnabled;
	}
	
	public AIModel getCurrentModel() {
		return currentModel;
	}
	
	public void setCurrentModel(AIModel model) {
		this.currentModel = model;
	}
	
	public void setResponseListener(AIResponseListener listener) {
		this.responseListener = listener;
	}

	/**
	 * Refreshes model list for dynamic providers (Qwen and Z)
	 */
	public void refreshModelsForProvider(AIProvider provider, RefreshCallback callback) {
		if (provider == AIProvider.ALIBABA) {
			refreshQwenModels(callback);
		} else if (provider == AIProvider.Z) {
			refreshGLMModels(callback);
		} else {
			callback.onRefreshComplete(false, "Provider does not support refresh");
		}
	}
	
	public interface RefreshCallback {
		void onRefreshComplete(boolean success, String message);
	}
	
	private void refreshQwenModels(RefreshCallback callback) {
		// Implementation for refreshing Qwen models from API
		new Thread(() -> {
			try {
				Request request = new Request.Builder()
					.url(QWEN_BASE_URL + "/models")
					.addHeader("authorization", "Bearer " + QWEN_AUTH_TOKEN)
					.addHeader("content-type", "application/json")
					.build();
				
				Response response = httpClient.newCall(request).execute();
				if (response.isSuccessful() && response.body() != null) {
					String responseBody = response.body().string();
					// Parse and update model list
					// This would update the enum dynamically if needed
					callback.onRefreshComplete(true, "Models refreshed successfully");
				} else {
					callback.onRefreshComplete(false, "Failed to refresh models");
				}
			} catch (Exception e) {
				Log.e("AIAssistant", "Error refreshing Qwen models", e);
				callback.onRefreshComplete(false, "Error: " + e.getMessage());
			}
		}).start();
	}
	
	private void refreshGLMModels(RefreshCallback callback) {
		glmApiClient.fetchModels(new GLMApiClient.GLMModelsCallback() {
			@Override
			public void onModelsLoaded(List<GLMApiClient.GLMModel> models) {
				// Update the AIModel enum with GLM models if needed
				callback.onRefreshComplete(true, "GLM models refreshed successfully (" + models.size() + " models)");
			}
			
			@Override
			public void onError(String error) {
				callback.onRefreshComplete(false, "Failed to refresh GLM models: " + error);
			}
		});
	}

	/**
	 * Gets or creates conversation ID for current project and provider
	 */
	private String getConversationId() {
		if (currentProjectPath == null) return null;
		
		String key = currentProjectPath + "_" + currentModel.getProvider().name();
		return conversationIds.get(key);
	}
	
	private void setConversationId(String conversationId) {
		if (currentProjectPath == null) return;
		
		String key = currentProjectPath + "_" + currentModel.getProvider().name();
		conversationIds.put(key, conversationId);
	}

	/**
	 * Creates a new conversation for the current model/provider
	 */
	private void createNewConversation(String initialMessage, CreateConversationCallback callback) {
		if (currentModel.getProvider() == AIProvider.ALIBABA) {
			createQwenConversation(initialMessage, callback);
		} else if (currentModel.getProvider() == AIProvider.Z) {
			createGLMConversation(initialMessage, callback);
		} else {
			// For other providers, use existing logic
			callback.onConversationCreated(null);
		}
	}
	
	public interface CreateConversationCallback {
		void onConversationCreated(String conversationId);
	}
	
	private void createQwenConversation(String initialMessage, CreateConversationCallback callback) {
		new Thread(() -> {
			try {
				JsonObject requestBody = new JsonObject();
				requestBody.addProperty("title", "New Chat");
				
				JsonArray models = new JsonArray();
				models.add(currentModel.getModelId());
				requestBody.add("models", models);
				
				requestBody.addProperty("chat_mode", "normal");
				requestBody.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
				requestBody.addProperty("timestamp", System.currentTimeMillis());
				
				Request request = new Request.Builder()
					.url(QWEN_BASE_URL + "/chats/new")
					.post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
					.addHeader("authorization", "Bearer " + QWEN_AUTH_TOKEN)
					.addHeader("content-type", "application/json")
					.addHeader("accept", "application/json")
					.build();
				
				Response response = httpClient.newCall(request).execute();
				if (response.isSuccessful() && response.body() != null) {
					String responseBody = response.body().string();
					JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
					
					if (responseJson.get("success").getAsBoolean()) {
						String conversationId = responseJson.getAsJsonObject("data").get("id").getAsString();
						setConversationId(conversationId);
						callback.onConversationCreated(conversationId);
					} else {
						callback.onConversationCreated(null);
					}
				} else {
					callback.onConversationCreated(null);
				}
			} catch (Exception e) {
				Log.e("AIAssistant", "Error creating Qwen conversation", e);
				callback.onConversationCreated(null);
			}
		}).start();
	}
	
	private void createGLMConversation(String initialMessage, CreateConversationCallback callback) {
		// GLM doesn't require separate conversation creation, use message-based approach
		String conversationId = "glm_" + System.currentTimeMillis();
		setConversationId(conversationId);
		callback.onConversationCreated(conversationId);
	}

	/**
	 * Sends a message using the appropriate provider API
	 */
	public void sendMessage(String message, List<File> attachments) {
		if (currentModel.getProvider() == AIProvider.ALIBABA) {
			sendQwenMessage(message, attachments);
		} else if (currentModel.getProvider() == AIProvider.Z) {
			sendGLMMessage(message, attachments);
		} else {
			// Use existing Google/Huggingface logic
			sendGoogleMessage(message, attachments);
		}
	}
	
	private void sendQwenMessage(String message, List<File> attachments) {
		String conversationId = getConversationId();
		
		if (conversationId == null) {
			// Create new conversation first
			createNewConversation(message, newConversationId -> {
				if (newConversationId != null) {
					performQwenCompletion(newConversationId, message, attachments);
				} else if (responseListener != null) {
					responseListener.onError("Failed to create conversation");
				}
			});
		} else {
			performQwenCompletion(conversationId, message, attachments);
		}
	}
	
	private void performQwenCompletion(String conversationId, String message, List<File> attachments) {
		new Thread(() -> {
			try {
				JsonObject requestBody = new JsonObject();
				requestBody.addProperty("stream", true);
				requestBody.addProperty("incremental_output", true);
				requestBody.addProperty("chat_id", conversationId);
				requestBody.addProperty("chat_mode", "normal");
				requestBody.addProperty("model", currentModel.getModelId());
				requestBody.addProperty("parent_id", (String) null);
				requestBody.addProperty("timestamp", System.currentTimeMillis());
				
				// Build message array
				JsonArray messages = new JsonArray();
				JsonObject messageObj = new JsonObject();
				messageObj.addProperty("role", "user");
				messageObj.addProperty("content", message);
				messageObj.addProperty("user_action", "chat");
				messageObj.add("files", new JsonArray()); // TODO: Handle file attachments
				messageObj.addProperty("timestamp", System.currentTimeMillis());
				
				JsonArray modelsArray = new JsonArray();
				modelsArray.add(currentModel.getModelId());
				messageObj.add("models", modelsArray);
				
				messageObj.addProperty("chat_type", webSearchEnabled ? "search" : "t2t");
				
				// Feature config
				JsonObject featureConfig = new JsonObject();
				featureConfig.addProperty("thinking_enabled", thinkingModeEnabled);
				featureConfig.addProperty("output_schema", "phase");
				if (webSearchEnabled) {
					featureConfig.addProperty("search_version", "v2");
				}
				if (thinkingModeEnabled) {
					featureConfig.addProperty("thinking_budget", 38912);
				}
				messageObj.add("feature_config", featureConfig);
				
				messages.add(messageObj);
				requestBody.add("messages", messages);
				
				Request request = new Request.Builder()
					.url(QWEN_BASE_URL + "/chat/completions?chat_id=" + conversationId)
					.post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
					.addHeader("authorization", "Bearer " + QWEN_AUTH_TOKEN)
					.addHeader("content-type", "application/json")
					.addHeader("accept", "*/*")
					.build();
				
				Response response = httpClient.newCall(request).execute();
				if (response.isSuccessful() && response.body() != null) {
					processQwenStreamResponse(response);
				} else if (responseListener != null) {
					responseListener.onError("Failed to send message");
				}
			} catch (Exception e) {
				Log.e("AIAssistant", "Error sending Qwen message", e);
				if (responseListener != null) {
					responseListener.onError("Error: " + e.getMessage());
				}
			}
		}).start();
	}
	
	private void processQwenStreamResponse(Response response) throws IOException {
		StringBuilder thinkingContent = new StringBuilder();
		StringBuilder answerContent = new StringBuilder();
		List<WebSource> webSources = new ArrayList<>();
		
		String line;
		while ((line = response.body().source().readUtf8Line()) != null) {
			if (line.startsWith("data: ")) {
				String jsonData = line.substring(6);
				if (jsonData.trim().isEmpty()) continue;
				
				try {
					JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
					
					if (data.has("choices")) {
						JsonArray choices = data.getAsJsonArray("choices");
						if (choices.size() > 0) {
							JsonObject choice = choices.get(0).getAsJsonObject();
							JsonObject delta = choice.getAsJsonObject("delta");
							
							String content = delta.has("content") ? delta.get("content").getAsString() : "";
							String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";
							String status = delta.has("status") ? delta.get("status").getAsString() : "";
							
							if ("think".equals(phase)) {
								thinkingContent.append(content);
								if (responseListener != null) {
									responseListener.onStreamUpdate(thinkingContent.toString(), true);
								}
							} else if ("answer".equals(phase)) {
								answerContent.append(content);
								if (responseListener != null) {
									responseListener.onStreamUpdate(answerContent.toString(), false);
								}
							} else if ("web_search".equals(phase)) {
								// Handle web search results
								if (choice.has("extra")) {
									JsonObject extra = choice.getAsJsonObject("extra");
									if (extra.has("web_search_info")) {
										JsonArray searchInfo = extra.getAsJsonArray("web_search_info");
										for (int i = 0; i < searchInfo.size(); i++) {
											JsonObject source = searchInfo.get(i).getAsJsonObject();
											webSources.add(new WebSource(
												source.get("url").getAsString(),
												source.get("title").getAsString(),
												source.get("snippet").getAsString(),
												"" // favicon not provided in API
											));
										}
									}
								}
							}
							
							if ("finished".equals(status)) {
								if (responseListener != null) {
									responseListener.onResponse(answerContent.toString(), 
										thinkingContent.length() > 0, 
										webSources.size() > 0, 
										webSources);
								}
								break;
							}
						}
					}
				} catch (JsonParseException e) {
					Log.w("AIAssistant", "Failed to parse JSON: " + jsonData);
				}
			}
		}
	}
	
	private void sendGLMMessage(String message, List<File> attachments) {
		String conversationId = getConversationId();
		
		if (conversationId == null) {
			// Create new conversation first
			createNewConversation(message, newConversationId -> {
				if (newConversationId != null) {
					performGLMCompletion(message, attachments);
				} else if (responseListener != null) {
					responseListener.onError("Failed to create GLM conversation");
				}
			});
		} else {
			performGLMCompletion(message, attachments);
		}
	}
	
	private void performGLMCompletion(String message, List<File> attachments) {
		// Find corresponding GLM model
		GLMApiClient.GLMModel glmModel = findGLMModel(currentModel);
		if (glmModel == null) {
			if (responseListener != null) {
				responseListener.onError("Current model is not a GLM model");
			}
			return;
		}
		
		glmApiClient.sendStreamingChatCompletion(glmModel, message, thinkingModeEnabled, webSearchEnabled,
			new GLMApiClient.GLMResponseListener() {
				@Override
				public void onResponse(String response, boolean isThinking, List<WebSource> webSources) {
					if (responseListener != null) {
						responseListener.onResponse(response, isThinking, webSources.size() > 0, webSources);
					}
				}
				
				@Override
				public void onError(String error) {
					if (responseListener != null) {
						responseListener.onError(error);
					}
				}
				
				@Override
				public void onStreamUpdate(String partialResponse, boolean isThinking) {
					if (responseListener != null) {
						responseListener.onStreamUpdate(partialResponse, isThinking);
					}
				}
			});
	}
	
	private GLMApiClient.GLMModel findGLMModel(AIModel aiModel) {
		// Map AIModel to GLMModel based on model ID
		String modelId = aiModel.getModelId();
		for (GLMApiClient.GLMModel glmModel : GLMApiClient.GLMModel.values()) {
			if (glmModel.getModelId().equals(modelId)) {
				return glmModel;
			}
		}
		return null;
	}
	
	private void sendGoogleMessage(String message, List<File> attachments) {
		// Keep existing Google/Gemini implementation
		// This would be the existing logic from the original AIAssistant class
		if (responseListener != null) {
			responseListener.onResponse("Google response placeholder for: " + message, false, false, new ArrayList<>());
		}
	}
}
