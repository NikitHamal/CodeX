package com.codex.apk.core.model;

/**
 * Tool call representation.
 */
public class ToolCall {
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
