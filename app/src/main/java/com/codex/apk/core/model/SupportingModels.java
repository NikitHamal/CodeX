package com.codex.apk.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation result class for collecting errors and warnings during validation.
 */
public class ValidationResult {
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean valid;
    
    private ValidationResult(Builder builder) {
        this.errors = new ArrayList<>(builder.errors);
        this.warnings = new ArrayList<>(builder.warnings);
        this.valid = this.errors.isEmpty();
    }
    
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public List<String> getWarnings() { return new ArrayList<>(warnings); }
    public boolean isValid() { return valid; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    
    public static class Builder {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public Builder addError(String error) { errors.add(error); return this; }
        public Builder addWarning(String warning) { warnings.add(warning); return this; }
        public Builder merge(ValidationResult other) {
            if (other != null) {
                errors.addAll(other.errors);
                warnings.addAll(other.warnings);
            }
            return this;
        }
        public ValidationResult build() { return new ValidationResult(this); }
    }
    
    public static Builder builder() { return new Builder(); }
}

/**
 * Required capabilities for AI requests.
 */
class RequiredCapabilities {
    private final boolean streaming;
    private final boolean vision;
    private final boolean tools;
    private final boolean webSearch;
    private final boolean thinking;
    private final boolean multimodal;
    
    public RequiredCapabilities() {
        this(false, false, false, false, false, false);
    }
    
    public RequiredCapabilities(boolean streaming, boolean vision, boolean tools, 
                               boolean webSearch, boolean thinking, boolean multimodal) {
        this.streaming = streaming;
        this.vision = vision;
        this.tools = tools;
        this.webSearch = webSearch;
        this.thinking = thinking;
        this.multimodal = multimodal;
    }
    
    public boolean requires(String capability) {
        switch (capability.toLowerCase()) {
            case "streaming": return streaming;
            case "vision": return vision;
            case "tools": return tools;
            case "websearch": return webSearch;
            case "thinking": return thinking;
            case "multimodal": return multimodal;
            default: return false;
        }
    }
    
    public boolean isCompatibleWith(ProviderCapabilities provider) {
        return (!streaming || provider.supportsStreaming()) &&
               (!vision || provider.supportsVision()) &&
               (!tools || provider.supportsTools()) &&
               (!webSearch || provider.supportsWebSearch()) &&
               (!thinking || provider.supportsThinking()) &&
               (!multimodal || provider.supportsMultimodal());
    }
}

/**
 * Attachment for messages (images, documents, etc.).
 */
class Attachment {
    private final String type;
    private final String url;
    private final byte[] data;
    private final String mimeType;
    private final String filename;
    
    public Attachment(String type, String url, byte[] data, String mimeType, String filename) {
        this.type = type;
        this.url = url;
        this.data = data;
        this.mimeType = mimeType;
        this.filename = filename;
    }
    
    public String getType() { return type; }
    public String getUrl() { return url; }
    public byte[] getData() { return data; }
    public String getMimeType() { return mimeType; }
    public String getFilename() { return filename; }
}

/**
 * Tool call representation.
 */
class ToolCall {
    private final String id;
    private final String name;
    private final String arguments;
    private final String result;
    
    public ToolCall(String id, String name, String arguments, String result) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
        this.result = result;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getArguments() { return arguments; }
    public String getResult() { return result; }
    
    public int getEstimatedTokenCount() {
        int tokens = 50; // Base overhead
        if (arguments != null) tokens += arguments.length() / 4;
        if (result != null) tokens += result.length() / 4;
        return tokens;
    }
}

/**
 * Token usage information.
 */
class TokenUsage {
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    
    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
    
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getTotalTokens() { return totalTokens; }
}

/**
 * Response metadata.
 */
class ResponseMetadata {
    private final String model;
    private final long processingTimeMs;
    private final String providerId;
    
    public ResponseMetadata(String model, long processingTimeMs, String providerId) {
        this.model = model;
        this.processingTimeMs = processingTimeMs;
        this.providerId = providerId;
    }
    
    public String getModel() { return model; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public String getProviderId() { return providerId; }
}

/**
 * Citation information.
 */
class Citation {
    private final String text;
    private final String url;
    private final String title;
    
    public Citation(String text, String url, String title) {
        this.text = text;
        this.url = url;
        this.title = title;
    }
    
