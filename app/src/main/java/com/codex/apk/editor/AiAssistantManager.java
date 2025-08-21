package com.codex.apk.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import com.codex.apk.AIChatFragment;
import com.codex.apk.AIAssistant;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.WebSource;
import com.codex.apk.AiProcessor; // Import AiProcessor
import com.codex.apk.ChatMessage;
import com.codex.apk.CodeEditorFragment;
import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;
import com.codex.apk.ToolSpec;
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DiffGenerator;
import com.codex.apk.QwenResponseParser; // Plan/file parsing
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.codex.apk.ToolExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.codex.apk.QwenResponseParser; // Plan/file parsing
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.codex.apk.ToolExecutor;

/**
 * Manages the interaction with the AIAssistant, handling UI updates and delegation
 * of AI-related actions from EditorActivity to the core AIAssistant logic.
 */
public class AiAssistantManager implements AIAssistant.AIActionListener { // Directly implement AIActionListener

    private static final String TAG = "AiAssistantManager";
    private final EditorActivity activity; // Reference to the hosting activity
    private final AIAssistant aiAssistant;
    private final FileManager fileManager;
    private final AiProcessor aiProcessor; // AiProcessor instance
    private final ExecutorService executorService;

    // Track the last plan message for status updates and autonomous run
    private Integer lastPlanMessagePosition = null;
    private int planProgressIndex = 0; // index into plan steps list for file-kind steps
    private int planStepRetryCount = 0;
    private boolean isExecutingPlan = false;
    private Deque<String> executedStepSummaries = new ArrayDeque<>();

    // Track current streaming AI message position
    private Integer currentStreamingMessagePosition = null;

    // Watchdog to prevent stuck running steps
    private final Handler planWatchdogHandler = new Handler(Looper.getMainLooper());
    private Runnable planWatchdogRunnable = null;
    private int planWatchdogForIndex = -1;

    // Cancel any active watchdog timer for the current plan step
    private void cancelPlanWatchdog() {
        if (planWatchdogRunnable != null) {
            planWatchdogHandler.removeCallbacks(planWatchdogRunnable);
            Log.d(TAG, "Plan watchdog cancelled for index=" + planWatchdogForIndex);
        }
        planWatchdogRunnable = null;
        planWatchdogForIndex = -1;
    }

