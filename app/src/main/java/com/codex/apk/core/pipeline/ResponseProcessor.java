package com.codex.apk.core.pipeline;

import com.codex.apk.core.model.AIResponse;

/**
 * Interface for response processors that can modify or analyze responses
 * after they are received from AI services. Processors can be used for
 * cross-cutting concerns like logging, metrics collection, response caching,
 * content filtering, and response transformation.
 */
public interface ResponseProcessor {
    
    /**
     * Processes and potentially modifies a response after it's received from the AI service.
     * 
     * @param response The original response
     * @param context The execution context containing metadata about the request/response
     * @return The potentially modified response, or the original if no changes needed
     * @throws ProcessorException if response processing fails
     */
    AIResponse process(AIResponse response, InterceptorContext context) throws ProcessorException;
    
    /**
     * Gets the priority of this processor. Lower values execute first.
     * 
     * @return Priority value (0 = highest priority)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Gets the name of this processor for logging and debugging purposes.
     * 
     * @return Processor name
     */
    String getName();
    
    /**
     * Indicates whether this processor can handle streaming responses.
     * If false, the processor will only be applied to complete responses.
     * 
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return false;
    }
    
    /**
     * Exception thrown by processors to indicate processing errors.
     */
    class ProcessorException extends Exception {
        private final boolean retryable;
        
        public ProcessorException(String message) {
            this(message, false);
        }
        
        public ProcessorException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
        
        public ProcessorException(String message, Throwable cause) {
            this(message, cause, false);
        }
        
        public ProcessorException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}