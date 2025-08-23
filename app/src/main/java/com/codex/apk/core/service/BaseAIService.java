package com.codex.apk.core.service;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.*;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Base implementation of AIService that provides common functionality using the
 * template method pattern. Concrete providers should extend this class and implement
 * the abstract methods for provider-specific behavior.
 */
public abstract class BaseAIService implements AIService {
    
    protected final ProviderConfig configuration;
    protected final OkHttpClient httpClient;
    protected final ProviderCapabilities capabilities;
    protected volatile boolean isShutdown = false;
    
    protected BaseAIService(ProviderConfig configuration, ProviderCapabilities capabilities) {
        this.configuration = configuration;
        this.capabilities = capabilities;
        this.httpClient = createHttpClient();
    }
    
    @Override
    public final CompletableFuture<Void> sendMessage(AIRequest request, 
                                                    Consumer<AIResponse> onResponse, 
                                                    Consumer<Throwable> onError) {
        if (isShutdown) {
            onError.accept(new IllegalStateException("Service has been shut down"));
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return validateRequest(request);
            } catch (Exception e) {
                onError.accept(mapToAIError(e, request.getId()));
                return null;
            }
        }).thenCompose(validatedRequest -> {
            if (validatedRequest == null) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                return executeRequest(validatedRequest, onResponse, onError);
            } catch (Exception e) {
                onError.accept(mapToAIError(e, request.getId()));
                return CompletableFuture.completedFuture(null);
            }
        }).exceptionally(error -> {
            logError("Request failed", error);
            onResponse.accept(createErrorResponse(request.getId(), error));
            return null;
        });
    }
    
    @Override
    public ProviderCapabilities getCapabilities() {
        return capabilities;
    }
    
    @Override
    public CompletableFuture<List<AIModel>> getModels() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchAvailableModels();
            } catch (Exception e) {
                logError("Failed to fetch models", e);
                return java.util.Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<HealthStatus> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                boolean healthy = performHealthCheck();
                long responseTime = System.currentTimeMillis() - startTime;
                return new HealthStatus(healthy, healthy ? "OK" : "Health check failed", responseTime);
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                return new HealthStatus(false, "Health check error: " + e.getMessage(), responseTime);
            }
        });
    }
    
    @Override
    public boolean canHandle(AIRequest request) {
        RequiredCapabilities required = request.getRequiredCapabilities();
        return required.isCompatibleWith(capabilities) && 
               supportsModel(request.getModel()) &&
               validateRequestSize(request);
    }
    
    @Override
    public AIRequest optimizeRequest(AIRequest request) {
        AIRequest.Builder builder = new AIRequest.Builder(request);
        
        // Apply provider-specific optimizations
        RequestParameters optimized = optimizeParameters(request.getParameters());
        builder.withParameters(optimized);
        
        // Optimize messages if needed
        List<Message> optimizedMessages = optimizeMessages(request.getMessages());
        builder.withMessages(optimizedMessages);
        
        try {
            return builder.build();
        } catch (AIRequest.ValidationException e) {
            logError("Failed to optimize request", e);
            return request; // Return original if optimization fails
        }
    }
    
    @Override
    public AIProvider getProviderType() {
        return configuration.getProviderType();
    }
    
    @Override
    public void shutdown() {
        isShutdown = true;
        try {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            performCleanup();
        } catch (Exception e) {
            logError("Error during shutdown", e);
        }
    }
    
    // Abstract methods that concrete implementations must provide
    
    /**
     * Builds a provider-specific HTTP request from the universal AI request.
     * 
     * @param request The universal AI request
     * @return HTTP request ready to be executed
     * @throws RequestBuildException if request building fails
     */
    protected abstract Request buildHttpRequest(AIRequest request) throws RequestBuildException;
    
    /**
     * Parses the HTTP response into a universal AI response.
     * 
     * @param response The HTTP response from the provider
     * @param requestId The original request ID
     * @return Parsed AI response
     * @throws ParseException if response parsing fails
     */
    protected abstract AIResponse parseResponse(Response response, String requestId) throws ParseException;
    
    /**
     * Handles streaming responses if the provider supports streaming.
     * 
     * @param response The HTTP response containing the stream
     * @param requestId The original request ID
     * @param onResponse Consumer to handle each response chunk
     * @param onError Consumer to handle errors
     * @return CompletableFuture that completes when streaming is done
     */
    protected abstract CompletableFuture<Void> handleStreamingResponse(Response response, String requestId,
                                                                      Consumer<AIResponse> onResponse,
                                                                      Consumer<Throwable> onError);
    
    /**
     * Fetches the list of available models from the provider.
     * 
     * @return List of available models
     * @throws Exception if model fetching fails
     */
    protected abstract List<AIModel> fetchAvailableModels() throws Exception;
    
    /**
     * Performs a provider-specific health check.
     * 
     * @return true if the provider is healthy
     * @throws Exception if health check fails
     */
    protected abstract boolean performHealthCheck() throws Exception;
    
    // Template method for request execution
    private CompletableFuture<Void> executeRequest(AIRequest request, 
                                                  Consumer<AIResponse> onResponse, 
                                                  Consumer<Throwable> onError) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildHttpRequest(request);
            } catch (Exception e) {
                onError.accept(e);
                return null;
            }
        }).thenCompose(httpRequest -> {
            if (httpRequest == null) {
                return CompletableFuture.completedFuture(null);
            }
            return executeHttpRequest(httpRequest, request, onResponse, onError);
        }).thenApply(result -> {
            logInfo("Completed request: " + request.getId());
            return null;
        });
    }
    
    private CompletableFuture<Void> executeHttpRequest(Request httpRequest, AIRequest aiRequest,
                                                      Consumer<AIResponse> onResponse,
                                                      Consumer<Throwable> onError) {
        return CompletableFuture.supplyAsync(() -> {
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new ServiceException("HTTP request failed: " + response.code() + " " + response.message());
                }
                
                if (aiRequest.isStreaming() && supportsStreaming()) {
                    return handleStreamingResponse(response, aiRequest.getId(), onResponse, onError);
                } else {
                    AIResponse aiResponse = parseResponse(response, aiRequest.getId());
                    onResponse.accept(aiResponse);
                    return CompletableFuture.completedFuture(null);
                }
            } catch (Exception e) {
                onError.accept(e);
                return CompletableFuture.completedFuture(null);
            }
        }).thenCompose(future -> future != null ? future : CompletableFuture.<Void>completedFuture(null));
    }
    
    // Helper methods
    
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(configuration.getTimeouts().getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .readTimeout(configuration.getTimeouts().getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .writeTimeout(configuration.getTimeouts().getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS);
        
        // Add custom headers
        if (!configuration.getCustomHeaders().isEmpty()) {
            builder.addInterceptor(chain -> {
                Request.Builder requestBuilder = chain.request().newBuilder();
                configuration.getCustomHeaders().forEach(requestBuilder::addHeader);
                return chain.proceed(requestBuilder.build());
            });
        }
        
        // Add retry interceptor if configured
        if (configuration.getRetryPolicy().getMaxRetries() > 0) {
            builder.addInterceptor(new RetryInterceptor(configuration.getRetryPolicy()));
        }
        
        return builder.build();
    }
    
    private AIRequest validateRequest(AIRequest request) throws ValidationException {
        ValidationResult validation = request.validate();
        if (validation.hasErrors()) {
            throw new ValidationException("Request validation failed: " + validation.getErrors());
        }
        
        if (!canHandle(request)) {
            throw new ValidationException("Provider cannot handle this request");
        }
        
        return request;
    }
    
    private boolean supportsModel(String modelId) {
        if (modelId == null) return true; // Provider will use default
        // This could be enhanced to check against a list of supported models
        return true;
    }
    
    private boolean validateRequestSize(AIRequest request) {
        // Estimate token count and check against limits
        int estimatedTokens = request.getMessages().stream()
            .mapToInt(Message::getEstimatedTokenCount)
            .sum();
        
        return estimatedTokens <= capabilities.getMaxTokens();
    }
    
    private boolean supportsStreaming() {
        return capabilities.supportsStreaming();
    }
    
    protected RequestParameters optimizeParameters(RequestParameters params) {
        // Base implementation - can be overridden by providers
        return params;
    }
    
    protected List<Message> optimizeMessages(List<Message> messages) {
        // Base implementation - can be overridden by providers
        return messages;
    }
    
    private AIResponse createErrorResponse(String requestId, Throwable error) {
        AIError aiError = error instanceof ServiceException ? 
            new AIError("SERVICE_ERROR", error.getMessage(), true, error) :
            new AIError("UNKNOWN_ERROR", error.getMessage(), false, error);
            
        return AIResponse.error(requestId, aiError);
    }
    
    private RuntimeException mapToAIError(Exception e, String requestId) {
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }
        return new ServiceException("Unexpected error: " + e.getMessage(), e);
    }
    
    protected void performCleanup() {
        // Override in subclasses if additional cleanup is needed
    }
    
    protected void logInfo(String message) {
        android.util.Log.i(getClass().getSimpleName(), message);
    }
    
    protected void logError(String message, Throwable error) {
        android.util.Log.e(getClass().getSimpleName(), message, error);
    }
    
    // Exception classes
    public static class RequestBuildException extends Exception {
        public RequestBuildException(String message) { super(message); }
        public RequestBuildException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class ParseException extends Exception {
        public ParseException(String message) { super(message); }
        public ParseException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }
    
    public static class ServiceException extends RuntimeException {
        public ServiceException(String message) { super(message); }
        public ServiceException(String message, Throwable cause) { super(message, cause); }
    }
}

/**
 * Simple retry interceptor for HTTP requests.
 */
class RetryInterceptor implements okhttp3.Interceptor {
    private final com.codex.apk.core.config.RetryPolicy retryPolicy;
    
    public RetryInterceptor(com.codex.apk.core.config.RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
    
    @Override
    public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
        okhttp3.Request request = chain.request();
        okhttp3.Response response = null;
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= retryPolicy.getMaxRetries(); attempt++) {
            try {
                response = chain.proceed(request);
                if (response.isSuccessful() || !shouldRetry(response.code())) {
                    return response;
                }
                response.close();
            } catch (Exception e) {
                lastException = e;
                if (attempt == retryPolicy.getMaxRetries()) {
                    break;
                }
            }
            
            // Wait before retry
            if (attempt < retryPolicy.getMaxRetries()) {
                try {
                    Thread.sleep(retryPolicy.getBackoffDelay().toMillis() * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (lastException instanceof java.io.IOException) {
            throw (java.io.IOException) lastException;
        }
        throw new java.io.IOException("Max retries exceeded", lastException);
    }
    
    private boolean shouldRetry(int responseCode) {
        return responseCode >= 500 || responseCode == 429; // Server errors and rate limiting
    }
}