    // Schedule a watchdog to fail a stuck running step after timeoutMs
    private void schedulePlanWatchdog(final int forIndex, long timeoutMs) {
        cancelPlanWatchdog();
        planWatchdogForIndex = forIndex;
        planWatchdogRunnable = new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "Plan watchdog fired for index=" + forIndex + ", currentIndex=" + planProgressIndex + ", isExecutingPlan=" + isExecutingPlan);
                if (!isExecutingPlan) { cancelPlanWatchdog(); return; }
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag == null || lastPlanMessagePosition == null) { cancelPlanWatchdog(); return; }
                ChatMessage pm = frag.getMessageAt(lastPlanMessagePosition);
                if (pm == null || pm.getPlanSteps() == null) { cancelPlanWatchdog(); return; }
                // Only act if we're still on the same running index
                if (planProgressIndex == forIndex) {
                    List<ChatMessage.PlanStep> steps = pm.getPlanSteps();
                    if (forIndex < steps.size()) {
                        ChatMessage.PlanStep ps = steps.get(forIndex);
                        String prev = ps != null ? ps.status : "<null>";
                        if (ps != null && "running".equals(prev)) {
                            ps.status = "failed";
                            planProgressIndex = forIndex + 1;
                            frag.updateMessage(lastPlanMessagePosition, pm);
                            Log.e(TAG, "Watchdog marked step failed and advanced. index=" + forIndex);
                        } else {
                            Log.w(TAG, "Watchdog fired but step not running. index=" + forIndex + ", status=" + prev);
                        }
                    }
                    planStepRetryCount = 0;
                    // Advance to next step attempt
                    sendNextPlanStepFollowUp();
                } else {
                    Log.i(TAG, "Watchdog fired for stale index; ignoring. firedFor=" + forIndex + ", nowAt=" + planProgressIndex);
                }
                cancelPlanWatchdog();
            }
        };
        Log.d(TAG, "Plan watchdog scheduled for index=" + forIndex + ", timeoutMs=" + timeoutMs);
        planWatchdogHandler.postDelayed(planWatchdogRunnable, timeoutMs);
    }

    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
                              FileManager fileManager, ExecutorService executorService) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.executorService = executorService;
        this.aiProcessor = new AiProcessor(projectDir, activity.getApplicationContext());

        String apiKey = SettingsActivity.getGeminiApiKey(activity);
        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);
        this.aiAssistant.setEnabledTools(com.codex.apk.ToolSpec.defaultFileToolsPlusSearchNet());

        // Model selection: prefer per-project last-used, else global default, else fallback
        SharedPreferences settingsPrefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences modelPrefs = activity.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
        String projectKey = "project_" + (projectName != null ? projectName : "default") + "_last_model";
        String lastUsed = settingsPrefs.getString(projectKey, null);
        String defaultModelName = modelPrefs.getString("default_model", null);
        String initialName = lastUsed != null ? lastUsed : (defaultModelName != null ? defaultModelName : AIModel.fromModelId("qwen3-coder-plus").getDisplayName());
        AIModel initialModel = AIModel.fromDisplayName(initialName);
        if (initialModel != null) {
            this.aiAssistant.setCurrentModel(initialModel);
        }
    }

    public AIAssistant getAIAssistant() { return aiAssistant; }

    public void onResume() {
        String updatedApiKey = SettingsActivity.getGeminiApiKey(activity);
        String currentApiKeyInAssistant = aiAssistant.getApiKey();
        if (!updatedApiKey.equals(currentApiKeyInAssistant)) {
            Log.i(TAG, "API Key changed in settings. Updating AIAssistant API key.");
            aiAssistant.setApiKey(updatedApiKey);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void sendAiPrompt(String userPrompt, List<ChatMessage> chatHistory, com.codex.apk.QwenConversationState qwenState, TabItem activeTabItem) {
        if (!isNetworkAvailable()) {
            activity.showToast("No internet connection.");
            return;
        }
        String currentFileContent = "";
        String currentFileName = "";
        if (activeTabItem != null) {
            currentFileContent = activeTabItem.getContent();
            currentFileName = activeTabItem.getFileName();
        }

        if (aiAssistant == null) {
            activity.showToast("AI Assistant is not available.");
            Log.e(TAG, "sendAiPrompt: AIAssistant not initialized!");
            return;
        }

        try {
            aiAssistant.sendPrompt(userPrompt, chatHistory, qwenState, currentFileName, currentFileContent);
            // Persist per-project last used model
            String projectKey = "project_" + (activity.getProjectName() != null ? activity.getProjectName() : "default") + "_last_model";
            activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString(projectKey, aiAssistant.getCurrentModel().getDisplayName())
                    .apply();
        } catch (Exception e) {
            activity.showToast("AI Error: " + e.getMessage());
            Log.e(TAG, "AI processing error", e);
        }
    }

    public void onAiAcceptActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User accepted AI actions for message at position: " + messagePosition);
        if (message.getProposedFileChanges() == null || message.getProposedFileChanges().isEmpty()) {
            activity.showToast("No proposed changes to apply.");
            return;
        }

        boolean isAgent = aiAssistant != null && aiAssistant.isAgentModeEnabled();

        if (!isAgent) {
            executorService.execute(() -> {
                try {
                    List<String> appliedSummaries = new ArrayList<>();
                    for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                        String summary = aiProcessor.applyFileAction(detail);
                        appliedSummaries.add(summary);
                    }
                    activity.runOnUiThread(() -> {
                        activity.showToast("AI actions applied successfully!");
                        message.setStatus(ChatMessage.STATUS_ACCEPTED);
                        message.setActionSummaries(appliedSummaries);
                        AIChatFragment aiChatFragment = activity.getAiChatFragment();
                        if (aiChatFragment != null) {
                            aiChatFragment.updateMessage(messagePosition, message);
                        }
                        activity.tabManager.refreshOpenTabsAfterAi();
                        activity.loadFileTree();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error applying AI actions: " + e.getMessage(), e);
                    activity.runOnUiThread(() -> activity.showToast("Failed to apply AI actions: " + e.getMessage()));
                }
            });
            return;
        }

        // Agent mode: auto-apply without additional approval
        executorService.execute(() -> {
            List<String> appliedSummaries = new ArrayList<>();
            List<ChatMessage.FileActionDetail> steps = message.getProposedFileChanges();

            boolean anyFailed = false;
            for (int i = 0; i < steps.size(); i++) {
                ChatMessage.FileActionDetail step = steps.get(i);

                try {
                    String summary = aiProcessor.applyFileAction(step);
                    appliedSummaries.add(summary);
                    executedStepSummaries.add(summary);
                    step.stepStatus = "completed";
                    step.stepMessage = "Completed";
                } catch (Exception ex) {
                    Log.e(TAG, "Agent step failed: " + step.getSummary(), ex);
                    step.stepStatus = "failed";
                    step.stepMessage = ex.getMessage();
                    executedStepSummaries.add("FAILED: " + step.getSummary() + " - " + ex.getMessage());
                    anyFailed = true;
                }

                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) {
                        frag.updateMessage(messagePosition, message);
                    }
                });
            }

            boolean anyFailedBatch = anyFailed;
            activity.runOnUiThread(() -> {
                message.setStatus(ChatMessage.STATUS_ACCEPTED);
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag != null) frag.updateMessage(messagePosition, message);
                activity.tabManager.refreshOpenTabsAfterAi();
                activity.loadFileTree();
                activity.showToast("Agent step applied");
                // After finishing this file_operation batch as part of plan, advance to next step automatically
                if (isExecutingPlan) {
                    // Mark the current plan step as completed/failed ONCE per batch
                    setCurrentRunningPlanStepStatus(anyFailedBatch ? "failed" : "completed");
                    // Refresh the plan message to reflect the status change
                    if (lastPlanMessagePosition != null) {
                        ChatMessage planMsg = frag != null ? frag.getMessageAt(lastPlanMessagePosition) : null;
                        if (frag != null && planMsg != null) frag.updateMessage(lastPlanMessagePosition, planMsg);
                    }
                    cancelPlanWatchdog();
                    sendNextPlanStepFollowUp();
                }
            });
        });
    }

    // Plan step status helpers
    private void setNextPlanStepStatus(String status) {
        if (lastPlanMessagePosition == null) return;
        AIChatFragment frag = activity.getAiChatFragment();
        if (frag == null) return;
        ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
        if (planMsg == null || planMsg.getPlanSteps() == null || planMsg.getPlanSteps().isEmpty()) return;

        List<ChatMessage.PlanStep> steps = planMsg.getPlanSteps();
        while (planProgressIndex < steps.size()) {
            ChatMessage.PlanStep ps = steps.get(planProgressIndex);
            if (ps != null && (ps.kind == null || ps.kind.equalsIgnoreCase("file")) &&
                    !"completed".equals(ps.status) && !"failed".equals(ps.status)) {
                ps.status = status;
                Log.d(TAG, "setNextPlanStepStatus: idx=" + planProgressIndex + " -> " + status);
                break;
            }
            planProgressIndex++;
        }
    }

    private void setCurrentRunningPlanStepStatus(String status) {
        if (lastPlanMessagePosition == null) return;
        AIChatFragment frag = activity.getAiChatFragment();
        if (frag == null) return;
        ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
        if (planMsg == null || planMsg.getPlanSteps() == null || planMsg.getPlanSteps().isEmpty()) return;
        List<ChatMessage.PlanStep> steps = planMsg.getPlanSteps();
        if (planProgressIndex < steps.size()) {
            ChatMessage.PlanStep ps = steps.get(planProgressIndex);
            if (ps != null) {
                String prev = ps.status;
                ps.status = status;
                Log.d(TAG, "setCurrentRunningPlanStepStatus: idx=" + planProgressIndex + " " + prev + " -> " + status);
                planProgressIndex++;
            }
        }
    }

    // Centralized cleanup to avoid lingering running states and UI placeholders
    private void finalizePlanExecution(String toastMessage, boolean sanitizeDanglingRunning) {
        AIChatFragment frag = activity.getAiChatFragment();
        if (sanitizeDanglingRunning && frag != null && lastPlanMessagePosition != null) {
            ChatMessage pm = frag.getMessageAt(lastPlanMessagePosition);
            if (pm != null && pm.getPlanSteps() != null) {
                boolean changed = false;
                for (ChatMessage.PlanStep ps : pm.getPlanSteps()) {
                    if (ps != null && "running".equals(ps.status)) { ps.status = "failed"; changed = true; }
                }
                if (changed) { frag.updateMessage(lastPlanMessagePosition, pm); }
            }
        }
        cancelPlanWatchdog();
        isExecutingPlan = false;
        if (frag != null) { frag.hideThinkingMessage(); }
        currentStreamingMessagePosition = null;
        lastPlanMessagePosition = null;
        planProgressIndex = 0;
        planStepRetryCount = 0;
        executedStepSummaries.clear();
        if (toastMessage != null) activity.showToast(toastMessage);
        Log.i(TAG, "Plan execution finalized. sanitizeDanglingRunning=" + sanitizeDanglingRunning);
    }

    // Orchestrate detailed autonomous follow-ups per plan step with rich context
    private void sendNextPlanStepFollowUp() {
        AIChatFragment frag = activity.getAiChatFragment();
        if (frag == null || lastPlanMessagePosition == null) {
            finalizePlanExecution("Plan completed", true);
            return;
        }
        ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
        if (planMsg == null || planMsg.getPlanSteps() == null || planMsg.getPlanSteps().isEmpty()) {
            finalizePlanExecution("Plan completed", true);
            return;
        }
        List<ChatMessage.PlanStep> steps = planMsg.getPlanSteps();

        // Find next file-kind step starting at planProgressIndex
        int idx = planProgressIndex;
        while (idx < steps.size()) {
            ChatMessage.PlanStep s = steps.get(idx);
            if (s != null && (s.kind == null || "file".equalsIgnoreCase(s.kind))
                    && !"completed".equals(s.status) && !"failed".equals(s.status)) {
                break;
            }
            idx++;
        }
        if (idx >= steps.size()) {
            // All steps done
            finalizePlanExecution("Plan completed", false);
            return;
        }

        // Build follow-up prompt for this step
        ChatMessage.PlanStep target = steps.get(idx);
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are executing an approved plan step.\n");
        prompt.append("Step ID: ").append(target.id != null ? target.id : String.valueOf(idx + 1)).append("\n");
        prompt.append("Step Title: ").append(target.title != null ? target.title : "").append("\n\n");
        prompt.append("Output strictly a single JSON object with action=\"file_operation\" inside a ```json fenced code block.\n");
        prompt.append("No natural language outside the JSON. Use fields appropriate to file operations.\n\n");
        prompt.append("Context summary:\n");
        prompt.append("- Plan (truncated): ").append(safeTruncate(planToJson(planMsg), 2000)).append("\n");
        if (!executedStepSummaries.isEmpty()) {
            prompt.append("- Executed steps so far:\n");
            int count = 0;
            for (String ssum : executedStepSummaries) {
                if (count++ >= 10) break;
                prompt.append("  • ").append(ssum).append("\n");
            }
        }
        prompt.append("- File tree (project root):\n");
        prompt.append(safeTruncate(buildFileTree(activity.getProjectDirectory(), 3, 200), 3000)).append("\n");
        TabItem active = activity.getActiveTab();
        if (active != null) {
            prompt.append("- Active file: ").append(active.getFileName()).append("\n");
            prompt.append("- Active file content (truncated):\n---\n");
            prompt.append(safeTruncate(active.getContent(), 2000)).append("\n---\n");
        }
        prompt.append("Proceed now with the step. Return only the JSON.\n");

        Log.d(TAG, "sendNextPlanStepFollowUp: next file-kind step idx=" + idx);
        isExecutingPlan = true;
        // Mark step running visually before sending
        setNextPlanStepStatus("running");
        activity.runOnUiThread(() -> {
            if (lastPlanMessagePosition != null) {
                ChatMessage pm = activity.getAiChatFragment().getMessageAt(lastPlanMessagePosition);
                if (pm != null) activity.getAiChatFragment().updateMessage(lastPlanMessagePosition, pm);
            }
        });

        // Cancel any previous watchdog and schedule a new one for this running step
        cancelPlanWatchdog();
        final int runningIndex = planProgressIndex;
        schedulePlanWatchdog(runningIndex, 30_000);

        sendAiPrompt(prompt.toString(), new ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
    }

    private String planToJson(ChatMessage planMsg) {
        try {
            String raw = planMsg.getRawApiResponse();
            if (raw != null && raw.trim().startsWith("{")) return raw;
        } catch (Exception ignored) {}
        return "{\"action\":\"plan\"}";
    }

    private String safeTruncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)) + "\n...";
    }

    private String buildFileTree(File root, int maxDepth, int maxEntries) {
        StringBuilder sb = new StringBuilder();
        buildFileTreeRec(root, 0, maxDepth, sb, new int[]{0}, maxEntries);
        return sb.toString();
    }

    // Extract the first JSON object from mixed text. Prefer ```json fenced blocks.
    private String extractFirstJsonObjectFromText(String input) {
        if (input == null) return null;
        try {
            String s = input.trim();
            // Prefer fenced code blocks ```json ... ```
            int fenceStart = indexOfIgnoreCase(s, "```json");
            if (fenceStart >= 0) {
                fenceStart = s.indexOf('{', fenceStart);
                if (fenceStart >= 0) {
                    int end = findMatchingBraceEnd(s, fenceStart);
                    if (end > fenceStart) return s.substring(fenceStart, end + 1);
                }
            }
            // Fallback: first top-level JSON object
            int firstBrace = s.indexOf('{');
            if (firstBrace >= 0) {
                int end = findMatchingBraceEnd(s, firstBrace);
                if (end > firstBrace) return s.substring(firstBrace, end + 1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int indexOfIgnoreCase(String haystack, String needle) {
        String h = haystack.toLowerCase();
        String n = needle.toLowerCase();
        return h.indexOf(n);
    }

    // Finds the end index of a balanced JSON object starting at '{'
    private int findMatchingBraceEnd(String s, int startIdx) {
        int depth = 0; boolean inString = false; boolean escape = false;
        for (int i = startIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) { escape = false; continue; }
                if (c == '\\') { escape = true; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private void buildFileTreeRec(File dir, int depth, int maxDepth, StringBuilder sb, int[] count, int maxEntries) {
        if (dir == null || !dir.exists() || count[0] >= maxEntries) return;
        if (depth > maxDepth) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (count[0]++ >= maxEntries) return;
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(f.isDirectory() ? "[d] " : "[f] ").append(f.getName()).append("\n");
            if (f.isDirectory()) buildFileTreeRec(f, depth + 1, maxDepth, sb, count, maxEntries);
        }
    }

    // Public API required by EditorActivity and UI
    public void onAiDiscardActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User discarded AI actions for message at position: " + messagePosition);
        message.setStatus(ChatMessage.STATUS_DISCARDED);
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        activity.showToast("AI actions discarded.");
    }

    public void onReapplyActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User requested to reapply AI actions for message at position: " + messagePosition);
        onAiAcceptActions(messagePosition, message);
    }

    public void acceptPlan(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User accepted plan for message at position: " + messagePosition);
        isExecutingPlan = true;
        planStepRetryCount = 0;
        message.setStatus(ChatMessage.STATUS_ACCEPTED);
        // Track the plan message position and reset progress
        lastPlanMessagePosition = messagePosition;
        planProgressIndex = 0;
        executedStepSummaries.clear();
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
            // Ensure no lingering thinking placeholder
            aiChatFragment.hideThinkingMessage();
        }
        currentStreamingMessagePosition = null;
        sendNextPlanStepFollowUp();
    }

    public void discardPlan(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User discarded plan for message at position: " + messagePosition);
        message.setStatus(ChatMessage.STATUS_DISCARDED);
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        // Centralized cleanup (also sanitizes any dangling running steps to failed)
        finalizePlanExecution("Plan discarded.", true);
    }

    public void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        Log.d(TAG, "User clicked on file change: " + fileActionDetail.path + " (" + fileActionDetail.type + ")");

        String fileNameToOpen = fileActionDetail.path; // Default to path
        if (fileActionDetail.type != null && fileActionDetail.type.equals("renameFile")) {
            fileNameToOpen = fileActionDetail.newPath; // Use newPath for renamed files
        }

        String oldFileContent = fileActionDetail.oldContent != null ? fileActionDetail.oldContent : "";
        String newFileContent = fileActionDetail.newContent != null ? fileActionDetail.newContent : "";

        // Prefer provided diffPatch if it looks like a valid unified diff; otherwise, generate fallback
        String providedPatch = fileActionDetail.diffPatch != null ? fileActionDetail.diffPatch.trim() : "";
        boolean looksUnified = false;
        if (!providedPatch.isEmpty()) {
            // Heuristics: presence of @@ hunk headers or ---/+++ file markers
            looksUnified = providedPatch.contains("@@ ") || (providedPatch.startsWith("--- ") && providedPatch.contains("\n+++ "));
        }

        String diffContent;
        if (!providedPatch.isEmpty() && looksUnified) {
            diffContent = providedPatch;
        } else {
            // Fallback: generate unified diff from contents with appropriate file marker paths
            if ("createFile".equals(fileActionDetail.type)) {
                diffContent = DiffGenerator.generateDiff("", newFileContent, "unified", "/dev/null", "b/" + fileNameToOpen);
            } else if ("deleteFile".equals(fileActionDetail.type)) {
                diffContent = DiffGenerator.generateDiff(oldFileContent, "", "unified", "a/" + fileNameToOpen, "/dev/null");
            } else if ("renameFile".equals(fileActionDetail.type)) {
                String oldPath = fileActionDetail.oldPath != null ? fileActionDetail.oldPath : fileNameToOpen;
                String newPath = fileActionDetail.newPath != null ? fileActionDetail.newPath : fileNameToOpen;
                diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + oldPath, "b/" + newPath);
            } else { // updateFile, smartUpdate, patchFile, modifyLines, etc.
                diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
            }
        }

        activity.tabManager.openDiffTab(fileNameToOpen, diffContent);
    }

    public void shutdown() { if (aiAssistant != null) aiAssistant.shutdown(); }

    // --- Implement AIAssistant.AIActionListener methods ---
    @Override
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName) {
        onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, proposedFileChanges, aiModelDisplayName, null, null);
    }
    
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation,
                                   List<String> suggestions,
                                   List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName,
                                   String thinkingContent, List<WebSource> webSources) {
        activity.runOnUiThread(() -> {
            AIChatFragment uiFrag = activity.getAiChatFragment();
            if (uiFrag == null) {
                Log.w(TAG, "AiChatFragment is null! Cannot add message to UI.");
                return;
            }

            // If we are executing a plan in agent mode, suppress any dangling thinking/streaming placeholder
            boolean agentEnabledAtUi = aiAssistant != null && aiAssistant.isAgentModeEnabled();
            if (agentEnabledAtUi && isExecutingPlan) {
                uiFrag.hideThinkingMessage();
                currentStreamingMessagePosition = null;
            }

            // Intercept model-agnostic tool calls: if the raw response is a tool_call, execute locally
            try {
                if (rawAiResponseJson != null && !rawAiResponseJson.isEmpty()) {
                    String normalized = extractFirstJsonObjectFromText(rawAiResponseJson);
                    String toParse = normalized != null ? normalized : rawAiResponseJson;
                    JsonObject maybe = JsonParser.parseString(toParse).getAsJsonObject();
                    if (maybe.has("action") && "tool_call".equalsIgnoreCase(maybe.get("action").getAsString()) && maybe.has("tool_calls") && maybe.get("tool_calls").isJsonArray()) {
                        JsonArray calls = maybe.getAsJsonArray("tool_calls");
                        JsonArray results = new JsonArray();
                        File projectDir = activity.getProjectDirectory();
                        for (int i = 0; i < calls.size(); i++) {
                            try {
                                JsonObject c = calls.get(i).getAsJsonObject();
                                String name = c.get("name").getAsString();
                                JsonObject args = c.has("args") && c.get("args").isJsonObject() ? c.getAsJsonObject("args") : new JsonObject();
                                JsonObject res = new JsonObject();
                                res.addProperty("name", name);
                                JsonObject exec = ToolExecutor.execute(projectDir, name, args);
                                // attach either parsed object or raw in "result"
                                res.add("result", exec);
                                results.add(res);
                            } catch (Exception inner) {
                                JsonObject res = new JsonObject();
                                res.addProperty("name", "unknown");
                                JsonObject err = new JsonObject();
                                err.addProperty("ok", false);
                                err.addProperty("error", inner.getMessage());
                                res.add("result", err);
                                results.add(res);
                            }
                        }
                        // Build continuation and send immediately; suppress UI emission for this intermediate response
                        String continuation = ToolExecutor.buildToolResultContinuation(results);
                        String fenced = "```json\n" + continuation + "\n```\n";
                        sendAiPrompt(fenced, new java.util.ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
                        return;
                    }
                }
            } catch (Exception ignore) {}

            // Use a mutable local copy for file actions to satisfy lambda finality rules
            List<ChatMessage.FileActionDetail> effectiveProposedFileChanges = proposedFileChanges;

            boolean isPlan = false;
            List<ChatMessage.PlanStep> planSteps = new ArrayList<>();
            QwenResponseParser.ParsedResponse parsed = null;
            try {
                if (rawAiResponseJson != null) {
                    String normalized = extractFirstJsonObjectFromText(rawAiResponseJson);
                    String toParse = normalized != null ? normalized : rawAiResponseJson;
                    parsed = QwenResponseParser.parseResponse(toParse);
                }
                // Fallback: some providers may put JSON only in the final answer text
                if (parsed == null && explanation != null && !explanation.isEmpty()) {
                    String exNorm = extractFirstJsonObjectFromText(explanation);
                    if (exNorm != null) {
                        try { parsed = QwenResponseParser.parseResponse(exNorm); } catch (Exception ignored) {}
                    }
                }
                if (parsed != null && "plan".equals(parsed.action)) {
                    isPlan = true;
                    planSteps = QwenResponseParser.toPlanSteps(parsed);
                } else if (parsed != null && ("file_operation".equals(parsed.action) || QwenResponseParser.looksLikeJson(parsed.explanation))) {
                    // If model returned file ops JSON but fileActions list is empty, convert and set
                    List<ChatMessage.FileActionDetail> ops = QwenResponseParser.toFileActionDetails(parsed);
                    if (effectiveProposedFileChanges == null || effectiveProposedFileChanges.isEmpty()) {
                        effectiveProposedFileChanges = ops;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Plan/file parse failed", e);
            }

            // Enforce exclusive outputs: if both a plan and file operations appear, resolve per mode
            boolean hasOps = effectiveProposedFileChanges != null && !effectiveProposedFileChanges.isEmpty();
            if (isPlan && hasOps) {
                Log.w(TAG, "Mixed 'plan' and 'file_operation' detected in single response; enforcing separation.");
                boolean agent = aiAssistant != null && aiAssistant.isAgentModeEnabled();
                if (isExecutingPlan || agent) {
                    // During plan execution or in agent mode, prefer actionable edits now
                    isPlan = false;
                    planSteps.clear();
                } else {
                    // In non-agent mode, prefer the plan and drop actions
                    effectiveProposedFileChanges = new ArrayList<>();
                }
            }

            // If this is a file_operation response during an executing plan, update the existing plan message
            if (!isPlan && isExecutingPlan && lastPlanMessagePosition != null) {
                if (effectiveProposedFileChanges != null && !effectiveProposedFileChanges.isEmpty()) {
                    planStepRetryCount = 0; // Reset retry count on successful action
                    AIChatFragment frag = activity.getAiChatFragment();
                    ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
                    if (planMsg != null) {
                        // Merge proposed file changes for this step
                        List<ChatMessage.FileActionDetail> merged = planMsg.getProposedFileChanges() != null ? planMsg.getProposedFileChanges() : new ArrayList<>();
                        merged.addAll(effectiveProposedFileChanges);
                        planMsg.setProposedFileChanges(merged);
                        // Update the single message and auto-apply
                        frag.updateMessage(lastPlanMessagePosition, planMsg);
                        // Cancel watchdog since we received ops for this step
                        cancelPlanWatchdog();
                        onAiAcceptActions(lastPlanMessagePosition, planMsg);
                        return;
                    }
                } else {
                    // AI returned no file ops, retry the step
                    planStepRetryCount++;
                    if (planStepRetryCount > 2) {
                        Log.e(TAG, "AI failed to produce file ops for step after 3 retries. Marking as failed.");
                        setCurrentRunningPlanStepStatus("failed");
                        cancelPlanWatchdog();
                        planStepRetryCount = 0; // Reset for next step
                        sendNextPlanStepFollowUp(); // Move to next step
                    } else {
                        Log.w(TAG, "AI did not return file operations. Retrying step (attempt " + planStepRetryCount + ")");
                        sendNextPlanStepFollowUp(); // Re-prompt for the same step
                    }
                    return;
                }
            }

            // Build final AI message for normal flow (not plan-merge)
            String finalExplanation = explanation != null ? explanation.trim() : "";
            if (rawAiResponseJson != null && !rawAiResponseJson.isEmpty()) {
                try {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(rawAiResponseJson).getAsJsonObject();
                    if (obj.has("action") && ("plan".equalsIgnoreCase(obj.get("action").getAsString()) || "file_operation".equalsIgnoreCase(obj.get("action").getAsString()))) {
                        // Suppress echoing structured JSON in the bubble; rely on dedicated UI sections
                        if (!finalExplanation.startsWith("Plan:")) {
                            // keep concise title if available, else empty
                            finalExplanation = finalExplanation;
                        }
                    }
                } catch (Exception ignore) {}
            }
            ChatMessage aiMessage = new ChatMessage(
                    ChatMessage.SENDER_AI,
                    finalExplanation,
                    null,
                    suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>(),
                    aiModelDisplayName,
                    System.currentTimeMillis(),
                    rawAiResponseJson, // always store raw response for long-press
                    effectiveProposedFileChanges,
                    ChatMessage.STATUS_PENDING_APPROVAL
            );
            if (thinkingContent != null && !thinkingContent.isEmpty()) {
                aiMessage.setThinkingContent(thinkingContent);
            }
            if (webSources != null && !webSources.isEmpty()) {
                aiMessage.setWebSources(webSources);
            } else if (rawAiResponseJson != null && rawAiResponseJson.contains("web_sources")) {
                try {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(rawAiResponseJson).getAsJsonObject();
                    if (obj.has("web_sources") && obj.get("web_sources").isJsonArray()) {
                        java.util.List<WebSource> parsedSources = new java.util.ArrayList<>();
                        com.google.gson.JsonArray arr = obj.getAsJsonArray("web_sources");
                        for (int i = 0; i < arr.size(); i++) {
                            com.google.gson.JsonObject s = arr.get(i).getAsJsonObject();
                            String url = s.has("url") ? s.get("url").getAsString() : "";
                            String title = s.has("title") ? s.get("title").getAsString() : url;
                            String snippet = s.has("snippet") ? s.get("snippet").getAsString() : "";
                            String favicon = s.has("favicon") ? s.get("favicon").getAsString() : "";
                            parsedSources.add(new WebSource(url, title, snippet, favicon));
                        }
                        if (!parsedSources.isEmpty()) aiMessage.setWebSources(parsedSources);
                    }
                } catch (Exception ignore) {}
            }

            AIChatFragment frag = activity.getAiChatFragment();
            if (frag != null) {
                int insertedPos = frag.addMessage(aiMessage);
                if (isPlan && planSteps != null && !planSteps.isEmpty()) {
                    aiMessage.setPlanSteps(planSteps);
                    frag.updateMessage(insertedPos, aiMessage);
                    lastPlanMessagePosition = insertedPos;
                    planProgressIndex = 0;
                    executedStepSummaries.clear();
                } else if (aiAssistant != null && aiAssistant.isAgentModeEnabled()
                           && effectiveProposedFileChanges != null && !effectiveProposedFileChanges.isEmpty()) {
                    onAiAcceptActions(insertedPos, aiMessage);
                }
            } else {
                Log.w(TAG, "AiChatFragment is null! Cannot add message to UI.");
            }
        });
    }

    @Override
    public void onAiError(String errorMessage) {
        activity.runOnUiThread(() -> {
            activity.showToast("AI Error: " + errorMessage);
            AIChatFragment aiChatFragment = activity.getAiChatFragment();
            if (aiChatFragment != null) {
                ChatMessage aiErrorMessage = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        "Error: " + errorMessage,
                        null, null,
                        aiAssistant.getCurrentModel().getDisplayName(),
                        System.currentTimeMillis(),
                        null, null,
                        ChatMessage.STATUS_NONE
                );
                aiChatFragment.addMessage(aiErrorMessage);
                // If a plan is executing, mark current step failed and advance to prevent stuck state
                boolean agentEnabled = aiAssistant != null && aiAssistant.isAgentModeEnabled();
                if (agentEnabled && isExecutingPlan) {
                    aiChatFragment.hideThinkingMessage();
                    currentStreamingMessagePosition = null;
                    setCurrentRunningPlanStepStatus("failed");
                    if (lastPlanMessagePosition != null) {
                        ChatMessage planMsg = aiChatFragment.getMessageAt(lastPlanMessagePosition);
                        if (planMsg != null) aiChatFragment.updateMessage(lastPlanMessagePosition, planMsg);
                    }
                    cancelPlanWatchdog();
                    sendNextPlanStepFollowUp();
                }
            }
        });
    }

    @Override
    public void onAiRequestStarted() {
        activity.runOnUiThread(() -> {
            // Suppress streaming placeholder during agent plan execution
            boolean suppress = aiAssistant != null && aiAssistant.isAgentModeEnabled() && isExecutingPlan;
            if (suppress) {
                AIChatFragment uiFrag = activity.getAiChatFragment();
                if (uiFrag != null) {
                    uiFrag.hideThinkingMessage();
                }
                currentStreamingMessagePosition = null;
                return;
            }
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                // Start a streaming AI message that will be updated with thoughts/answer
                ChatMessage aiMsg = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        activity.getString(com.codex.apk.R.string.ai_is_thinking),
                        null, null,
                        aiAssistant.getCurrentModel().getDisplayName(),
                        System.currentTimeMillis(),
                        null, null,
                        ChatMessage.STATUS_NONE
                );
                currentStreamingMessagePosition = chatFragment.addMessage(aiMsg);
            }
        });
    }

    @Override
    public void onAiStreamUpdate(String partialResponse, boolean isThinking) {
        activity.runOnUiThread(() -> {
            // Suppress streaming updates during agent plan execution
            if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && isExecutingPlan) return;
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment == null || currentStreamingMessagePosition == null) return;
            ChatMessage msg = chatFragment.getMessageAt(currentStreamingMessagePosition);
            if (msg == null) return;
            if (isThinking) {
                // Move content from typing indicator to Thoughts section as soon as we have text
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    msg.setContent(""); // disable typing indicator view
                    msg.setThinkingContent(partialResponse);
                }
            } else {
                // Stream main answer tokens into content
                msg.setContent(partialResponse != null ? partialResponse : "");
            }
            chatFragment.updateMessage(currentStreamingMessagePosition, msg);
        });
    }

    @Override
    public void onAiRequestCompleted() {
        // No-op: we finalize/replace the streaming message in onAiActionsProcessed
    }

    @Override
    public void onQwenConversationStateUpdated(com.codex.apk.QwenConversationState state) {
        activity.runOnUiThread(() -> {
            if (activity != null) {
                activity.onQwenConversationStateUpdated(state);
            }
        });
    }
}