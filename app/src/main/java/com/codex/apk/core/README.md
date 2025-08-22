# CodeX Modular AI Architecture

## Overview

This document describes the new modular, provider-agnostic AI architecture for CodeX. The architecture transforms the existing collection of individual API clients into a unified, enterprise-grade system that supports multiple AI providers through consistent interfaces.

## Architecture Components

### Core Interfaces
- **`AIService`** - Universal provider interface for all AI operations
- **`AIServiceFactory`** - Factory pattern for provider instantiation
- **`RequestPipeline`** - Middleware system for cross-cutting concerns
- **`AIServiceManager`** - Central orchestrator with fallback strategies

### Data Models
- **`AIRequest`** - Provider-agnostic request model with builder pattern
- **`AIResponse`** - Normalized response model supporting streaming
- **`Message`** - Universal message representation
- **`RequestParameters`** - Type-safe parameter system

### Provider Implementations
- **`OpenAICompatibleService`** - For DeepInfra, ApiAirforce, GptOss, FREE providers
- **`GeminiOfficialService`** - Official Google Gemini API
- **`GeminiFreeService`** - Cookie-based Gemini access
- **`QwenService`** - Alibaba Qwen with conversation state management

## Quick Start

### 1. Basic Setup

```java
// Create provider registry
ProviderRegistry registry = new ProviderRegistry();

// Register providers
registry.register(AIProvider.DEEPINFRA, OpenAICompatibleServiceFactory.create(AIProvider.DEEPINFRA));
registry.register(AIProvider.GOOGLE, GeminiServiceFactory.createOfficial());
registry.register(AIProvider.COOKIES, GeminiServiceFactory.createFree());

// Create service configuration
ServiceConfiguration config = ServiceConfiguration.builder()
    .withProviderConfig(AIProvider.DEEPINFRA, 
        ProviderConfig.builder(AIProvider.DEEPINFRA)
            .withBaseUrl("https://api.deepinfra.com/v1/openai")
            .withApiKey("your-api-key")
            .build())
    .withProviderConfig(AIProvider.GOOGLE,
        ProviderConfig.builder(AIProvider.GOOGLE)
            .withApiKey("your-gemini-api-key")
            .build())
    .build();

// Create service manager
RequestPipeline pipeline = new DefaultRequestPipeline();
AIServiceManager serviceManager = new AIServiceManager(registry, pipeline, config);
```

### 2. Making AI Requests

```java
// Build a request
AIRequest request = AIRequest.builder()
    .withModel("gpt-3.5-turbo")
    .addMessage(Message.user("Hello, world!"))
    .withParameters(RequestParameters.builder()
        .withTemperature(0.7)
        .withMaxTokens(100)
        .withStream(true)
        .build())
    .build();

// Execute request
serviceManager.executeRequest(request)
    .subscribe(
        response -> {
            if (response.isStreaming()) {
                System.out.println("Streaming: " + response.getContent());
            } else {
                System.out.println("Final: " + response.getContent());
            }
        },
        error -> System.err.println("Error: " + error.getMessage()),
        () -> System.out.println("Completed")
    );
```

### 3. Provider Switching

```java
// Switch to different provider
serviceManager.switchProvider(AIProvider.GOOGLE);

// Request will now use Gemini
AIRequest geminiRequest = AIRequest.builder()
    .withModel("gemini-1.5-flash")
    .addMessage(Message.user("Explain quantum computing"))
    .withParameters(RequestParameters.creative())
    .build();

serviceManager.executeRequest(geminiRequest)
    .subscribe(response -> System.out.println(response.getContent()));
```

### 4. Using Tools

```java
// Register tools
ToolRegistry toolRegistry = new ToolRegistry();
// Tools are automatically registered in the constructor

// Create request with tools
AIRequest toolRequest = AIRequest.builder()
    .withModel("qwen3-coder-plus")
    .addMessage(Message.user("Create a new file called hello.txt with content 'Hello World'"))
    .withTools(toolRegistry.getAllToolSpecs())
    .requireCapabilities(new RequiredCapabilities(false, false, true, false, false, false))
    .build();

serviceManager.executeRequest(toolRequest)
    .subscribe(response -> {
        if (response.hasToolCalls()) {
            // Execute tool calls
            for (ToolCall toolCall : response.getToolCalls()) {
                ToolResult result = toolRegistry.executeToolSync(toolCall, executionContext);
                System.out.println("Tool result: " + result.getContent());
            }
        }
    });
```

## Migration from Legacy Code

