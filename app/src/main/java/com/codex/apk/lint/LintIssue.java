package com.codex.apk.lint;

public class LintIssue {
    public enum Severity { INFO, WARNING, ERROR }

    public final String path;
    public final int line;
    public final int column;
    public final Severity severity;
    public final String message;

    public LintIssue(String path, int line, int column, Severity severity, String message) {
        this.path = path;
        this.line = line;
        this.column = column;
        this.severity = severity;
        this.message = message;
    }

    @Override
    public String toString() {
        return path + ":" + line + ":" + column + " [" + severity + "] " + message;
    }
}


