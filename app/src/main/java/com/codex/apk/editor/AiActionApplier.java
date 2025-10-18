package com.codex.apk.editor;

import android.util.Log;

import com.codex.apk.AIChatFragment;
import com.codex.apk.ChatMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handles applying AI-generated actions, both manual approvals and agent-driven plans.
 */
public class AiActionApplier {
    private static final String TAG = "AiActionApplier";

    private final EditorActivity activity;
    private final AiProcessor aiProcessor;
    private final PlanExecutor planExecutor;
    private final ExecutorService executorService;

    public AiActionApplier(EditorActivity activity,
                           AiProcessor aiProcessor,
                           PlanExecutor planExecutor,
                           ExecutorService executorService) {
        this.activity = activity;
        this.aiProcessor = aiProcessor;
        this.planExecutor = planExecutor;
        this.executorService = executorService;
    }

    /**
     * Applies actions approved by the user (non-agent mode).
     */
    public void applyAcceptedActions(int messagePosition, ChatMessage message) {
        executorService.execute(() -> {
            try {
                List<String> appliedSummaries = new ArrayList<>();
                List<File> changedFiles = new ArrayList<>();
                for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                    String summary = aiProcessor.applyFileAction(detail);
                    appliedSummaries.add(summary);
                    // Track changed files to refresh them
                    File fileToRefresh = new File(activity.getProjectDirectory(), detail.path);
                    if (fileToRefresh.exists()) {
                        changedFiles.add(fileToRefresh);
                    }
                    if ("renameFile".equalsIgnoreCase(detail.type) && detail.newPath != null) {
                        File newFile = new File(activity.getProjectDirectory(), detail.newPath);
                        if (newFile.exists()) {
                            changedFiles.add(newFile);
                        }
                    }
                }
                activity.runOnUiThread(() -> {
                    activity.showToast("AI actions applied successfully!");
                    message.setStatus(ChatMessage.STATUS_ACCEPTED);
                    message.setActionSummaries(appliedSummaries);
                    AIChatFragment aiChatFragment = activity.getAiChatFragment();
                    if (aiChatFragment != null) {
                        aiChatFragment.updateMessage(messagePosition, message);
                    }
                    // Refresh tabs and file tree
                    activity.tabManager.refreshOpenTabsAfterAi();
                    activity.loadFileTree();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error applying AI actions: " + e.getMessage(), e);
                activity.runOnUiThread(() -> activity.showToast("Failed to apply AI actions: " + e.getMessage()));
            }
        });
    }

    /**
     * Applies actions when agent mode is active.
     */
    public void applyAgentActions(int messagePosition, ChatMessage message) {
        executorService.execute(() -> {
            List<String> appliedSummaries = new ArrayList<>();
            List<ChatMessage.FileActionDetail> steps = message.getProposedFileChanges();

            boolean anyFailed = false;
            for (int i = 0; i < steps.size(); i++) {
                ChatMessage.FileActionDetail step = steps.get(i);

                try {
                    String summary = aiProcessor.applyFileAction(step);
                    appliedSummaries.add(summary);
                    if (planExecutor != null && planExecutor.isExecutingPlan()) {
                        planExecutor.addExecutedStepSummary(summary);
                    }
                    step.stepStatus = "completed";
                    step.stepMessage = "Completed";
                } catch (Exception ex) {
                    Log.e(TAG, "Agent step failed: " + step.getSummary(), ex);
                    step.stepStatus = "failed";
                    step.stepMessage = ex.getMessage();
                    if (planExecutor != null && planExecutor.isExecutingPlan()) {
                        planExecutor.addExecutedStepSummary("FAILED: " + step.getSummary() + " - " + ex.getMessage());
                    }
                    anyFailed = true;
                }

                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) {
                        frag.updateMessage(messagePosition, message);
                    }
                });
            }

            activity.runOnUiThread(() -> {
                message.setStatus(ChatMessage.STATUS_ACCEPTED);
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag != null) frag.updateMessage(messagePosition, message);
                activity.tabManager.refreshOpenTabsAfterAi();
                activity.loadFileTree();
                activity.showToast(anyFailed ? "Agent steps completed with issues" : "Agent step applied");
                if (planExecutor != null && planExecutor.isExecutingPlan()) {
                    planExecutor.onStepActionsApplied();
                }
            });
        });
    }
}
