package com.codex.apk;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class AIChatFragment extends Fragment implements ChatMessageAdapter.OnAiActionInteractionListener {

    private List<ChatMessage> chatHistory;
    private QwenConversationState qwenConversationState;
    private ChatMessageAdapter chatMessageAdapter;

    private AIChatUIManager uiManager;
    private AIChatHistoryManager historyManager;

    private AIChatFragmentListener listener;
    private AIAssistant aiAssistant;
    private ChatMessage currentAiStatusMessage = null;
    public boolean isAiProcessing = false;
    private String projectPath;
    private final List<java.io.File> pendingAttachments = new java.util.ArrayList<>();
    private androidx.activity.result.ActivityResultLauncher<String[]> pickFilesLauncher;

    public interface AIChatFragmentListener {
        AIAssistant getAIAssistant();
        void sendAiPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState);
        void onAiAcceptActions(int messagePosition, ChatMessage message);
        void onAiDiscardActions(int messagePosition, ChatMessage message);
        void onReapplyActions(int messagePosition, ChatMessage message);
        void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail);
        void onQwenConversationStateUpdated(QwenConversationState state);
        void onPlanAcceptClicked(int messagePosition, ChatMessage message);
        void onPlanDiscardClicked(int messagePosition, ChatMessage message);
    }

    // Hook used by UI manager to trigger attachment selection
    public void onAttachButtonClicked() {
        if (pickFilesLauncher != null) {
            pickFilesLauncher.launch(new String[]{"image/*", "application/pdf", "text/*", "application/octet-stream", "application/zip"});
        }
    }

    // Called by UI to remove an attachment from the pending list
    public void removePendingAttachmentAt(int index) {
        if (index >= 0 && index < pendingAttachments.size()) {
            pendingAttachments.remove(index);
            if (uiManager != null) uiManager.showAttachedFilesPreview(pendingAttachments);
        }
    }

    public static AIChatFragment newInstance(String projectPath) {
        AIChatFragment fragment = new AIChatFragment();
        Bundle args = new Bundle();
        args.putString("project_path", projectPath);
        fragment.setArguments(args);
        return fragment;
    }

    public String getProjectPath() { return projectPath; }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AIChatFragmentListener) {
            listener = (AIChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AIChatFragmentListener");
        }
        // Prepare file picker
        pickFilesLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris == null || uris.isEmpty()) return;
            android.content.ContentResolver cr = requireContext().getContentResolver();
            for (android.net.Uri uri : uris) {
                try (java.io.InputStream in = cr.openInputStream(uri)) {
                    if (in == null) continue;
                    String name = queryDisplayName(cr, uri);
                    if (name == null || name.isEmpty()) name = "attachment";
                    java.io.File out = new java.io.File(requireContext().getCacheDir(), System.currentTimeMillis() + "_" + name);
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) != -1) fos.write(buf, 0, r);
                    }
                    pendingAttachments.add(out);
                } catch (Exception ignore) {}
            }
            Toast.makeText(requireContext(), pendingAttachments.size() + " file(s) attached", Toast.LENGTH_SHORT).show();
            if (uiManager != null) uiManager.showAttachedFilesPreview(pendingAttachments);
        });
    }
    private String queryDisplayName(android.content.ContentResolver cr, android.net.Uri uri) {
        String name = null;
        android.database.Cursor cursor = cr.query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) name = cursor.getString(0);
            } finally { cursor.close(); }
        }
        return name;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectPath = getArguments() != null ? getArguments().getString("project_path") : "default_project";
        chatHistory = new ArrayList<>();
        qwenConversationState = new QwenConversationState();
        historyManager = new AIChatHistoryManager(requireContext(), projectPath);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_ai_chat_tab, container, false);
        uiManager = new AIChatUIManager(this, view);

        chatMessageAdapter = new ChatMessageAdapter(requireContext(), chatHistory);
        chatMessageAdapter.setOnAiActionInteractionListener(this);
        uiManager.setupRecyclerView(chatMessageAdapter);

        // Update attach icon visibility/state based on current model
        if (listener != null && listener.getAIAssistant() != null) {
            uiManager.updateSettingsButtonState(listener.getAIAssistant());
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        historyManager.loadChatState(chatHistory, qwenConversationState);
        removeDuplicateRestoredAiJsonMessages();
        aiAssistant = listener.getAIAssistant();
        uiManager.updateUiVisibility(chatHistory.isEmpty());
        uiManager.setListeners();

        if (aiAssistant != null) {
            com.codex.apk.ai.AIModel currentModel = aiAssistant.getCurrentModel();
            if (currentModel != null) {
                // Verify that the current model is still enabled
                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("model_settings", Context.MODE_PRIVATE);
                String key = "model_" + currentModel.getProvider().name() + "_" + currentModel.getModelId() + "_enabled";
                boolean isEnabled = prefs.getBoolean(key, true);

                if (isEnabled) {
                    uiManager.textSelectedModel.setText(currentModel.getDisplayName());
                } else {
                    // The previously selected model is now disabled.
                    aiAssistant.setCurrentModel(null);
                    uiManager.textSelectedModel.setText("Select a model");
                }
            } else {
                uiManager.textSelectedModel.setText("Select a model");
            }
            uiManager.updateSettingsButtonState(aiAssistant);
        }

        // Scroll to last message when chat opens
        uiManager.scrollToBottom();
    }

    public AIAssistant getAIAssistant() {
        return this.aiAssistant;
    }

    public QwenConversationState getQwenState() { return this.qwenConversationState; }

    public ChatMessage getMessageAt(int position) {
        if (position >= 0 && position < chatHistory.size()) return chatHistory.get(position);
        return null;
    }

    public void sendPrompt() {
        if (aiAssistant == null || aiAssistant.getCurrentModel() == null) {
            Toast.makeText(requireContext(), "Please select an AI model first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt = uiManager.getText().trim();
        if (prompt.isEmpty() || isAiProcessing) {
            if(isAiProcessing) Toast.makeText(requireContext(), "AI is processing...", Toast.LENGTH_SHORT).show();
            else Toast.makeText(requireContext(), getString(R.string.please_enter_a_message), Toast.LENGTH_SHORT).show();
            return;
        }

        uiManager.setSendButtonEnabled(false);

        ChatMessage userMsg = new ChatMessage(ChatMessage.SENDER_USER, prompt, System.currentTimeMillis());
        if (!pendingAttachments.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (java.io.File f : pendingAttachments) names.add(f.getAbsolutePath());
            userMsg.setUserAttachmentPaths(names);
        }
        addMessage(userMsg);


        uiManager.setText("");
        if (listener != null) {
            // Directly call assistant with attachments only for COOKIES provider
            if (aiAssistant != null && aiAssistant.getCurrentModel() != null
                && aiAssistant.getCurrentModel().getProvider() == com.codex.apk.ai.AIProvider.COOKIES) {
                aiAssistant.sendMessage(prompt, new ArrayList<>(chatHistory), qwenConversationState, new java.util.ArrayList<>(pendingAttachments));
            } else {
                listener.sendAiPrompt(prompt, new ArrayList<>(chatHistory), qwenConversationState);
            }
            pendingAttachments.clear();
            if (uiManager != null) uiManager.showAttachedFilesPreview(pendingAttachments);
        }
    }

    public int addMessage(ChatMessage message) {
        if (message == null || message.getContent() == null) return -1;

        int indexChangedOrAdded = -1;

        if (message.getSender() == ChatMessage.SENDER_AI) {
            if (message.getContent().equals(getString(R.string.ai_is_thinking))) {
                if (!isAiProcessing) {
                    chatHistory.add(message);
                    currentAiStatusMessage = message;
                    isAiProcessing = true;
                    indexChangedOrAdded = chatHistory.size() - 1;
                    chatMessageAdapter.notifyItemInserted(indexChangedOrAdded);
                    uiManager.scrollToBottom();
                }
            } else {
                if (isAiProcessing && currentAiStatusMessage != null) {
                    int index = chatHistory.indexOf(currentAiStatusMessage);
                    if (index != -1) {
                        chatHistory.set(index, message);
                        currentAiStatusMessage = message;
                        chatMessageAdapter.notifyItemChanged(index);
                        indexChangedOrAdded = index;
                    } else {
                        chatHistory.add(message);
                        indexChangedOrAdded = chatHistory.size() - 1;
                        chatMessageAdapter.notifyItemInserted(indexChangedOrAdded);
                    }
                } else {
                    chatHistory.add(message);
                    indexChangedOrAdded = chatHistory.size() - 1;
                    chatMessageAdapter.notifyItemInserted(indexChangedOrAdded);
                }
                isAiProcessing = false;
                currentAiStatusMessage = null;
                uiManager.setSendButtonEnabled(true);
                uiManager.scrollToBottom();
            }
        } else {
            chatHistory.add(message);
            indexChangedOrAdded = chatHistory.size() - 1;
            chatMessageAdapter.notifyItemInserted(indexChangedOrAdded);
            uiManager.scrollToBottom();
        }
        uiManager.updateUiVisibility(chatHistory.isEmpty());
        // Persist chat history so it restores when reopening the project
        if (historyManager != null) {
            historyManager.saveChatState(chatHistory, qwenConversationState);
        }
        return indexChangedOrAdded;
    }

    private void removeDuplicateRestoredAiJsonMessages() {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return;
        }

        boolean removed = false;
        for (int i = chatHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = chatHistory.get(i);
            if (msg == null || msg.getSender() != ChatMessage.SENDER_AI) {
                continue;
            }
            String content = msg.getContent();
            String raw = msg.getRawAiResponseJson();
            if (content == null || raw == null) {
                continue;
            }
            String trimmedContent = content.trim();
            String trimmedRaw = raw.trim();
            if (trimmedContent.isEmpty()) {
                continue;
            }
            if (!trimmedContent.equals(trimmedRaw)) {
                continue;
            }
            if (!com.codex.apk.QwenResponseParser.looksLikeJson(trimmedContent)) {
                continue;
            }
            chatHistory.remove(i);
            removed = true;
        }

        if (removed) {
            if (chatMessageAdapter != null) {
                chatMessageAdapter.notifyDataSetChanged();
            }
            if (historyManager != null) {
                historyManager.saveChatState(chatHistory, qwenConversationState);
            }
        }
    }

    public void updateThinkingMessage(String newContent) {
        if (!isAiProcessing || currentAiStatusMessage == null) return;
        currentAiStatusMessage.setContent(newContent);
        int idx = chatHistory.indexOf(currentAiStatusMessage);
        if (idx != -1) {
            chatMessageAdapter.notifyItemChanged(idx);
        }
    }

    public void hideThinkingMessage() {
        // Always clear the processing state even if the current status message reference is lost
        if (isAiProcessing && currentAiStatusMessage != null) {
            int index = chatHistory.indexOf(currentAiStatusMessage);
            if (index != -1) {
                chatHistory.remove(index);
                chatMessageAdapter.notifyItemRemoved(index);
            }
        }
        isAiProcessing = false;
        currentAiStatusMessage = null;
        uiManager.setSendButtonEnabled(true);
    }

    public void updateMessage(int position, ChatMessage updatedMessage) {
        if (position >= 0 && position < chatHistory.size()) {
            chatHistory.set(position, updatedMessage);
            chatMessageAdapter.notifyItemChanged(position);
            if (uiManager != null) uiManager.scrollToBottom();
            historyManager.saveChatState(chatHistory, qwenConversationState);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onAcceptClicked(int pos, ChatMessage msg) { if (listener != null) listener.onAiAcceptActions(pos, msg); }
    @Override
    public void onDiscardClicked(int pos, ChatMessage msg) { if (listener != null) listener.onAiDiscardActions(pos, msg); }
    @Override
    public void onReapplyClicked(int pos, ChatMessage msg) { if (listener != null) listener.onReapplyActions(pos, msg); }
    @Override
    public void onFileChangeClicked(ChatMessage.FileActionDetail detail) { if (listener != null) listener.onAiFileChangeClicked(detail); }
    @Override
    public void onPlanAcceptClicked(int pos, ChatMessage msg) { if (listener != null) listener.onPlanAcceptClicked(pos, msg); }
    @Override
    public void onPlanDiscardClicked(int pos, ChatMessage msg) { if (listener != null) listener.onPlanDiscardClicked(pos, msg); }

    public void onQwenConversationStateUpdated(QwenConversationState state) {
        if (state != null) {
            this.qwenConversationState = state;
            historyManager.saveChatState(chatHistory, qwenConversationState);
        }
    }
}
