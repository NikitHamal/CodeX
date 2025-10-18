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

/**
 * Coordinates execution of AI-requested tool invocations, updating the chat UI as tools run.
 */
public class ToolExecutionCoordinator {
    private static final String TAG = "ToolExecutionCoordinator";

    private final EditorActivity activity;
    private final ExecutorService executorService;
    private final Consumer<JsonArray> continuationCallback;
    private List<ChatMessage.ToolUsage> lastToolUsages = new ArrayList<>();

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

    public JsonArray executeTools(JsonArray toolCalls,
                                  File projectDir,
                                  Integer toolsMessagePosition,
                                  AIChatFragment uiFrag) {
        JsonArray results = new JsonArray();
        if (toolCalls == null || projectDir == null) return results;

        executorService.execute(() -> {
            long startAll = System.currentTimeMillis();
            for (int i = 0; i < toolCalls.size(); i++) {
                JsonObject call = toolCalls.get(i).getAsJsonObject();
                ChatMessage.ToolUsage usage = lastToolUsages.get(i);
                long startOne = System.currentTimeMillis();
                try {
                    String name = call.get("name").getAsString();
                    JsonObject args = call.has("args") && call.get("args").isJsonObject()
                            ? call.getAsJsonObject("args")
                            : new JsonObject();
                    JsonObject result = ToolExecutor.execute(projectDir, name, args);

                    JsonObject payload = new JsonObject();
                    payload.addProperty("name", name);
                    payload.add("result", result);
                    synchronized (results) {
                        results.add(payload);
                    }

                    updateUsage(usage, name, args, result, System.currentTimeMillis() - startOne);
                } catch (Exception ex) {
                    Log.w(TAG, "Tool execution failed", ex);
                    JsonObject payload = new JsonObject();
                    payload.addProperty("name", "unknown");
                    JsonObject err = new JsonObject();
                    err.addProperty("ok", false);
                    err.addProperty("error", ex.getMessage());
                    payload.add("result", err);
                    synchronized (results) {
                        results.add(payload);
                    }
                    usage.ok = false;
                    usage.resultJson = err.toString();
                    usage.status = "failed";
                    usage.durationMs = System.currentTimeMillis() - startOne;
                }

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
                JsonArray snapshot;
                synchronized (results) {
                    snapshot = results.deepCopy();
                }
                continuationCallback.accept(snapshot);
            }

        });

        return results;
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
