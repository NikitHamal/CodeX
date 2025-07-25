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
import com.codex.apk.SettingsActivity;
import com.codex.apk.TabItem;
import com.codex.apk.DiffUtils; // Assuming DiffUtils is available

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

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

        String apiKey = SettingsActivity.getGeminiApiKey(activity);
        if (apiKey.isEmpty()) {
            activity.showToast("AI API Key not set. Please configure it in Settings.");
        }

        // Initialize AIAssistant, passing 'this' as the AIActionListener
        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);
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
            activity.showToast("API Key updated. AI Assistant re-initialized.");
        }
    }

    /**
     * Triggers a refresh of the AI codebase index.
     */
    public void refreshCodebaseIndex() {
        if (aiAssistant != null) {
            aiAssistant.refreshCodebaseIndex();
        }
    }

    /**
     * Sends the user's prompt to the AI Assistant.
     * @param userPrompt The prompt from the user.
     * @param activeTabItem The currently active TabItem for context.
     */
    public void sendAiPrompt(String userPrompt, TabItem activeTabItem) {
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

        String apiKey;
        if (aiAssistant.getCurrentModel() == AIAssistant.AIModel.GEMINI_2_0_FLASH) {
            apiKey = SettingsActivity.getGeminiApiKey(activity);
            if (apiKey.isEmpty()) {
                activity.getDialogHelper().showApiKeyDialog(() -> {
                    aiAssistant.setApiKey(SettingsActivity.getGeminiApiKey(activity));
                    sendAiPrompt(userPrompt, activeTabItem);
                });
                return;
            }
        } else if (aiAssistant.getCurrentModel() == AIAssistant.AIModel.DEEPSEEK_R1) {
            apiKey = SettingsActivity.getHuggingFaceToken(activity);
            if (apiKey.isEmpty()) {
                activity.getDialogHelper().showApiKeyDialog(() -> {
                    aiAssistant.setApiKey(SettingsActivity.getHuggingFaceToken(activity));
                    sendAiPrompt(userPrompt, activeTabItem);
                });
                return;
            }
        } else {
            apiKey = "no-key-required";
        }

        aiAssistant.setApiKey(apiKey);

        try {
            aiAssistant.sendPrompt(userPrompt, currentFileName, currentFileContent);
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
                    refreshCodebaseIndex(); // Re-index after changes
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
                activity.getAiChatFragment().addMessage(aiMessage); // Add to local list and UI
                activity.getAiChatFragment().saveChatHistoryToPrefs(); // Save to SharedPreferences
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
                aiChatFragment.saveChatHistoryToPrefs();
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
    public void onAiRequestCompleted() {
        // No specific UI update needed here, as the final response is added by onAiActionsProcessed or onAiError
    }

    @Override
    public void onAiContextBuildingStarted() {
        // No-op, progress is shown via indexing status in AIChatFragment
    }

    @Override
    public void onAiContextBuildingCompleted() {
        // No-op, AI is thinking message will appear in AIChatFragment
    }

    @Override
    public void onIndexingStarted(int totalFiles) {
        activity.runOnUiThread(() -> {
            AIChatFragment aiChatFragment = activity.getAiChatFragment();
            CodeEditorFragment codeEditorFragment = activity.getCodeEditorFragment();
            if (aiChatFragment != null) {
                aiChatFragment.onIndexingStarted(totalFiles);
            }
            if (codeEditorFragment != null) {
                codeEditorFragment.onIndexingStarted(totalFiles);
            }
        });
    }

    @Override
    public void onIndexingProgress(int indexedCount, int totalFiles, String currentFile) {
        activity.runOnUiThread(() -> {
            AIChatFragment aiChatFragment = activity.getAiChatFragment();
            CodeEditorFragment codeEditorFragment = activity.getCodeEditorFragment();
            if (aiChatFragment != null) {
                aiChatFragment.onIndexingProgress(indexedCount, totalFiles, currentFile);
            }
            if (codeEditorFragment != null) {
                codeEditorFragment.onIndexingProgress(indexedCount, totalFiles, currentFile);
            }
        });
    }

    @Override
    public void onIndexingCompleted() {
        activity.runOnUiThread(() -> {
            AIChatFragment aiChatFragment = activity.getAiChatFragment();
            CodeEditorFragment codeEditorFragment = activity.getCodeEditorFragment();
            if (aiChatFragment != null) {
                aiChatFragment.onIndexingCompleted();
            }
            if (codeEditorFragment != null) {
                codeEditorFragment.onIndexingCompleted();
            }
        });
    }

    @Override
    public void onIndexingError(String errorMessage) {
        activity.runOnUiThread(() -> {
            AIChatFragment aiChatFragment = activity.getAiChatFragment();
            CodeEditorFragment codeEditorFragment = activity.getCodeEditorFragment();
            if (aiChatFragment != null) {
                aiChatFragment.onIndexingError(errorMessage);
            }
            if (codeEditorFragment != null) {
                codeEditorFragment.onIndexingError(errorMessage);
            }
        });
    }
}