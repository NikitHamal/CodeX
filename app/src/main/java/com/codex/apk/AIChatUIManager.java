package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.android.material.button.MaterialButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.List;
import java.util.Map;
import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;

public class AIChatUIManager {

    private final Context context;
    private final View rootView;
    private final AIChatFragment fragment;

    public RecyclerView recyclerViewChatHistory;
    public EditText editTextAiPrompt;
    public MaterialButton buttonAiSend;
    public RecyclerView recyclerAttachedFilesPreview;
    public LinearLayout layoutEmptyState;
    public TextView textGreeting;
    public LinearLayout layoutInputSection;
    public LinearLayout layoutModelSelectorCustom;
    public TextView textSelectedModel;
    public LinearLayout linearPromptInput;
    public ImageView buttonAiSettings;
    public ImageView buttonAiAttach;

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
        buttonAiSend = rootView.findViewById(R.id.button_ai_send);
        layoutEmptyState = rootView.findViewById(R.id.layout_empty_state);
        textGreeting = rootView.findViewById(R.id.text_greeting);
        layoutInputSection = rootView.findViewById(R.id.layout_input_section);
        layoutModelSelectorCustom = rootView.findViewById(R.id.layout_model_selector_custom);
        textSelectedModel = rootView.findViewById(R.id.text_selected_model);
        linearPromptInput = rootView.findViewById(R.id.linear_prompt_input);
        buttonAiSettings = rootView.findViewById(R.id.button_ai_settings);
        buttonAiAttach = rootView.findViewById(R.id.button_ai_attach);
        recyclerAttachedFilesPreview = rootView.findViewById(R.id.recycler_attached_files_preview);
        if (recyclerAttachedFilesPreview != null) {
            recyclerAttachedFilesPreview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        }

