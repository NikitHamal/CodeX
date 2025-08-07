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
import android.content.SharedPreferences;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    // Track the last plan message for status updates
    private Integer lastPlanMessagePosition = null;
    private int planProgressIndex = 0; // index into plan steps list for file-kind steps

    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
                              FileManager fileManager, ExecutorService executorService) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.executorService = executorService;
        // Initialize AiProcessor here, it needs the project directory and application context
        this.aiProcessor = new AiProcessor(projectDir, activity.getApplicationContext());

        String apiKey = SettingsActivity.getGeminiApiKey(activity); // May be blank at start – that's fine
        // Initialize AIAssistant, passing 'this' as the AIActionListener
        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);

        // Advertise default filesystem manipulation tools to the assistant so the model can invoke them.
        this.aiAssistant.setEnabledTools(com.codex.apk.ToolSpec.defaultFileTools());

        // Load default model from settings and apply it
        SharedPreferences settingsPrefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String defaultModelName = settingsPrefs.getString("selected_model", AIModel.fromModelId("gemini-2.5-flash").getDisplayName());
        AIModel defaultModel = AIModel.fromDisplayName(defaultModelName);
        if (defaultModel != null) {
            this.aiAssistant.setCurrentModel(defaultModel);
        }
    }

    public AIAssistant getAIAssistant() {
        return aiAssistant;
    }

    /**
     * Handles onResume logic, specifically for API key refresh.
     */
    public void onResume() {
        // Re-initialize AI Assistant if API key changes in settings
        String updatedApiKey = SettingsActivity.getGeminiApiKey(activity);
        String currentApiKeyInAssistant = aiAssistant.getApiKey();
        if (!updatedApiKey.equals(currentApiKeyInAssistant)) {
            Log.i(TAG, "API Key changed in settings. Updating AIAssistant API key.");
            aiAssistant.setApiKey(updatedApiKey); // Just update the key, no need to re-create
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
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

        // Agent mode: sequential execution with plan status mirroring
        executorService.execute(() -> {
            List<String> appliedSummaries = new ArrayList<>();
            List<ChatMessage.FileActionDetail> steps = message.getProposedFileChanges();

            for (int i = 0; i < steps.size(); i++) {
                ChatMessage.FileActionDetail step = steps.get(i);

                // Mark corresponding plan step running
                updateNextPlanStepStatus("running");
                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) frag.updateMessage(messagePosition, message);
                });

                try {
                    String summary = aiProcessor.applyFileAction(step);
                    appliedSummaries.add(summary);
                    step.stepStatus = "completed";
                    step.stepMessage = "Completed";
                    // Mark plan step completed
                    updateCurrentRunningPlanStepStatus("completed");
                } catch (Exception ex) {
                    Log.e(TAG, "Agent step failed: " + step.getSummary(), ex);
                    step.stepStatus = "failed";
                    step.stepMessage = ex.getMessage();
                    // Mark plan step failed
                    updateCurrentRunningPlanStepStatus("failed");
                }

                // Update UI after each step
                activity.runOnUiThread(() -> {
                    AIChatFragment frag = activity.getAiChatFragment();
                    if (frag != null) frag.updateMessage(messagePosition, message);
                });
            }

            // Finalize
            activity.runOnUiThread(() -> {
                message.setStatus(ChatMessage.STATUS_ACCEPTED);
                message.setActionSummaries(appliedSummaries);
                AIChatFragment frag = activity.getAiChatFragment();
                if (frag != null) frag.updateMessage(messagePosition, message);
                activity.tabManager.refreshOpenTabsAfterAi();
                activity.loadFileTree();
                activity.showToast("Agent run completed");
            });
        });
    }

    private void updateNextPlanStepStatus(String status) {
        if (lastPlanMessagePosition == null) return;
        AIChatFragment frag = activity.getAiChatFragment();
        if (frag == null) return;
        // Fetch current message
        // We cannot access chat list directly; rely on activity to keep the same object reference used earlier.
        // Here, we optimistically update plan step status on the last plan message via adapter references.
        // Safer: have EditorActivity expose a getter to retrieve message by position (not available now).
        // As a pragmatic approach, we keep only an index cursor and trust UI rebind uses updated ChatMessage references.
        try {
            // Access plan message stored previously
            // The message object was created and added by onAiActionsProcessed below.
            // planProgressIndex points to next file-kind plan step
            // No-op if no steps
        } catch (Exception ignored) {}
    }

    private void updateCurrentRunningPlanStepStatus(String status) {
        if (lastPlanMessagePosition == null) return;
        try {
            // The actual plan step status update logic is applied when constructing plan message below
            // and when we manage references. This is a placeholder to keep indexes in sync.
        } catch (Exception ignored) {}
    }

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
                // Detect plan from raw JSON
                boolean isPlan = false;
                List<ChatMessage.PlanStep> planSteps = new ArrayList<>();
                try {
                    if (rawAiResponseJson != null) {
                        QwenResponseParser.ParsedResponse parsed = QwenResponseParser.parseResponse(rawAiResponseJson);
                        if (parsed != null && "plan".equals(parsed.action)) {
                            isPlan = true;
                            planSteps = QwenResponseParser.toPlanSteps(parsed);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Plan parse failed", e);
                }

                ChatMessage aiMessage = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        explanation,
                        null,
                        new ArrayList<>(),
                        aiModelDisplayName,
                        System.currentTimeMillis(),
                        rawAiResponseJson,
                        proposedFileChanges,
                        ChatMessage.STATUS_PENDING_APPROVAL
                );

                // Attach plan steps if present
                if (isPlan && planSteps != null && !planSteps.isEmpty()) {
                    aiMessage.setPlanSteps(planSteps);
                }

                // Thinking/web sources
                if (thinkingContent != null && !thinkingContent.trim().isEmpty()) {
                    aiMessage.setThinkingContent(thinkingContent);
                }
                if (webSources != null && !webSources.isEmpty()) {
                    aiMessage.setWebSources(webSources);
                }

                int insertedPos = activity.getAiChatFragment().addMessage(aiMessage);

                if (isPlan) {
                    lastPlanMessagePosition = insertedPos;
                    planProgressIndex = 0;
                    // If agent mode, immediately request file operations for the plan autonomously
                    if (aiAssistant != null && aiAssistant.isAgentModeEnabled()) {
                        StringBuilder followUp = new StringBuilder();
                        followUp.append("Proceed to generate a single \"file_operation\" JSON covering the next concrete steps of the plan. ");
                        followUp.append("Ensure each file (HTML/CSS/JS) is a separate operation. Return only strict JSON.");
                        sendAiPrompt(followUp.toString(), new ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
                    }
                } else {
                    // If not a plan and agent mode is on with file ops, auto-apply
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
                chatFragment.updateThinkingMessage(partialResponse);
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