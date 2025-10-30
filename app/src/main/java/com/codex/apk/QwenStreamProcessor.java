package com.codex.apk;

import android.util.Log;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.WebSource;
import com.codex.apk.ChatMessage;
import com.codex.apk.ToolExecutor;
import com.codex.apk.editor.AiAssistantManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.Response;

public class QwenStreamProcessor {

    @FunctionalInterface
    public interface PartialUpdateCallback {
        void onUpdate(String partialResult, boolean isThinking);
    }

    private static final String TAG = "QwenStreamProcessor";

    private final AIAssistant.AIActionListener actionListener;
    private final QwenConversationState conversationState;
    private final AIModel model;
    private final File projectDir;

    public QwenStreamProcessor(AIAssistant.AIActionListener actionListener, QwenConversationState conversationState, AIModel model, File projectDir) {
        this.actionListener = actionListener;
        this.conversationState = conversationState;
        this.model = model;
        this.projectDir = projectDir;
    }

    public static class StreamProcessingResult {
        public final boolean isContinuation;
        public final String continuationJson;

        public StreamProcessingResult(boolean isContinuation, String continuationJson) {
            this.isContinuation = isContinuation;
            this.continuationJson = continuationJson;
        }
    }

    private ChatMessage.ToolUsage buildToolUsage(String name, JsonObject args) {
        ChatMessage.ToolUsage usage = new ChatMessage.ToolUsage(name != null ? name : "tool");
        if (args != null) {
            usage.argsJson = args.toString();
            if (args.has("path")) {
                usage.filePath = args.get("path").getAsString();
            } else if (args.has("oldPath")) {
                usage.filePath = args.get("oldPath").getAsString();
            }
        }
        usage.status = "running";
        return usage;
    }

    private void updateToolUsageFromResult(ChatMessage.ToolUsage usage,
                                           String name,
                                           JsonObject args,
                                           JsonObject result,
                                           long durationMs) {
        if (usage == null) return;
        usage.durationMs = durationMs;
        usage.ok = result != null && result.has("ok") && result.get("ok").getAsBoolean();
        usage.status = usage.ok ? "completed" : "failed";
        usage.resultJson = result != null ? result.toString() : null;

        if (args != null && ("readFile".equals(name) || "listFiles".equals(name) ||
                "searchInProject".equals(name) || "grepSearch".equals(name))) {
            if (args.has("path") && (usage.filePath == null || usage.filePath.isEmpty())) {
                usage.filePath = args.get("path").getAsString();
            }
        }
    }

    private void recordToolUsages(List<ChatMessage.ToolUsage> usages) {
        if (usages == null || usages.isEmpty()) {
            return;
        }
        if (actionListener instanceof AiAssistantManager) {
            ((AiAssistantManager) actionListener).recordToolUsages(usages);
        }
    }

