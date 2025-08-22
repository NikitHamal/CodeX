package com.codex.apk.core.pipeline;

import com.codex.apk.core.model.AIRequest;

/**
 * Interface for request interceptors that can modify or validate requests
 * before they are sent to AI services. Interceptors can be used for
 * cross-cutting concerns like logging, authentication, rate limiting,
 * request transformation, and validation.
 */
public interface RequestInterceptor {
    
    /**
     * Intercepts and potentially modifies a request before it's sent to the AI service.
     * 
     * @param request The original request
     * @param context The execution context containing metadata about the request
     * @return The potentially modified request, or the original if no changes needed
     * @throws InterceptorException if the request should be rejected or an error occurs
     */
    AIRequest intercept(AIRequest request, InterceptorContext context) throws InterceptorException;
    
    /**
     * Gets the priority of this interceptor. Lower values execute first.
     * 
     * @return Priority value (0 = highest priority)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Gets the name of this interceptor for logging and debugging purposes.
     * 
     * @return Interceptor name
     */
    String getName();
    
    /**
     * Exception thrown by interceptors to indicate request rejection or processing errors.
     */
    class InterceptorException extends Exception {
        private final boolean retryable;
        
        public InterceptorException(String message) {
            this(message, false);
        }
        
        public InterceptorException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
        
        public InterceptorException(String message, Throwable cause) {
            this(message, cause, false);
        }
        
        public InterceptorException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}