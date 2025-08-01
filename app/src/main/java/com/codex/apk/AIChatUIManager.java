package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;
import java.util.Map;

public class AIChatUIManager {

    private final Context context;
    private final View rootView;
    private final AIChatFragment fragment;

    public RecyclerView recyclerViewChatHistory;
    public EditText editTextAiPrompt;
    public ImageView buttonAiSend;
    public LinearLayout layoutEmptyState;
    public TextView textGreeting;
    public LinearLayout layoutInputSection;
    public LinearLayout layoutModelSelectorCustom;
    public TextView textSelectedModel;
    public LinearLayout linearPromptInput;
    public ImageView buttonAiSettings;

    private BottomSheetDialog modelPickerDialog;
    private BottomSheetDialog aiSettingsDialog;
    private BottomSheetDialog webSourcesDialog;

    public AIChatUIManager(AIChatFragment fragment, View rootView) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        this.rootView = rootView;
        initializeViews();
    }

    private void initializeViews() {
        recyclerViewChatHistory = rootView.findViewById(R.id.recycler_view_chat_history);
        editTextAiPrompt = rootView.findViewById(R.id.edit_text_ai_prompt);
        // The send button is a MaterialButton in the layout, but we can use ImageView for basic clicks
        buttonAiSend = rootView.findViewById(R.id.button_ai_send);
        layoutEmptyState = rootView.findViewById(R.id.layout_empty_state);
        textGreeting = rootView.findViewById(R.id.text_greeting);
        layoutInputSection = rootView.findViewById(R.id.layout_input_section);
        layoutModelSelectorCustom = rootView.findViewById(R.id.layout_model_selector_custom);
        textSelectedModel = rootView.findViewById(R.id.text_selected_model);
        linearPromptInput = rootView.findViewById(R.id.linear_prompt_input);
        buttonAiSettings = rootView.findViewById(R.id.button_ai_settings);
    }

    public void setupRecyclerView(ChatMessageAdapter adapter) {
        recyclerViewChatHistory.setLayoutManager(new LinearLayoutManager(context));
        recyclerViewChatHistory.setAdapter(adapter);
    }

    public void updateUiVisibility(boolean isChatEmpty) {
        if (isChatEmpty) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewChatHistory.setVisibility(View.GONE);
            editTextAiPrompt.setHint(R.string.how_can_codex_help_you_today);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewChatHistory.setVisibility(View.VISIBLE);
            editTextAiPrompt.setHint(R.string.reply_to_codex);
        }
        layoutInputSection.setVisibility(View.VISIBLE);
        layoutModelSelectorCustom.setVisibility(View.VISIBLE);
        linearPromptInput.setVisibility(View.VISIBLE);
    }

    public void showChatLoadError(String error) {
        layoutEmptyState.setVisibility(View.VISIBLE);
        textGreeting.setText(error != null ? error : context.getString(R.string.error_loading_chat_interface));
        recyclerViewChatHistory.setVisibility(View.GONE);
        layoutInputSection.setVisibility(View.GONE);
    }

    public void showModelPickerDialog(AIAssistant aiAssistant) {
        if (aiAssistant == null) return;

        modelPickerDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_model_picker, null);
        modelPickerDialog.setContentView(dialogView);

        RecyclerView googleModels = dialogView.findViewById(R.id.recycler_google_models);
        RecyclerView huggingfaceModels = dialogView.findViewById(R.id.recycler_huggingface_models);
        RecyclerView alibabaModels = dialogView.findViewById(R.id.recycler_alibaba_models);
        RecyclerView zModels = dialogView.findViewById(R.id.recycler_z_models);

        ImageView refreshAlibaba = dialogView.findViewById(R.id.button_refresh_alibaba);
        ImageView refreshZ = dialogView.findViewById(R.id.button_refresh_z);
        ImageView closeButton = dialogView.findViewById(R.id.button_close);

        setupProviderModels(googleModels, AIAssistant.AIProvider.GOOGLE, aiAssistant);
        setupProviderModels(huggingfaceModels, AIAssistant.AIProvider.HUGGINGFACE, aiAssistant);
        setupProviderModels(alibabaModels, AIAssistant.AIProvider.ALIBABA, aiAssistant);
        setupProviderModels(zModels, AIAssistant.AIProvider.Z, aiAssistant);

        refreshAlibaba.setOnClickListener(v -> {
            aiAssistant.refreshModelsForProvider(AIAssistant.AIProvider.ALIBABA, new AIAssistant.RefreshCallback() {
                @Override
                public void onRefreshComplete(boolean success, String message) {
                    fragment.requireActivity().runOnUiThread(() -> {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            setupProviderModels(alibabaModels, AIAssistant.AIProvider.ALIBABA, aiAssistant);
                        }
                    });
                }
            });
        });

        refreshZ.setOnClickListener(v -> {
            aiAssistant.refreshModelsForProvider(AIAssistant.AIProvider.Z, new AIAssistant.RefreshCallback() {
                @Override
                public void onRefreshComplete(boolean success, String message) {
                    fragment.requireActivity().runOnUiThread(() -> {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        if (success) {
                            setupProviderModels(zModels, AIAssistant.AIProvider.Z, aiAssistant);
                        }
                    });
                }
            });
        });

        closeButton.setOnClickListener(v -> modelPickerDialog.dismiss());

        modelPickerDialog.show();
    }

    private void setupProviderModels(RecyclerView recyclerView, AIAssistant.AIProvider provider, AIAssistant aiAssistant) {
        Map<AIAssistant.AIProvider, List<AIAssistant.AIModel>> modelsByProvider = AIAssistant.AIModel.getModelsByProvider();
        List<AIAssistant.AIModel> providerModels = modelsByProvider.get(provider);

        if (providerModels != null && !providerModels.isEmpty()) {
            ModelPickerAdapter adapter = new ModelPickerAdapter(providerModels, aiAssistant.getCurrentModel(),
                model -> {
                    aiAssistant.setCurrentModel(model);
                    textSelectedModel.setText(model.getDisplayName());
                    modelPickerDialog.dismiss();
                    updateSettingsButtonState(aiAssistant);
                });

            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.GONE);
        }
    }

    public void showAiSettingsDialog(AIAssistant aiAssistant) {
        if (aiAssistant == null) return;

        aiSettingsDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_ai_settings, null);
        aiSettingsDialog.setContentView(dialogView);

        MaterialSwitch switchThinking = dialogView.findViewById(R.id.switch_thinking_mode);
        MaterialSwitch switchWebSearch = dialogView.findViewById(R.id.switch_web_search);

        AIAssistant.ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();

        switchThinking.setChecked(aiAssistant.isThinkingModeEnabled());
        switchThinking.setEnabled(capabilities.supportsThinking);
        switchThinking.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setThinkingModeEnabled(isChecked));

        switchWebSearch.setChecked(aiAssistant.isWebSearchEnabled());
        switchWebSearch.setEnabled(capabilities.supportsWebSearch);
        switchWebSearch.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setWebSearchEnabled(isChecked));

        aiSettingsDialog.show();
    }

    public void updateSettingsButtonState(AIAssistant aiAssistant) {
        if (buttonAiSettings == null || aiAssistant == null) return;

        AIAssistant.ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
        boolean hasSettings = capabilities.supportsThinking || capabilities.supportsWebSearch;

        buttonAiSettings.setEnabled(hasSettings);
        buttonAiSettings.setAlpha(hasSettings ? 1.0f : 0.5f);
    }

    public void setListeners() {
        buttonAiSend.setOnClickListener(v -> fragment.sendPrompt());
        layoutModelSelectorCustom.setOnClickListener(v -> showModelPickerDialog(fragment.getAIAssistant()));
        buttonAiSettings.setOnClickListener(v -> showAiSettingsDialog(fragment.getAIAssistant()));
        editTextAiPrompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                fragment.sendPrompt();
                return true;
            }
            return false;
        });
    }

    public void scrollToBottom() {
        if (recyclerViewChatHistory.getAdapter() != null && recyclerViewChatHistory.getAdapter().getItemCount() > 0) {
            recyclerViewChatHistory.scrollToPosition(recyclerViewChatHistory.getAdapter().getItemCount() - 1);
        }
    }

    public void setText(String text) {
        editTextAiPrompt.setText(text);
    }

    public String getText() {
        return editTextAiPrompt.getText().toString();
    }
}
