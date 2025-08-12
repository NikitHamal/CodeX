package com.codex.apk;

public class CustomAgent {
    public final String id;
    public String name;
    public String prompt;
    public String modelId;

    public CustomAgent(String id, String name, String prompt, String modelId) {
        this.id = id;
        this.name = name;
        this.prompt = prompt;
        this.modelId = modelId;
    }
}