    public StreamProcessingResult process(Response response) throws IOException {
        StringBuilder thinkingContent = new StringBuilder();
        StringBuilder answerContent = new StringBuilder();
        List<com.codex.apk.ai.WebSource> webSources = new ArrayList<>();
        Set<String> seenWebUrls = new HashSet<>();
        StringBuilder rawStreamData = new StringBuilder();

        String line;
        boolean firstLineChecked = false;
        while ((line = response.body().source().readUtf8Line()) != null) {
            rawStreamData.append(line).append("\n");
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (!firstLineChecked) {
                firstLineChecked = true;
                // Some servers send an initial JSON line before SSE data:
                if (t.startsWith("{") || t.startsWith("[")) {
                    try {
                        JsonObject data = JsonParser.parseString(t).getAsJsonObject();
                        if (data.has("response.created")) {
                            JsonObject created = data.getAsJsonObject("response.created");
                            if (created.has("chat_id")) conversationState.setConversationId(created.get("chat_id").getAsString());
                            if (created.has("response_id")) conversationState.setLastParentId(created.get("response_id").getAsString());
                            if (actionListener != null) actionListener.onQwenConversationStateUpdated(conversationState);
                            continue;
                        }
                    } catch (Exception ignore) {}
                }
            }
            if ("data: [DONE]".equals(t) || "[DONE]".equals(t)) {
                String finalContentDone = answerContent.length() > 0 ? answerContent.toString() : thinkingContent.toString();
                notifyListener(rawStreamData.toString(), finalContentDone, thinkingContent.toString(), webSources);
                if (actionListener != null) actionListener.onQwenConversationStateUpdated(conversationState);
                break;
            }
            if (t.startsWith("data: ")) {
                String jsonData = t.substring(6);
                if (jsonData.trim().isEmpty()) continue;
                String trimmedJson = jsonData.trim();
                if (!(trimmedJson.startsWith("{") || trimmedJson.startsWith("["))) continue;
                try {
                    JsonObject data = JsonParser.parseString(trimmedJson).getAsJsonObject();
                    if (data.has("response.created")) {
                        JsonObject created = data.getAsJsonObject("response.created");
                        if (created.has("chat_id")) conversationState.setConversationId(created.get("chat_id").getAsString());
                        if (created.has("response_id")) conversationState.setLastParentId(created.get("response_id").getAsString());
                        if (actionListener != null) actionListener.onQwenConversationStateUpdated(conversationState);
                        continue;
                    }

                    if (data.has("choices")) {
                        JsonArray choices = data.getAsJsonArray("choices");
                        if (choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            String status = delta.has("status") ? delta.get("status").getAsString() : "";
                            String content = delta.has("content") ? delta.get("content").getAsString() : "";
                            String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";
                            JsonObject extra = delta.has("extra") && delta.get("extra").isJsonObject() ? delta.getAsJsonObject("extra") : null;

                            if ("think".equals(phase)) {
                                thinkingContent.append(content);
                                if (actionListener != null) actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
                            } else if ("answer".equals(phase)) {
                                answerContent.append(content);
                                if (actionListener != null) actionListener.onAiStreamUpdate(answerContent.toString(), false);
                            }

                            // Collect web sources when present in extra
                            if (extra != null && extra.has("sources") && extra.get("sources").isJsonArray()) {
                                try {
                                    JsonArray srcArr = extra.getAsJsonArray("sources");
                                    for (int i = 0; i < srcArr.size(); i++) {
                                        JsonObject s = srcArr.get(i).getAsJsonObject();
                                        String url = s.has("url") ? s.get("url").getAsString() : null;
                                        String title = s.has("title") ? s.get("title").getAsString() : null;
                                        String snippet = s.has("snippet") ? s.get("snippet").getAsString() : null;
                                        String favicon = s.has("favicon") ? s.get("favicon").getAsString() : null;
                                        if (url != null && !seenWebUrls.contains(url)) {
                                            seenWebUrls.add(url);
                                            webSources.add(new WebSource(url, title, snippet, favicon));
                                        }
                                    }
                                } catch (Exception ignore) {}
                            }

                            if ("finished".equals(status)) {
                                String finalContent = answerContent.length() > 0 ? answerContent.toString() : thinkingContent.toString();
                                // Defensive: some responses end with empty content but valid fenced JSON earlier
                                String jsonToParse = extractJsonFromCodeBlock(finalContent);
                                if (jsonToParse == null && QwenResponseParser.looksLikeJson(finalContent)) {
                                    jsonToParse = finalContent;
                                }

                                if (jsonToParse != null) {
                                    try {
                                        JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
                                        if (maybe.has("action") && "tool_call".equals(maybe.get("action").getAsString())) {
                                            JsonArray calls = maybe.getAsJsonArray("tool_calls");
                                            JsonArray results = new JsonArray();
                                            List<ChatMessage.ToolUsage> usages = new ArrayList<>();
                                            for (int i = 0; i < calls.size(); i++) {
                                                JsonObject c = calls.get(i).getAsJsonObject();
                                                String name = c.get("name").getAsString();
                                                JsonObject args = c.has("args") && c.get("args").isJsonObject() ? c.getAsJsonObject("args") : new JsonObject();
                                                ChatMessage.ToolUsage usage = buildToolUsage(name, args);
                                                usages.add(usage);
                                                long start = System.currentTimeMillis();
                                                JsonObject toolResult;
                                                try {
                                                    toolResult = ToolExecutor.execute(projectDir, name, args);
                                                } catch (Exception ex) {
                                                    toolResult = new JsonObject();
                                                    toolResult.addProperty("ok", false);
                                                    toolResult.addProperty("error", ex.getMessage());
                                                }
                                                updateToolUsageFromResult(usage, name, args, toolResult, System.currentTimeMillis() - start);
                                                JsonObject res = new JsonObject();
                                                res.addProperty("name", name);
                                                res.add("result", toolResult);
                                                results.add(res);
                                            }
                                            recordToolUsages(usages);
                                            String continuation = ToolExecutor.buildToolResultContinuation(results);
                                            return new StreamProcessingResult(true, continuation);
                                        }
                                    } catch (Exception e) {
                                        // Not a tool call, proceed with normal parsing
                                    }
                                }

                                // If still empty, try to salvage from last non-empty delta content
                                if ((finalContent == null || finalContent.trim().isEmpty())) {
                                    finalContent = recoverContentFromRaw(rawStreamData.toString());
                                }
                                notifyListener(rawStreamData.toString(), finalContent, thinkingContent.toString(), webSources);
                                if (actionListener != null) actionListener.onQwenConversationStateUpdated(conversationState);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing stream data chunk", e);
                    if (actionListener != null) actionListener.onAiError("Stream error: " + e.getMessage());
                    break;
                }
            }
        }
        if (actionListener != null) actionListener.onAiRequestCompleted();
        return new StreamProcessingResult(false, null);
    }

    private void notifyListener(String rawResponse, String finalContent, String thinkingContent, List<com.codex.apk.ai.WebSource> webSources) {
        String jsonToParse = extractJsonFromCodeBlock(finalContent);
        if (jsonToParse == null && QwenResponseParser.looksLikeJson(finalContent)) {
            jsonToParse = finalContent;
        }

        if (jsonToParse != null) {
            QwenResponseParser.parseResponseAsync(jsonToParse, rawResponse, new QwenResponseParser.ParseResultListener() {
                @Override
                public void onParseSuccess(QwenResponseParser.ParsedResponse parsed) {
                    if (parsed != null && parsed.isValid) {
                        if ("plan".equals(parsed.action)) {
                            List<ChatMessage.PlanStep> planSteps = QwenResponseParser.toPlanSteps(parsed);
                            actionListener.onAiActionsProcessed(rawResponse, parsed.explanation, new ArrayList<>(), new ArrayList<>(), planSteps, model.getDisplayName());
                        } else if (parsed.action != null && parsed.action.contains("file")) {
                            List<ChatMessage.FileActionDetail> details = QwenResponseParser.toFileActionDetails(parsed);
                            enrichFileActionDetails(details);
                            notifyAiActionsProcessed(rawResponse, parsed.explanation, new ArrayList<>(), details, thinkingContent, webSources);
                        } else {
                            notifyAiActionsProcessed(rawResponse, parsed.explanation, new ArrayList<>(), new ArrayList<>(), thinkingContent, webSources);
                        }
                    } else {
                        notifyAiActionsProcessed(rawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), thinkingContent, webSources);
                    }
                }

                @Override
                public void onParseFailed() {
                    Log.e(TAG, "Failed to parse extracted JSON, treating as text.");
                    notifyAiActionsProcessed(rawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), thinkingContent, webSources);
                }
            });
        } else {
            notifyAiActionsProcessed(rawResponse, finalContent, new ArrayList<>(), new ArrayList<>(), thinkingContent, webSources);
        }
    }

    public String recoverContentFromRaw(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Heuristic: concatenate all answer-phase content fragments (delta.content) between code fences
        StringBuilder sb = new StringBuilder();
        try {
            String[] lines = raw.split("\n");
            boolean inside = false;
            for (String l : lines) {
                String t = l.trim();
                if (!t.startsWith("data: ")) continue;
                String json = t.substring(6).trim();
                if (!(json.startsWith("{") || json.startsWith("["))) continue;
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (!obj.has("choices")) continue;
                JsonArray choices = obj.getAsJsonArray("choices");
                if (choices.size() == 0) continue;
                JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                if (delta == null) continue;
                String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";
                String content = delta.has("content") && !delta.get("content").isJsonNull() ? delta.get("content").getAsString() : "";
                if (content == null) content = "";
                // Track code fence blocks; if the model emitted ```json ... ``` chunks, try to reassemble
                if ("answer".equals(phase)) {
                    if (content.startsWith("```")) inside = true;
                    if (inside) sb.append(content);
                    if (content.endsWith("```")) inside = false;
                }
            }
        } catch (Exception ignore) {}
        return sb.toString();
    }

    private void enrichFileActionDetails(List<ChatMessage.FileActionDetail> details) {
        if (details == null || projectDir == null) return;
        for (ChatMessage.FileActionDetail d : details) {
            try {
                switch (d.type) {
                    case "createFile": {
                        d.oldContent = "";
                        if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.newContent != null ? d.newContent : "";
                        if (d.newContent.isEmpty() && d.replaceWith != null) d.newContent = d.replaceWith; // fallback
                        break;
                    }
                    case "updateFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        if (d.newContent == null || d.newContent.isEmpty()) {
                            String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                            String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                            String computed = null;
                            if (pattern != null && !pattern.isEmpty() && repl != null) {
                                computed = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
                            } else if (d.insertLines != null && d.startLine > 0) {
                                computed = com.codex.apk.util.FileOps.applyModifyLines(old, d.startLine, d.deleteCount, d.insertLines);
                            }
                            if (computed != null) {
                                d.newContent = computed;
                            } else if (d.diffPatch != null && !d.diffPatch.isEmpty()) {
                                // Leave newContent as null; diff will be shown using provided unified patch
                            } else {
                                d.newContent = d.oldContent; // fallback to avoid nulls
                            }
                        }
                        break;
                    }
                    case "searchAndReplace": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                        String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                        d.newContent = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
                        break;
                    }
                    case "smartUpdate": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        String mode = d.updateType != null ? d.updateType : "full";
                        if ("append".equals(mode)) {
                            d.newContent = (old != null ? old : "") + (d.newContent != null ? d.newContent : d.contentType != null ? d.contentType : "");
                        } else if ("prepend".equals(mode)) {
                            d.newContent = (d.newContent != null ? d.newContent : "") + (old != null ? old : "");
                        } else if ("replace".equals(mode)) {
                            String pattern = d.searchPattern != null ? d.searchPattern : d.search;
                            String repl = d.replaceWith != null ? d.replaceWith : d.replace;
                            d.newContent = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
                        } else {
                            // full or unknown
                            if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.replaceWith != null ? d.replaceWith : "";
                        }
                        break;
                    }
                    case "deleteFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = "";
                        break;
                    }
                    case "renameFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.oldPath));
                        d.oldContent = old;
                        d.newContent = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.newPath));
                        break;
                    }
                    case "modifyLines": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        d.newContent = com.codex.apk.util.FileOps.applyModifyLines(old, d.startLine, d.deleteCount, d.insertLines);
                        break;
                    }
                    case "patchFile": {
                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
                        d.oldContent = old;
                        // Applying a unified diff is non-trivial; leave newContent empty to rely on diffPatch
                        break;
                    }
                    default: {
                        // No-op
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to enrich action detail for path " + d.path + ": " + e.getMessage());
            }
        }
    }

    private String extractJsonFromCodeBlock(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        String genericPattern = "```\\s*([\\s\\S]*?)```";
        pattern = java.util.regex.Pattern.compile(genericPattern);
        matcher = pattern.matcher(content);
        if (matcher.find()) {
            String extracted = matcher.group(1).trim();
            if (QwenResponseParser.looksLikeJson(extracted)) {
                return extracted;
            }
        }
        return null;
    }

    private void notifyAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> fileActions, String thinking, List<com.codex.apk.ai.WebSource> sources) {
        if (actionListener instanceof com.codex.apk.editor.AiAssistantManager) {
            ((com.codex.apk.editor.AiAssistantManager) actionListener).onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, fileActions, model.getDisplayName(), thinking, sources);
        } else {
            String fallback = com.codex.apk.util.ResponseUtils.buildExplanationWithThinking(explanation, thinking);
            actionListener.onAiActionsProcessed(rawAiResponseJson, fallback, suggestions, fileActions, model.getDisplayName());
        }
    }

    public static boolean isErrorChunk(JsonObject chunk) {
        // Simple check for now, can be expanded
        return chunk.has("error");
    }

    public static void processChunk(JsonObject data, QwenConversationState state, StringBuilder finalText, PartialUpdateCallback callback, AIAssistant.AIActionListener listener) {
        try {
            if (data.has("response.created")) {
                JsonObject created = data.getAsJsonObject("response.created");
                if (created.has("chat_id")) state.setConversationId(created.get("chat_id").getAsString());
                if (created.has("response_id")) state.setLastParentId(created.get("response_id").getAsString());
                if (listener != null) listener.onQwenConversationStateUpdated(state);
                return;
            }

            if (data.has("choices")) {
                JsonArray choices = data.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    if (choice.has("delta")) {
                        JsonObject delta = choice.getAsJsonObject("delta");
                        String content = delta.has("content") ? delta.get("content").getAsString() : "";
                        String phase = delta.has("phase") ? delta.get("phase").getAsString() : "answer";
                        finalText.append(content);
                        if (callback != null) {
                            callback.onUpdate(finalText.toString(), "think".equals(phase));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error processing stream chunk in QwenStreamProcessor", e);
        }
    }
}