    public String getText() { return text; }
    public String getUrl() { return url; }
    public String getTitle() { return title; }
}

/**
 * Thinking content for models that expose reasoning.
 */
class ThinkingContent {
    private final String content;
    private final boolean isVisible;
    
    public ThinkingContent(String content, boolean isVisible) {
        this.content = content;
        this.isVisible = isVisible;
    }
    
    public String getContent() { return content; }
    public boolean isVisible() { return isVisible; }
    public boolean hasContent() { return content != null && !content.trim().isEmpty(); }
}

/**
 * AI error information.
 */
class AIError {
    private final String code;
    private final String message;
    private final boolean retryable;
    private final Throwable cause;
    
    public AIError(String code, String message, boolean retryable, Throwable cause) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
        this.cause = cause;
    }
    
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public boolean isRetryable() { return retryable; }
    public Throwable getCause() { return cause; }
}

/**
 * Execution context for requests.
 */
class ExecutionContext {
    private final String userId;
    private final String sessionId;
    private final java.io.File projectDir;
    
    public ExecutionContext(String userId, String sessionId, java.io.File projectDir) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.projectDir = projectDir;
    }
    
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public java.io.File getProjectDir() { return projectDir; }
}

/**
 * Provider capabilities model.
 */
class ProviderCapabilities {
    private final boolean supportsStreaming;
    private final boolean supportsVision;
    private final boolean supportsTools;
    private final boolean supportsWebSearch;
    private final boolean supportsThinking;
    private final boolean supportsMultimodal;
    private final int maxTokens;
    private final java.util.Set<String> supportedFormats;
    
    public ProviderCapabilities(boolean supportsStreaming, boolean supportsVision, boolean supportsTools,
                               boolean supportsWebSearch, boolean supportsThinking, boolean supportsMultimodal,
                               int maxTokens, java.util.Set<String> supportedFormats) {
        this.supportsStreaming = supportsStreaming;
        this.supportsVision = supportsVision;
        this.supportsTools = supportsTools;
        this.supportsWebSearch = supportsWebSearch;
        this.supportsThinking = supportsThinking;
        this.supportsMultimodal = supportsMultimodal;
        this.maxTokens = maxTokens;
        this.supportedFormats = supportedFormats;
    }
    
    public boolean supportsStreaming() { return supportsStreaming; }
    public boolean supportsVision() { return supportsVision; }
    public boolean supportsTools() { return supportsTools; }
    public boolean supportsWebSearch() { return supportsWebSearch; }
    public boolean supportsThinking() { return supportsThinking; }
    public boolean supportsMultimodal() { return supportsMultimodal; }
    public int getMaxTokens() { return maxTokens; }
    public java.util.Set<String> getSupportedFormats() { return supportedFormats; }
    
    public boolean canHandle(AIRequest request) {
        RequiredCapabilities required = request.getRequiredCapabilities();
        return required.isCompatibleWith(this);
    }
    
    public AIRequest optimizeRequest(AIRequest request) {
        // Basic optimization - could be extended per provider
        return request;
    }
}

/**
 * Provider information.
 */
class ProviderInfo {
    private final com.codex.apk.ai.AIProvider type;
    private final String displayName;
    private final String description;
    private final ProviderCapabilities capabilities;
    
    public ProviderInfo(com.codex.apk.ai.AIProvider type, String displayName, String description, ProviderCapabilities capabilities) {
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.capabilities = capabilities;
    }
    
    public com.codex.apk.ai.AIProvider getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ProviderCapabilities getCapabilities() { return capabilities; }
}

/**
 * Health status for providers.
 */
class HealthStatus {
    private final boolean healthy;
    private final String message;
    private final long responseTimeMs;
    
    public HealthStatus(boolean healthy, String message, long responseTimeMs) {
        this.healthy = healthy;
        this.message = message;
        this.responseTimeMs = responseTimeMs;
    }
    
    public boolean isHealthy() { return healthy; }
    public String getMessage() { return message; }
    public long getResponseTimeMs() { return responseTimeMs; }
    
    public static HealthStatus healthy() { return new HealthStatus(true, "OK", 0); }
    public static HealthStatus unhealthy(String message) { return new HealthStatus(false, message, -1); }
}