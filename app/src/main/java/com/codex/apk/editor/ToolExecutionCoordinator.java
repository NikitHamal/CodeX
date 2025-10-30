package com.codex.apk.editor;

import android.util.Log;

import com.codex.apk.ChatMessage;
import com.codex.apk.ToolExecutor;
import com.codex.apk.AIChatFragment;
import com.codex.apk.EditorActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.codex.apk.ParallelToolExecutor;

/**
 * Coordinates execution of AI-requested tool invocations, updating the chat UI as tools run.
 */
public class ToolExecutionCoordinator {
    private static final String TAG = "ToolExecutionCoordinator";

    private final EditorActivity activity;
    private final ExecutorService executorService;
    private final Consumer<JsonArray> continuationCallback;
    private List<ChatMessage.ToolUsage> lastToolUsages = new ArrayList<>();
    private ParallelToolExecutor parallelToolExecutor;

    public ToolExecutionCoordinator(EditorActivity activity,
                                    ExecutorService executorService,
                                    Consumer<JsonArray> continuationCallback) {
        this.activity = activity;
        this.executorService = executorService;
        this.continuationCallback = continuationCallback;
    }

    public Integer displayRunningTools(AIChatFragment uiFrag,
                                       String modelDisplayName,
                                       String rawResponse,
                                       JsonArray toolCalls) {
        if (uiFrag == null || toolCalls == null) return null;

        List<ChatMessage.ToolUsage> usages = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            JsonObject call = toolCalls.get(i).getAsJsonObject();
            String toolName = call.has("name") ? call.get("name").getAsString() : "tool";
            ChatMessage.ToolUsage usage = new ChatMessage.ToolUsage(toolName);
            if (call.has("args") && call.get("args").isJsonObject()) {
                JsonObject args = call.getAsJsonObject("args");
                usage.argsJson = args.toString();
                if (args.has("path")) {
                    usage.filePath = args.get("path").getAsString();
                } else if (args.has("oldPath")) {
                    usage.filePath = args.get("oldPath").getAsString();
                }
            }
            usage.status = "running";
            usages.add(usage);
        }

        ChatMessage toolsMessage = new ChatMessage(
                ChatMessage.SENDER_AI,
                "Running tools...",
                null,
                null,
                modelDisplayName,
                System.currentTimeMillis(),
                rawResponse,
                new ArrayList<>(),
                ChatMessage.STATUS_NONE
        );
        toolsMessage.setToolUsages(usages);

        lastToolUsages = usages;
        return uiFrag.addMessage(toolsMessage);
    }

    public void executeTools(JsonArray toolCalls,
                             File projectDir,
                             Integer toolsMessagePosition,
                             AIChatFragment uiFrag) {
        if (toolCalls == null || projectDir == null) return;

        if (parallelToolExecutor == null) {
            parallelToolExecutor = new ParallelToolExecutor(projectDir);
        }

        parallelToolExecutor.executeTools(lastToolUsages).thenAccept(results -> {
            long startAll = System.currentTimeMillis();
            JsonArray jsonResults = new JsonArray();

            for (int i = 0; i < results.size(); i++) {
                ParallelToolExecutor.ToolResult toolResult = results.get(i);
                ChatMessage.ToolUsage usage = lastToolUsages.get(i);
                updateUsage(usage, toolResult.toolName, new JsonObject(), toolResult.result, 0); // Duration is not available per tool

                JsonObject payload = new JsonObject();
                payload.addProperty("toolName", toolResult.toolName);
                payload.add("result", toolResult.result);
                jsonResults.add(payload);

                if (uiFrag != null && toolsMessagePosition != null) {
                    int finalIndex = toolsMessagePosition;
                    activity.runOnUiThread(() -> uiFrag.updateMessage(finalIndex, uiFrag.getMessageAt(finalIndex)));
                }
            }

            long allDuration = System.currentTimeMillis() - startAll;
            if (!lastToolUsages.isEmpty()) {
                ChatMessage.ToolUsage first = lastToolUsages.get(0);
                if (first != null) {
                    first.resultJson = "Completed in " + allDuration + " ms";
                }
            }

            if (continuationCallback != null) {
                continuationCallback.accept(jsonResults);
            }
        });
    }

    private void updateUsage(ChatMessage.ToolUsage usage,
                              String name,
                              JsonObject args,
                              JsonObject result,
                              long durationMs) {
        usage.durationMs = durationMs;
        usage.ok = result.has("ok") && result.get("ok").getAsBoolean();
        usage.status = usage.ok ? "completed" : "failed";
        usage.resultJson = result.toString();

        if (("readFile".equals(name) || "listFiles".equals(name)
                || "searchInProject".equals(name) || "grepSearch".equals(name)) && args != null) {
            if (args.has("path") && (usage.filePath == null || usage.filePath.isEmpty())) {
                usage.filePath = args.get("path").getAsString();
            }
        }
    }

    public List<ChatMessage.ToolUsage> getLastToolUsages() {
        return lastToolUsages;
    }

    public static String buildContinuationPayload(JsonArray results) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "tool_result");
        payload.add("results", results);
        return payload.toString();
    }
}
