package com.codex.apk.editor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.codex.apk.AIChatFragment;
import com.codex.apk.AIAssistant;
import com.codex.apk.AiProcessor; // Import AiProcessor
import com.codex.apk.ChatMessage;
import com.codex.apk.CodeEditorFragment;
import com.codex.apk.EditorActivity;
import com.codex.apk.FileManager;
import com.codex.apk.ToolSpec;
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DiffUtils; // Assuming DiffUtils is available
import android.content.SharedPreferences;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
        String defaultModelName = settingsPrefs.getString("selected_model", AIModel.GEMINI_2_5_FLASH.getDisplayName());
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

    /**
     * Sends the user's prompt to the AI Assistant.
     * @param userPrompt The prompt from the user.
     * @param activeTabItem The currently active TabItem for context.
     */
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

        // API Key handling logic can remain the same or be adapted if needed
        // For now, let's assume it's handled correctly within AIAssistant or QwenApiClient

        try {
            // The new sendPrompt method will need to handle history and state
            aiAssistant.sendPrompt(userPrompt, chatHistory, qwenState, currentFileName, currentFileContent);
        } catch (Exception e) {
            activity.showToast("AI Error: " + e.getMessage());
            Log.e(TAG, "AI processing error", e);
        }
    }

    /**
     * Handles the acceptance of AI proposed actions.
     * @param messagePosition The position of the chat message.
     * @param message The chat message containing proposed changes.
     */
    public void onAiAcceptActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User accepted AI actions for message at position: " + messagePosition);
        if (message.getProposedFileChanges() == null || message.getProposedFileChanges().isEmpty()) {
            activity.showToast("No proposed changes to apply.");
            return;
        }

        executorService.execute(() -> {
            try {
                // Apply each proposed change
                List<String> appliedSummaries = new ArrayList<>();
                for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                    String summary = aiProcessor.applyFileAction(detail); // Use the aiProcessor instance
                    appliedSummaries.add(summary);
                }

                activity.runOnUiThread(() -> {
                    activity.showToast("AI actions applied successfully!");
                    // Update the message status to ACCEPTED
                    message.setStatus(ChatMessage.STATUS_ACCEPTED);
                    // Update the action summaries to reflect what was actually applied
                    message.setActionSummaries(appliedSummaries); // Use setter
                    AIChatFragment aiChatFragment = activity.getAiChatFragment();
                    if (aiChatFragment != null) {
                        aiChatFragment.updateMessage(messagePosition, message);
                    }

                    // Refresh open tabs and file tree after changes are applied
                    activity.tabManager.refreshOpenTabsAfterAi(); // Call via activity's TabManager
                    activity.loadFileTree(); // Call via activity's FileTreeManager
                });
            } catch (Exception e) {
                Log.e(TAG, "Error applying AI actions: " + e.getMessage(), e);
                activity.runOnUiThread(() -> {
                    activity.showToast("Failed to apply AI actions: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Handles the discarding of AI proposed actions.
     * @param messagePosition The position of the chat message.
     * @param message The chat message containing proposed changes.
     */
    public void onAiDiscardActions(int messagePosition, ChatMessage message) {
        Log.d(TAG, "User discarded AI actions for message at position: " + messagePosition);
        // Update the message status to DISCARDED
        message.setStatus(ChatMessage.STATUS_DISCARDED);
        AIChatFragment aiChatFragment = activity.getAiChatFragment();
        if (aiChatFragment != null) {
            aiChatFragment.updateMessage(messagePosition, message);
        }
        activity.showToast("AI actions discarded.");
    }

    /**
     * Handles the reapplication of AI proposed actions.
     * This method is called by EditorActivity.
     * @param messagePosition The position of the chat message.
     * @param message The chat message containing proposed changes.
     */
    public void onReapplyActions(int messagePosition, ChatMessage message) { // Made public for EditorActivity
        Log.d(TAG, "User requested to reapply AI actions for message at position: " + messagePosition);
        // Reapplying is essentially the same as accepting
        onAiAcceptActions(messagePosition, message);
    }

    /**
     * Handles a click on a proposed file change, opening a diff tab.
     * @param fileActionDetail The detail of the file action.
     */
    public void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        Log.d(TAG, "User clicked on file change: " + fileActionDetail.path + " (" + fileActionDetail.type + ")");

        String fileNameToOpen = fileActionDetail.path; // Default to path
        if (fileActionDetail.type != null && fileActionDetail.type.equals("renameFile")) {
            fileNameToOpen = fileActionDetail.newPath; // Use newPath for renamed files
        }

        // Generate diff content
        String diffContent = "";
        String oldFileContent = fileActionDetail.oldContent != null ? fileActionDetail.oldContent : "";
        String newFileContent = fileActionDetail.newContent != null ? fileActionDetail.newContent : "";

        if (fileActionDetail.type.equals("createFile")) {
            diffContent = "--- /dev/null\n+++ b/" + fileNameToOpen + "\n" +
                    DiffUtils.generateUnifiedDiff("", newFileContent);
        } else if (fileActionDetail.type.equals("deleteFile")) {
            diffContent = "--- a/" + fileNameToOpen + "\n+++ /dev/null\n" +
                    DiffUtils.generateUnifiedDiff(oldFileContent, "");
        } else if (fileActionDetail.type.equals("renameFile")) {
            diffContent = "--- a/" + fileActionDetail.oldPath + "\n+++ b/" + fileActionDetail.newPath + "\n" +
                    DiffUtils.generateUnifiedDiff(oldFileContent, newFileContent);
        }
        else { // updateFile or modifyLines
            diffContent = "--- a/" + fileNameToOpen + "\n+++ b/" + fileNameToOpen + "\n" +
                    DiffUtils.generateUnifiedDiff(oldFileContent, newFileContent);
        }

        // Open a new tab to display the diff
        activity.tabManager.openDiffTab(fileNameToOpen, diffContent); // Call via activity's TabManager
    }

    /**
     * Shuts down the AI Assistant and its associated resources.
     * This method is called by EditorActivity.
     */
    public void shutdown() { // Made public for EditorActivity
        if (aiAssistant != null) {
            aiAssistant.shutdown();
        }
    }

    // --- Implement AIAssistant.AIActionListener methods to pass updates to AIChatFragment ---
    @Override
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName) {
        onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, proposedFileChanges, aiModelDisplayName, null, null);
    }
    
    // Enhanced version with thinking content and web sources
    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, 
                                   List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName,
                                   String thinkingContent, List<WebSource> webSources) {
        Log.d(TAG, "onAiActionsProcessed called with:");
        Log.d(TAG, "  - Explanation: " + explanation);
        Log.d(TAG, "  - Suggestions count: " + (suggestions != null ? suggestions.size() : 0));
        Log.d(TAG, "  - File changes count: " + (proposedFileChanges != null ? proposedFileChanges.size() : 0));
        Log.d(TAG, "  - Model: " + aiModelDisplayName);
        
        activity.runOnUiThread(() -> {
            if (activity.getAiChatFragment() != null) {
                ChatMessage aiMessage = new ChatMessage(
                        ChatMessage.SENDER_AI,
                        explanation,
                        null, // Action summaries will be generated from proposedFileChanges if accepted
                        suggestions,
                        aiModelDisplayName,
                        System.currentTimeMillis(),
                        rawAiResponseJson,
                        proposedFileChanges,
                        ChatMessage.STATUS_PENDING_APPROVAL
                );
                
                // Set thinking content and web sources
                if (thinkingContent != null && !thinkingContent.trim().isEmpty()) {
                    aiMessage.setThinkingContent(thinkingContent);
                }
                if (webSources != null && !webSources.isEmpty()) {
                    aiMessage.setWebSources(webSources);
                }
                
                Log.d(TAG, "Adding AI message to chat fragment with status: " + aiMessage.getStatus());
                activity.getAiChatFragment().addMessage(aiMessage); // Add to local list and UI
                activity.getAiChatFragment().saveChatState(); // Save to SharedPreferences
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
                aiChatFragment.saveChatState();
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