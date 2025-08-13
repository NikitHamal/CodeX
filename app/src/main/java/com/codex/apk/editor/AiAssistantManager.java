package com.codex.apk.editor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
import com.codex.apk.ProjectVerifier;
import android.content.SharedPreferences;


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
    private List<WebSource> lastStreamingWebSources = null;

    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
                              FileManager fileManager, ExecutorService executorService) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.executorService = executorService;
        this.aiProcessor = new AiProcessor(projectDir, activity.getApplicationContext());

        String apiKey = SettingsActivity.getGeminiApiKey(activity);
        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);
        this.aiAssistant.setEnabledTools(com.codex.apk.ToolSpec.defaultFileTools());

        SharedPreferences settingsPrefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String defaultModelName = settingsPrefs.getString("selected_model", AIModel.fromModelId("qwen3-coder-plus").getDisplayName());
        AIModel defaultModel = AIModel.fromDisplayName(defaultModelName);
        if (defaultModel != null) {
            this.aiAssistant.setCurrentModel(defaultModel);
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
                    // Post-apply verification
                    ProjectVerifier verifier = new ProjectVerifier();
                    ProjectVerifier.VerificationResult vr = verifier.verify(message.getProposedFileChanges(), activity.getProjectDirectory());

                    activity.runOnUiThread(() -> {
                        // Always keep chat concise; do not append lint details
                        activity.showToast(vr.ok ? "AI actions applied successfully!" : "Applied with issues.");
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

            for (int i = 0; i < steps.size(); i++) {
                ChatMessage.FileActionDetail step = steps.get(i);

                setNextPlanStepStatus("running");
                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) {
                        frag.updateMessage(messagePosition, message);
                        if (lastPlanMessagePosition != null) {
                            ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
                            if (planMsg != null) frag.updateMessage(lastPlanMessagePosition, planMsg);
                        }
                    }
                });

                try {
                    String summary = aiProcessor.applyFileAction(step);
                    appliedSummaries.add(summary);
                    executedStepSummaries.add(summary);
                    step.stepStatus = "completed";
                    step.stepMessage = "Completed";
                    setCurrentRunningPlanStepStatus("completed");
                } catch (Exception ex) {
                    Log.e(TAG, "Agent step failed: " + step.getSummary(), ex);
                    step.stepStatus = "failed";
                    step.stepMessage = ex.getMessage();
                    executedStepSummaries.add("FAILED: " + step.getSummary() + " - " + ex.getMessage());
                    setCurrentRunningPlanStepStatus("failed");
                }

                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) {
                        frag.updateMessage(messagePosition, message);
                        if (lastPlanMessagePosition != null) {
                            ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
                            if (planMsg != null) frag.updateMessage(lastPlanMessagePosition, planMsg);
                        }
                    }
                });
            }

            // Post-batch verification for this message batch
            ProjectVerifier verifier = new ProjectVerifier();
            ProjectVerifier.VerificationResult vr = verifier.verify(steps, activity.getProjectDirectory());

            activity.runOnUiThread(() -> {
                message.setStatus(ChatMessage.STATUS_ACCEPTED);
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag != null) frag.updateMessage(messagePosition, message);
                activity.tabManager.refreshOpenTabsAfterAi();
                activity.loadFileTree();
                activity.showToast(vr.ok ? "Agent step applied" : "Applied with issues; continuing.");
                // After finishing this file_operation batch as part of plan, advance to next step automatically
                if (isExecutingPlan) {
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
                    !("completed".equals(ps.status) || "failed".equals(ps.status))) {
                ps.status = status;
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
                ps.status = status;
                planProgressIndex++;
            }
        }
    }

    // Orchestrate detailed autonomous follow-ups per plan step with rich context
    private void sendNextPlanStepFollowUp() {
        if (!isNetworkAvailable()) return;
        if (lastPlanMessagePosition == null) return;
        AIChatFragment frag = activity.getAiChatFragment();
        if (frag == null) return;
        ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
        if (planMsg == null || planMsg.getPlanSteps() == null || planMsg.getPlanSteps().isEmpty()) {
            isExecutingPlan = false;
            activity.showToast("Plan completed");
            return;
        }

        // find next file-kind step not completed
        List<ChatMessage.PlanStep> steps = planMsg.getPlanSteps();
        int idx = planProgressIndex;
        while (idx < steps.size()) {
            ChatMessage.PlanStep ps = steps.get(idx);
            if (ps != null && (ps.kind == null || ps.kind.equalsIgnoreCase("file")) &&
                !("completed".equals(ps.status) || "failed".equals(ps.status))) {
                break;
            }
            idx++;
        }
        if (idx >= steps.size()) {
            isExecutingPlan = false;
            activity.runOnUiThread(() -> activity.showToast("Plan completed"));
            return;
        }
        planProgressIndex = idx; // align index

        // Build a rich, concise step prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are executing step ").append(steps.get(idx).id).append(": ").append(steps.get(idx).title).append(".\n");
        prompt.append("Return a single strict JSON object with action=\"file_operation\" in a ```json fenced code block.\n");
        prompt.append("Constraints: produce separate operations per individual file (HTML/CSS/JS). No natural language outside JSON.\n\n");
        prompt.append("Context summary:\n");
        prompt.append("- Plan: ").append(safeTruncate(planToJson(planMsg), 2000)).append("\n");
        if (!executedStepSummaries.isEmpty()) {
            prompt.append("- Executed steps so far:\n");
            int count = 0; for (String s : executedStepSummaries) { if (count++ >= 10) break; prompt.append("  • ").append(s).append("\n"); }
        }
        prompt.append("- File tree (project root):\n");
        prompt.append(safeTruncate(buildFileTree(activity.getProjectDirectory(), 3, 200), 3000)).append("\n");
        TabItem active = activity.getActiveTab();
        if (active != null) {
            prompt.append("- Active file: ").append(active.getFileName()).append("\n");
            prompt.append("- Active file content (truncated):\n");
            prompt.append("---\n");
            prompt.append(safeTruncate(active.getContent(), 2000)).append("\n");
            prompt.append("---\n");
        }
        prompt.append("Proceed now with step ").append(steps.get(idx).id).append(".");

        isExecutingPlan = true;
        // Mark step running visually before sending
        setNextPlanStepStatus("running");
        activity.runOnUiThread(() -> {
            if (lastPlanMessagePosition != null) {
                ChatMessage pm = activity.getAiChatFragment().getMessageAt(lastPlanMessagePosition);
                if (pm != null) activity.getAiChatFragment().updateMessage(lastPlanMessagePosition, pm);
            }
        });

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
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        sendNextPlanStepFollowUp();
    }

    public void discardPlan(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User discarded plan for message at position: " + messagePosition);
        isExecutingPlan = false;
        message.setStatus(ChatMessage.STATUS_DISCARDED);
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        activity.showToast("Plan discarded.");
    }

    public void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        Log.d(TAG, "User clicked on file change: " + fileActionDetail.path + " (" + fileActionDetail.type + ")");

        String fileNameToOpen = fileActionDetail.path; // Default to path
        if (fileActionDetail.type != null && fileActionDetail.type.equals("renameFile")) {
            fileNameToOpen = fileActionDetail.newPath; // Use newPath for renamed files
        }

        String diffContent = "";
        String oldFileContent = fileActionDetail.oldContent != null ? fileActionDetail.oldContent : "";
        String newFileContent = fileActionDetail.newContent != null ? fileActionDetail.newContent : "";

        if (fileActionDetail.type.equals("createFile")) {
            diffContent = DiffGenerator.generateDiff("", newFileContent, "unified", "/dev/null", "b/" + fileNameToOpen);
        } else if (fileActionDetail.type.equals("deleteFile")) {
            diffContent = DiffGenerator.generateDiff(oldFileContent, "", "unified", "a/" + fileNameToOpen, "/dev/null");
        } else if (fileActionDetail.type.equals("renameFile")) {
            diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + fileActionDetail.oldPath, "b/" + fileActionDetail.newPath);
        }
        else { // updateFile or modifyLines
            diffContent = DiffGenerator.generateDiff(oldFileContent, newFileContent, "unified", "a/" + fileNameToOpen, "b/" + fileNameToOpen);
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
            if (activity.getAiChatFragment() != null) {
                boolean isPlan = false;
                List<ChatMessage.PlanStep> planSteps = new ArrayList<>();
                try {
                    boolean agentEnabled = aiAssistant != null && aiAssistant.isAgentModeEnabled();
                    if (rawAiResponseJson != null && agentEnabled) {
                        QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(rawAiResponseJson);
                        if (parsed != null && "plan".equals(parsed.action)) {
                            isPlan = true;
                            planSteps = QwenResponseParser.toPlanSteps(parsed);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Plan parse failed", e);
                }

                // If this is a file_operation response during an executing plan, update the existing plan message
                if (!isPlan && isExecutingPlan && lastPlanMessagePosition != null) {
                    if (proposedFileChanges != null && !proposedFileChanges.isEmpty()) {
                        planStepRetryCount = 0; // Reset retry count on successful action
                        AIChatFragment frag = activity.getAiChatFragment();
                        ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
                        if (planMsg != null) {
                            // Merge proposed file changes for this step
                            List<ChatMessage.FileActionDetail> merged = planMsg.getProposedFileChanges() != null ? planMsg.getProposedFileChanges() : new ArrayList<>();
                            merged.addAll(proposedFileChanges);
                            planMsg.setProposedFileChanges(merged);
                            // Update the single message and auto-apply
                            frag.updateMessage(lastPlanMessagePosition, planMsg);
                            onAiAcceptActions(lastPlanMessagePosition, planMsg);
                            return;
                        }
                    } else {
                        // AI returned no file ops, retry the step
                        planStepRetryCount++;
                        if (planStepRetryCount > 2) {
                            Log.e(TAG, "AI failed to produce file ops for step after 3 retries. Marking as failed.");
                            setCurrentRunningPlanStepStatus("failed");
                            planStepRetryCount = 0; // Reset for next step
                            sendNextPlanStepFollowUp(); // Move to next step
                        } else {
                            Log.w(TAG, "AI did not return file operations. Retrying step (attempt " + planStepRetryCount + ")");
                            sendNextPlanStepFollowUp(); // Re-prompt for the same step
                        }
                        return;
                    }
                }

                ChatMessage aiMessage = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        explanation,
                        null,
                        new ArrayList<>(),
                        aiModelDisplayName,
                        System.currentTimeMillis(),
                        rawAiResponseJson, // always store raw response for long-press
                        proposedFileChanges,
                        ChatMessage.STATUS_PENDING_APPROVAL
                );

                if (isPlan && planSteps != null && !planSteps.isEmpty()) {
                    aiMessage.setPlanSteps(planSteps);
                }

                if (thinkingContent != null && !thinkingContent.trim().isEmpty()) aiMessage.setThinkingContent(thinkingContent);
                if (webSources != null && !webSources.isEmpty()) {
                    aiMessage.setWebSources(webSources);
                } else if (lastStreamingWebSources != null && !lastStreamingWebSources.isEmpty()) {
                    aiMessage.setWebSources(new ArrayList<>(lastStreamingWebSources));
                }

                int insertedPos = activity.getAiChatFragment().addMessage(aiMessage);

                if (isPlan) {
                    lastPlanMessagePosition = insertedPos;
                    planProgressIndex = 0;
                    executedStepSummaries.clear();
                    // Auto-execution removed, user must click "Accept"
                } else {
                    if (aiAssistant != null && aiAssistant.isAgentModeEnabled() && proposedFileChanges != null && !proposedFileChanges.isEmpty()) {
                        onAiAcceptActions(insertedPos, aiMessage);
                    }
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
            }
        });
    }

    @Override
    public void onAiRequestStarted() {
        activity.runOnUiThread(() -> {
            lastStreamingWebSources = null; // reset cache for new request
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null && !chatFragment.isAiProcessing) {
                chatFragment.addMessage(new ChatMessage(
                        ChatMessage.SENDER_AI,
                        "AI is thinking...",
                        null, null,
                        aiAssistant.getCurrentModel().getDisplayName(),
                        System.currentTimeMillis(),
                        null, null,
                        ChatMessage.STATUS_NONE
                ));
            }
        });
    }

    @Override
    public void onAiStreamUpdate(String partialResponse, boolean isThinking) {
        activity.runOnUiThread(() -> {
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                if (isThinking) {
                    chatFragment.updateStreamingThinkingContent(partialResponse);
                } else {
                    chatFragment.updateStreamingAnswerContent(partialResponse);
                }
            }
        });
    }

    @Override
    public void onAiWebSourcesUpdate(java.util.List<WebSource> webSources) {
        activity.runOnUiThread(() -> {
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                chatFragment.updateStreamingWebSources(webSources);
            }
            // cache latest web sources for final message
            if (webSources != null && !webSources.isEmpty()) {
                lastStreamingWebSources = new ArrayList<>(webSources);
            }
        });
    }

    @Override
    public void onAiRequestCompleted() {
        activity.runOnUiThread(() -> {
            AIChatFragment chatFragment = activity.getAiChatFragment();
            if (chatFragment != null) {
                chatFragment.hideThinkingMessage();
            }
            // clear after completion
            lastStreamingWebSources = null;
        });
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