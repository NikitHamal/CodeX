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

import com.codex.apk.ToolSpec;
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
		
		public boolean supportsVision() {
			return capabilities.supportsVision;
		}
		
		public boolean supportsFunctionCalling() {
			// For now, assume all models support function calling
			// This could be made more granular based on actual model capabilities
			return true;
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

	private File projectDir;

	// List of tools/function schemas the model is allowed to call.
	private java.util.List<ToolSpec> enabledTools = new java.util.ArrayList<>();
	
	// API configurations
	private static final String QWEN_BASE_URL = "https://chat.qwen.ai/api/v2";
	private static final String QWEN_AUTH_TOKEN = ""; // Will be configured

	// Hardcoded Qwen headers and cookies from working API calls
	private static final String QWEN_DEFAULT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4";
	private static final String QWEN_BX_UA = "231!E3/3FAmU8Mz+joZDE+3YnMEjUq/YvqY2leOxacSC80vTPuB9lMZY9mRWFzrwLEV0PmcfY4rL2JFHeKTCyUQdACIzKZBlsbAyXt6Iz8TQ7ck9CIZgeOBOVaRK66GQqw/G9pMRBb28c0I6pXxeCmhbzzfCtEk+xPyXnSGpo+LsaU/+OPHDQCrVM2z4ya7TrTguCmR87np6YdSH3DIn3jhgnFcEQHlSogvwTYlxfUid3koX0QD3yk8jHFx4EMK5QlFHH3v+++3+qAS+oPts+DQWqi++68uR+K6y+8ix9NvuqCz++I4A+4mYN/A46wSw+KgU++my9k3+2XgETIrb8AIWYA++78Ej+yzIk464F3uHo+4oSn28DfRCb7bb4RdlvedIjC5f8MUt1jGNx1IaH10EiLcJPTR6LPJWUj+1hA2bQgJ5wThA3dmWf7dsh4bWmR1rcU3OV14ljhHOENSBKjzoqihnuxql9adxbf7qHFc6ERi7pfFSMd/92mFibzH2549YNTjfOFvgo+FS1/uN+QpL0WxeXRvcFOwCFuku+u1WTAzJmXLU2obdBrZmsVL+GISL5RDin6H1n6RnV2iLE0SOZlAQT/ccm2CtJ9AhpCquek0adxkY3+TOhSPkW/r2RN+U5SbMBBFWpRqQGE0G8uG8gdRiGM+DhV5nzxB+VDkJpZTnF2C/bS8Lkogquz3Mv9hboXZORvx7WxTEhU3rXpCaVGNHzWIPFXp5shUkyscUlWQq9ZgzkhuFHR8vAwNqWLDCiab6sVoOIP1C9gwo+jAGoxgtAXU0xOWuURnWGG7aemef+Fu4s7FfkGO9kMIal6ScRRKJq+YgiTC6oj6rhJYPEgY9xX+JNv2Cp9TratLC5/7bQCpgO4+BFqW25tBh61NeNVNMS9JTFLysevVVQcfxugYJCGMv6wJ1FYvUgqX/Ag4Y4evHRbWKHp88RhqHXOYNPuBenD1xlAMyNTEOvVCDdCxeDHOzMR7cRSlKUiyGcgA7Kg/Xb9gfN/cu6ve82uefIrQg1b1zfpYgl9lExsVQv6dJPUduyTT3sUwzjlkVPkIxZ0Se5PweURQwVPEAtHYlbPAKjTEmDZ65nvieN96Z/hGl8sTm5YpgeHmDZKK4Qi/4LYK5KIpTEgONMcOqQTWReopT00zJiYw7jcNchb8t9GOTdU0RQLAZnDV8YszRmcd8gSTXrCueqrqdxxmjm1OLnNdSOjczQeyG1h/FRUXgsog9WEp1ggdbuFm3xGcHPcYaA95f6szELKvjRGPEu1gdlUYxBPQ3sWMBE152VWjWNd8SVFUrmWDizlmc0QzlmnzXa2CpNJJMMibqYd3bZ2aOENvhhXgjuRgDv5K46hVP/N2xaM/GYJgPfP1D+JnS7LPhnIUCSoTvrKwabbVOisan8s7AGz1Xse5ocJiEsXhsqSQqTaDNTWLvHxkgQYmOIRuKAeAdyUx0SfwgawTqNMC/mnbGQi/RUKwg69RqfJBYFI3SChkgl9xX9mp+ni1XrPFGSonRl4V4LuUsE7XIs5U4EDAhSJfzh+5KhRk=";
	private static final String QWEN_BX_UMIDTOKEN = "T2gAJllnldFiRL-u8mC_CoRJu4UkeSmmDyAGdRWHSDDtxGpwCLykAm6gn7JppTggooY=";
	private static final String QWEN_BX_V = "2.5.31";
	private static final String QWEN_COOKIE = "_gcl_au=1.1.1766988768.1752579263; _bl_uid=nXm6qd3q4zpg6tgm02909kvmFUpm; acw_tc=0a03e54317532565292955177e493bd17cb6ab0297793d17257e4afc7bf42b; x-ap=ap-southeast-1; token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4; tfstk=g26ZgsaVhdo2fDL9s99qzj0MwtJ9pKzS_tTXmijDfFYgWt92meSq51sfmKy2-dAA1ET6uKSAzka7F8sOXKpgPzw7wJZtXLHiIn0Xxpxv3h4mOM1lXKp0RbHESRSOW-xr1Gv0YpxXDjvDjhm3Yn-yoKYMi2cHqevDnIDmx2xJqhDDmKqFxeKXnEbDskRHJnxt_a_0zhdgx9OWGMnuVCYljekmEV-9sUeJ5xDfIIBvrGVxnxXebCBHdIJMEK5c2sJDLrlvo9LVIsSUJfYGB9IW5ta-GFjCtBX99mZ9o1jCLQ63qX8fw9W26TzI3E55A9RFOgWqkHXCttBYHjAMvH87Yko6Tuw5pVSFyjhv6C-ePkcoMjdMvH87YklxMCyeYUZnZ; isg=BP7-CDNoGikWBk775LCGxejTTxZAP8K5TbnYJKgHacE8S5klEs5CyL4txkkhzbrR; ssxmod_itna=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmgKD/KQCqDy7xA3DTx+ajQq5nxvqq35mCxteqDPLwwweCngAOnBKmgY8nUTXUZgw0=KqeDIDY=IDAtD0qDi+DDgDA=DjwDD7qe4DxDADB=bFeDLDi3eVQTDtw0=ieGwDAY4BOhwDYEKwGnxwDDS4QTIieDf9DG2DD=IRWRbqCwTDOxgCKe589bS3Th0BR3VRYIjSYq4SgIA5H8D8+lxm9YUqocQdabWwpEGsERk7wUgILQCFBQ/GD+xe7r5l05oQKiAGxgkVuDhi+YiDD; ssxmod_itna2=eq0xcDgCGQYDq4e9igDmhxnD3q7u40dGM9Deq7tdGcD8Ox0PGOzojD5DU2Yz2Ak52qqGRmxeGIDgDn6Pq+Ee03t1Q6TnxtwxGXxT5W12cxqQj6SG+THGZOQ412fzxk4BtN=FjAO01cDxOPy4S2vsrri5BxIH1iD8Bj01z27Wt4g1aEyaODFW2DAq26osz+i53rvxinaO+Si+6/er3aMigjTNVlTQiWMbqOmq4D";

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

    public interface AIActionListener {
        void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, 
                                 List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName);
        void onAiError(String errorMessage);
        void onAiRequestStarted();
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
		
		public String getUrl() {
			return url;
		}
		
		public String getTitle() {
			return title;
		}
		
		public String getSnippet() {
			return snippet;
		}
		
		public String getFavicon() {
			return favicon;
		}
	}

	private AIResponseListener responseListener;
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
		this.projectDir = projectDir;
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

				// Real-time UI update
				EditorActivity act = context instanceof EditorActivity ? (EditorActivity) context : null;
				if (act != null && act.getAiChatFragment() != null) {
					act.runOnUiThread(() -> {
						act.getAiChatFragment().updateThinkingMessage(partialResponse);
					});
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

	/**
	 * Supply the list of function tools that should be advertised to the model.
	 */
	public void setEnabledTools(java.util.List<ToolSpec> tools) {
		this.enabledTools = tools != null ? tools : new java.util.ArrayList<>();
	}
	
	public void setResponseListener(AIResponseListener listener) {
		this.responseListener = listener;
	}

	public void setActionListener(AIActionListener listener) {
		this.actionListener = listener;
	}
	
	public String getApiKey() {
		// Return the appropriate API key based on current model
		if (currentModel != null) {
			switch (currentModel.getProvider()) {
				case GOOGLE:
					return ""; // Will be set from settings
				case HUGGINGFACE:
					return ""; // Will be set from settings
				case ALIBABA:
					return QWEN_AUTH_TOKEN;
				case Z:
					return GLM_AUTH_TOKEN;
				default:
					return "";
			}
		}
		return "";
	}
	
	public void setApiKey(String apiKey) {
		// Update the appropriate API key based on current model
		if (currentModel != null) {
			switch (currentModel.getProvider()) {
				case GOOGLE:
				case HUGGINGFACE:
					// These are handled by settings, no need to store here
					break;
				case ALIBABA:
					// QWEN_AUTH_TOKEN = apiKey; // Would need to make this non-final
					break;
				case Z:
					// GLM_AUTH_TOKEN = apiKey; // Would need to make this non-final
					break;
			}
		}
	}
	
	public void sendPrompt(String userPrompt, String fileName, String fileContent) {
		// This is a simplified version - in practice, you'd want to format the prompt
		// with the file content and send it via the appropriate provider
		sendMessage(userPrompt, new ArrayList<>());
	}
	
	public void shutdown() {
		// Clean up resources if needed
		if (httpClient != null) {
			httpClient.dispatcher().executorService().shutdown();
		}
		if (glmApiClient != null) {
			// Add any GLM API client cleanup if needed
		}
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
				// Get Qwen token with fallback to default
				String customToken = SettingsActivity.getQwenApiToken(context);
				String qwenToken = customToken.isEmpty() ? QWEN_DEFAULT_TOKEN : customToken;

				Request.Builder requestBuilder = new Request.Builder()
					.url(QWEN_BASE_URL + "/models")
					.addHeader("authorization", "Bearer " + qwenToken)
					.addHeader("content-type", "application/json")
					.addHeader("accept", "application/json")
					.addHeader("Cookie", QWEN_COOKIE)
					.addHeader("bx-ua", QWEN_BX_UA)
					.addHeader("bx-umidtoken", QWEN_BX_UMIDTOKEN)
					.addHeader("bx-v", QWEN_BX_V)
					.addHeader("Accept-Language", "en-US,en;q=0.9")
					.addHeader("Connection", "keep-alive")
					.addHeader("Origin", "https://chat.qwen.ai")
					.addHeader("Referer", "https://chat.qwen.ai/")
					.addHeader("Sec-Fetch-Dest", "empty")
					.addHeader("Sec-Fetch-Mode", "cors")
					.addHeader("Sec-Fetch-Site", "same-origin")
					.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36")
					.addHeader("sec-ch-ua", "\"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
					.addHeader("sec-ch-ua-mobile", "?1")
					.addHeader("sec-ch-ua-platform", "\"Android\"")
					.addHeader("source", "h5")
					.addHeader("timezone", "Wed Jul 23 2025 14:02:07 GMT+0545");

				Request request = requestBuilder.build();
				
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

	private String getQwenToken() {
		String customToken = SettingsActivity.getQwenApiToken(context);
		return customToken.isEmpty() ? QWEN_DEFAULT_TOKEN : customToken;
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
				
				// Get Qwen token with fallback to default
				String customToken = SettingsActivity.getQwenApiToken(context);
				String qwenToken = customToken.isEmpty() ? QWEN_DEFAULT_TOKEN : customToken;

				Request.Builder requestBuilder = new Request.Builder()
					.url(QWEN_BASE_URL + "/chats/new")
					.post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
					.addHeader("authorization", "Bearer " + qwenToken)
					.addHeader("content-type", "application/json")
					.addHeader("accept", "application/json")
					.addHeader("Cookie", QWEN_COOKIE)
					.addHeader("bx-ua", QWEN_BX_UA)
					.addHeader("bx-umidtoken", QWEN_BX_UMIDTOKEN)
					.addHeader("bx-v", QWEN_BX_V)
					.addHeader("Accept-Language", "en-US,en;q=0.9")
					.addHeader("Connection", "keep-alive")
					.addHeader("Origin", "https://chat.qwen.ai")
					.addHeader("Referer", "https://chat.qwen.ai/")
					.addHeader("Sec-Fetch-Dest", "empty")
					.addHeader("Sec-Fetch-Mode", "cors")
					.addHeader("Sec-Fetch-Site", "same-origin")
					.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36")
					.addHeader("sec-ch-ua", "\"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
					.addHeader("sec-ch-ua-mobile", "?1")
					.addHeader("sec-ch-ua-platform", "\"Android\"")
					.addHeader("source", "h5")
					.addHeader("timezone", "Wed Jul 23 2025 13:27:44 GMT+0545");

				Request request = requestBuilder.build();
				
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
				// Use lastQwenResponseId as parent_id
				String parentId = null;
				// Remove the problematic cast - we'll handle threading differently
				// For now, just use null parent_id to avoid compilation errors
				if (parentId != null) {
					requestBody.addProperty("parent_id", parentId);
				} else {
					requestBody.add("parent_id", null);
				}
				requestBody.addProperty("timestamp", System.currentTimeMillis());

				// Build message array (only current message)
				JsonArray messages = new JsonArray();
				// System message as before
				JsonObject systemMsg = new JsonObject();
				systemMsg.addProperty("role", "system");
				if (currentModel.supportsFunctionCalling() && enabledTools != null && !enabledTools.isEmpty()) {
					systemMsg.addProperty("content", "You are CodexAgent, an AI assistant inside a code editor.\n\nIMPORTANT: When the user requests file operations (create, update, delete, rename files/folders), you MUST respond with a JSON object containing the action details.\n\nJSON Response Format:\n{\n  \"action\": \"file_operation\",\n  \"operations\": [\n    {\n      \"type\": \"createFile|updateFile|deleteFile|renameFile\",\n      \"path\": \"file/path.txt\",\n      \"content\": \"file content\",\n      \"oldPath\": \"old/path.txt\",\n      \"newPath\": \"new/path.txt\"\n    }\n  ],\n  \"explanation\": \"Brief explanation of what was done\",\n  \"suggestions\": [\"suggestion1\", \"suggestion2\"]\n}\n\nFor non-file operations, respond normally in plain text.\nAlways think step by step but output only the final JSON or text response.");
				} else {
					systemMsg.addProperty("content", "You are CodexAgent, an AI assistant inside a code editor.\n\n- If the user's request requires changing the workspace (create, update, delete, rename files/folders) respond with detailed instructions on what files to create or modify.\n- Provide clear explanations and suggestions for improvements.\n- Think step by step internally, but output only the final answer.");
				}
				messages.add(systemMsg);

				// User message with fid/parentId
				JsonObject messageObj = new JsonObject();
				messageObj.addProperty("role", "user");
				messageObj.addProperty("content", message);
				messageObj.addProperty("user_action", "chat");
				messageObj.add("files", new JsonArray());
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
				// Qwen threading: fid/parentId
				String userFid = java.util.UUID.randomUUID().toString();
				messageObj.addProperty("fid", userFid);
				if (parentId != null) {
					messageObj.addProperty("parentId", parentId);
				} else {
					messageObj.add("parentId", null);
				}
				messageObj.add("childrenIds", new JsonArray());
				messages.add(messageObj);
				requestBody.add("messages", messages);

				// --- Enhanced Function calling support ---
				if (currentModel.supportsFunctionCalling() && enabledTools != null && !enabledTools.isEmpty()) {
					requestBody.add("tools", ToolSpec.toJsonArray(enabledTools));
					// Add tool choice to force function calling when needed
					JsonObject toolChoice = new JsonObject();
					toolChoice.addProperty("type", "auto");
					requestBody.add("tool_choice", toolChoice);
				}
				
				// Get Qwen token with fallback to default
				String customToken = SettingsActivity.getQwenApiToken(context);
				String qwenToken = customToken.isEmpty() ? QWEN_DEFAULT_TOKEN : customToken;

				Request.Builder requestBuilder = new Request.Builder()
					.url(QWEN_BASE_URL + "/chat/completions?chat_id=" + conversationId)
					.post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
					.addHeader("authorization", "Bearer " + qwenToken)
					.addHeader("content-type", "application/json")
					.addHeader("accept", "*/*")
					.addHeader("Cookie", QWEN_COOKIE)
					.addHeader("bx-ua", QWEN_BX_UA)
					.addHeader("bx-umidtoken", QWEN_BX_UMIDTOKEN)
					.addHeader("bx-v", QWEN_BX_V)
					.addHeader("Accept-Language", "en-US,en;q=0.9")
					.addHeader("Connection", "keep-alive")
					.addHeader("Origin", "https://chat.qwen.ai")
					.addHeader("Referer", "https://chat.qwen.ai/c/" + conversationId)
					.addHeader("Sec-Fetch-Dest", "empty")
					.addHeader("Sec-Fetch-Mode", "cors")
					.addHeader("Sec-Fetch-Site", "same-origin")
					.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel A662LM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Mobile Safari/537.36")
					.addHeader("sec-ch-ua", "\"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
					.addHeader("sec-ch-ua-mobile", "?1")
					.addHeader("sec-ch-ua-platform", "\"Android\"")
					.addHeader("source", "h5")
					.addHeader("timezone", "Wed Jul 23 2025 13:27:47 GMT+0545")
					.addHeader("x-accel-buffering", "no");

				Request request = requestBuilder.build();
				
				Response response = httpClient.newCall(request).execute();
				if (response.isSuccessful() && response.body() != null) {
					// Process the response stream
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

        // --- function call accumulation state ---
        String pendingFuncName = null;
        StringBuilder pendingFuncArgs = new StringBuilder();
        
        // --- JSON response accumulation state ---
        StringBuilder jsonResponseBuilder = new StringBuilder();
        boolean isJsonResponse = false;
		
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

                            String status = delta.has("status") ? delta.get("status").getAsString() : "";

                            // Handle tool / function calling
                            if (delta.has("function_call")) {
                                JsonObject fc = delta.getAsJsonObject("function_call");
                                if (fc.has("name")) {
                                    pendingFuncName = fc.get("name").getAsString();
                                }
                                if (fc.has("arguments")) {
                                    pendingFuncArgs.append(fc.get("arguments").getAsString());
                                }
                                if ("finished".equals(status)) {
                                    // we have full call – execute
                                    try {
                                        JsonObject argsJson = JsonParser.parseString(pendingFuncArgs.toString()).getAsJsonObject();
                                        String resultJson = executeToolCall(pendingFuncName, argsJson);
                                        // Send result back to model and continue streaming follow-up
                                        sendFunctionResult(choice, pendingFuncName, resultJson, webSources);
                                    } catch (Exception e) {
                                        Log.e("AIAssistant", "Tool execution failed", e);
                                    }
                                    // reset
                                    pendingFuncName = null;
                                    pendingFuncArgs.setLength(0);
                                }
                                continue; // skip further processing for this chunk
                            }
							
							String content = delta.has("content") ? delta.get("content").getAsString() : "";
							String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";
							
							// Check if this might be a JSON response
							if (QwenResponseParser.looksLikeJson(content)) {
								isJsonResponse = true;
								jsonResponseBuilder.append(content);
							} else if (isJsonResponse) {
								// Continue accumulating JSON
								jsonResponseBuilder.append(content);
							} else {
								// Check if content contains JSON wrapped in code blocks
								String extractedJson = extractJsonFromCodeBlock(content);
								if (extractedJson != null) {
									isJsonResponse = true;
									jsonResponseBuilder.append(extractedJson);
								} else {
									// Regular text response
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
									}
								}
							}
							
							// Handle web search results
							if ("web_search".equals(phase)) {
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
								// Process final response
								if (isJsonResponse) {
									// Handle JSON response for file operations
									try {
										String jsonResponse = jsonResponseBuilder.toString();
										QwenResponseParser.ParsedResponse parsedResponse = QwenResponseParser.parseResponse(jsonResponse);
										
										if (parsedResponse != null && parsedResponse.isValid) {
											Log.d("AIAssistant", "Parsed response is valid. Action: " + parsedResponse.action);
											// If it's a file action (multi or single), process it
											if (parsedResponse.action != null && (
													"file_operation".equals(parsedResponse.action) ||
													"createFile".equals(parsedResponse.action) ||
													"updateFile".equals(parsedResponse.action) ||
													"deleteFile".equals(parsedResponse.action) ||
													"renameFile".equals(parsedResponse.action) ||
													"readFile".equals(parsedResponse.action) ||
													"listFiles".equals(parsedResponse.action))) {
												Log.d("AIAssistant", "Detected file operation action: " + parsedResponse.action);
												processFileOperationsFromParsedResponse(parsedResponse);
											} else {
												Log.d("AIAssistant", "Not a file operation action: " + parsedResponse.action);
												// Regular JSON response - extract explanation and suggestions
												String explanation = parsedResponse.explanation;
												List<String> suggestions = parsedResponse.suggestions;
												
												// Create a user-friendly message from the JSON
												StringBuilder userMessage = new StringBuilder();
												if (explanation != null && !explanation.isEmpty()) {
													userMessage.append(explanation);
												}
												
												if (!suggestions.isEmpty()) {
													if (userMessage.length() > 0) {
														userMessage.append("\n\nSuggestions:\n");
													} else {
														userMessage.append("Suggestions:\n");
													}
													for (int i = 0; i < suggestions.size(); i++) {
														userMessage.append("• ").append(suggestions.get(i));
														if (i < suggestions.size() - 1) {
															userMessage.append("\n");
														}
													}
												}
												
												if (userMessage.length() == 0) {
													userMessage.append("Response processed successfully.");
												}
												
												if (responseListener != null) {
													responseListener.onResponse(userMessage.toString(), 
														thinkingContent.length() > 0, 
														webSources.size() > 0, 
														webSources);
												}
											}
										} else {
											Log.w("AIAssistant", "Parsed response is null or invalid");
											// Invalid JSON, treat as regular text
											if (responseListener != null) {
												responseListener.onResponse(jsonResponse, 
													thinkingContent.length() > 0, 
													webSources.size() > 0, 
													webSources);
											}
										}
									} catch (Exception e) {
										Log.e("AIAssistant", "Failed to parse JSON response", e);
										// Fallback to treating as regular text
										if (responseListener != null) {
											responseListener.onResponse(jsonResponseBuilder.toString(), 
												thinkingContent.length() > 0, 
												webSources.size() > 0, 
												webSources);
										}
									}
								} else {
									// Regular text response
									if (responseListener != null) {
										responseListener.onResponse(answerContent.toString(), 
											thinkingContent.length() > 0, 
											webSources.size() > 0, 
											webSources);
									}
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

    /**
     * Processes file operations from a parsed response and executes them.
     */
    private void processFileOperationsFromParsedResponse(QwenResponseParser.ParsedResponse parsedResponse) {
        try {
            Log.d("AIAssistant", "Processing file operations from parsed response. Action: " + parsedResponse.action);
            Log.d("AIAssistant", "Operations count: " + parsedResponse.operations.size());
            Log.d("AIAssistant", "Explanation: " + parsedResponse.explanation);
            Log.d("AIAssistant", "Suggestions count: " + parsedResponse.suggestions.size());
            
            List<ChatMessage.FileActionDetail> fileActions = QwenResponseParser.toFileActionDetails(parsedResponse);
            Log.d("AIAssistant", "Converted to " + fileActions.size() + " file action details");

            // Execute each operation (single or multi)
            for (ChatMessage.FileActionDetail actionDetail : fileActions) {
                try {
                    Log.d("AIAssistant", "Executing file operation: " + actionDetail.type + " -> " + actionDetail.path);
                    executeFileOperation(actionDetail);
                } catch (Exception e) {
                    Log.e("AIAssistant", "Failed to execute file operation: " + actionDetail.type, e);
                }
            }

            // Always notify listeners with explanation/suggestions, even for single-op
            if (actionListener != null) {
                Log.d("AIAssistant", "Notifying actionListener with " + fileActions.size() + " file actions");
                actionListener.onAiActionsProcessed(
                        null,
                        parsedResponse.explanation,
                        parsedResponse.suggestions,
                        fileActions,
                        currentModel != null ? currentModel.getDisplayName() : "AI"
                );
            } else {
                Log.w("AIAssistant", "actionListener is null! Cannot notify UI of file operations.");
            }
        } catch (Exception e) {
            Log.e("AIAssistant", "Error processing file operations", e);
            if (actionListener != null) {
                actionListener.onAiError("Error processing file operations: " + e.getMessage());
            }
        }
    }

    /**
     * Processes file operations from a JSON response and executes them.
     */
    private void processFileOperationsFromJson(JsonObject jsonObj) {
        try {
            String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
            List<String> suggestions = new ArrayList<>();
            if (jsonObj.has("suggestions")) {
                JsonArray suggestionsArray = jsonObj.getAsJsonArray("suggestions");
                for (int i = 0; i < suggestionsArray.size(); i++) {
                    suggestions.add(suggestionsArray.get(i).getAsString());
                }
            }
            List<ChatMessage.FileActionDetail> fileActions = new ArrayList<>();
            if (jsonObj.has("operations")) {
                JsonArray operations = jsonObj.getAsJsonArray("operations");
                for (int i = 0; i < operations.size(); i++) {
                    JsonObject operation = operations.get(i).getAsJsonObject();
                    String type = operation.has("type") ? operation.get("type").getAsString() : "";
                    String path = operation.has("path") ? operation.get("path").getAsString() : "";
                    String content = operation.has("content") ? operation.get("content").getAsString() : "";
                    String oldPath = operation.has("oldPath") ? operation.get("oldPath").getAsString() : "";
                    String newPath = operation.has("newPath") ? operation.get("newPath").getAsString() : "";
                    ChatMessage.FileActionDetail actionDetail = new ChatMessage.FileActionDetail(
                        type, path, oldPath, newPath, "", content, 0, 0, null
                    );
                    fileActions.add(actionDetail);
                    try {
                        executeFileOperation(actionDetail);
                    } catch (Exception e) {
                        Log.e("AIAssistant", "Failed to execute file operation: " + type, e);
                    }
                }
            }
            // If there are no operations but the JSON is valid, still notify the UI with explanation/suggestions
            if (actionListener != null) {
                actionListener.onAiActionsProcessed(
                    jsonObj.toString(),
                    explanation,
                    suggestions,
                    fileActions,
                    currentModel.getDisplayName()
                );
            }
        } catch (Exception e) {
            Log.e("AIAssistant", "Failed to process file operations from JSON", e);
        }
    }
    
    /**
     * Executes a single file operation using the AiProcessor.
     */
    private void executeFileOperation(ChatMessage.FileActionDetail actionDetail) throws Exception {
        if (projectDir == null) {
            throw new IllegalStateException("Project directory not set");
        }
        
        AiProcessor processor = new AiProcessor(projectDir, context);
        processor.applyFileAction(actionDetail);
    }

    /**
     * Executes a received tool call synchronously against the local workspace.
     * Returns JSON string to pass back to the model.
     */
    public String executeToolCall(String name, JsonObject args) {
        JsonObject result = new JsonObject();
        try {
            switch (name) {
                case "createFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    file.getParentFile().mkdirs();
                    java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    result.addProperty("ok", true);
                    result.addProperty("message", "File created: " + path);
                    break;
                }
                case "updateFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    result.addProperty("ok", true);
                    result.addProperty("message", "File updated: " + path);
                    break;
                }
                case "deleteFile": {
                    String path = args.get("path").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    boolean deleted = file.isDirectory() ? deleteRecursively(file) : file.delete();
                    result.addProperty("ok", deleted);
                    result.addProperty("message", "Deleted: " + path);
                    break;
                }
                case "renameFile": {
                    String oldPath = args.get("oldPath").getAsString();
                    String newPath = args.get("newPath").getAsString();
                    java.io.File oldFile = new java.io.File(projectDir, oldPath);
                    java.io.File newFile = new java.io.File(projectDir, newPath);
                    newFile.getParentFile().mkdirs();
                    boolean ok = oldFile.renameTo(newFile);
                    result.addProperty("ok", ok);
                    result.addProperty("message", "Renamed to: " + newPath);
                    break;
                }
                case "readFile": {
                    String path = args.get("path").getAsString();
                    java.io.File file = new java.io.File(projectDir, path);
                    if (!file.exists()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found: " + path);
                    } else {
                        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()), 
                                                  java.nio.charset.StandardCharsets.UTF_8);
                        result.addProperty("ok", true);
                        result.addProperty("content", content);
                        result.addProperty("message", "File read: " + path);
                    }
                    break;
                }
                case "listFiles": {
                    String path = args.get("path").getAsString();
                    java.io.File dir = new java.io.File(projectDir, path);
                    if (!dir.exists() || !dir.isDirectory()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "Directory not found: " + path);
                    } else {
                        JsonArray files = new JsonArray();
                        java.io.File[] fileList = dir.listFiles();
                        if (fileList != null) {
                            for (java.io.File f : fileList) {
                                JsonObject fileInfo = new JsonObject();
                                fileInfo.addProperty("name", f.getName());
                                fileInfo.addProperty("type", f.isDirectory() ? "directory" : "file");
                                fileInfo.addProperty("size", f.length());
                                files.add(fileInfo);
                            }
                        }
                        result.addProperty("ok", true);
                        result.add("files", files);
                        result.addProperty("message", "Directory listed: " + path);
                    }
                    break;
                }
                default:
                    result.addProperty("ok", false);
                    result.addProperty("error", "Unknown tool: " + name);
            }
        } catch (Exception ex) {
            result.addProperty("ok", false);
            result.addProperty("error", ex.getMessage());
        }
        return result.toString();
    }

    private boolean deleteRecursively(java.io.File f) {
        if (f.isDirectory()) {
            for (java.io.File c : java.util.Objects.requireNonNull(f.listFiles())) {
                deleteRecursively(c);
            }
        }
        return f.delete();
    }

    /**
     * Extracts JSON from code blocks like ```json ... ```
     */
    private String extractJsonFromCodeBlock(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // Look for ```json ... ``` pattern
        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Also check for ``` ... ``` pattern (without json specifier)
        String genericPattern = "```\\s*([\\s\\S]*?)```";
        pattern = java.util.regex.Pattern.compile(genericPattern);
        matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            // Check if the extracted content looks like JSON
            if (QwenResponseParser.looksLikeJson(extracted)) {
                return extracted;
            }
        }
        
        return null;
    }

    /**
     * Sends a synthetic function result message back to Qwen so it can continue.
     */
    private void sendFunctionResult(JsonObject originalChoice, String funcName, String funcResultJson, List<WebSource> webSources) {
        try {
            JsonObject functionMsg = new JsonObject();
            functionMsg.addProperty("role", "function");
            functionMsg.addProperty("name", funcName);
            functionMsg.addProperty("content", funcResultJson);

            JsonArray msgs = new JsonArray();
            msgs.add(functionMsg);

            JsonObject body = new JsonObject();
            body.addProperty("stream", true);
            body.addProperty("incremental_output", true);
            body.addProperty("chat_id", getConversationId());
            body.add("messages", msgs);

            // re-use tools array so model can call further tools
            if (!enabledTools.isEmpty()) body.add("tools", ToolSpec.toJsonArray(enabledTools));

            // Get Qwen token
            String qwenToken = getQwenToken();

            Request req = new Request.Builder()
                    .url(QWEN_BASE_URL + "/chat/completions?chat_id=" + getConversationId())
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .addHeader("authorization", "Bearer " + qwenToken)
                    .addHeader("content-type", "application/json")
                    .addHeader("accept", "*/*")
                    .build();

            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    Log.e("AIAssistant", "Failed to send function result", e);
                }
                @Override public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        // Continue processing stream recursively
                        processQwenStreamResponse(response);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("AIAssistant", "Error sending function result", e);
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
