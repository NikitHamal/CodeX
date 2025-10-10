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

        // Long press on model selector is now removed.
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
        List<AIModel> enabledModels = new java.util.ArrayList<>();
        for (AIModel model : AIModel.getAllModels()) {
            String key = "model_" + model.getProvider().name() + "_" + model.getModelId() + "_enabled";
            if (prefs.getBoolean(key, true)) {
                enabledModels.add(model);
            }
        }

        if (enabledModels.isEmpty()) {
            Toast.makeText(context, "No enabled models. Please enable models in Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] modelNames = new String[enabledModels.size()];
        for (int i = 0; i < enabledModels.size(); i++) {
            modelNames[i] = enabledModels.get(i).getDisplayName();
        }

        String currentModelName = (aiAssistant.getCurrentModel() != null) ? aiAssistant.getCurrentModel().getDisplayName() : "";
        int selectedIndex = -1;
        for (int i = 0; i < modelNames.length; i++) {
            if (modelNames[i].equals(currentModelName)) {
                selectedIndex = i;
                break;
            }
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Select AI Model")
                .setSingleChoiceItems(modelNames, selectedIndex, (dialog, which) -> {
                    AIModel selectedModel = enabledModels.get(which);
                    aiAssistant.setCurrentModel(selectedModel);
                    textSelectedModel.setText(selectedModel.getDisplayName());
                    // Ensure selector width recalculates after text change
                    if (layoutModelSelectorCustom != null) {
                        layoutModelSelectorCustom.requestLayout();
                    }
                    if (textSelectedModel != null) {
                        textSelectedModel.post(() -> {
                            if (layoutModelSelectorCustom != null) layoutModelSelectorCustom.requestLayout();
                        });
                    }
                    updateSettingsButtonState(aiAssistant);
                    // Persist last used model per project
                    android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
                    sp.edit().putString("selected_model", selectedModel.getDisplayName()).apply();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    public void showAiSettingsDialog(AIAssistant aiAssistant) {
        if (aiAssistant == null) return;

        aiSettingsDialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_ai_settings, null);
        aiSettingsDialog.setContentView(dialogView);

        MaterialSwitch switchThinking = dialogView.findViewById(R.id.switch_thinking_mode);
        MaterialSwitch switchWebSearch = dialogView.findViewById(R.id.switch_web_search);
        MaterialSwitch switchAgent = dialogView.findViewById(R.id.switch_agent_mode);
        View rowThinking = dialogView.findViewById(R.id.row_thinking_mode);

        ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
        boolean supportsThinking = capabilities.supportsThinking;

        // Hide entire Thinking row if model doesn't support thinking
        if (rowThinking != null) {
            rowThinking.setVisibility(supportsThinking ? View.VISIBLE : View.GONE);
        }

        switchThinking.setChecked(aiAssistant.isThinkingModeEnabled());
        switchThinking.setEnabled(supportsThinking);
        switchThinking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            aiAssistant.setThinkingModeEnabled(isChecked);
        });

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

        // The settings button should always be enabled because "Agent Mode" is always available.
        // The individual settings inside the dialog are enabled/disabled based on capabilities.
        buttonAiSettings.setEnabled(true);

        // Show attach button only for COOKIES (Gemini reverse-engineered) models
        if (buttonAiAttach != null) {
            boolean showAttach = aiAssistant.getCurrentModel() != null && aiAssistant.getCurrentModel().getProvider() == AIProvider.COOKIES;
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
                    && fragment.getAIAssistant().getCurrentModel().getProvider() == AIProvider.COOKIES) {
                    fragment.onAttachButtonClicked();
                } else {
                    Toast.makeText(context, "Attachments available only for Gemini (Cookies) models", Toast.LENGTH_SHORT).show();
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
                // Vertical item: thumbnail/icon in a rounded card with a clear button overlay, filename below
                LinearLayout root = new LinearLayout(context);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(4), dp(4), dp(4), dp(4));

                android.widget.FrameLayout frame = new android.widget.FrameLayout(context);
                LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(dp(64), dp(64));
                frame.setLayoutParams(frameLp);

                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(context);
                android.widget.FrameLayout.LayoutParams cardLp = new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                card.setLayoutParams(cardLp);
                card.setRadius(dp(8));
                card.setCardElevation(0f);
                card.setStrokeWidth(0);

                ImageView thumb = new ImageView(context);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                card.addView(thumb);

                ImageView clear = new ImageView(context);
                android.widget.FrameLayout.LayoutParams clearLp = new android.widget.FrameLayout.LayoutParams(dp(18), dp(18));
                clearLp.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
                clear.setLayoutParams(clearLp);
                clear.setImageResource(R.drawable.icon_close_round);
                clear.setColorFilter(context.getColor(R.color.on_surface));
                clear.setBackgroundResource(R.drawable.rounded_edittext_background);
                clear.setPadding(dp(2), dp(2), dp(2), dp(2));

                frame.addView(card);
                frame.addView(clear);

                TextView name = new TextView(context);
                name.setTextColor(context.getColor(R.color.on_surface_variant));
                name.setTextSize(12);
                name.setMaxLines(1);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(dp(72), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                nameLp.topMargin = dp(4);
                name.setLayoutParams(nameLp);

                root.addView(frame);
                root.addView(name);

                return new RecyclerView.ViewHolder(root) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                LinearLayout root = (LinearLayout) holder.itemView;
                android.widget.FrameLayout frame = (android.widget.FrameLayout) root.getChildAt(0);
                com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) frame.getChildAt(0);
                ImageView thumb = (ImageView) card.getChildAt(0);
                ImageView clear = (ImageView) frame.getChildAt(1);
                TextView name = (TextView) root.getChildAt(1);

                java.io.File f = files.get(position);
                name.setText(f.getName());

                if (isImageFile(f)) {
                    android.graphics.Bitmap bmp = decodeSampledBitmapFromFile(f.getAbsolutePath(), dp(64), dp(64));
                    if (bmp != null) thumb.setImageBitmap(bmp); else thumb.setImageResource(R.drawable.icon_file_round);
                } else {
                    thumb.setScaleType(ImageView.ScaleType.CENTER);
                    thumb.setImageResource(R.drawable.icon_file_round);
                }

                clear.setOnClickListener(v -> {
                    // Ask fragment to remove
                    fragment.removePendingAttachmentAt(position);
                });

                // Tap to preview using an implicit intent
                holder.itemView.setOnClickListener(v -> {
                    try {
                        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", f);
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                        intent.setData(uri);
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);
                    } catch (Exception ignored) {}
                });
            }
            @Override public int getItemCount() { return files.size(); }

            private int dp(int v) { return (int) (v * context.getResources().getDisplayMetrics().density); }
            private boolean isImageFile(java.io.File f) {
                String n = f.getName().toLowerCase();
                return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".heic") || n.endsWith(".heif");
            }
            private android.graphics.Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(path, options);
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                try { return android.graphics.BitmapFactory.decodeFile(path, options); } catch (Exception e) { return null; }
            }
            private int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
                int height = options.outHeight;
                int width = options.outWidth;
                int inSampleSize = 1;
                if (height > reqHeight || width > reqWidth) {
                    final int halfHeight = height / 2;
                    final int halfWidth = width / 2;
                    while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                        inSampleSize *= 2;
                    }
                }
                return inSampleSize;
            }
        });
    }
}
