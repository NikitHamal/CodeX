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

    public interface AIChatFragmentListener {
        AIAssistant getAIAssistant();
        void sendAiPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState);
        void onAiAcceptActions(int messagePosition, ChatMessage message);
        void onAiDiscardActions(int messagePosition, ChatMessage message);
        void onReapplyActions(int messagePosition, ChatMessage message);
        void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail);
        void onQwenConversationStateUpdated(QwenConversationState state);
    }

    public static AIChatFragment newInstance(String projectPath) {
        AIChatFragment fragment = new AIChatFragment();
        Bundle args = new Bundle();
        args.putString("project_path", projectPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AIChatFragmentListener) {
            listener = (AIChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AIChatFragmentListener");
        }
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

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        historyManager.loadChatState(chatHistory, qwenConversationState);
        aiAssistant = listener.getAIAssistant();
        uiManager.updateUiVisibility(chatHistory.isEmpty());
        uiManager.setListeners();
        if (aiAssistant != null) {
            uiManager.textSelectedModel.setText(aiAssistant.getCurrentModel().getDisplayName());
            uiManager.updateSettingsButtonState(aiAssistant);
        }
    }

    public AIAssistant getAIAssistant() {
        return this.aiAssistant;
    }

    public void sendPrompt() {
        String prompt = uiManager.getText().trim();
        if (prompt.isEmpty() || isAiProcessing) {
            if(isAiProcessing) Toast.makeText(requireContext(), "AI is processing...", Toast.LENGTH_SHORT).show();
            else Toast.makeText(requireContext(), getString(R.string.please_enter_a_message), Toast.LENGTH_SHORT).show();
            return;
        }

        uiManager.setSendButtonEnabled(false);

        ChatMessage userMsg = new ChatMessage(ChatMessage.SENDER_USER, prompt, System.currentTimeMillis());
        addMessage(userMsg);

        // Show "AI is thinking..." message
        ChatMessage thinkingMessage = new ChatMessage(ChatMessage.SENDER_AI, getString(R.string.ai_is_thinking), System.currentTimeMillis());
        // Set the model name for the thinking message
        if (aiAssistant != null && aiAssistant.getCurrentModel() != null) {
            thinkingMessage.setAiModelName(aiAssistant.getCurrentModel().getDisplayName());
        }
        addMessage(thinkingMessage);

        uiManager.setText("");
        if (listener != null) {
            listener.sendAiPrompt(prompt, new ArrayList<>(chatHistory), qwenConversationState);
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
        return indexChangedOrAdded;
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
        if (!isAiProcessing || currentAiStatusMessage == null) return;
        int index = chatHistory.indexOf(currentAiStatusMessage);
        if (index != -1) {
            chatHistory.remove(index);
            chatMessageAdapter.notifyItemRemoved(index);
        }
        isAiProcessing = false;
        currentAiStatusMessage = null;
        uiManager.setSendButtonEnabled(true);
    }

    public void updateMessage(int position, ChatMessage updatedMessage) {
        if (position >= 0 && position < chatHistory.size()) {
            chatHistory.set(position, updatedMessage);
            chatMessageAdapter.notifyItemChanged(position);
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

    public void onQwenConversationStateUpdated(QwenConversationState state) {
        if (state != null) {
            this.qwenConversationState = state;
            historyManager.saveChatState(chatHistory, qwenConversationState);
        }
    }
}
