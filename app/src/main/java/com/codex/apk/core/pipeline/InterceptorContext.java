package com.codex.apk.core.pipeline;

import com.codex.apk.ai.AIProvider;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object shared between interceptors and processors during request execution.
 * Contains metadata about the request, timing information, and a key-value store
 * for sharing data between pipeline components.
 */
public class InterceptorContext {
    
    private final String requestId;
    private final AIProvider provider;
    private final Instant startTime;
    private final Map<String, Object> attributes;
    private Instant serviceCallStart;
    private Instant serviceCallEnd;
    
    public InterceptorContext(String requestId, AIProvider provider) {
        this.requestId = requestId;
        this.provider = provider;
        this.startTime = Instant.now();
        this.attributes = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the unique identifier for this request.
     * 
     * @return Request ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * Gets the AI provider for this request.
     * 
     * @return Provider type
     */
    public AIProvider getProvider() {
        return provider;
    }
    
    /**
     * Gets the time when request processing started.
     * 
     * @return Start time
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Marks the start of the actual service call.
     */
    public void markServiceCallStart() {
        this.serviceCallStart = Instant.now();
    }
    
    /**
     * Marks the end of the actual service call.
     */
    public void markServiceCallEnd() {
        this.serviceCallEnd = Instant.now();
    }
    
    /**
     * Gets the time when the service call started.
     * 
     * @return Service call start time, or null if not yet started
     */
    public Instant getServiceCallStart() {
        return serviceCallStart;
    }
    
    /**
     * Gets the time when the service call ended.
     * 
     * @return Service call end time, or null if not yet ended
     */
    public Instant getServiceCallEnd() {
        return serviceCallEnd;
    }
    
    /**
     * Stores an attribute in the context for sharing between pipeline components.
     * 
     * @param key Attribute key
     * @param value Attribute value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * Retrieves an attribute from the context.
     * 
     * @param key Attribute key
     * @return Attribute value, or null if not found
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * Retrieves an attribute from the context with type casting.
     * 
     * @param key Attribute key
     * @param type Expected type
     * @return Attribute value cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Checks if an attribute exists in the context.
     * 
     * @param key Attribute key
     * @return true if the attribute exists
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
    
    /**
     * Removes an attribute from the context.
     * 
     * @param key Attribute key
     * @return The previous value, or null if not found
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }
    
    /**
     * Gets all attribute keys in the context.
     * 
     * @return Set of attribute keys
     */
    public java.util.Set<String> getAttributeKeys() {
        return attributes.keySet();
    }
    
    /**
     * Calculates the total processing time from start to now.
     * 
     * @return Processing time in milliseconds
     */
    public long getProcessingTimeMillis() {
        return java.time.Duration.between(startTime, Instant.now()).toMillis();
    }
    
    /**
     * Calculates the service call duration if both start and end times are available.
     * 
     * @return Service call duration in milliseconds, or -1 if timing not available
     */
    public long getServiceCallDurationMillis() {
        if (serviceCallStart != null && serviceCallEnd != null) {
            return java.time.Duration.between(serviceCallStart, serviceCallEnd).toMillis();
        }
        return -1;
    }
}