        // Long press on model selector to choose a custom agent
        layoutModelSelectorCustom.setOnLongClickListener(v -> {
            showAgentPickerDialog(fragment.getAIAssistant());
            return true;
        });
    }

    public void setupRecyclerView(ChatMessageAdapter adapter) {
        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setStackFromEnd(true);
        recyclerViewChatHistory.setLayoutManager(llm);
        recyclerViewChatHistory.setItemAnimator(null); // Prevent overlapping glitches during rapid streaming updates
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

        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
        List<String> modelNamesList = new java.util.ArrayList<>();
        for (AIModel model : AIModel.getAllModels()) {
            String key = "model_" + model.getDisplayName() + "_enabled";
            if (prefs.getBoolean(key, true)) {
                modelNamesList.add(model.getDisplayName());
            }
        }
        String[] modelNames = modelNamesList.toArray(new String[0]);
        String currentModel = aiAssistant.getCurrentModel().getDisplayName();
        int selectedIndex = -1;

        for (int i = 0; i < modelNames.length; i++) {
            if (modelNames[i].equals(currentModel)) {
                selectedIndex = i;
                break;
            }
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Select AI Model")
                .setSingleChoiceItems(modelNames, selectedIndex, (dialog, which) -> {
                    String selectedModelName = modelNames[which];
                    AIModel selectedModel = AIModel.fromDisplayName(selectedModelName);
                    if (selectedModel != null) {
                        aiAssistant.setCurrentModel(selectedModel);
                        textSelectedModel.setText(selectedModelName);
                        updateSettingsButtonState(aiAssistant);
                        // Persist last used model per project
                        android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                        sp.edit().putString("selected_model", selectedModelName).apply();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAgentPickerDialog(AIAssistant aiAssistant) {
        List<CustomAgent> agents = SettingsActivity.getCustomAgents(context);
        if (agents.isEmpty()) {
            Toast.makeText(context, "No custom agents configured in Settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dlg = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_ai_settings, null);
        dlg.setContentView(view);
        // Reuse simple list: show names as options
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                TextView tv = new TextView(context);
                tv.setPadding(32, 32, 32, 32);
                tv.setTextColor(context.getColor(R.color.on_surface));
                return new RecyclerView.ViewHolder(tv) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView tv = (TextView) holder.itemView;
                CustomAgent a = agents.get(position);
                tv.setText(a.name + " (" + a.modelId + ")");
                tv.setOnClickListener(v -> {
                    AIModel model = AIModel.fromModelId(a.modelId);
                    if (model != null) {
                        aiAssistant.setCurrentModel(model);
                        textSelectedModel.setText(model.getDisplayName());
                        // Persist last used model per project
                        android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                        sp.edit().putString("selected_model", model.getDisplayName()).apply();
                        if (a.prompt != null && !a.prompt.isEmpty()) {
                            // Prepend custom agent prompt to current input for next send
                            String existing = editTextAiPrompt.getText().toString();
                            editTextAiPrompt.setText(a.prompt + "\n\n" + existing);
                            editTextAiPrompt.setSelection(editTextAiPrompt.getText().length());
                        }
                        dlg.dismiss();
                    } else {
                        Toast.makeText(context, "Model not found: " + a.modelId, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public int getItemCount() { return agents.size(); }
        });
        dlg.setContentView(rv);
        dlg.show();
    }


    public void showAiSettingsDialog(AIAssistant aiAssistant) {
        if (aiAssistant == null) return;

        aiSettingsDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_ai_settings, null);
        aiSettingsDialog.setContentView(dialogView);

        MaterialSwitch switchThinking = dialogView.findViewById(R.id.switch_thinking_mode);
        MaterialSwitch switchWebSearch = dialogView.findViewById(R.id.switch_web_search);
        MaterialSwitch switchAgent = dialogView.findViewById(R.id.switch_agent_mode);

        ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();

        switchThinking.setChecked(aiAssistant.isThinkingModeEnabled());
        switchThinking.setEnabled(capabilities.supportsThinking);
        switchThinking.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setThinkingModeEnabled(isChecked));

        switchWebSearch.setChecked(aiAssistant.isWebSearchEnabled());
        switchWebSearch.setEnabled(capabilities.supportsWebSearch);
        switchWebSearch.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setWebSearchEnabled(isChecked));

        // Agent mode has no provider capability constraint
        switchAgent.setChecked(aiAssistant.isAgentModeEnabled());
        switchAgent.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setAgentModeEnabled(isChecked));

        aiSettingsDialog.show();
    }

    public void updateSettingsButtonState(AIAssistant aiAssistant) {
        if (buttonAiSettings == null || aiAssistant == null) return;

        ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
        boolean hasSettings = capabilities.supportsThinking || capabilities.supportsWebSearch;

        buttonAiSettings.setEnabled(hasSettings);

        // Show attach button only for FREE (Gemini reverse-engineered) models
        if (buttonAiAttach != null) {
            boolean showAttach = aiAssistant.getCurrentModel() != null && aiAssistant.getCurrentModel().getProvider() == AIProvider.FREE;
            buttonAiAttach.setVisibility(showAttach ? View.VISIBLE : View.GONE);
        }
    }

    public void setListeners() {
        buttonAiSend.setOnClickListener(v -> fragment.sendPrompt());
        layoutModelSelectorCustom.setOnClickListener(v -> showModelPickerDialog(fragment.getAIAssistant()));
        buttonAiSettings.setOnClickListener(v -> showAiSettingsDialog(fragment.getAIAssistant()));
        if (buttonAiAttach != null) {
            buttonAiAttach.setOnClickListener(v -> {
                if (fragment.getAIAssistant() != null && fragment.getAIAssistant().getCurrentModel() != null
                    && fragment.getAIAssistant().getCurrentModel().getProvider() == AIProvider.FREE) {
                    fragment.onAttachButtonClicked();
                } else {
                    Toast.makeText(context, "Attachments available only for Gemini Free models", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            recyclerViewChatHistory.post(() -> {
                int last = recyclerViewChatHistory.getAdapter().getItemCount() - 1;
                recyclerViewChatHistory.smoothScrollToPosition(last);
            });
        }
    }

    public void setText(String text) {
        editTextAiPrompt.setText(text);
    }

    public String getText() {
        return editTextAiPrompt.getText().toString();
    }

    public void setSendButtonEnabled(boolean isEnabled) {
        buttonAiSend.setEnabled(isEnabled);
        buttonAiSend.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    public void showAttachedFilesPreview(List<java.io.File> files) {
        if (recyclerAttachedFilesPreview == null) return;
        if (files == null || files.isEmpty()) {
            recyclerAttachedFilesPreview.setVisibility(View.GONE);
            recyclerAttachedFilesPreview.setAdapter(null);
            return;
        }
        recyclerAttachedFilesPreview.setVisibility(View.VISIBLE);
        recyclerAttachedFilesPreview.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                LinearLayout container = new LinearLayout(context);
                container.setOrientation(LinearLayout.HORIZONTAL);
                ImageView iv = new ImageView(context);
                int size = (int) (32 * context.getResources().getDisplayMetrics().density / context.getResources().getDisplayMetrics().density); // dp-to-dp placeholder
                iv.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
                iv.setImageResource(R.drawable.icon_file_round);
                container.addView(iv);
                TextView tv = new TextView(context);
                tv.setTextColor(context.getColor(R.color.on_surface_variant));
                tv.setTextSize(12);
                container.addView(tv);
                return new RecyclerView.ViewHolder(container) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                LinearLayout container = (LinearLayout) holder.itemView;
                ImageView iv = (ImageView) container.getChildAt(0);
                TextView tv = (TextView) container.getChildAt(1);
                java.io.File f = files.get(position);
                tv.setText(f.getName());
                holder.itemView.setPadding(8,8,8,8);
            }
            @Override public int getItemCount() { return files.size(); }
        });
    }
}
