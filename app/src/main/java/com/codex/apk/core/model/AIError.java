package com.codex.apk.core.model;

/**
 * AI error information.
 */
public class AIError {
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
