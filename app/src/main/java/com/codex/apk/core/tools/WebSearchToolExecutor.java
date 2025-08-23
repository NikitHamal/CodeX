package com.codex.apk.core.tools;

import com.codex.apk.core.model.ExecutionContext;
import com.codex.apk.core.model.ToolCall;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

/**
 * Minimal web search executor placeholder. Returns a search URL instead of performing network.
 */
class WebSearchToolExecutor implements ToolExecutor {
    @Override
    public ToolResult execute(ToolCall call, ExecutionContext context) throws Exception {
        String query = safe(extract(call.getArguments(), "query"));
        String engine = safe(extract(call.getArguments(), "engine"));
        String url;
        if (engine.equalsIgnoreCase("duckduckgo")) {
            url = "https://duckduckgo.com/?q=" + enc(query);
        } else if (engine.equalsIgnoreCase("bing")) {
            url = "https://www.bing.com/search?q=" + enc(query);
        } else {
            url = "https://www.google.com/search?q=" + enc(query);
        }
        return ToolResult.success(call.getId(), url);
    }

    @Override
    public Set<Permission> getRequiredPermissions() {
        return EnumSet.of(Permission.NETWORK_ACCESS);
    }

    @Override
    public boolean isCompatibleWith(ExecutionContext context) {
        return true;
    }

    private String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private String safe(String s) { return s == null ? "" : s; }

    // extremely naive key extractor from a json-ish string: "key":"value"
    private String extract(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = json.indexOf('"', i + needle.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}
