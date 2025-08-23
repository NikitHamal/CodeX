package com.codex.apk.core.model;

/**
 * Execution context for requests.
 */
public class ExecutionContext {
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
