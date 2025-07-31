package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.android.material.button.MaterialButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Base64; // Added import for Base64

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException; // Added import for UnsupportedEncodingException
import java.lang.reflect.Type;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// AIChatFragment now implements ChatMessageAdapter.OnAiActionInteractionListener
public class AIChatFragment extends Fragment implements
        ChatMessageAdapter.OnAiActionInteractionListener {

    private static final String TAG = "AIChatFragment";
    private static final String PREFS_NAME = "ai_chat_prefs";
    // CHAT_HISTORY_KEY will now be a prefix, actual key will include projectPath
    private static final String CHAT_HISTORY_KEY_PREFIX = "chat_history_";
    // Old generic key for migration purposes
    private static final String OLD_GENERIC_CHAT_HISTORY_KEY = "chat_history";


    private RecyclerView recyclerViewChatHistory;
    private ChatMessageAdapter chatMessageAdapter;
    private List<ChatMessage> chatHistory;
    private EditText editTextAiPrompt;
    private MaterialButton buttonAiSend;

    // New UI elements for empty state and custom model selector
    private LinearLayout layoutEmptyState;
    private TextView textGreeting;

    private LinearLayout layoutInputSection;
    private LinearLayout layoutModelSelectorCustom;
    private TextView textSelectedModel;
    private LinearLayout linearPromptInput;
    private ImageView buttonAiSettings;

    // Model picker dialog components
    private BottomSheetDialog modelPickerDialog;
    private BottomSheetDialog aiSettingsDialog;
    private BottomSheetDialog webSourcesDialog;

    private AIChatFragmentListener listener; // Keep the listener interface
    private AIAssistant aiAssistant; // This will be obtained from the listener

    // To manage the "AI is thinking..." message
    private ChatMessage currentAiStatusMessage = null;
    public boolean isAiProcessing = false;

    private String projectPath; // New field to store the current project's path

    /**
     * Interface for actions related to AI chat that need to be handled by the parent activity.
     * This interface is implemented by EditorActivity.
     */
    public interface AIChatFragmentListener {
        AIAssistant getAIAssistant();
        void sendAiPrompt(String userPrompt);
        void onAiAcceptActions(int messagePosition, ChatMessage message);
        void onAiDiscardActions(int messagePosition, ChatMessage message);
        void onReapplyActions(int messagePosition, ChatMessage message);
        void onAiFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail);
    }

    /**
     * Factory method to create a new instance of this fragment with a project path.
     * @param projectPath The absolute path of the current project directory.
     * @return A new instance of fragment AIChatFragment.
     */
    public static AIChatFragment newInstance(String projectPath) {
        AIChatFragment fragment = new AIChatFragment();
        Bundle args = new Bundle();
        args.putString("projectPath", projectPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the hosting activity implements the listener interface
        if (context instanceof AIChatFragmentListener) {
            listener = (AIChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement AIChatFragmentListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve projectPath from arguments
        if (getArguments() != null) {
            projectPath = getArguments().getString("projectPath");
        } else {
            Log.e(TAG, "projectPath not provided to AIChatFragment!");
            // Fallback for safety, though this should ideally not happen
            projectPath = "default_project";
        }

        chatHistory = new ArrayList<>();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadChatHistoryFromPrefs();
        updateUiVisibility(); // Also call this here to set initial state

        // Additional debugging for view dimensions
        view.post(() -> {
            Log.d(TAG, "Fragment view dimensions: " + view.getWidth() + "x" + view.getHeight());
            if (layoutInputSection != null) {
                Log.d(TAG, "Input section dimensions: " + layoutInputSection.getWidth() + "x" + layoutInputSection.getHeight());
                Log.d(TAG, "Input section visibility: " + layoutInputSection.getVisibility());
            }
            if (linearPromptInput != null) {
                Log.d(TAG, "Prompt input dimensions: " + linearPromptInput.getWidth() + "x" + linearPromptInput.getHeight());
                Log.d(TAG, "Prompt input visibility: " + linearPromptInput.getVisibility());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_ai_chat_tab, container, false);

        // Initialize UI components with null checks
        try {
            recyclerViewChatHistory = view.findViewById(R.id.recycler_view_chat_history);
            editTextAiPrompt = view.findViewById(R.id.edit_text_ai_prompt);
            buttonAiSend = view.findViewById(R.id.button_ai_send);

            // Empty state UI elements
            layoutEmptyState = view.findViewById(R.id.layout_empty_state);
            textGreeting = view.findViewById(R.id.text_greeting);

            // Input section UI elements
            layoutInputSection = view.findViewById(R.id.layout_input_section);
            layoutModelSelectorCustom = view.findViewById(R.id.layout_model_selector_custom);
            textSelectedModel = view.findViewById(R.id.text_selected_model);
            linearPromptInput = view.findViewById(R.id.linear_prompt_input);
            buttonAiSettings = view.findViewById(R.id.button_ai_settings);
            
            // Log what we found for debugging
            Log.d(TAG, "UI Components found: recyclerView=" + (recyclerViewChatHistory != null) +
                      ", editText=" + (editTextAiPrompt != null) +
                      ", sendButton=" + (buttonAiSend != null) +
                      ", emptyState=" + (layoutEmptyState != null) +
                      ", greeting=" + (textGreeting != null) +
                      ", inputSection=" + (layoutInputSection != null) +
                      ", modelSelector=" + (layoutModelSelectorCustom != null) +
                      ", promptInput=" + (linearPromptInput != null));

            // Verify critical components exist
            if (recyclerViewChatHistory == null) {
                throw new RuntimeException("RecyclerView not found in layout");
            }
            if (editTextAiPrompt == null) {
                throw new RuntimeException("EditText not found in layout");
            }
            if (buttonAiSend == null) {
                throw new RuntimeException("Send button not found in layout");
            }
            if (layoutEmptyState == null) {
                throw new RuntimeException("Empty state layout not found");
            }
            if (textGreeting == null) {
                throw new RuntimeException("Greeting text not found");
            }
            if (layoutInputSection == null) {
                throw new RuntimeException("Input section layout not found");
            }
            if (layoutModelSelectorCustom == null) {
                throw new RuntimeException("Model selector layout not found");
            }
            if (linearPromptInput == null) {
                throw new RuntimeException("Prompt input layout not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI components: " + e.getMessage(), e);
            // Show error in the fragment instead of returning a different view
            showChatLoadError("Failed to initialize chat interface: " + e.getMessage());
            return view; // Return the original view but show error state
        }

        // Set up RecyclerView
        try {
            chatMessageAdapter = new ChatMessageAdapter(requireContext(), chatHistory);
            chatMessageAdapter.setOnAiActionInteractionListener(this); // Set this fragment as the listener
            recyclerViewChatHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerViewChatHistory.setAdapter(chatMessageAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            showChatLoadError("Failed to initialize chat history: " + e.getMessage());
            return view;
        }

        // Initialize AI Assistant from listener
        if (listener != null) {
            try {
                aiAssistant = listener.getAIAssistant();
                if (aiAssistant != null) {
                    // Set initial selected model text
                    if (textSelectedModel != null) {
                        textSelectedModel.setText(aiAssistant.getCurrentModel().getDisplayName());
                    }
                } else {
                    Log.e(TAG, "AIAssistant is null from listener!");
                    showChatLoadError("AI Assistant not available. Please check your settings.");
                    return view;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting AI Assistant from listener", e);
                showChatLoadError("Failed to initialize AI Assistant: " + e.getMessage());
                return view;
            }
        } else {
            Log.e(TAG, "Listener is null in onCreateView!");
            showChatLoadError("Chat interface not properly connected to editor.");
            return view;
        }


        // Set up custom model selector click listener
        if (layoutModelSelectorCustom != null) {
            layoutModelSelectorCustom.setOnClickListener(v -> showModelPickerDialog());
        }

        // Set up AI settings button click listener
        if (buttonAiSettings != null) {
            buttonAiSettings.setOnClickListener(v -> showAiSettingsDialog());
        }

        // Set up send button click listener
        if (buttonAiSend != null) {
            buttonAiSend.setOnClickListener(v -> sendPrompt());
        }

        // Set up IME action listener for Enter key
        if (editTextAiPrompt != null) {
            editTextAiPrompt.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendPrompt();
                    return true;
                }
                return false;
            });
        }

        // Update UI visibility based on chat history
        updateUiVisibility();

        // Force visibility of input section as a safety measure
        if (layoutInputSection != null) {
            layoutInputSection.setVisibility(View.VISIBLE);
            Log.d(TAG, "Input section visibility set to VISIBLE");
        }

        return view;
    }

    /**
     * Generates a project-specific key for SharedPreferences.
     * This ensures chat history is isolated per project.
     * @return A unique key for the current project's chat history.
     */
    private String getChatHistoryKey() {
        if (projectPath == null || projectPath.isEmpty()) {
            Log.w(TAG, "projectPath is null or empty, using generic chat history key as fallback.");
            return CHAT_HISTORY_KEY_PREFIX + "generic_fallback"; // Use a distinct fallback
        }
        // Use Base64 encoding of the projectPath to ensure a unique and safe key
        try {
            byte[] pathBytes = projectPath.getBytes("UTF-8");
            // Use Base64.NO_WRAP to prevent newlines and Base64.URL_SAFE for URL-friendly characters
            String encodedPath = Base64.encodeToString(pathBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            return CHAT_HISTORY_KEY_PREFIX + encodedPath;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UTF-8 encoding not supported, falling back to simple sanitization.", e);
            // Fallback to simple sanitization if Base64 fails (highly unlikely on Android)
            return CHAT_HISTORY_KEY_PREFIX + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
        }
    }


    /**
     * Shows the model selection dialog.
     */
    private void showModelSelectorDialog() {
        if (aiAssistant == null) {
            Toast.makeText(requireContext(), "AI Assistant not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> modelNamesList = AIAssistant.AIModel.getAllDisplayNames();
        String[] modelNames = modelNamesList.toArray(new String[0]);
        String currentModel = aiAssistant.getCurrentModel().getDisplayName();
        int selectedIndex = -1;

        // Find current model index
        for (int i = 0; i < modelNames.length; i++) {
            if (modelNames[i].equals(currentModel)) {
                selectedIndex = i;
                break;
            }
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select AI Model")
                .setSingleChoiceItems(modelNames, selectedIndex, (dialog, which) -> {
                    String selectedModelName = modelNames[which];
                    onModelSelected(selectedModelName);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onModelSelected(String selectedModelDisplayName) {
        if (aiAssistant != null) {
            AIAssistant.AIModel selectedModel = AIAssistant.AIModel.fromDisplayName(selectedModelDisplayName);
            if (selectedModel != null) {
                aiAssistant.setCurrentModel(selectedModel);
                textSelectedModel.setText(selectedModel.getDisplayName()); // Update the displayed model name
                Toast.makeText(requireContext(), "AI Model set to: " + selectedModel.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sends the user's prompt to the AI Assistant.
     */
    private void sendPrompt() {
        String prompt = editTextAiPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user message to chat history
        addMessage(new ChatMessage(ChatMessage.SENDER_USER, prompt, System.currentTimeMillis()));
        editTextAiPrompt.setText(""); // Clear input field

        // Notify activity to send prompt to AI
        if (listener != null) {
            listener.sendAiPrompt(prompt);
        }
    }

    /**
     * Adds a message to the chat history and updates the RecyclerView.
     * This method handles both user and AI messages, including the "AI is thinking..." state.
     * @param message The ChatMessage object to add.
     */
    public void addMessage(ChatMessage message) {
        if (message == null) {
            Log.e(TAG, "addMessage: message is null");
            return;
        }
        if (chatMessageAdapter == null || recyclerViewChatHistory == null || chatHistory == null) {
            Log.e(TAG, "addMessage: UI components not initialized");
            return;
        }
        if (message.getContent() == null) {
            Log.e(TAG, "addMessage: message content is null");
            return;
        }
        if (message.getSender() == ChatMessage.SENDER_AI) {
            if (message.getContent().equals("AI is thinking...")) {
                // Handle "AI is thinking..." message
                if (!isAiProcessing) {
                    chatHistory.add(message);
                    currentAiStatusMessage = message;
                    isAiProcessing = true;
                    chatMessageAdapter.notifyItemInserted(chatHistory.size() - 1);
                    recyclerViewChatHistory.scrollToPosition(chatHistory.size() - 1);
                } else {
                    // If AI is already thinking, just update the existing thinking message (no new insertion)
                    Log.d(TAG, "AI is already thinking, not adding new 'thinking' message.");
                }
            } else {
                // Full AI response message
                if (isAiProcessing && currentAiStatusMessage != null) {
                    int index = chatHistory.indexOf(currentAiStatusMessage);
                    if (index != -1) {
                        chatHistory.set(index, message); // Replace the status message with the actual response
                        chatMessageAdapter.notifyItemChanged(index);
                        recyclerViewChatHistory.scrollToPosition(index);
                    } else {
                        // Fallback: if status message somehow not found, add as new
                        chatHistory.add(message);
                        chatMessageAdapter.notifyItemInserted(chatHistory.size() - 1);
                        recyclerViewChatHistory.scrollToPosition(chatHistory.size() - 1);
                    }
                } else {
                    // Add as new message if no status message was present
                    chatHistory.add(message);
                    chatMessageAdapter.notifyItemInserted(chatHistory.size() - 1);
                    recyclerViewChatHistory.scrollToPosition(chatHistory.size() - 1);
                }
                isAiProcessing = false; // Reset processing state
                currentAiStatusMessage = null; // Clear reference
            }
        } else {
            // For user messages - don't set processing state here
            chatHistory.add(message);
            chatMessageAdapter.notifyItemInserted(chatHistory.size() - 1);
            recyclerViewChatHistory.scrollToPosition(chatHistory.size() - 1);
        }
        updateUiVisibility(); // Update UI after adding message
    }

    /**
     * Updates the content of the current streaming AI message (either thinking or answer phase).
     * Called on the UI thread.
     */
    public void updateThinkingMessage(String newContent) {
        if (!isAiProcessing || currentAiStatusMessage == null) return;
        currentAiStatusMessage.setContent(newContent);
        int idx = chatHistory.indexOf(currentAiStatusMessage);
        if (idx != -1 && chatMessageAdapter != null) {
            chatMessageAdapter.notifyItemChanged(idx);
            recyclerViewChatHistory.scrollToPosition(idx);
        }
    }

    /**
     * Updates an existing message in the chat history and notifies the adapter.
     * This is used to update the status of AI messages (e.g., after Accept/Discard).
     * @param position The position of the message to update.
     * @param updatedMessage The updated ChatMessage object.
     */
    public void updateMessage(int position, ChatMessage updatedMessage) {
        if (position >= 0 && position < chatHistory.size()) {
            chatHistory.set(position, updatedMessage);
            chatMessageAdapter.notifyItemChanged(position);
            saveChatHistoryToPrefs(); // Save updated history
        }
    }

    /**
     * Updates the visibility of the empty state layout and chat history RecyclerView.
     */
    private void updateUiVisibility() {
        if (chatHistory.isEmpty()) {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.VISIBLE);
            }
            if (recyclerViewChatHistory != null) {
                recyclerViewChatHistory.setVisibility(View.GONE);
            }
            // For the first message, the prompt hint is different
            if (editTextAiPrompt != null) {
                editTextAiPrompt.setHint("How can CodeX help you today?");
            }
        } else {
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }
            if (recyclerViewChatHistory != null) {
                recyclerViewChatHistory.setVisibility(View.VISIBLE);
            }
            // After the first message, the prompt hint is different
            if (editTextAiPrompt != null) {
                editTextAiPrompt.setHint("Reply to CodeX");
            }
        }

        // Always ensure input section components are visible
        if (layoutInputSection != null) {
            layoutInputSection.setVisibility(View.VISIBLE);
        }
        if (layoutModelSelectorCustom != null) {
            layoutModelSelectorCustom.setVisibility(View.VISIBLE);
        }
        if (linearPromptInput != null) {
            linearPromptInput.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Loads chat history from SharedPreferences using the project-specific key.
     * If no history is found for the project-specific key, it attempts to migrate
     * history from the old generic key.
     */
    public void loadChatHistoryFromPrefs() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        Type type = new TypeToken<List<ChatMessage>>() {}.getType();

        // 1. Try to load from the project-specific key
        String projectSpecificJson = prefs.getString(getChatHistoryKey(), null);
        if (projectSpecificJson != null) {
            List<ChatMessage> loadedHistory = gson.fromJson(projectSpecificJson, type);
            if (loadedHistory != null) {
                chatHistory.clear();
                chatHistory.addAll(loadedHistory);
                Log.d(TAG, "Loaded chat history for project: " + projectPath);
                return; // History found for this project, no migration needed
            }
        }

        // 2. If no project-specific history, try to load from the old generic key (for migration)
        String oldGenericJson = prefs.getString(OLD_GENERIC_CHAT_HISTORY_KEY, null);
        if (oldGenericJson != null) {
            List<ChatMessage> loadedHistory = gson.fromJson(oldGenericJson, type);
            if (loadedHistory != null && !loadedHistory.isEmpty()) {
                chatHistory.clear();
                chatHistory.addAll(loadedHistory);
                Log.d(TAG, "Migrating chat history from old generic key for project: " + projectPath);
                // Immediately save to the new project-specific key
                saveChatHistoryToPrefs();
                // Optionally, remove the old generic key's data to clean up.
                // For now, we'll leave it to avoid accidental data loss for other projects
                // that haven't been opened yet and migrated.
                // prefs.edit().remove(OLD_GENERIC_CHAT_HISTORY_KEY).apply();
            }
        }
        Log.d(TAG, "No chat history found for project: " + projectPath + ". Starting fresh.");
    }

    /**
     * Saves chat history to SharedPreferences using the project-specific key.
     */
    public void saveChatHistoryToPrefs() {
        if (!isAdded()) {
            return;
        }
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(chatHistory);
        editor.putString(getChatHistoryKey(), json); // Use project-specific key
        editor.apply();
    }

    /**
     * Exports chat history for a given project path to a JSON file.
     * This method is static so it can be called directly from MainActivity for export.
     * @param context The application context.
     * @param projectPath The absolute path of the project.
     * @param outputFile The file to write the chat history JSON to.
     * @return true if export was successful, false otherwise.
     */
    public static boolean exportChatHistoryToJson(Context context, String projectPath, File outputFile) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Recreate the project-specific key for the given projectPath
        String chatHistoryKey;
        try {
            byte[] pathBytes = projectPath.getBytes("UTF-8");
            String encodedPath = Base64.encodeToString(pathBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            chatHistoryKey = CHAT_HISTORY_KEY_PREFIX + encodedPath;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UTF-8 encoding not supported during export, falling back to simple sanitization.", e);
            chatHistoryKey = CHAT_HISTORY_KEY_PREFIX + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
        }

        String json = prefs.getString(chatHistoryKey, null);
        if (json == null) {
            Log.d(TAG, "No chat history found for project " + projectPath + " to export.");
            return false;
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(json);
            Log.d(TAG, "Chat history exported to: " + outputFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting chat history to JSON", e);
            return false;
        }
    }

    /**
     * Imports chat history from a JSON file for a given project path.
     * This method is static so it can be called directly from MainActivity for import.
     * @param context The application context.
     * @param projectPath The absolute path of the project where history should be imported.
     * @param inputFile The JSON file containing the chat history.
     * @return true if import was successful, false otherwise.
     */
    public static boolean importChatHistoryFromJson(Context context, String projectPath, File inputFile) {
        if (!inputFile.exists()) {
            Log.e(TAG, "Chat history import file does not exist: " + inputFile.getAbsolutePath());
            return false;
        }

        try (FileReader reader = new FileReader(inputFile)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<ChatMessage>>() {}.getType();
            List<ChatMessage> importedHistory = gson.fromJson(reader, type);

            if (importedHistory != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                // Recreate the project-specific key for the given projectPath
                String chatHistoryKey;
                try {
                    byte[] pathBytes = projectPath.getBytes("UTF-8");
                    String encodedPath = Base64.encodeToString(pathBytes, Base64.NO_WRAP | Base64.URL_SAFE);
                    chatHistoryKey = CHAT_HISTORY_KEY_PREFIX + encodedPath;
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UTF-8 encoding not supported during import, falling back to simple sanitization.", e);
                    chatHistoryKey = CHAT_HISTORY_KEY_PREFIX + projectPath.replaceAll("[^a-zA-Z0-9_]", "_");
                }

                editor.putString(chatHistoryKey, gson.toJson(importedHistory));
                editor.apply();
                Log.d(TAG, "Chat history imported from " + inputFile.getAbsolutePath() + " for project: " + projectPath);
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading chat history JSON file during import", e);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing chat history JSON during import", e);
        }
        return false;
    }

    /**
     * Shows the new model picker dialog with provider sections and refresh functionality
     */
    private void showModelPickerDialog() {
        if (aiAssistant == null) return;
        
        modelPickerDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_model_picker, null);
        modelPickerDialog.setContentView(dialogView);
        
        // Initialize RecyclerViews for each provider
        RecyclerView googleModels = dialogView.findViewById(R.id.recycler_google_models);
        RecyclerView huggingfaceModels = dialogView.findViewById(R.id.recycler_huggingface_models);
        RecyclerView alibabaModels = dialogView.findViewById(R.id.recycler_alibaba_models);
        RecyclerView zModels = dialogView.findViewById(R.id.recycler_z_models);
        
        ImageView refreshAlibaba = dialogView.findViewById(R.id.button_refresh_alibaba);
        ImageView refreshZ = dialogView.findViewById(R.id.button_refresh_z);
        ImageView closeButton = dialogView.findViewById(R.id.button_close);
        
        // Set up adapters for each provider
        setupProviderModels(googleModels, AIAssistant.AIProvider.GOOGLE);
        setupProviderModels(huggingfaceModels, AIAssistant.AIProvider.HUGGINGFACE);
        setupProviderModels(alibabaModels, AIAssistant.AIProvider.ALIBABA);
        setupProviderModels(zModels, AIAssistant.AIProvider.Z);
        
        // Set up refresh listeners
        refreshAlibaba.setOnClickListener(v -> {
            aiAssistant.refreshModelsForProvider(AIAssistant.AIProvider.ALIBABA, new AIAssistant.RefreshCallback() {
                @Override
                public void onRefreshComplete(boolean success, String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            setupProviderModels(alibabaModels, AIAssistant.AIProvider.ALIBABA);
                        }
                    });
                }
            });
        });
        
        refreshZ.setOnClickListener(v -> {
            aiAssistant.refreshModelsForProvider(AIAssistant.AIProvider.Z, new AIAssistant.RefreshCallback() {
                @Override
                public void onRefreshComplete(boolean success, String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            setupProviderModels(zModels, AIAssistant.AIProvider.Z);
                        }
                    });
                }
            });
        });
        
        closeButton.setOnClickListener(v -> modelPickerDialog.dismiss());
        
        modelPickerDialog.show();
    }
    
    /**
     * Sets up models for a specific provider in the RecyclerView
     */
    private void setupProviderModels(RecyclerView recyclerView, AIAssistant.AIProvider provider) {
        Map<AIAssistant.AIProvider, List<AIAssistant.AIModel>> modelsByProvider = AIAssistant.AIModel.getModelsByProvider();
        List<AIAssistant.AIModel> providerModels = modelsByProvider.get(provider);
        
        if (providerModels != null && !providerModels.isEmpty()) {
            ModelPickerAdapter adapter = new ModelPickerAdapter(providerModels, aiAssistant.getCurrentModel(), 
                model -> {
                    aiAssistant.setCurrentModel(model);
                    textSelectedModel.setText(model.getDisplayName());
                    modelPickerDialog.dismiss();
                    
                    // Update settings dialog state based on new model capabilities
                    updateSettingsButtonState();
                });
            
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.GONE);
        }
    }
    
    /**
     * Shows the AI settings dialog with thinking mode and web search toggles
     */
    private void showAiSettingsDialog() {
        if (aiAssistant == null) return;
        
        aiSettingsDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_ai_settings, null);
        aiSettingsDialog.setContentView(dialogView);
        
        MaterialSwitch switchThinking = dialogView.findViewById(R.id.switch_thinking_mode);
        MaterialSwitch switchWebSearch = dialogView.findViewById(R.id.switch_web_search);
        
        // Get current model capabilities
        AIAssistant.ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
        
        // Set up thinking mode toggle
        switchThinking.setChecked(aiAssistant.isThinkingModeEnabled());
        switchThinking.setEnabled(capabilities.supportsThinking);
        switchThinking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            aiAssistant.setThinkingModeEnabled(isChecked);
        });
        
        // Set up web search toggle
        switchWebSearch.setChecked(aiAssistant.isWebSearchEnabled());
        switchWebSearch.setEnabled(capabilities.supportsWebSearch);
        switchWebSearch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            aiAssistant.setWebSearchEnabled(isChecked);
        });
        
        aiSettingsDialog.show();
    }
    
    /**
     * Shows the web sources dialog with clickable links
     */
    private void showWebSourcesDialog(java.util.List<AIAssistant.WebSource> webSources) {
        webSourcesDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_web_sources, null);
        webSourcesDialog.setContentView(dialogView);
        
        RecyclerView recyclerWebSources = dialogView.findViewById(R.id.recycler_web_sources);
        WebSourcesAdapter adapter = new WebSourcesAdapter(webSources);
        recyclerWebSources.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerWebSources.setAdapter(adapter);
        
        webSourcesDialog.show();
    }
    
    /**
     * Updates the settings button state based on current model capabilities
     */
    private void updateSettingsButtonState() {
        if (buttonAiSettings == null || aiAssistant == null) return;
        
        AIAssistant.ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
        boolean hasSettings = capabilities.supportsThinking || capabilities.supportsWebSearch;
        
        buttonAiSettings.setEnabled(hasSettings);
        buttonAiSettings.setAlpha(hasSettings ? 1.0f : 0.5f);
    }

    // Add fallback error UI for chat loading
    public void showChatLoadError(String error) {
        if (layoutEmptyState != null && textGreeting != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            textGreeting.setText(error != null ? error : "Error loading chat interface");
        }
        if (recyclerViewChatHistory != null) {
            recyclerViewChatHistory.setVisibility(View.GONE);
        }
        if (layoutInputSection != null) {
            layoutInputSection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Clear the listener to prevent memory leaks
    }

    // --- ChatMessageAdapter.OnAiActionInteractionListener implementations ---

    @Override
    public void onAcceptClicked(int messagePosition, ChatMessage message) {
        if (listener != null) {
            listener.onAiAcceptActions(messagePosition, message);
        }
    }

    @Override
    public void onDiscardClicked(int messagePosition, ChatMessage message) {
        if (listener != null) {
            listener.onAiDiscardActions(messagePosition, message);
        }
    }

    @Override
    public void onReapplyClicked(int messagePosition, ChatMessage message) {
        if (listener != null) {
            listener.onReapplyActions(messagePosition, message);
        }
    }

    @Override
    public void onFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail) {
        if (listener != null) {
            listener.onAiFileChangeClicked(fileActionDetail);
        }
    }
}
