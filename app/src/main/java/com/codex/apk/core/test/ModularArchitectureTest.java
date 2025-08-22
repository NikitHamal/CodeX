package com.codex.apk.core.test;

import com.codex.apk.core.service.*;
import com.codex.apk.core.model.*;
import com.codex.apk.core.config.*;
import com.codex.apk.core.registry.ProviderRegistry;
import com.codex.apk.core.providers.OpenAICompatibleServiceFactory;
import com.codex.apk.ai.AIProvider;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Integration tests for the modular AI architecture.
 * These tests validate the core functionality and integration between components.
 */
public class ModularArchitectureTest {
    
    /**
     * Tests the basic service registration and request flow.
     */
    public static void testBasicServiceFlow() {
        try {
            // Create provider registry
            ProviderRegistry registry = new ProviderRegistry();
            
            // Register OpenAI-compatible providers
            registry.register(AIProvider.DEEPINFRA, OpenAICompatibleServiceFactory.create(AIProvider.DEEPINFRA));
            registry.register(AIProvider.FREE, OpenAICompatibleServiceFactory.create(AIProvider.FREE));
            
            // Create service configuration
            ServiceConfiguration config = ServiceConfiguration.builder()
                .withProviderConfig(AIProvider.DEEPINFRA, 
                    ProviderConfig.builder(AIProvider.DEEPINFRA)
                        .withBaseUrl("https://api.deepinfra.com/v1/openai")
                        .withApiKey("test-key")
                        .build())
                .withProviderConfig(AIProvider.FREE,
                    ProviderConfig.defaults(AIProvider.FREE))
                .withRequestTimeout(Duration.ofMinutes(2))
                .build();
            
            // Create request pipeline
            RequestPipeline pipeline = new DefaultRequestPipeline();
            
            // Create service manager
            AIServiceManager serviceManager = new AIServiceManager(registry, pipeline, config);
            
            // Test provider switching
            serviceManager.switchProvider(AIProvider.FREE);
            assert serviceManager.getCurrentProvider() == AIProvider.FREE;
            
            // Test request building
            AIRequest request = AIRequest.builder()
                .withModel("gpt-3.5-turbo")
                .addMessage(Message.user("Hello, world!"))
                .withParameters(RequestParameters.builder()
                    .withTemperature(0.7)
                    .withMaxTokens(100)
                    .withStream(false)
                    .build())
                .build();
            
            // Validate request
            ValidationResult validation = request.validate();
            assert validation.isValid() : "Request should be valid";
            
            // Test service capability checking
            AIService service = serviceManager.getService(AIProvider.FREE);
            assert service.canHandle(request) : "Service should be able to handle the request";
            
            // Test health check
            CompletableFuture<HealthStatus> healthFuture = service.healthCheck();
            HealthStatus health = healthFuture.get();
            System.out.println("Health check result: " + health.isHealthy());
            
            System.out.println("✅ Basic service flow test passed");
            
        } catch (Exception e) {
            System.err.println("❌ Basic service flow test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tests configuration validation and provider capabilities.
     */
    public static void testConfigurationValidation() {
        try {
            // Test valid configuration
            ProviderConfig validConfig = ProviderConfig.builder(AIProvider.DEEPINFRA)
                .withBaseUrl("https://api.deepinfra.com/v1/openai")
                .withApiKey("test-key")
                .build();
            
            ValidationResult validation = validConfig.validate();
            assert validation.isValid() : "Valid configuration should pass validation";
            
            // Test invalid configuration
            try {
                ProviderConfig invalidConfig = ProviderConfig.builder(AIProvider.DEEPINFRA)
                    .withBaseUrl("invalid-url")
                    .build(); // Missing required API key
                
                assert false : "Invalid configuration should throw exception";
            } catch (IllegalArgumentException e) {
                // Expected
            }
            
            // Test configuration merging
            ProviderConfig base = ProviderConfig.defaults(AIProvider.FREE);
            ProviderConfig override = ProviderConfig.builder(AIProvider.FREE)
                .withApiKey("new-key")
                .build();
            
            ProviderConfig merged = base.merge(override);
            assert "new-key".equals(merged.getApiKey()) : "Merged config should have new API key";
            
            System.out.println("✅ Configuration validation test passed");
            
        } catch (Exception e) {
            System.err.println("❌ Configuration validation test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tests data model creation and validation.
     */
    public static void testDataModels() {
        try {
            // Test message creation
            Message userMessage = Message.user("Hello");
            assert userMessage.getRole() == Message.MessageRole.USER;
            assert "Hello".equals(userMessage.getContent());
            
            Message systemMessage = Message.system("You are a helpful assistant");
            assert systemMessage.getRole() == Message.MessageRole.SYSTEM;
            
            // Test request parameters
            RequestParameters params = RequestParameters.builder()
                .withTemperature(0.8)
                .withMaxTokens(1000)
                .withTopP(0.9)
                .build();
            
            ValidationResult paramValidation = params.validate();
            assert paramValidation.isValid() : "Valid parameters should pass validation";
            
            // Test invalid parameters
            RequestParameters invalidParams = RequestParameters.builder()
                .withTemperature(-1.0) // Invalid temperature
                .build();
            
            ValidationResult invalidValidation = invalidParams.validate();
            assert invalidValidation.hasErrors() : "Invalid parameters should have errors";
            
            // Test request building
            AIRequest request = AIRequest.builder()
                .withModel("test-model")
                .addMessage(userMessage)
                .addMessage(systemMessage)
                .withParameters(params)
                .build();
            
            assert request.getMessages().size() == 2;
            assert "test-model".equals(request.getModel());
            
            System.out.println("✅ Data models test passed");
            
        } catch (Exception e) {
            System.err.println("❌ Data models test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tests provider registry functionality.
     */
    public static void testProviderRegistry() {
        try {
            ProviderRegistry registry = new ProviderRegistry();
            
            // Test registration
            AIServiceFactory factory = OpenAICompatibleServiceFactory.create(AIProvider.FREE);
            registry.register(AIProvider.FREE, factory);
            
            assert registry.isRegistered(AIProvider.FREE) : "Provider should be registered";
            assert registry.size() == 1 : "Registry should have one provider";
            
            // Test retrieval
            AIServiceFactory retrieved = registry.getFactory(AIProvider.FREE);
            assert retrieved != null : "Factory should be retrievable";
            assert retrieved.getProviderType() == AIProvider.FREE;
            
            // Test provider info
            ProviderInfo info = registry.getProviderInfo(AIProvider.FREE);
            assert info != null : "Provider info should be available";
            assert info.getType() == AIProvider.FREE;
            
            // Test unregistration
            AIServiceFactory unregistered = registry.unregister(AIProvider.FREE);
            assert unregistered != null : "Unregistered factory should be returned";
            assert !registry.isRegistered(AIProvider.FREE) : "Provider should no longer be registered";
            
            System.out.println("✅ Provider registry test passed");
            
        } catch (Exception e) {
            System.err.println("❌ Provider registry test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Runs all tests.
     */
    public static void runAllTests() {
        System.out.println("🧪 Running modular architecture integration tests...\n");
        
        testDataModels();
        testConfigurationValidation();
        testProviderRegistry();
        testBasicServiceFlow();
        
        System.out.println("\n🎉 All integration tests completed!");
    }
    
    public static void main(String[] args) {
        runAllTests();
    }
}

/**
 * Default implementation of RequestPipeline for testing.
 */
class DefaultRequestPipeline implements RequestPipeline {
    private final java.util.List<com.codex.apk.core.pipeline.RequestInterceptor> interceptors = new java.util.ArrayList<>();
    private final java.util.List<com.codex.apk.core.pipeline.ResponseProcessor> processors = new java.util.ArrayList<>();
    
    @Override
    public io.reactivex.rxjava3.core.Observable<AIResponse> execute(AIRequest request, AIService service) {
        // Simple implementation that just forwards to the service
        return service.sendMessage(request);
    }
    
    @Override
    public RequestPipeline addInterceptor(com.codex.apk.core.pipeline.RequestInterceptor interceptor) {
        interceptors.add(interceptor);
        return this;
    }
    
    @Override
    public RequestPipeline addProcessor(com.codex.apk.core.pipeline.ResponseProcessor processor) {
        processors.add(processor);
        return this;
    }
    
    @Override
    public RequestPipeline removeInterceptor(com.codex.apk.core.pipeline.RequestInterceptor interceptor) {
        interceptors.remove(interceptor);
        return this;
    }
    
    @Override
    public RequestPipeline removeProcessor(com.codex.apk.core.pipeline.ResponseProcessor processor) {
        processors.remove(processor);
        return this;
    }
    
    @Override
    public RequestPipeline clear() {
        interceptors.clear();
        processors.clear();
        return this;
    }
    
    @Override
    public int getInterceptorCount() {
        return interceptors.size();
    }
    
    @Override
    public int getProcessorCount() {
        return processors.size();
    }
}