### Using the Compatibility Layer

The `LegacyAIAssistantAdapter` maintains the existing `AIAssistant` API while using the new architecture underneath:

```java
// Replace existing AIAssistant creation
AIServiceManager serviceManager = createServiceManager(); // Your setup
LegacyAIAssistantAdapter adapter = new LegacyAIAssistantAdapter(
    context, executorService, actionListener, serviceManager);

// Existing code continues to work
adapter.sendMessage(message, history, qwenState, attachments);
adapter.refreshModelsForProvider(provider, callback);
```

### Gradual Migration Strategy

1. **Phase 1**: Replace `AIAssistant` with `LegacyAIAssistantAdapter`
2. **Phase 2**: Migrate high-level components to use `AIServiceManager` directly
3. **Phase 3**: Update UI components to use new data models
4. **Phase 4**: Remove legacy adapter and fully embrace new architecture

## Provider-Specific Configuration

### OpenAI-Compatible Providers

```java
ProviderConfig deepinfraConfig = ProviderConfig.builder(AIProvider.DEEPINFRA)
    .withBaseUrl("https://api.deepinfra.com/v1/openai")
    .withApiKey("your-deepinfra-key")
    .withTimeouts(new TimeoutConfig(
        Duration.ofSeconds(30),  // connection
        Duration.ofSeconds(60),  // read
        Duration.ofSeconds(30),  // write
        Duration.ofMinutes(5)    // total
    ))
    .build();
```

### Gemini Official

```java
ProviderConfig geminiConfig = ProviderConfig.builder(AIProvider.GOOGLE)
    .withApiKey("your-gemini-api-key")
    .withRetryPolicy(new RetryPolicy(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30)))
    .build();
```

### Gemini Free (Cookie-based)

```java
ProviderConfig geminiCookieConfig = ProviderConfig.builder(AIProvider.COOKIES)
    .addProviderSpecificConfig("psid", "__Secure-1PSID-cookie-value")
    .addProviderSpecificConfig("psidts", "__Secure-1PSIDTS-cookie-value")
    .build();
```

### Qwen/Alibaba

```java
ProviderConfig qwenConfig = ProviderConfig.builder(AIProvider.ALIBABA)
    .withBaseUrl("https://chat.qwen.ai/api/v2")
    .addProviderSpecificConfig("midtoken", "cached-midtoken")
    .build();
```

## Production Features

### Circuit Breaker

```java
CircuitBreaker circuitBreaker = CircuitBreaker.builder()
    .withName("gemini-api")
    .withFailureThreshold(5)
    .withTimeout(Duration.ofSeconds(30))
    .withResetTimeout(Duration.ofMinutes(1))
    .build();

// Use with service calls
circuitBreaker.execute(() -> {
    return serviceManager.executeRequest(request).blockingFirst();
});
```

### Health Monitoring

```java
// Check all services
serviceManager.performHealthChecks()
    .thenAccept(healthMap -> {
        healthMap.forEach((provider, health) -> {
            System.out.println(provider + ": " + (health.isHealthy() ? "OK" : "FAILED"));
        });
    });

// Check specific provider
serviceManager.performHealthCheck(AIProvider.GOOGLE)
    .thenAccept(health -> {
        System.out.println("Gemini health: " + health.getMessage());
    });
```

### Request Pipeline with Interceptors

```java
RequestPipeline pipeline = new DefaultRequestPipeline()
    .addInterceptor(new LoggingInterceptor())
    .addInterceptor(new RateLimitInterceptor())
    .addProcessor(new MetricsProcessor())
    .addProcessor(new CacheProcessor());
```

## Benefits of the New Architecture

1. **Provider Agnostic** - Single interface works with all providers
2. **Production Ready** - Circuit breakers, retry policies, health monitoring
3. **Type Safe** - Strong typing with validation prevents runtime errors
4. **Extensible** - Easy to add new providers and capabilities
5. **Observable** - Comprehensive metrics and monitoring
6. **Backward Compatible** - Existing code continues to work during migration

## Testing

Run the integration tests to validate the architecture:

```java
ModularArchitectureTest.runAllTests();
```

## Next Steps

1. **Setup**: Configure providers and create service manager
2. **Migrate**: Use compatibility layer for gradual migration  
3. **Extend**: Add new providers using established patterns
4. **Monitor**: Implement health checks and metrics collection
5. **Optimize**: Fine-tune configurations for your use case

The modular architecture provides a solid foundation for scaling CodeX's AI integration capabilities while maintaining reliability and performance.