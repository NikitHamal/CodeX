# CodeX App - All Comments Documentation

This document contains all comments (starting with `//`) extracted from the Java files in the CodeX application, organized by file location with line numbers and context.

## Summary
- **Total Java Files Analyzed:** 126
- **Total Comments Found:** 809
- **Files with Comments:** 79

## Files Organization

---

## com/codex/apk/AIAssistant.java

**Location:** `app/src/main/java/com/codex/apk/AIAssistant.java`

### Line 31
**Comment:** Legacy constructor for compatibility

**Context:**
```java
      27|        this.currentModel = AIModel.fromModelId("qwen3-coder-plus");
      28|        initializeApiClients(context, null);
      29|    }
>>>   30|
      31|    // Legacy constructor for compatibility
      32|    public AIAssistant(Context context, String apiKey, File projectDir, String projectName,
      33|        ExecutorService executorService, AIActionListener actionListener) {
      34|        this.actionListener = actionListener;
```

### Line 58
**Comment:** For now, attachments are not handled in this refactored version.

**Context:**
```java
      54|        apiClients.put(AIProvider.OPENROUTER, new OpenRouterApiClient(context, actionListener));
      55|    }
      56|
>>>   57|    public void sendPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState, String fileName, String fileContent) {
      58|        // For now, attachments are not handled in this refactored version.
      59|        // This would need to be threaded through if a model that uses them is selected.
      60|        sendMessage(userPrompt, chatHistory, qwenState, new ArrayList<>(), fileName, fileContent);
      61|    }
```

### Line 59
**Comment:** This would need to be threaded through if a model that uses them is selected.

**Context:**
```java
      55|    }
      56|
      57|    public void sendPrompt(String userPrompt, List<ChatMessage> chatHistory, QwenConversationState qwenState, String fileName, String fileContent) {
>>>   58|        // For now, attachments are not handled in this refactored version.
      59|        // This would need to be threaded through if a model that uses them is selected.
      60|        sendMessage(userPrompt, chatHistory, qwenState, new ArrayList<>(), fileName, fileContent);
      61|    }
      62|
```

### Line 76
**Comment:** Choose system prompt based on agent mode

**Context:**
```java
      72|            if (fileContent != null && !fileContent.isEmpty()) {
      73|                finalMessage = "Here is the content of the file `" + fileName + "`:\n\n```\n" + fileContent + "\n```\n\nNow, please perform the following task: " + message;
      74|            }
>>>   75|
      76|            // Choose system prompt based on agent mode
      77|            String system = null;
      78|            if (agentModeEnabled && enabledTools != null && !enabledTools.isEmpty()) {
      79|                system = PromptManager.getDefaultFileOpsPrompt();
```

### Line 86
**Comment:** Note: Gemini Free context is maintained via server-side conversation metadata (cid,rid,rcid)

**Context:**
```java
      82|            }
      83|            if (system != null && !system.isEmpty()) {
      84|                finalMessage = system + "\n\n" + finalMessage;
>>>   85|            }
      86|            // Note: Gemini Free context is maintained via server-side conversation metadata (cid,rid,rcid)
      87|            List<File> safeAttachments = attachments;
      88|            if (currentModel != null && currentModel.getProvider() != AIProvider.COOKIES) {
      89|                safeAttachments = new ArrayList<>();
```

### Line 130
**Comment:** Getters and Setters

**Context:**
```java
     126|        void onAiRequestCompleted();
     127|        void onQwenConversationStateUpdated(QwenConversationState state);
     128|    }
>>>  129|
     130|    // Getters and Setters
     131|    public AIModel getCurrentModel() { return currentModel; }
     132|    public void setCurrentModel(AIModel model) { this.currentModel = model; }
     133|    public boolean isThinkingModeEnabled() { return thinkingModeEnabled; }
```

---

## com/codex/apk/AIChatFragment.java

**Location:** `app/src/main/java/com/codex/apk/AIChatFragment.java`

### Line 44
**Comment:** Hook used by UI manager to trigger attachment selection

**Context:**
```java
      40|        void onPlanAcceptClicked(int messagePosition, ChatMessage message);
      41|        void onPlanDiscardClicked(int messagePosition, ChatMessage message);
      42|    }
>>>   43|
      44|    // Hook used by UI manager to trigger attachment selection
      45|    public void onAttachButtonClicked() {
      46|        if (pickFilesLauncher != null) {
      47|            pickFilesLauncher.launch(new String[]{"image/*", "application/pdf", "text/*", "application/octet-stream", "application/zip"});
```

### Line 51
**Comment:** Called by UI to remove an attachment from the pending list

**Context:**
```java
      47|            pickFilesLauncher.launch(new String[]{"image/*", "application/pdf", "text/*", "application/octet-stream", "application/zip"});
      48|        }
      49|    }
>>>   50|
      51|    // Called by UI to remove an attachment from the pending list
      52|    public void removePendingAttachmentAt(int index) {
      53|        if (index >= 0 && index < pendingAttachments.size()) {
      54|            pendingAttachments.remove(index);
```

### Line 77
**Comment:** Prepare file picker

**Context:**
```java
      73|            listener = (AIChatFragmentListener) context;
      74|        } else {
      75|            throw new RuntimeException(context.toString() + " must implement AIChatFragmentListener");
>>>   76|        }
      77|        // Prepare file picker
      78|        pickFilesLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(), uris -> {
      79|            if (uris == null || uris.isEmpty()) return;
      80|            android.content.ContentResolver cr = requireContext().getContentResolver();
```

### Line 129
**Comment:** Update attach icon visibility/state based on current model

**Context:**
```java
     125|        chatMessageAdapter = new ChatMessageAdapter(requireContext(), chatHistory);
     126|        chatMessageAdapter.setOnAiActionInteractionListener(this);
     127|        uiManager.setupRecyclerView(chatMessageAdapter);
>>>  128|
     129|        // Update attach icon visibility/state based on current model
     130|        if (listener != null && listener.getAIAssistant() != null) {
     131|            uiManager.updateSettingsButtonState(listener.getAIAssistant());
     132|        }
```

### Line 148
**Comment:** Verify that the current model is still enabled

**Context:**
```java
     144|
     145|        if (aiAssistant != null) {
     146|            com.codex.apk.ai.AIModel currentModel = aiAssistant.getCurrentModel();
>>>  147|            if (currentModel != null) {
     148|                // Verify that the current model is still enabled
     149|                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("model_settings", Context.MODE_PRIVATE);
     150|                String key = "model_" + currentModel.getProvider().name() + "_" + currentModel.getModelId() + "_enabled";
     151|                boolean isEnabled = prefs.getBoolean(key, true);
```

### Line 156
**Comment:** The previously selected model is now disabled.

**Context:**
```java
     152|
     153|                if (isEnabled) {
     154|                    uiManager.textSelectedModel.setText(currentModel.getDisplayName());
>>>  155|                } else {
     156|                    // The previously selected model is now disabled.
     157|                    aiAssistant.setCurrentModel(null);
     158|                    uiManager.textSelectedModel.setText("Select a model");
     159|                }
```

### Line 166
**Comment:** Scroll to last message when chat opens

**Context:**
```java
     162|            }
     163|            uiManager.updateSettingsButtonState(aiAssistant);
     164|        }
>>>  165|
     166|        // Scroll to last message when chat opens
     167|        uiManager.scrollToBottom();
     168|    }
     169|
```

### Line 207
**Comment:** Directly call assistant with attachments only for COOKIES provider

**Context:**
```java
     203|
     204|
     205|        uiManager.setText("");
>>>  206|        if (listener != null) {
     207|            // Directly call assistant with attachments only for COOKIES provider
     208|            if (aiAssistant != null && aiAssistant.getCurrentModel() != null
     209|                && aiAssistant.getCurrentModel().getProvider() == com.codex.apk.ai.AIProvider.COOKIES) {
     210|                aiAssistant.sendMessage(prompt, new ArrayList<>(chatHistory), qwenConversationState, new java.util.ArrayList<>(pendingAttachments));
```

### Line 263
**Comment:** Persist chat history so it restores when reopening the project

**Context:**
```java
     259|            chatMessageAdapter.notifyItemInserted(indexChangedOrAdded);
     260|            uiManager.scrollToBottom();
     261|        }
>>>  262|        uiManager.updateUiVisibility(chatHistory.isEmpty());
     263|        // Persist chat history so it restores when reopening the project
     264|        if (historyManager != null) {
     265|            historyManager.saveChatState(chatHistory, qwenConversationState);
     266|        }
```

### Line 280
**Comment:** Always clear the processing state even if the current status message reference is lost

**Context:**
```java
     276|        }
     277|    }
     278|
>>>  279|    public void hideThinkingMessage() {
     280|        // Always clear the processing state even if the current status message reference is lost
     281|        if (isAiProcessing && currentAiStatusMessage != null) {
     282|            int index = chatHistory.indexOf(currentAiStatusMessage);
     283|            if (index != -1) {
```

---

## com/codex/apk/AIChatHistoryManager.java

**Location:** `app/src/main/java/com/codex/apk/AIChatHistoryManager.java`

### Line 37
**Comment:** Load Chat History

**Context:**
```java
      33|
      34|    public void loadChatState(List<ChatMessage> chatHistory, QwenConversationState qwenState) {
      35|        Type historyType = new TypeToken<List<ChatMessage>>() {}.getType();
>>>   36|
      37|        // Load Chat History
      38|        String historyKey = getProjectSpecificKey(CHAT_HISTORY_KEY_PREFIX);
      39|        String historyJson = prefs.getString(historyKey, null);
      40|        if (historyJson != null) {
```

### Line 47
**Comment:** Migration from old generic key

**Context:**
```java
      43|                chatHistory.clear();
      44|                chatHistory.addAll(loadedHistory);
      45|            }
>>>   46|        } else {
      47|            // Migration from old generic key
      48|            String oldGenericJson = prefs.getString(OLD_GENERIC_CHAT_HISTORY_KEY, null);
      49|            if (oldGenericJson != null) {
      50|                List<ChatMessage> loadedHistory = gson.fromJson(oldGenericJson, historyType);
```

### Line 58
**Comment:** Load Qwen Conversation State

**Context:**
```java
      54|                }
      55|            }
      56|        }
>>>   57|
      58|        // Load Qwen Conversation State
      59|        String qwenStateKey = getProjectSpecificKey(QWEN_CONVERSATION_STATE_KEY_PREFIX);
      60|        String qwenStateJson = prefs.getString(qwenStateKey, null);
      61|        if (qwenStateJson != null) {
```

### Line 67
**Comment:** Restore Gemini FREE conversation metadata per project if present, by copying to SettingsActivity scoping

**Context:**
```java
      63|            qwenState.setConversationId(loadedState.getConversationId());
      64|            qwenState.setLastParentId(loadedState.getLastParentId());
      65|        }
>>>   66|
      67|        // Restore Gemini FREE conversation metadata per project if present, by copying to SettingsActivity scoping
      68|        try {
      69|            String meta = prefs.getString(getProjectSpecificKey(FREE_CONV_META_KEY_PREFIX), null);
      70|            if (meta != null && !meta.isEmpty()) {
```

### Line 71
**Comment:** Use default free model id if not known here; SettingsActivity stores per model.

**Context:**
```java
      67|        // Restore Gemini FREE conversation metadata per project if present, by copying to SettingsActivity scoping
      68|        try {
      69|            String meta = prefs.getString(getProjectSpecificKey(FREE_CONV_META_KEY_PREFIX), null);
>>>   70|            if (meta != null && !meta.isEmpty()) {
      71|                // Use default free model id if not known here; SettingsActivity stores per model.
      72|                SettingsActivity.setFreeConversationMetadata(context, "gemini-2.5-flash", meta);
      73|            }
      74|        } catch (Exception ignore) {}
```

### Line 80
**Comment:** Save Chat History

**Context:**
```java
      76|
      77|    public void saveChatState(List<ChatMessage> chatHistory, QwenConversationState qwenState) {
      78|        SharedPreferences.Editor editor = prefs.edit();
>>>   79|
      80|        // Save Chat History
      81|        String historyKey = getProjectSpecificKey(CHAT_HISTORY_KEY_PREFIX);
      82|        String historyJson = gson.toJson(chatHistory);
      83|        editor.putString(historyKey, historyJson);
```

### Line 85
**Comment:** Save Qwen Conversation State

**Context:**
```java
      81|        String historyKey = getProjectSpecificKey(CHAT_HISTORY_KEY_PREFIX);
      82|        String historyJson = gson.toJson(chatHistory);
      83|        editor.putString(historyKey, historyJson);
>>>   84|
      85|        // Save Qwen Conversation State
      86|        String qwenStateKey = getProjectSpecificKey(QWEN_CONVERSATION_STATE_KEY_PREFIX);
      87|        String qwenStateJson = qwenState.toJson();
      88|        editor.putString(qwenStateKey, qwenStateJson);
```

### Line 90
**Comment:** Persist last known FREE conversation metadata for this project

**Context:**
```java
      86|        String qwenStateKey = getProjectSpecificKey(QWEN_CONVERSATION_STATE_KEY_PREFIX);
      87|        String qwenStateJson = qwenState.toJson();
      88|        editor.putString(qwenStateKey, qwenStateJson);
>>>   89|
      90|        // Persist last known FREE conversation metadata for this project
      91|        try {
      92|            String modelId = "gemini-2.5-flash";
      93|            String meta = SettingsActivity.getFreeConversationMetadata(context, modelId);
```

### Line 138
**Comment:** Also remove the old, generic key to fix the bug where history reappears.

**Context:**
```java
     134|        editor.remove(historyKey);
     135|        editor.remove(qwenStateKey);
     136|        editor.remove(freeMetaKey);
>>>  137|
     138|        // Also remove the old, generic key to fix the bug where history reappears.
     139|        editor.remove(OLD_GENERIC_CHAT_HISTORY_KEY);
     140|
     141|        editor.apply();
```

---

## com/codex/apk/AIChatUIManager.java

**Location:** `app/src/main/java/com/codex/apk/AIChatUIManager.java`

### Line 69
**Comment:** Long press on model selector to choose a custom agent

**Context:**
```java
      65|        if (recyclerAttachedFilesPreview != null) {
      66|            recyclerAttachedFilesPreview.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
      67|        }
>>>   68|
      69|        // Long press on model selector to choose a custom agent
      70|        layoutModelSelectorCustom.setOnLongClickListener(v -> {
      71|            showAgentPickerDialog(fragment.getAIAssistant());
      72|            return true;
```

### Line 143
**Comment:** Ensure selector width recalculates after text change

**Context:**
```java
     139|                .setSingleChoiceItems(modelNames, selectedIndex, (dialog, which) -> {
     140|                    AIModel selectedModel = enabledModels.get(which);
     141|                    aiAssistant.setCurrentModel(selectedModel);
>>>  142|                    textSelectedModel.setText(selectedModel.getDisplayName());
     143|                    // Ensure selector width recalculates after text change
     144|                    if (layoutModelSelectorCustom != null) {
     145|                        layoutModelSelectorCustom.requestLayout();
     146|                    }
```

### Line 153
**Comment:** Persist last used model per project

**Context:**
```java
     149|                            if (layoutModelSelectorCustom != null) layoutModelSelectorCustom.requestLayout();
     150|                        });
     151|                    }
>>>  152|                    updateSettingsButtonState(aiAssistant);
     153|                    // Persist last used model per project
     154|                    android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
     155|                    sp.edit().putString("selected_model", selectedModel.getDisplayName()).apply();
     156|                    dialog.dismiss();
```

### Line 171
**Comment:** Reuse simple list: show names as options

**Context:**
```java
     167|        }
     168|        BottomSheetDialog dlg = new BottomSheetDialog(context);
     169|        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_ai_settings, null);
>>>  170|        dlg.setContentView(view);
     171|        // Reuse simple list: show names as options
     172|        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(context);
     173|        rv.setLayoutManager(new LinearLayoutManager(context));
     174|        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
```

### Line 192
**Comment:** Persist last used model per project

**Context:**
```java
     188|                        aiAssistant.setCurrentModel(model);
     189|                        textSelectedModel.setText(model.getDisplayName());
     190|                        if (layoutModelSelectorCustom != null) layoutModelSelectorCustom.requestLayout();
>>>  191|                        textSelectedModel.post(() -> { if (layoutModelSelectorCustom != null) layoutModelSelectorCustom.requestLayout(); });
     192|                        // Persist last used model per project
     193|                        android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
     194|                        sp.edit().putString("selected_model", model.getDisplayName()).apply();
     195|                        if (a.prompt != null && !a.prompt.isEmpty()) {
```

### Line 196
**Comment:** Prepend custom agent prompt to current input for next send

**Context:**
```java
     192|                        // Persist last used model per project
     193|                        android.content.SharedPreferences sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
     194|                        sp.edit().putString("selected_model", model.getDisplayName()).apply();
>>>  195|                        if (a.prompt != null && !a.prompt.isEmpty()) {
     196|                            // Prepend custom agent prompt to current input for next send
     197|                            String existing = editTextAiPrompt.getText().toString();
     198|                            editTextAiPrompt.setText(a.prompt + "\n\n" + existing);
     199|                            editTextAiPrompt.setSelection(editTextAiPrompt.getText().length());
```

### Line 229
**Comment:** Hide entire Thinking row if model doesn't support thinking

**Context:**
```java
     225|
     226|        ModelCapabilities capabilities = aiAssistant.getCurrentModel().getCapabilities();
     227|        boolean supportsThinking = capabilities.supportsThinking;
>>>  228|
     229|        // Hide entire Thinking row if model doesn't support thinking
     230|        if (rowThinking != null) {
     231|            rowThinking.setVisibility(supportsThinking ? View.VISIBLE : View.GONE);
     232|        }
```

### Line 244
**Comment:** Agent mode has no provider capability constraint

**Context:**
```java
     240|        switchWebSearch.setChecked(aiAssistant.isWebSearchEnabled());
     241|        switchWebSearch.setEnabled(capabilities.supportsWebSearch);
     242|        switchWebSearch.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setWebSearchEnabled(isChecked));
>>>  243|
     244|        // Agent mode has no provider capability constraint
     245|        switchAgent.setChecked(aiAssistant.isAgentModeEnabled());
     246|        switchAgent.setOnCheckedChangeListener((buttonView, isChecked) -> aiAssistant.setAgentModeEnabled(isChecked));
     247|
```

### Line 255
**Comment:** The settings button should always be enabled because "Agent Mode" is always available.

**Context:**
```java
     251|
     252|    public void updateSettingsButtonState(AIAssistant aiAssistant) {
     253|        if (buttonAiSettings == null || aiAssistant == null) return;
>>>  254|
     255|        // The settings button should always be enabled because "Agent Mode" is always available.
     256|        // The individual settings inside the dialog are enabled/disabled based on capabilities.
     257|        buttonAiSettings.setEnabled(true);
     258|
```

### Line 256
**Comment:** The individual settings inside the dialog are enabled/disabled based on capabilities.

**Context:**
```java
     252|    public void updateSettingsButtonState(AIAssistant aiAssistant) {
     253|        if (buttonAiSettings == null || aiAssistant == null) return;
     254|
>>>  255|        // The settings button should always be enabled because "Agent Mode" is always available.
     256|        // The individual settings inside the dialog are enabled/disabled based on capabilities.
     257|        buttonAiSettings.setEnabled(true);
     258|
     259|        // Show attach button only for COOKIES (Gemini reverse-engineered) models
```

### Line 259
**Comment:** Show attach button only for COOKIES (Gemini reverse-engineered) models

**Context:**
```java
     255|        // The settings button should always be enabled because "Agent Mode" is always available.
     256|        // The individual settings inside the dialog are enabled/disabled based on capabilities.
     257|        buttonAiSettings.setEnabled(true);
>>>  258|
     259|        // Show attach button only for COOKIES (Gemini reverse-engineered) models
     260|        if (buttonAiAttach != null) {
     261|            boolean showAttach = aiAssistant.getCurrentModel() != null && aiAssistant.getCurrentModel().getProvider() == AIProvider.COOKIES;
     262|            buttonAiAttach.setVisibility(showAttach ? View.VISIBLE : View.GONE);
```

### Line 321
**Comment:** Vertical item: thumbnail/icon in a rounded card with a clear button overlay, filename below

**Context:**
```java
     317|        }
     318|        recyclerAttachedFilesPreview.setVisibility(View.VISIBLE);
     319|        recyclerAttachedFilesPreview.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
>>>  320|            @Override public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
     321|                // Vertical item: thumbnail/icon in a rounded card with a clear button overlay, filename below
     322|                LinearLayout root = new LinearLayout(context);
     323|                root.setOrientation(LinearLayout.VERTICAL);
     324|                root.setPadding(dp(4), dp(4), dp(4), dp(4));
```

### Line 387
**Comment:** Ask fragment to remove

**Context:**
```java
     383|                    thumb.setImageResource(R.drawable.icon_file_round);
     384|                }
     385|
>>>  386|                clear.setOnClickListener(v -> {
     387|                    // Ask fragment to remove
     388|                    fragment.removePendingAttachmentAt(position);
     389|                });
     390|
```

### Line 391
**Comment:** Tap to preview using an implicit intent

**Context:**
```java
     387|                    // Ask fragment to remove
     388|                    fragment.removePendingAttachmentAt(position);
     389|                });
>>>  390|
     391|                // Tap to preview using an implicit intent
     392|                holder.itemView.setOnClickListener(v -> {
     393|                    try {
     394|                        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", f);
```

---

## com/codex/apk/AdvancedFileManager.java

**Location:** `app/src/main/java/com/codex/apk/AdvancedFileManager.java`

### Line 25
**Comment:** File change listener

**Context:**
```java
      21|    private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
      22|    private final Context context;
      23|    private final File projectDir;
>>>   24|
      25|    // File change listener
      26|    public interface FileChangeListener {
      27|        void onFileCreated(File file);
      28|        void onFileModified(File file);
```

### Line 55
**Comment:** Read current content

**Context:**
```java
      51|                                             String contentType, String errorHandling) throws IOException {
      52|        FileOperationResult result = new FileOperationResult();
      53|
>>>   54|        try {
      55|            // Read current content
      56|            String currentContent = readFileContent(file);
      57|
      58|            // Apply update based on type
```

### Line 58
**Comment:** Apply update based on type

**Context:**
```java
      54|        try {
      55|            // Read current content
      56|            String currentContent = readFileContent(file);
>>>   57|
      58|            // Apply update based on type
      59|            String finalContent = applyUpdateType(currentContent, newContent, updateType);
      60|
      61|            // Validate content if requested
```

### Line 61
**Comment:** Validate content if requested

**Context:**
```java
      57|
      58|            // Apply update based on type
      59|            String finalContent = applyUpdateType(currentContent, newContent, updateType);
>>>   60|
      61|            // Validate content if requested
      62|            if (validateContent) {
      63|                ValidationResult validation = validateContent(finalContent, contentType);
      64|                if (!validation.isValid()) {
```

### Line 70
**Comment:** Idempotency: if no changes, skip write and return success

**Context:**
```java
      66|                        throw new IllegalArgumentException("Content validation failed: " + validation.getReason());
      67|                    }
      68|                }
>>>   69|            }
      70|            // Idempotency: if no changes, skip write and return success
      71|            if (finalContent.equals(currentContent)) {
      72|                result.setSuccess(true);
      73|                result.setMessage("No changes; skipped write");
```

### Line 78
**Comment:** Write the file

**Context:**
```java
      74|                result.setDiff("");
      75|                return result;
      76|            }
>>>   77|
      78|            // Write the file
      79|            writeFileContent(file, finalContent);
      80|
      81|            // Generate diff
```

### Line 81
**Comment:** Generate diff

**Context:**
```java
      77|
      78|            // Write the file
      79|            writeFileContent(file, finalContent);
>>>   80|
      81|            // Generate diff
      82|            String diff = generateDiff(currentContent, finalContent);
      83|            result.setDiff(diff);
      84|
```

### Line 111
**Comment:** Replace entire content

**Context:**
```java
     107|                return currentContent + "\n" + newContent;
     108|            case "prepend":
     109|                return newContent + "\n" + currentContent;
>>>  110|            case "replace":
     111|                // Replace entire content
     112|                return newContent;
     113|            case "smart":
     114|                return applySmartUpdate(currentContent, newContent);
```

### Line 126
**Comment:** Simple smart merging - can be enhanced with more sophisticated algorithms

**Context:**
```java
     122|    /**
     123|     * Smart update with intelligent merging
     124|     */
>>>  125|    private String applySmartUpdate(String currentContent, String newContent) {
     126|        // Simple smart merging - can be enhanced with more sophisticated algorithms
     127|        String[] currentLines = currentContent.split("\n");
     128|        String[] newLines = newContent.split("\n");
     129|
```

### Line 130
**Comment:** Find common patterns and merge intelligently

**Context:**
```java
     126|        // Simple smart merging - can be enhanced with more sophisticated algorithms
     127|        String[] currentLines = currentContent.split("\n");
     128|        String[] newLines = newContent.split("\n");
>>>  129|
     130|        // Find common patterns and merge intelligently
     131|        StringBuilder result = new StringBuilder();
     132|
     133|        // Add current content
```

### Line 133
**Comment:** Add current content

**Context:**
```java
     129|
     130|        // Find common patterns and merge intelligently
     131|        StringBuilder result = new StringBuilder();
>>>  132|
     133|        // Add current content
     134|        result.append(currentContent);
     135|
     136|        // Add new content if it's not already present
```

### Line 136
**Comment:** Add new content if it's not already present

**Context:**
```java
     132|
     133|        // Add current content
     134|        result.append(currentContent);
>>>  135|
     136|        // Add new content if it's not already present
     137|        if (!currentContent.contains(newContent)) {
     138|            result.append("\n").append(newContent);
     139|        }
```

### Line 149
**Comment:** Robust unified diff application (context-aware)

**Context:**
```java
     145|     * Apply unified diff patch
     146|     */
     147|    private String applyPatch(String currentContent, String patchContent) {
>>>  148|        try {
     149|            // Robust unified diff application (context-aware)
     150|            return UnifiedDiff.applyUnifiedDiff(currentContent, patchContent);
     151|        } catch (Exception e) {
     152|            Log.e(TAG, "Patch application failed", e);
```

### Line 172
**Comment:** Apply hunks in order, adjust offsets as we go

**Context:**
```java
     168|
     169|        public static String applyUnifiedDiff(String original, String patch) {
     170|            List<String> src = new ArrayList<>(Arrays.asList(original.split("\n", -1)));
>>>  171|            List<Hunk> hunks = parseHunks(patch);
     172|            // Apply hunks in order, adjust offsets as we go
     173|            int offset = 0;
     174|            for (Hunk h : hunks) {
     175|                int applyPos = Math.max(0, Math.min(src.size(), h.startOld - 1 + offset));
```

### Line 176
**Comment:** Try exact context match first; if not, do a fuzzy search window

**Context:**
```java
     172|            // Apply hunks in order, adjust offsets as we go
     173|            int offset = 0;
     174|            for (Hunk h : hunks) {
>>>  175|                int applyPos = Math.max(0, Math.min(src.size(), h.startOld - 1 + offset));
     176|                // Try exact context match first; if not, do a fuzzy search window
     177|                int matchedIndex = findBestMatch(src, h, applyPos);
     178|                if (matchedIndex < 0) continue; // skip hunk if no reasonable match
     179|                // Remove old lines
```

### Line 179
**Comment:** Remove old lines

**Context:**
```java
     175|                int applyPos = Math.max(0, Math.min(src.size(), h.startOld - 1 + offset));
     176|                // Try exact context match first; if not, do a fuzzy search window
     177|                int matchedIndex = findBestMatch(src, h, applyPos);
>>>  178|                if (matchedIndex < 0) continue; // skip hunk if no reasonable match
     179|                // Remove old lines
     180|                int removeCount = Math.min(h.lenOld, Math.max(0, src.size() - matchedIndex));
     181|                for (int i = 0; i < removeCount; i++) {
     182|                    src.remove(matchedIndex);
```

### Line 184
**Comment:** Insert new lines

**Context:**
```java
     180|                int removeCount = Math.min(h.lenOld, Math.max(0, src.size() - matchedIndex));
     181|                for (int i = 0; i < removeCount; i++) {
     182|                    src.remove(matchedIndex);
>>>  183|                }
     184|                // Insert new lines
     185|                List<String> toInsert = new ArrayList<>();
     186|                for (String l : h.lines) if (l.length() > 0 && (l.charAt(0) == ' ' || l.charAt(0) == '+')) {
     187|                    if (l.startsWith(" ") || l.startsWith("+")) {
```

### Line 222
**Comment:** @@ -a,b +c,d @@

**Context:**
```java
     218|            return hunks;
     219|        }
     220|
>>>  221|        private static int[] parseHunkHeader(String header) {
     222|            // @@ -a,b +c,d @@
     223|            try {
     224|                String core = header.substring(2, header.indexOf("@@", 2)).trim();
     225|                String[] parts = core.split(" ");
```

### Line 243
**Comment:** Try exact expected index first

**Context:**
```java
     239|            try { return Integer.parseInt(s); } catch (Exception e) { return def; }
     240|        }
     241|
>>>  242|        private static int findBestMatch(List<String> src, Hunk h, int expectedIndex) {
     243|            // Try exact expected index first
     244|            if (contextMatches(src, expectedIndex, h)) return expectedIndex;
     245|            // Fuzzy search in a small window around expected index
     246|            int window = 50;
```

### Line 245
**Comment:** Fuzzy search in a small window around expected index

**Context:**
```java
     241|
     242|        private static int findBestMatch(List<String> src, Hunk h, int expectedIndex) {
     243|            // Try exact expected index first
>>>  244|            if (contextMatches(src, expectedIndex, h)) return expectedIndex;
     245|            // Fuzzy search in a small window around expected index
     246|            int window = 50;
     247|            for (int delta = 1; delta <= window; delta++) {
     248|                int left = expectedIndex - delta;
```

### Line 257
**Comment:** Compare context lines (' ' and '-' from old) against source

**Context:**
```java
     253|            return -1;
     254|        }
     255|
>>>  256|        private static boolean contextMatches(List<String> src, int index, Hunk h) {
     257|            // Compare context lines (' ' and '-' from old) against source
     258|            int i = index;
     259|            for (String l : h.lines) {
     260|                if (l.startsWith(" ") || l.startsWith("-")) {
```

### Line 286
**Comment:** Fallback to simple diff

**Context:**
```java
     282|        try {
     283|            return DiffGenerator.generateDiff(oldContent, newContent, format, oldFile, newFile);
     284|        } catch (Exception e) {
>>>  285|            Log.e(TAG, "Enhanced diff generation failed", e);
     286|            // Fallback to simple diff
     287|            StringBuilder diff = new StringBuilder();
     288|            diff.append("--- ").append(oldFile).append("\n");
     289|            diff.append("+++ ").append(newFile).append("\n");
```

### Line 327
**Comment:** File type specific validation

**Context:**
```java
     323|            result.setReason("Content is empty");
     324|            return result;
     325|        }
>>>  326|
     327|        // File type specific validation
     328|        if (contentType != null) {
     329|            switch (contentType.toLowerCase()) {
     330|                case "html":
```

### Line 393
**Comment:** Getters and setters

**Context:**
```java
     389|        private String message;
     390|        private String diff;
     391|        private String errorDetails;
>>>  392|
     393|        // Getters and setters
     394|        public boolean isSuccess() { return success; }
     395|        public void setSuccess(boolean success) { this.success = success; }
     396|
```

---

## com/codex/apk/AgentsActivity.java

**Location:** `app/src/main/java/com/codex/apk/AgentsActivity.java`

### Line 93
**Comment:** Add new agent

**Context:**
```java
      89|            }
      90|
      91|            java.util.List<CustomAgent> agents = SettingsActivity.getCustomAgents(this);
>>>   92|            if (agentToEdit == null) {
      93|                // Add new agent
      94|                agents.add(new CustomAgent(java.util.UUID.randomUUID().toString(), name, prompt, selectedModel.getModelId()));
      95|            } else {
      96|                // Update existing agent
```

### Line 96
**Comment:** Update existing agent

**Context:**
```java
      92|            if (agentToEdit == null) {
      93|                // Add new agent
      94|                agents.add(new CustomAgent(java.util.UUID.randomUUID().toString(), name, prompt, selectedModel.getModelId()));
>>>   95|            } else {
      96|                // Update existing agent
      97|                agentToEdit.name = name;
      98|                agentToEdit.prompt = prompt;
      99|                agentToEdit.modelId = selectedModel.getModelId();
```

---

## com/codex/apk/AiProcessor.java

**Location:** `app/src/main/java/com/codex/apk/AiProcessor.java`

### Line 192
**Comment:** Use centralized delete logic that supports files and directories

**Context:**
```java
     188|
     189|        if (!fileToDelete.exists()) {
     190|            throw new IOException("File not found for deletion: " + path);
>>>  191|        }
     192|        // Use centralized delete logic that supports files and directories
     193|        FileOps.deleteRecursively(fileToDelete);
     194|
     195|        return "Deleted file/directory: " + path;
```

### Line 211
**Comment:** Delegate to FileOps (creates parent dirs as needed)

**Context:**
```java
     207|
     208|        if (newFile.exists()) {
     209|            throw new IOException("Target file/directory already exists for rename: " + newPath);
>>>  210|        }
     211|        // Delegate to FileOps (creates parent dirs as needed)
     212|        boolean success = FileOps.renameFile(projectDir, oldPath, newPath);
     213|        if (!success) {
     214|            throw new IOException("Failed to rename file from " + oldPath + " to " + newPath);
```

---

## com/codex/apk/AnyProviderApiClient.java

**Location:** `app/src/main/java/com/codex/apk/AnyProviderApiClient.java`

### Line 75
**Comment:** Build request JSON (OpenAI-style)

**Context:**
```java
      71|            Response response = null;
      72|            try {
      73|                if (actionListener != null) actionListener.onAiRequestStarted();
>>>   74|
      75|                // Build request JSON (OpenAI-style)
      76|                String providerModel = mapToProviderModel(model != null ? model.getModelId() : null);
      77|                JsonObject body = buildOpenAIStyleBody(providerModel, message, history, thinkingModeEnabled);
      78|
```

### Line 99
**Comment:** Stream OpenAI-like SSE

**Context:**
```java
      95|                    if (actionListener != null) actionListener.onAiError("Free endpoint request failed: " + (response != null ? response.code() : -1) + (snippet != null ? (" | body: " + snippet) : ""));
      96|                    return;
      97|                }
>>>   98|
      99|                // Stream OpenAI-like SSE
     100|                StringBuilder finalText = new StringBuilder();
     101|                StringBuilder rawSse = new StringBuilder();
     102|                streamOpenAiSse(response, finalText, rawSse);
```

### Line 129
**Comment:** Convert existing history (keep it light)

**Context:**
```java
     125|    }
     126|
     127|    protected JsonObject buildOpenAIStyleBody(String modelId, String userMessage, List<ChatMessage> history, boolean thinkingModeEnabled) {
>>>  128|        JsonArray messages = new JsonArray();
     129|        // Convert existing history (keep it light)
     130|        if (history != null) {
     131|            for (ChatMessage m : history) {
     132|                String role = m.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant";
```

### Line 141
**Comment:** Append the new user message (AIAssistant has already prepended system prompt when needed)

**Context:**
```java
     137|                msg.addProperty("content", content);
     138|                messages.add(msg);
     139|            }
>>>  140|        }
     141|        // Append the new user message (AIAssistant has already prepended system prompt when needed)
     142|        JsonObject user = new JsonObject();
     143|        user.addProperty("role", "user");
     144|        user.addProperty("content", userMessage);
```

### Line 151
**Comment:** Pollinations supports seed and referrer; seed helps cache-busting and diversity

**Context:**
```java
     147|        JsonObject root = new JsonObject();
     148|        root.addProperty("model", modelId);
     149|        root.add("messages", messages);
>>>  150|        root.addProperty("stream", true);
     151|        // Pollinations supports seed and referrer; seed helps cache-busting and diversity
     152|        root.addProperty("seed", random.nextInt(Integer.MAX_VALUE));
     153|        root.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
     154|        // If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal
```

### Line 154
**Comment:** If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal

**Context:**
```java
     150|        root.addProperty("stream", true);
     151|        // Pollinations supports seed and referrer; seed helps cache-busting and diversity
     152|        root.addProperty("seed", random.nextInt(Integer.MAX_VALUE));
>>>  153|        root.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
     154|        // If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal
     155|        if (thinkingModeEnabled) {
     156|            // Some providers accept x-show-reasoning header; we rely on minimal body here
     157|        }
```

### Line 156
**Comment:** Some providers accept x-show-reasoning header; we rely on minimal body here

**Context:**
```java
     152|        root.addProperty("seed", random.nextInt(Integer.MAX_VALUE));
     153|        root.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
     154|        // If provider supports reasoning visibility, prefer default (no explicit toggle); keep payload minimal
>>>  155|        if (thinkingModeEnabled) {
     156|            // Some providers accept x-show-reasoning header; we rely on minimal body here
     157|        }
     158|        return root;
     159|    }
```

### Line 165
**Comment:** Throttle

**Context:**
```java
     161|    protected void streamOpenAiSse(Response response, StringBuilder finalText, StringBuilder rawAnswer) throws IOException {
     162|        BufferedSource source = response.body().source();
     163|        try { source.timeout().timeout(60, TimeUnit.SECONDS); } catch (Exception ignore) {}
>>>  164|        StringBuilder eventBuf = new StringBuilder();
     165|        // Throttle
     166|        long[] lastEmitNs = new long[]{0L};
     167|        int[] lastSentLen = new int[]{0};
     168|        while (true) {
```

### Line 185
**Comment:** Force final emit

**Context:**
```java
     181|        }
     182|        if (eventBuf.length() > 0) {
     183|            handleOpenAiEvent(eventBuf.toString(), finalText, rawAnswer, lastEmitNs, lastSentLen);
>>>  184|        }
     185|        // Force final emit
     186|        if (actionListener != null && finalText.length() != lastSentLen[0]) {
     187|            actionListener.onAiStreamUpdate(finalText.toString(), false);
     188|        }
```

### Line 216
**Comment:** Non-streaming fallback chunk

**Context:**
```java
     212|                                maybeEmit(finalText, lastEmitNs, lastSentLen);
     213|                            }
     214|                        }
>>>  215|                    } else if (choice.has("message") && choice.get("message").isJsonObject()) {
     216|                        // Non-streaming fallback chunk
     217|                        JsonObject msg = choice.getAsJsonObject("message");
     218|                        if (msg.has("content") && !msg.get("content").isJsonNull()) {
     219|                            finalText.append(msg.get("content").getAsString());
```

### Line 249
**Comment:** Pollinations exposes many backends by name; pass through most names.

**Context:**
```java
     245|
     246|    protected String mapToProviderModel(String modelId) {
     247|        if (modelId == null || modelId.isEmpty()) return "openai"; // sensible default
>>>  248|        String lower = modelId.toLowerCase(Locale.ROOT);
     249|        // Pollinations exposes many backends by name; pass through most names.
     250|        return lower;
     251|    }
     252|
```

---

## com/codex/apk/ChatMessage.java

**Location:** `app/src/main/java/com/codex/apk/ChatMessage.java`

### Line 20
**Comment:** Status constants for AI messages with proposed actions

**Context:**
```java
      16|public class ChatMessage {
      17|    public static final int SENDER_USER = 0;
      18|    public static final int SENDER_AI = 1;
>>>   19|
      20|    // Status constants for AI messages with proposed actions
      21|    public static final int STATUS_NONE = -1; // Default for user messages or AI thinking/error messages
      22|    public static final int STATUS_PENDING_APPROVAL = 0; // AI proposed actions, waiting for user decision
      23|    public static final int STATUS_ACCEPTED = 1; // User accepted the AI's proposed actions
```

### Line 34
**Comment:** New fields for AI proposed actions and their status

**Context:**
```java
      30|    private String aiModelName; // For AI messages, the name of the AI model used
      31|    private long timestamp; // Timestamp for ordering messages
      32|    private List<String> userAttachmentPaths; // For user messages, list of attached file display names/paths
>>>   33|
      34|    // New fields for AI proposed actions and their status
      35|    private String rawAiResponseJson; // The raw JSON response from the AI model
      36|    private List<FileActionDetail> proposedFileChanges; // Parsed list of proposed file changes
      37|    private int status; // Current status of the AI message (e.g., PENDING_APPROVAL, ACCEPTED, DISCARDED)
```

### Line 39
**Comment:** New fields for thinking mode and web search

**Context:**
```java
      35|    private String rawAiResponseJson; // The raw JSON response from the AI model
      36|    private List<FileActionDetail> proposedFileChanges; // Parsed list of proposed file changes
      37|    private int status; // Current status of the AI message (e.g., PENDING_APPROVAL, ACCEPTED, DISCARDED)
>>>   38|
      39|    // New fields for thinking mode and web search
      40|    private String thinkingContent; // The thinking/reasoning content from AI
      41|    private List<WebSource> webSources; // Web sources used in the response
      42|
```

### Line 43
**Comment:** Plan steps (for agent plan rendering)

**Context:**
```java
      39|    // New fields for thinking mode and web search
      40|    private String thinkingContent; // The thinking/reasoning content from AI
      41|    private List<WebSource> webSources; // Web sources used in the response
>>>   42|
      43|    // Plan steps (for agent plan rendering)
      44|    private List<PlanStep> planSteps;
      45|
      46|    // Qwen threading fields
```

### Line 46
**Comment:** Qwen threading fields

**Context:**
```java
      42|
      43|    // Plan steps (for agent plan rendering)
      44|    private List<PlanStep> planSteps;
>>>   45|
      46|    // Qwen threading fields
      47|    private String fid; // Unique message id
      48|    private String parentId; // Parent message id
      49|    private List<String> childrenIds; // Children message ids
```

### Line 92
**Comment:** Getters

**Context:**
```java
      88|        this.parentId = null;
      89|        this.childrenIds = new ArrayList<>();
      90|    }
>>>   91|
      92|    // Getters
      93|    public int getSender() { return sender; }
      94|    public String getContent() { return content; }
      95|    public List<String> getActionSummaries() { return actionSummaries; }
```

### Line 109
**Comment:** Getters and setters for Qwen threading fields

**Context:**
```java
     105|    public String getThinkingContent() { return thinkingContent; }
     106|    public List<WebSource> getWebSources() { return webSources; }
     107|    public List<String> getUserAttachmentPaths() { return userAttachmentPaths; }
>>>  108|
     109|    // Getters and setters for Qwen threading fields
     110|    public String getFid() { return fid; }
     111|    public void setFid(String fid) { this.fid = fid; }
     112|    public String getParentId() { return parentId; }
```

### Line 118
**Comment:** Setters (for updating message properties after creation, e.g., status)

**Context:**
```java
     114|    public List<String> getChildrenIds() { return childrenIds; }
     115|    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = childrenIds; }
     116|
>>>  117|
     118|    // Setters (for updating message properties after creation, e.g., status)
     119|    public void setContent(String content) { this.content = content; }
     120|    public void setStatus(int status) { this.status = status; }
     121|    public void setActionSummaries(List<String> actionSummaries) { this.actionSummaries = actionSummaries; }
```

### Line 168
**Comment:** NEW: Advanced operation fields

**Context:**
```java
     164|        public List<String> insertLines; // For modifyLines
     165|        public String search; // For searchAndReplace
     166|        public String replace; // For searchAndReplace
>>>  167|
     168|        // NEW: Advanced operation fields
     169|        public String updateType; // "full", "append", "prepend", "replace", "patch", "smart"
     170|        public String searchPattern; // Regex pattern for smart replacements
     171|        public String replaceWith; // Replacement content for smart updates
```

### Line 184
**Comment:** Agent execution status fields

**Context:**
```java
     180|        public String errorHandling; // "strict", "lenient", "auto-revert"
     181|        public boolean generateDiff; // Whether to generate diff
     182|        public String diffFormat; // "unified", "context", "side-by-side"
>>>  183|
     184|        // Agent execution status fields
     185|        public String stepStatus; // pending | running | completed | failed
     186|        public String stepMessage; // latest progress/error message
     187|
```

### Line 188
**Comment:** Comprehensive constructor

**Context:**
```java
     184|        // Agent execution status fields
     185|        public String stepStatus; // pending | running | completed | failed
     186|        public String stepMessage; // latest progress/error message
>>>  187|
     188|        // Comprehensive constructor
     189|        public FileActionDetail(String type, String path, String oldPath, String newPath,
     190|                                String oldContent, String newContent, int startLine,
     191|                                int deleteCount, List<String> insertLines, String search, String replace) {
```

### Line 204
**Comment:** Initialize advanced fields

**Context:**
```java
     200|            this.insertLines = insertLines != null ? new ArrayList<>(insertLines) : null;
     201|            this.search = search;
     202|            this.replace = replace;
>>>  203|
     204|            // Initialize advanced fields
     205|            this.updateType = "full";
     206|            this.createBackup = true;
     207|            this.validateContent = true;
```

### Line 214
**Comment:** Initialize agent status

**Context:**
```java
     210|            this.errorHandling = "strict";
     211|            this.metadata = new HashMap<>();
     212|            this.validationRules = new ArrayList<>();
>>>  213|
     214|            // Initialize agent status
     215|            this.stepStatus = "pending";
     216|            this.stepMessage = "";
     217|        }
```

### Line 219
**Comment:** Enhanced constructor with advanced options

**Context:**
```java
     215|            this.stepStatus = "pending";
     216|            this.stepMessage = "";
     217|        }
>>>  218|
     219|        // Enhanced constructor with advanced options
     220|        public FileActionDetail(String type, String path, String oldPath, String newPath,
     221|                                String oldContent, String newContent, int startLine,
     222|                                int deleteCount, List<String> insertLines, String search, String replace,
```

### Line 245
**Comment:** Method to get a displayable summary of the action

**Context:**
```java
     241|            this.metadata = new HashMap<>();
     242|            this.validationRules = new ArrayList<>();
     243|        }
>>>  244|
     245|        // Method to get a displayable summary of the action
     246|        public String getSummary() {
     247|            switch (type) {
     248|                case "createFile":
```

### Line 291
**Comment:** Only include AI-specific fields if it's an AI message

**Context:**
```java
     287|        map.put("content", content);
     288|        map.put("timestamp", timestamp);
     289|        map.put("status", status); // Include status
>>>  290|
     291|        // Only include AI-specific fields if it's an AI message
     292|        if (sender == SENDER_AI) {
     293|            map.put("actionSummaries", actionSummaries);
     294|            map.put("suggestions", suggestions);
```

### Line 298
**Comment:** Serialize proposedFileChanges to JSON string

**Context:**
```java
     294|            map.put("suggestions", suggestions);
     295|            map.put("aiModelName", aiModelName);
     296|            map.put("rawAiResponseJson", rawAiResponseJson);
>>>  297|
     298|            // Serialize proposedFileChanges to JSON string
     299|            if (proposedFileChanges != null && !proposedFileChanges.isEmpty()) {
     300|                Gson gson = new Gson();
     301|                map.put("proposedFileChanges", gson.toJson(proposedFileChanges));
```

### Line 307
**Comment:** User message attachments

**Context:**
```java
     303|                map.put("proposedFileChanges", null);
     304|            }
     305|
>>>  306|        } else {
     307|            // User message attachments
     308|            if (userAttachmentPaths != null && !userAttachmentPaths.isEmpty()) {
     309|                map.put("userAttachmentPaths", new ArrayList<>(userAttachmentPaths));
     310|            }
```

### Line 362
**Comment:** Deserialize proposedFileChanges from JSON string

**Context:**
```java
     358|            String rawAiResponseJson = (String) map.get("rawAiResponseJson");
     359|            int status = map.containsKey("status") ? ((Number) map.get("status")).intValue() : STATUS_NONE;
     360|
>>>  361|
     362|            // Deserialize proposedFileChanges from JSON string
     363|            List<FileActionDetail> proposedFileChanges = null;
     364|            String proposedFileChangesJson = (String) map.get("proposedFileChanges");
     365|            if (proposedFileChangesJson != null && !proposedFileChangesJson.isEmpty()) {
```

---

## com/codex/apk/ChatMessageAdapter.java

**Location:** `app/src/main/java/com/codex/apk/ChatMessageAdapter.java`

### Line 122
**Comment:** Long-press to show raw response for this step

**Context:**
```java
     118|                int colorId;
     119|                switch (s) { case "running": colorId = R.color.warning_container; break; case "completed": colorId = R.color.success_container; break; case "failed": colorId = R.color.error_container; break; default: colorId = R.color.surface_container; }
     120|                if (status.getBackground() instanceof GradientDrawable) { GradientDrawable bg = (GradientDrawable) status.getBackground().mutate(); bg.setColor(itemView.getResources().getColor(colorId)); }
>>>  121|
     122|                // Long-press to show raw response for this step
     123|                itemView.setOnLongClickListener(v -> {
     124|                    Context ctx = itemView.getContext();
     125|
```

### Line 126
**Comment:** Use BottomSheetDialog for better UX

**Context:**
```java
     122|                // Long-press to show raw response for this step
     123|                itemView.setOnLongClickListener(v -> {
     124|                    Context ctx = itemView.getContext();
>>>  125|
     126|                    // Use BottomSheetDialog for better UX
     127|                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ctx, R.style.ThemeOverlay_Material3_BottomSheetDialog);
     128|                    View sheetView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_raw_api_response, null);
     129|                    bottomSheetDialog.setContentView(sheetView);
```

### Line 145
**Comment:** Update title for plan step context

**Context:**
```java
     141|                        textRawResponse.setText("No raw response captured for this step yet.");
     142|                        textRawResponse.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant));
     143|                    }
>>>  144|
     145|                    // Update title for plan step context
     146|                    TextView titleText = sheetView.findViewById(R.id.text_title);
     147|                    if (titleText == null) {
     148|                        // Find the title TextView in the layout
```

### Line 148
**Comment:** Find the title TextView in the layout

**Context:**
```java
     144|
     145|                    // Update title for plan step context
     146|                    TextView titleText = sheetView.findViewById(R.id.text_title);
>>>  147|                    if (titleText == null) {
     148|                        // Find the title TextView in the layout
     149|                        TextView headerTitle = sheetView.findViewById(R.id.text_raw_response_title);
     150|                        if (headerTitle != null) {
     151|                            headerTitle.setText("Plan Step Raw Response");
```

### Line 199
**Comment:** Vertical: thumbnail/card (64dp) + filename below

**Context:**
```java
     195|                recyclerAttachments.setVisibility(View.VISIBLE);
     196|                recyclerAttachments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
     197|                recyclerAttachments.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
>>>  198|                    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
     199|                        // Vertical: thumbnail/card (64dp) + filename below
     200|                        LinearLayout root = new LinearLayout(context);
     201|                        root.setOrientation(LinearLayout.VERTICAL);
     202|                        root.setPadding(dp(4), dp(4), dp(4), dp(4));
```

### Line 320
**Comment:** Long click is set in bind with the bound message to avoid outer messages reference

**Context:**
```java
     316|            layoutPlanActions = itemView.findViewById(R.id.layout_plan_actions);
     317|            buttonAcceptPlan = itemView.findViewById(R.id.button_accept_plan);
     318|            buttonDiscardPlan = itemView.findViewById(R.id.button_discard_plan);
>>>  319|            markdownFormatter = MarkdownFormatter.getInstance(context);
     320|            // Long click is set in bind with the bound message to avoid outer messages reference
     321|        }
     322|
     323|        private void showWebSourcesDialog(List<WebSource> webSources) {
```

### Line 344
**Comment:** Use BottomSheetDialog instead of AlertDialog for better UX

**Context:**
```java
     340|            dialog.show();
     341|        }
     342|
>>>  343|        private void showRawApiResponseDialog(ChatMessage message) {
     344|            // Use BottomSheetDialog instead of AlertDialog for better UX
     345|            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.ThemeOverlay_Material3_BottomSheetDialog);
     346|            View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_raw_api_response, null);
     347|            bottomSheetDialog.setContentView(sheetView);
```

### Line 363
**Comment:** Copy functionality

**Context:**
```java
     359|                textRawResponse.setText("No raw API response available.");
     360|                textRawResponse.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
     361|            }
>>>  362|
     363|            // Copy functionality
     364|            buttonCopy.setOnClickListener(v -> {
     365|                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
     366|                if (clipboard != null) {
```

### Line 373
**Comment:** Share functionality

**Context:**
```java
     369|                    android.widget.Toast.makeText(context, "Raw response copied to clipboard", android.widget.Toast.LENGTH_SHORT).show();
     370|                }
     371|            });
>>>  372|
     373|            // Share functionality
     374|            buttonShare.setOnClickListener(v -> {
     375|                if (rawResponse != null && !rawResponse.isEmpty()) {
     376|                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
```

### Line 386
**Comment:** Close functionality

**Context:**
```java
     382|                    android.widget.Toast.makeText(context, "No response to share", android.widget.Toast.LENGTH_SHORT).show();
     383|                }
     384|            });
>>>  385|
     386|            // Close functionality
     387|            buttonClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
     388|
     389|            bottomSheetDialog.show();
```

### Line 391
**Comment:** Set expanded state for better UX

**Context:**
```java
     387|            buttonClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
     388|
     389|            bottomSheetDialog.show();
>>>  390|
     391|            // Set expanded state for better UX
     392|            bottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
     393|        }
     394|
```

### Line 399
**Comment:** Replace visible [[n]] with (n)

**Context:**
```java
     395|        private void applyCitationSpans(TextView tv, List<WebSource> sources) {
     396|            if (tv == null || sources == null || sources.isEmpty()) return;
     397|            CharSequence text = tv.getText();
>>>  398|            if (text == null) return;
     399|            // Replace visible [[n]] with (n)
     400|            String visible = text.toString().replaceAll("\\[\\[(\\d+)\\]\\]", "($1)");
     401|            SpannableStringBuilder ssb = new SpannableStringBuilder(visible);
     402|            // Find (n) again to attach spans
```

### Line 402
**Comment:** Find (n) again to attach spans

**Context:**
```java
     398|            if (text == null) return;
     399|            // Replace visible [[n]] with (n)
     400|            String visible = text.toString().replaceAll("\\[\\[(\\d+)\\]\\]", "($1)");
>>>  401|            SpannableStringBuilder ssb = new SpannableStringBuilder(visible);
     402|            // Find (n) again to attach spans
     403|            Pattern p = Pattern.compile("\\((\\d+)\\)");
     404|            Matcher m = p.matcher(visible);
     405|            while (m.find()) {
```

### Line 428
**Comment:** Minimal indicator: keep subtle fade animation

**Context:**
```java
     424|            itemView.setOnLongClickListener(v -> { showRawApiResponseDialog(message); return true; });
     425|
     426|            layoutTypingIndicator.setVisibility(isTyping ? View.VISIBLE : View.GONE);
>>>  427|            if (isTyping) {
     428|                // Minimal indicator: keep subtle fade animation
     429|                AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f);
     430|                anim.setDuration(800);
     431|                anim.setRepeatMode(AlphaAnimation.REVERSE);
```

### Line 462
**Comment:** Suppress raw JSON echo when plan UI is present

**Context:**
```java
     458|                }
     459|                if (planTitle != null) {
     460|                    displayContent = planTitle;
>>>  461|                } else if (content != null && content.trim().startsWith("{") && (raw != null && raw.contains("\"action\"") && raw.contains("\"plan\""))) {
     462|                    // Suppress raw JSON echo when plan UI is present
     463|                    displayContent = "";
     464|                } else if (content != null && content.trim().startsWith("{") && com.codex.apk.QwenResponseParser.looksLikeJson(content)) {
     465|                    // Suppress any other JSON content
```

### Line 465
**Comment:** Suppress any other JSON content

**Context:**
```java
     461|                } else if (content != null && content.trim().startsWith("{") && (raw != null && raw.contains("\"action\"") && raw.contains("\"plan\""))) {
     462|                    // Suppress raw JSON echo when plan UI is present
     463|                    displayContent = "";
>>>  464|                } else if (content != null && content.trim().startsWith("{") && com.codex.apk.QwenResponseParser.looksLikeJson(content)) {
     465|                    // Suppress any other JSON content
     466|                    displayContent = "";
     467|                }
     468|            }
```

### Line 485
**Comment:** Default to collapsed on bind

**Context:**
```java
     481|
     482|            if (layoutThinkingSection.getVisibility() == View.VISIBLE) {
     483|                String processedThinking = markdownFormatter.preprocessMarkdown(message.getThinkingContent());
>>>  484|                markdownFormatter.setThinkingMarkdown(textThinkingContent, processedThinking);
     485|                // Default to collapsed on bind
     486|                textThinkingContent.setVisibility(View.GONE);
     487|                textThinkingHeaderTitle.setRotation(0f);
     488|                View thinkingHeader = layoutThinkingSection.findViewById(R.id.layout_thinking_header);
```

### Line 493
**Comment:** Animate the drawable rotation

**Context:**
```java
     489|                if (thinkingHeader != null) {
     490|                    thinkingHeader.setOnClickListener(v -> {
     491|                        boolean expanded = textThinkingContent.getVisibility() == View.VISIBLE;
>>>  492|                        textThinkingContent.setVisibility(expanded ? View.GONE : View.VISIBLE);
     493|                        // Animate the drawable rotation
     494|                        android.graphics.drawable.Drawable[] drawables = textThinkingHeaderTitle.getCompoundDrawables();
     495|                        android.graphics.drawable.Drawable endDrawable = drawables[2]; // 0:left, 1:top, 2:right, 3:bottom
     496|                        if (endDrawable != null) {
```

### Line 513
**Comment:** Link (n) citations in the main message to sources

**Context:**
```java
     509|
     510|            if (layoutWebSources.getVisibility() == View.VISIBLE) {
     511|                buttonWebSources.setText("Web sources (" + message.getWebSources().size() + ")");
>>>  512|                buttonWebSources.setOnClickListener(v -> showWebSourcesDialog(message.getWebSources()));
     513|                // Link (n) citations in the main message to sources
     514|                applyCitationSpans(textMessage, message.getWebSources());
     515|            }
     516|
```

### Line 523
**Comment:** Show file actions Accept/Discard only when not in agent mode

**Context:**
```java
     519|                FileActionAdapter fileActionAdapter = new FileActionAdapter(message.getProposedFileChanges(), fileActionDetail -> { if (listener != null) listener.onFileChangeClicked(fileActionDetail); });
     520|                fileChangesContainer.setAdapter(fileActionAdapter);
     521|            }
>>>  522|
     523|            // Show file actions Accept/Discard only when not in agent mode
     524|            boolean isAgent = false;
     525|            if (context instanceof EditorActivity) {
     526|                AIAssistant assistant = ((EditorActivity) context).aiAssistantManager != null ? ((EditorActivity) context).aiAssistantManager.getAIAssistant() : null;
```

---

## com/codex/apk/CodeEditorFragment.java

**Location:** `app/src/main/java/com/codex/apk/CodeEditorFragment.java`

### Line 34
**Comment:** Listener to communicate with EditorActivity

**Context:**
```java
      30|    private ViewPager2 fileViewPager;
      31|    private SimpleSoraTabAdapter tabAdapter;
      32|    private TabLayout tabLayout;
>>>   33|
      34|    // Listener to communicate with EditorActivity
      35|    private CodeEditorFragmentListener listener;
      36|
      37|    /**
```

### Line 67
**Comment:** Ensure the hosting activity implements the listener interface

**Context:**
```java
      63|
      64|    @Override
      65|    public void onAttach(@NonNull Context context) {
>>>   66|        super.onAttach(context);
      67|        // Ensure the hosting activity implements the listener interface
      68|        if (context instanceof CodeEditorFragmentListener) {
      69|            listener = (CodeEditorFragmentListener) context;
      70|        } else {
```

### Line 78
**Comment:** Inflate the layout for this fragment

**Context:**
```java
      74|
      75|    @Nullable
      76|    @Override
>>>   77|    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
      78|        // Inflate the layout for this fragment
      79|        View view = inflater.inflate(R.layout.layout_code_editor_tab, container, false);
      80|
      81|        fileViewPager = view.findViewById(R.id.file_view_pager);
```

### Line 101
**Comment:** Pass 'this' as TabActionListener so TabAdapter can call back to this fragment

**Context:**
```java
      97|            return;
      98|        }
      99|
>>>  100|        List<TabItem> openTabs = listener.getOpenTabsList();
     101|        // Pass 'this' as TabActionListener so TabAdapter can call back to this fragment
     102|        tabAdapter = new SimpleSoraTabAdapter(getContext(), openTabs, this, listener.getFileManager()); // 'this' refers to CodeEditorFragment
     103|
     104|        fileViewPager.setAdapter(tabAdapter);
```

### Line 117
**Comment:** This is a workaround to get the TextView and set its properties,

**Context:**
```java
     113|                } else {
     114|                    fileViewPager.setCurrentItem(position, true);
     115|                }
>>>  116|            });
     117|            // This is a workaround to get the TextView and set its properties,
     118|            // as the default tab layout doesn't expose it directly.
     119|            tab.view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
     120|                @Override
```

### Line 118
**Comment:** as the default tab layout doesn't expose it directly.

**Context:**
```java
     114|                    fileViewPager.setCurrentItem(position, true);
     115|                }
     116|            });
>>>  117|            // This is a workaround to get the TextView and set its properties,
     118|            // as the default tab layout doesn't expose it directly.
     119|            tab.view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
     120|                @Override
     121|                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
```

### Line 130
**Comment:** Remove the listener to avoid multiple calls

**Context:**
```java
     126|                            if (child instanceof TextView) {
     127|                                TextView textView = (TextView) child;
     128|                                textView.setSingleLine(true);
>>>  129|                                textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
     130|                                // Remove the listener to avoid multiple calls
     131|                                tab.view.removeOnLayoutChangeListener(this);
     132|                                break;
     133|                            }
```

### Line 179
**Comment:** Simply notify the adapter that data has changed.

**Context:**
```java
     175|        if (fileViewPager == null || tabAdapter == null || listener == null) {
     176|            Log.e(TAG, "refreshFileTabLayout: One or more UI components or listener are null.");
     177|            return;
>>>  178|        }
     179|        // Simply notify the adapter that data has changed.
     180|        tabAdapter.notifyDataSetChanged();
     181|    }
     182|
```

### Line 292
**Comment:** Clean up resources held by the SimpleSoraTabAdapter when the view is destroyed

**Context:**
```java
     288|
     289|    @Override
     290|    public void onDestroyView() {
>>>  291|        super.onDestroyView();
     292|        // Clean up resources held by the SimpleSoraTabAdapter when the view is destroyed
     293|        if (tabAdapter != null) {
     294|            tabAdapter.cleanup();
     295|        }
```

---

## com/codex/apk/CodeXApplication.java

**Location:** `app/src/main/java/com/codex/apk/CodeXApplication.java`

### Line 22
**Comment:** Set up theme based on user preferences at app startup

**Context:**
```java
      18|    public void onCreate() {
      19|        super.onCreate();
      20|        instance = this;
>>>   21|
      22|        // Set up theme based on user preferences at app startup
      23|        ThemeManager.setupTheme(this);
      24|
      25|        // Set up crash handler
```

### Line 25
**Comment:** Set up crash handler

**Context:**
```java
      21|
      22|        // Set up theme based on user preferences at app startup
      23|        ThemeManager.setupTheme(this);
>>>   24|
      25|        // Set up crash handler
      26|        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      27|            @Override
      28|            public void uncaughtException(Thread thread, Throwable e) {
```

---

## com/codex/apk/DeepInfraApiClient.java

**Location:** `app/src/main/java/com/codex/apk/DeepInfraApiClient.java`

### Line 163
**Comment:** Not a valid plan, treat as text

**Context:**
```java
     159|                    if (parsed != null && parsed.isValid) {
     160|                        List<ChatMessage.FileActionDetail> fileActions = QwenResponseParser.toFileActionDetails(parsed);
     161|                        actionListener.onAiActionsProcessed(jsonToParse, parsed.explanation, new ArrayList<>(), fileActions, modelDisplayName);
>>>  162|                    } else {
     163|                        // Not a valid plan, treat as text
     164|                        actionListener.onAiActionsProcessed(finalText.toString(), finalText.toString(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), modelDisplayName);
     165|                    }
     166|                } catch (Exception e) {
```

### Line 171
**Comment:** No JSON found, treat as plain text

**Context:**
```java
     167|                    Log.w(TAG, "DeepInfra JSON parse failed, treating as text: " + e.getMessage());
     168|                    actionListener.onAiActionsProcessed(finalText.toString(), finalText.toString(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), modelDisplayName);
     169|                }
>>>  170|            } else {
     171|                // No JSON found, treat as plain text
     172|                actionListener.onAiActionsProcessed(finalText.toString(), finalText.toString(), new java.util.ArrayList<>(), new java.util.ArrayList<>(), modelDisplayName);
     173|            }
     174|        }
```

### Line 283
**Comment:** Look for ```json ... ``` pattern

**Context:**
```java
     279|        if (content == null || content.trim().isEmpty()) {
     280|            return null;
     281|        }
>>>  282|
     283|        // Look for ```json ... ``` pattern
     284|        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
     285|        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
     286|        java.util.regex.Matcher matcher = pattern.matcher(content);
```

### Line 292
**Comment:** Also check for ``` ... ``` pattern (without json specifier)

**Context:**
```java
     288|        if (matcher.find()) {
     289|            return matcher.group(1).trim();
     290|        }
>>>  291|
     292|        // Also check for ``` ... ``` pattern (without json specifier)
     293|        String genericPattern = "```\\s*([\\s\\S]*?)```";
     294|        pattern = java.util.regex.Pattern.compile(genericPattern);
     295|        matcher = pattern.matcher(content);
```

### Line 299
**Comment:** Check if the extracted content looks like JSON

**Context:**
```java
     295|        matcher = pattern.matcher(content);
     296|
     297|        if (matcher.find()) {
>>>  298|            String extracted = matcher.group(1).trim();
     299|            // Check if the extracted content looks like JSON
     300|            if (looksLikeJson(extracted)) {
     301|                return extracted;
     302|            }
```

---

## com/codex/apk/DeepseekParser.java

**Location:** `app/src/main/java/com/codex/apk/DeepseekParser.java`

### Line 21
**Comment:** Parse the JSON response

**Context:**
```java
      17|    }
      18|
      19|    public String parseDeepseekResponse(String responseBody) {
>>>   20|        try {
      21|            // Parse the JSON response
      22|            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
      23|
      24|            // Extract the assistant's message content
```

### Line 24
**Comment:** Extract the assistant's message content

**Context:**
```java
      20|        try {
      21|            // Parse the JSON response
      22|            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
>>>   23|
      24|            // Extract the assistant's message content
      25|            if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
      26|                JsonObject firstChoice = jsonResponse.getAsJsonArray("choices")
      27|                    .get(0).getAsJsonObject();
```

### Line 35
**Comment:** The content contains our JSON response - extract it

**Context:**
```java
      31|
      32|                    if (message.has("content")) {
      33|                        String content = message.get("content").getAsString().trim();
>>>   34|
      35|                        // The content contains our JSON response - extract it
      36|                        try {
      37|                            // Remove any non-JSON content before parsing
      38|                            int jsonStart = content.indexOf("{");
```

### Line 37
**Comment:** Remove any non-JSON content before parsing

**Context:**
```java
      33|                        String content = message.get("content").getAsString().trim();
      34|
      35|                        // The content contains our JSON response - extract it
>>>   36|                        try {
      37|                            // Remove any non-JSON content before parsing
      38|                            int jsonStart = content.indexOf("{");
      39|                            int jsonEnd = content.lastIndexOf("}") + 1;
      40|                            if (jsonStart != -1 && jsonEnd != -1) {
```

### Line 49
**Comment:** Fallback to returning the raw content if JSON parsing fails

**Context:**
```java
      45|                        } catch (JsonSyntaxException e) {
      46|                            Log.e(TAG, "Failed to parse content JSON", e);
      47|                        }
>>>   48|
      49|                        // Fallback to returning the raw content if JSON parsing fails
      50|                        return content;
      51|                    }
      52|                }
```

### Line 55
**Comment:** If we couldn't find the expected structure, log and return raw response

**Context:**
```java
      51|                    }
      52|                }
      53|            }
>>>   54|
      55|            // If we couldn't find the expected structure, log and return raw response
      56|            Log.w(TAG, "Unexpected Deepseek response format");
      57|            copyToClipboard("Deepseek Raw Response", responseBody);
      58|            return responseBody;
```

---

## com/codex/apk/DialogHelper.java

**Location:** `app/src/main/java/com/codex/apk/DialogHelper.java`

### Line 64
**Comment:** List of available font families with their display names

**Context:**
```java
      60|		});
      61|	}
      62|
>>>   63|	public void showFontFamilyDialog(String currentFontFamily, FontFamilySelectionListener listener) {
      64|		// List of available font families with their display names
      65|		String[] fontFamilies = new String[]{"Poppins", "Fira Code", "JetBrains Mono"};
      66|		String[] fontFamilyValues = new String[]{"poppins", "firacode", "jetbrainsmono"};
      67|
```

### Line 68
**Comment:** Find current selection index

**Context:**
```java
      64|		// List of available font families with their display names
      65|		String[] fontFamilies = new String[]{"Poppins", "Fira Code", "JetBrains Mono"};
      66|		String[] fontFamilyValues = new String[]{"poppins", "firacode", "jetbrainsmono"};
>>>   67|
      68|		// Find current selection index
      69|		int currentSelection = 0;
      70|		for (int i = 0; i < fontFamilyValues.length; i++) {
      71|			if (fontFamilyValues[i].equals(currentFontFamily)) {
```

### Line 80
**Comment:** Do nothing here, we'll handle selection in positive button

**Context:**
```java
      76|
      77|		new MaterialAlertDialogBuilder(context)
      78|		.setTitle("Select Font Family")
>>>   79|		.setSingleChoiceItems(fontFamilies, currentSelection, (dialog, which) -> {
      80|			// Do nothing here, we'll handle selection in positive button
      81|		})
      82|		.setPositiveButton("Apply", (dialog, which) -> {
      83|			int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
```

### Line 156
**Comment:** Pre-fill existing value (if any) so users can update it easily

**Context:**
```java
     152|	public void showApiKeyDialog(String preferenceKey, String dialogTitle, Runnable onSave) {
     153|		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_api_key, null);
     154|		TextInputEditText editTextApiKey = dialogView.findViewById(R.id.edittext_api_key);
>>>  155|
     156|		// Pre-fill existing value (if any) so users can update it easily
     157|		SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
     158|		String existing = prefs.getString(preferenceKey, "");
     159|		editTextApiKey.setText(existing);
```

### Line 182
**Comment:** Legacy helper – defaults to Gemini key for backward compatibility

**Context:**
```java
     178|			}
     179|		});
     180|	}
>>>  181|
     182|	// Legacy helper – defaults to Gemini key for backward compatibility
     183|	public void showApiKeyDialog(Runnable onSave) {
     184|		showApiKeyDialog("gemini_api_key", "Set API Key", onSave);
     185|	}
```

---

## com/codex/apk/DiffGenerator.java

**Location:** `app/src/main/java/com/codex/apk/DiffGenerator.java`

### Line 23
**Comment:** Myers diff

**Context:**
```java
      19|    public static String generateUnifiedDiff(String oldContent, String newContent, String oldFile, String newFile) {
      20|        try {
      21|            String[] a = oldContent.split("\n", -1);
>>>   22|            String[] b = newContent.split("\n", -1);
      23|            // Myers diff
      24|            List<Edit> edits = myersDiff(a, b);
      25|            StringBuilder out = new StringBuilder();
      26|            out.append("--- ").append(oldFile).append("\n");
```

### Line 111
**Comment:** --- Minimal Myers diff implementation for line sequences ---

**Context:**
```java
     107|    public static String generateDiff(String oldContent, String newContent) {
     108|        return generateUnifiedDiff(oldContent, newContent, "original", "modified");
     109|    }
>>>  110|
     111|    // --- Minimal Myers diff implementation for line sequences ---
     112|    private static class Edit {
     113|        int aStart, aEnd, bStart, bEnd;
     114|        Edit(int aStart, int aEnd, int bStart, int bEnd) {
```

### Line 196
**Comment:** Context before

**Context:**
```java
     192|            Hunk h = new Hunk();
     193|            h.aStart = aStart; h.bStart = bStart;
     194|            h.aLen = Math.max(0, aEnd - aStart);
>>>  195|            h.bLen = Math.max(0, bEnd - bStart);
     196|            // Context before
     197|            for (int i = aStart, j = bStart; i < e.aStart && j < e.bStart; i++, j++) h.lines.add(" " + a[i]);
     198|            // Deletions
     199|            for (int i = e.aStart; i < e.aEnd; i++) h.lines.add("-" + a[i]);
```

### Line 198
**Comment:** Deletions

**Context:**
```java
     194|            h.aLen = Math.max(0, aEnd - aStart);
     195|            h.bLen = Math.max(0, bEnd - bStart);
     196|            // Context before
>>>  197|            for (int i = aStart, j = bStart; i < e.aStart && j < e.bStart; i++, j++) h.lines.add(" " + a[i]);
     198|            // Deletions
     199|            for (int i = e.aStart; i < e.aEnd; i++) h.lines.add("-" + a[i]);
     200|            // Insertions
     201|            for (int j = e.bStart; j < e.bEnd; j++) h.lines.add("+" + b[j]);
```

### Line 200
**Comment:** Insertions

**Context:**
```java
     196|            // Context before
     197|            for (int i = aStart, j = bStart; i < e.aStart && j < e.bStart; i++, j++) h.lines.add(" " + a[i]);
     198|            // Deletions
>>>  199|            for (int i = e.aStart; i < e.aEnd; i++) h.lines.add("-" + a[i]);
     200|            // Insertions
     201|            for (int j = e.bStart; j < e.bEnd; j++) h.lines.add("+" + b[j]);
     202|            // Context after
     203|            for (int i = e.aEnd, j = e.bEnd; i < aEnd && j < bEnd; i++, j++) h.lines.add(" " + a[i]);
```

### Line 202
**Comment:** Context after

**Context:**
```java
     198|            // Deletions
     199|            for (int i = e.aStart; i < e.aEnd; i++) h.lines.add("-" + a[i]);
     200|            // Insertions
>>>  201|            for (int j = e.bStart; j < e.bEnd; j++) h.lines.add("+" + b[j]);
     202|            // Context after
     203|            for (int i = e.aEnd, j = e.bEnd; i < aEnd && j < bEnd; i++, j++) h.lines.add(" " + a[i]);
     204|            hunks.add(h);
     205|        }
```

---

## com/codex/apk/DiffUtils.java

**Location:** `app/src/main/java/com/codex/apk/DiffUtils.java`

### Line 119
**Comment:** LCS DP

**Context:**
```java
     115|        if (newContent == null) newContent = "";
     116|        String[] a = oldContent.split("\n", -1);
     117|        String[] b = newContent.split("\n", -1);
>>>  118|        int n = a.length, m = b.length;
     119|        // LCS DP
     120|        int[][] dp = new int[n + 1][m + 1];
     121|        for (int i = 1; i <= n; i++) {
     122|            for (int j = 1; j <= m; j++) {
```

---

## com/codex/apk/EditorActivity.java

**Location:** `app/src/main/java/com/codex/apk/EditorActivity.java`

### Line 38
**Comment:** EditorActivity now implements the listeners for its child fragments,

**Context:**
```java
      34|import java.util.List;
      35|import java.util.concurrent.ExecutorService;
      36|import java.util.concurrent.Executors;
>>>   37|
      38|// EditorActivity now implements the listeners for its child fragments,
      39|// but delegates the actual logic to the new manager classes.
      40|public class EditorActivity extends AppCompatActivity implements
      41|        CodeEditorFragment.CodeEditorFragmentListener,
```

### Line 39
**Comment:** but delegates the actual logic to the new manager classes.

**Context:**
```java
      35|import java.util.concurrent.ExecutorService;
      36|import java.util.concurrent.Executors;
      37|
>>>   38|// EditorActivity now implements the listeners for its child fragments,
      39|// but delegates the actual logic to the new manager classes.
      40|public class EditorActivity extends AppCompatActivity implements
      41|        CodeEditorFragment.CodeEditorFragmentListener,
      42|        AIChatFragment.AIChatFragmentListener {
```

### Line 46
**Comment:** Managers

**Context:**
```java
      42|        AIChatFragment.AIChatFragmentListener {
      43|
      44|    private static final String TAG = "EditorActivity";
>>>   45|
      46|    // Managers
      47|    public EditorUiManager uiManager; // Made public for external access from managers
      48|    public FileTreeManager fileTreeManager; // Made public for external access from managers
      49|    public TabManager tabManager; // Made public for external access from managers
```

### Line 52
**Comment:** References to the fragments hosted in the main ViewPager2 (still needed for direct calls from Activity)

**Context:**
```java
      48|    public FileTreeManager fileTreeManager; // Made public for external access from managers
      49|    public TabManager tabManager; // Made public for external access from managers
      50|    public AiAssistantManager aiAssistantManager; // Made public for external access from managers
>>>   51|
      52|    // References to the fragments hosted in the main ViewPager2 (still needed for direct calls from Activity)
      53|    private CodeEditorFragment codeEditorFragment;
      54|    private AIChatFragment aiChatFragment;
      55|
```

### Line 56
**Comment:** Core project properties (still kept here as they define the context of the activity)

**Context:**
```java
      52|    // References to the fragments hosted in the main ViewPager2 (still needed for direct calls from Activity)
      53|    private CodeEditorFragment codeEditorFragment;
      54|    private AIChatFragment aiChatFragment;
>>>   55|
      56|    // Core project properties (still kept here as they define the context of the activity)
      57|    private String projectPath;
      58|    private String projectName;
      59|    private File projectDir;
```

### Line 69
**Comment:** In onCreate or fragment setup logic, ensure chat fragment is attached and visible

**Context:**
```java
      65|    private List<File> pendingFilesToOpen = new ArrayList<>();
      66|    private String pendingDiffFileName;
      67|    private String pendingDiffContent;
>>>   68|
      69|    // In onCreate or fragment setup logic, ensure chat fragment is attached and visible
      70|    // Remove ensureChatFragment and its call in onCreate, as there is no fragment_container_chat in the layout.
      71|
      72|    public ExecutorService getExecutorService() {
```

### Line 70
**Comment:** Remove ensureChatFragment and its call in onCreate, as there is no fragment_container_chat in the layout.

**Context:**
```java
      66|    private String pendingDiffFileName;
      67|    private String pendingDiffContent;
      68|
>>>   69|    // In onCreate or fragment setup logic, ensure chat fragment is attached and visible
      70|    // Remove ensureChatFragment and its call in onCreate, as there is no fragment_container_chat in the layout.
      71|
      72|    public ExecutorService getExecutorService() {
      73|        return executorService;
```

### Line 79
**Comment:** Set up theme based on user preferences

**Context:**
```java
      75|
      76|    @Override
      77|    protected void onCreate(Bundle savedInstanceState) {
>>>   78|        super.onCreate(savedInstanceState);
      79|        // Set up theme based on user preferences
      80|        ThemeManager.setupTheme(this);
      81|        setContentView(R.layout.editor);
      82|
```

### Line 83
**Comment:** Initialize core utilities

**Context:**
```java
      79|        // Set up theme based on user preferences
      80|        ThemeManager.setupTheme(this);
      81|        setContentView(R.layout.editor);
>>>   82|
      83|        // Initialize core utilities
      84|        executorService = Executors.newCachedThreadPool();
      85|        projectPath = getIntent().getStringExtra("projectPath");
      86|        projectName = getIntent().getStringExtra("projectName");
```

### Line 102
**Comment:** DialogHelper will need references to the new managers for its callbacks, and it needs EditorActivity

**Context:**
```java
      98|            return;
      99|        }
     100|
>>>  101|        fileManager = new FileManager(this, projectDir);
     102|        // DialogHelper will need references to the new managers for its callbacks, and it needs EditorActivity
     103|        dialogHelper = new DialogHelper(this, fileManager, this);
     104|
     105|        // Initialize ViewModel
```

### Line 105
**Comment:** Initialize ViewModel

**Context:**
```java
     101|        fileManager = new FileManager(this, projectDir);
     102|        // DialogHelper will need references to the new managers for its callbacks, and it needs EditorActivity
     103|        dialogHelper = new DialogHelper(this, fileManager, this);
>>>  104|
     105|        // Initialize ViewModel
     106|        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(EditorViewModel.class);
     107|
     108|        // Initialize managers, passing necessary dependencies
```

### Line 108
**Comment:** Initialize managers, passing necessary dependencies

**Context:**
```java
     104|
     105|        // Initialize ViewModel
     106|        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(EditorViewModel.class);
>>>  107|
     108|        // Initialize managers, passing necessary dependencies
     109|        // Pass 'this' (EditorActivity) to managers so they can access Activity-level context and methods
     110|        uiManager = new EditorUiManager(this, projectDir, fileManager, dialogHelper, executorService, viewModel.getOpenTabs());
     111|        fileTreeManager = new FileTreeManager(this, fileManager, dialogHelper, viewModel.getFileItems(), viewModel.getOpenTabs());
```

### Line 109
**Comment:** Pass 'this' (EditorActivity) to managers so they can access Activity-level context and methods

**Context:**
```java
     105|        // Initialize ViewModel
     106|        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(EditorViewModel.class);
     107|
>>>  108|        // Initialize managers, passing necessary dependencies
     109|        // Pass 'this' (EditorActivity) to managers so they can access Activity-level context and methods
     110|        uiManager = new EditorUiManager(this, projectDir, fileManager, dialogHelper, executorService, viewModel.getOpenTabs());
     111|        fileTreeManager = new FileTreeManager(this, fileManager, dialogHelper, viewModel.getFileItems(), viewModel.getOpenTabs());
     112|        tabManager = new TabManager(this, fileManager, dialogHelper, viewModel.getOpenTabs());
```

### Line 113
**Comment:** Pass projectDir to AiAssistantManager for FileWatcher initialization

**Context:**
```java
     109|        // Pass 'this' (EditorActivity) to managers so they can access Activity-level context and methods
     110|        uiManager = new EditorUiManager(this, projectDir, fileManager, dialogHelper, executorService, viewModel.getOpenTabs());
     111|        fileTreeManager = new FileTreeManager(this, fileManager, dialogHelper, viewModel.getFileItems(), viewModel.getOpenTabs());
>>>  112|        tabManager = new TabManager(this, fileManager, dialogHelper, viewModel.getOpenTabs());
     113|        // Pass projectDir to AiAssistantManager for FileWatcher initialization
     114|        aiAssistantManager = new AiAssistantManager(this, projectDir, projectName, fileManager, executorService);
     115|
     116|        // Setup components using managers
```

### Line 116
**Comment:** Setup components using managers

**Context:**
```java
     112|        tabManager = new TabManager(this, fileManager, dialogHelper, viewModel.getOpenTabs());
     113|        // Pass projectDir to AiAssistantManager for FileWatcher initialization
     114|        aiAssistantManager = new AiAssistantManager(this, projectDir, projectName, fileManager, executorService);
>>>  115|
     116|        // Setup components using managers
     117|        uiManager.initializeViews();
     118|        uiManager.setupToolbar(); // Toolbar setup is part of UI
     119|        fileTreeManager.setupFileTree(); // File tree setup
```

### Line 121
**Comment:** Setup TabLayout with ViewPager2

**Context:**
```java
     117|        uiManager.initializeViews();
     118|        uiManager.setupToolbar(); // Toolbar setup is part of UI
     119|        fileTreeManager.setupFileTree(); // File tree setup
>>>  120|
     121|        // Setup TabLayout with ViewPager2
     122|        TabLayout tabLayout = findViewById(R.id.tab_layout);
     123|        ViewPager2 viewPager = findViewById(R.id.view_pager);
     124|        MainPagerAdapter adapter = new MainPagerAdapter(this);
```

### Line 128
**Comment:** Connect TabLayout with ViewPager2

**Context:**
```java
     124|        MainPagerAdapter adapter = new MainPagerAdapter(this);
     125|        viewPager.setAdapter(adapter);
     126|        viewPager.setUserInputEnabled(false); // Disable swipe, only tab clicks
>>>  127|
     128|        // Connect TabLayout with ViewPager2
     129|        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
     130|            if (position == 0) {
     131|                tab.setText(getString(R.string.chat));
```

### Line 139
**Comment:** Apply default settings

**Context:**
```java
     135|        }).attach();
     136|    }
     137|
>>>  138|    public void onCodeEditorFragmentReady() {
     139|        // Apply default settings
     140|        boolean wrapEnabled = SettingsActivity.isDefaultWordWrap(this);
     141|        boolean readOnlyEnabled = SettingsActivity.isDefaultReadOnly(this);
     142|        applyWrapToAllTabs(wrapEnabled);
```

### Line 145
**Comment:** Open index.html if no tabs are open initially

**Context:**
```java
     141|        boolean readOnlyEnabled = SettingsActivity.isDefaultReadOnly(this);
     142|        applyWrapToAllTabs(wrapEnabled);
     143|        applyReadOnlyToAllTabs(readOnlyEnabled);
>>>  144|
     145|        // Open index.html if no tabs are open initially
     146|        if (viewModel.getOpenTabs().isEmpty()) {
     147|            File indexHtml = new File(projectDir, "index.html");
     148|            if (indexHtml.exists() && indexHtml.isFile()) {
```

### Line 153
**Comment:** Process any pending files

**Context:**
```java
     149|                tabManager.openFile(indexHtml); // Use tabManager to open file
     150|            }
     151|        }
>>>  152|
     153|        // Process any pending files
     154|        for (File file : pendingFilesToOpen) {
     155|            tabManager.openFile(file);
     156|        }
```

### Line 159
**Comment:** Process any pending diff

**Context:**
```java
     155|            tabManager.openFile(file);
     156|        }
     157|        pendingFilesToOpen.clear();
>>>  158|
     159|        // Process any pending diff
     160|        if (pendingDiffFileName != null && pendingDiffContent != null) {
     161|            tabManager.openDiffTab(pendingDiffFileName, pendingDiffContent);
     162|            pendingDiffFileName = null;
```

### Line 232
**Comment:** Save the setting globally

**Context:**
```java
     228|        } else if (id == R.id.action_toggle_wrap) {
     229|            item.setChecked(!item.isChecked());
     230|            boolean isChecked = item.isChecked();
>>>  231|            applyWrapToAllTabs(isChecked);
     232|            // Save the setting globally
     233|            SettingsActivity.getPreferences(this).edit().putBoolean("default_word_wrap", isChecked).apply();
     234|            return true;
     235|        } else if (id == R.id.action_toggle_read_only) {
```

### Line 239
**Comment:** Save the setting globally

**Context:**
```java
     235|        } else if (id == R.id.action_toggle_read_only) {
     236|            item.setChecked(!item.isChecked());
     237|            boolean isChecked = item.isChecked();
>>>  238|            applyReadOnlyToAllTabs(isChecked);
     239|            // Save the setting globally
     240|            SettingsActivity.getPreferences(this).edit().putBoolean("default_read_only", isChecked).apply();
     241|            return true;
     242|        }
```

### Line 248
**Comment:** Check if drawer should handle back press first

**Context:**
```java
     244|    }
     245|
     246|    @Override
>>>  247|    public void onBackPressed() {
     248|        // Check if drawer should handle back press first
     249|        if (uiManager.onBackPressed()) {
     250|            return;
     251|        }
```

### Line 252
**Comment:** Otherwise delegate to normal back press handling

**Context:**
```java
     248|        // Check if drawer should handle back press first
     249|        if (uiManager.onBackPressed()) {
     250|            return;
>>>  251|        }
     252|        // Otherwise delegate to normal back press handling
     253|        uiManager.handleBackPressed(); // Delegate back press handling
     254|    }
     255|
```

### Line 268
**Comment:** Shutdown AiAssistant to stop FileWatcher

**Context:**
```java
     264|        super.onDestroy();
     265|        if (executorService != null && !executorService.isShutdown()) {
     266|            executorService.shutdownNow();
>>>  267|        }
     268|        // Shutdown AiAssistant to stop FileWatcher
     269|        if (aiAssistantManager != null) {
     270|            aiAssistantManager.shutdown(); // FIX: Call shutdown on AiAssistantManager
     271|        }
```

### Line 274
**Comment:** --- CodeEditorFragmentListener methods implementation (delegating to TabManager and UiManager) ---

**Context:**
```java
     270|            aiAssistantManager.shutdown(); // FIX: Call shutdown on AiAssistantManager
     271|        }
     272|    }
>>>  273|
     274|    // --- CodeEditorFragmentListener methods implementation (delegating to TabManager and UiManager) ---
     275|
     276|    @Override
     277|    public List<TabItem> getOpenTabsList() {
```

### Line 333
**Comment:** No-op

**Context:**
```java
     329|        tabManager.showTabOptionsMenu(anchorView, position); // Delegate to TabManager
     330|    }
     331|
>>>  332|    public void onActiveTabContentChanged(String content, String fileName) {
     333|        // No-op
     334|    }
     335|
     336|    @Override
```

### Line 344
**Comment:** --- AIChatFragmentListener methods implementation (delegating to AiAssistantManager) ---

**Context:**
```java
     340|            fileTreeManager.refreshSelection();
     341|        }
     342|    }
>>>  343|
     344|    // --- AIChatFragmentListener methods implementation (delegating to AiAssistantManager) ---
     345|
     346|    @Override
     347|    public AIAssistant getAIAssistant() {
```

### Line 357
**Comment:** Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted

**Context:**
```java
     353|        tabManager.getActiveTabItem(); // Ensure active tab is retrieved before sending prompt
     354|        aiAssistantManager.sendAiPrompt(userPrompt, chatHistory, qwenState, tabManager.getActiveTabItem()); // Delegate to AiAssistantManager
     355|    }
>>>  356|
     357|    // Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted
     358|    // as these are handled internally by AiAssistantManager's AIActionListener.
     359|    // The AIChatFragment will call these methods directly on itself, and AiAssistantManager's
     360|    // AIActionListener will then update the AIChatFragment.
```

### Line 358
**Comment:** as these are handled internally by AiAssistantManager's AIActionListener.

**Context:**
```java
     354|        aiAssistantManager.sendAiPrompt(userPrompt, chatHistory, qwenState, tabManager.getActiveTabItem()); // Delegate to AiAssistantManager
     355|    }
     356|
>>>  357|    // Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted
     358|    // as these are handled internally by AiAssistantManager's AIActionListener.
     359|    // The AIChatFragment will call these methods directly on itself, and AiAssistantManager's
     360|    // AIActionListener will then update the AIChatFragment.
     361|
```

### Line 359
**Comment:** The AIChatFragment will call these methods directly on itself, and AiAssistantManager's

**Context:**
```java
     355|    }
     356|
     357|    // Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted
>>>  358|    // as these are handled internally by AiAssistantManager's AIActionListener.
     359|    // The AIChatFragment will call these methods directly on itself, and AiAssistantManager's
     360|    // AIActionListener will then update the AIChatFragment.
     361|
     362|    @Override
```

### Line 360
**Comment:** AIActionListener will then update the AIChatFragment.

**Context:**
```java
     356|
     357|    // Removed direct delegation of onAiErrorReceived, onAiRequestStarted, onAiRequestCompleted
     358|    // as these are handled internally by AiAssistantManager's AIActionListener.
>>>  359|    // The AIChatFragment will call these methods directly on itself, and AiAssistantManager's
     360|    // AIActionListener will then update the AIChatFragment.
     361|
     362|    @Override
     363|    public void onAiAcceptActions(int messagePosition, ChatMessage message) {
```

### Line 399
**Comment:** Public methods for managers to call back to EditorActivity for UI updates or core actions

**Context:**
```java
     395|    public void onPlanDiscardClicked(int messagePosition, ChatMessage message) {
     396|        aiAssistantManager.discardPlan(messagePosition, message);
     397|    }
>>>  398|
     399|    // Public methods for managers to call back to EditorActivity for UI updates or core actions
     400|    public void showToast(String message) {
     401|        runOnUiThread(() -> Toast.makeText(EditorActivity.this, message, Toast.LENGTH_SHORT).show());
     402|    }
```

### Line 421
**Comment:** Setters for fragment references, called by MainPagerAdapter

**Context:**
```java
     417|    public ViewPager2 getMainViewPager() {
     418|        return uiManager.getMainViewPager();
     419|    }
>>>  420|
     421|    // Setters for fragment references, called by MainPagerAdapter
     422|    public void setCodeEditorFragment(CodeEditorFragment fragment) {
     423|        this.codeEditorFragment = fragment;
     424|    }
```

### Line 430
**Comment:** Getters for fragment references, used by managers

**Context:**
```java
     426|    public void setAIChatFragment(AIChatFragment fragment) {
     427|        this.aiChatFragment = fragment;
     428|    }
>>>  429|
     430|    // Getters for fragment references, used by managers
     431|    public AIChatFragment getAiChatFragment() {
     432|        return aiChatFragment;
     433|    }
```

### Line 451
**Comment:** Public methods for DialogHelper/FileTreeAdapter to call back to EditorActivity for manager actions

**Context:**
```java
     447|    public QwenConversationState getQwenState() {
     448|        return aiChatFragment != null ? aiChatFragment.getQwenState() : new QwenConversationState();
     449|    }
>>>  450|
     451|    // Public methods for DialogHelper/FileTreeAdapter to call back to EditorActivity for manager actions
     452|    public void showNewFileDialog(File parentDirectory) {
     453|        fileTreeManager.showNewFileDialog(parentDirectory);
     454|    }
```

### Line 502
**Comment:** Launch the new PreviewActivity

**Context:**
```java
     498|            }
     499|        });
     500|    }
>>>  501|
     502|    // Launch the new PreviewActivity
     503|    private void launchPreviewActivity() {
     504|        Intent previewIntent = new Intent(this, PreviewActivity.class);
     505|        previewIntent.putExtra(PreviewActivity.EXTRA_PROJECT_PATH, projectPath);
```

### Line 508
**Comment:** Get current active file content if available

**Context:**
```java
     504|        Intent previewIntent = new Intent(this, PreviewActivity.class);
     505|        previewIntent.putExtra(PreviewActivity.EXTRA_PROJECT_PATH, projectPath);
     506|        previewIntent.putExtra(PreviewActivity.EXTRA_PROJECT_NAME, projectName);
>>>  507|
     508|        // Get current active file content if available
     509|        TabItem activeTab = tabManager.getActiveTabItem();
     510|        if (activeTab != null) {
     511|            previewIntent.putExtra(PreviewActivity.EXTRA_HTML_CONTENT, activeTab.getContent());
```

---

## com/codex/apk/FileActionAdapter.java

**Location:** `app/src/main/java/com/codex/apk/FileActionAdapter.java`

### Line 53
**Comment:** Iterate from end to keep the latest action for each effective path

**Context:**
```java
      49|
      50|    private List<ChatMessage.FileActionDetail> getDisplayActions() {
      51|        List<ChatMessage.FileActionDetail> input = this.fileActions;
>>>   52|        if (input == null || input.isEmpty()) return new ArrayList<>();
      53|        // Iterate from end to keep the latest action for each effective path
      54|        LinkedHashMap<String, ChatMessage.FileActionDetail> map = new LinkedHashMap<>();
      55|        for (int i = input.size() - 1; i >= 0; i--) {
      56|            ChatMessage.FileActionDetail a = input.get(i);
```

### Line 68
**Comment:** map preserves insertion order (from end); reverse to retain overall chronological order

**Context:**
```java
      64|            if (!map.containsKey(key)) {
      65|                map.put(key, a);
      66|            }
>>>   67|        }
      68|        // map preserves insertion order (from end); reverse to retain overall chronological order
      69|        ArrayList<ChatMessage.FileActionDetail> out = new ArrayList<>(map.values());
      70|        java.util.Collections.reverse(out);
      71|        return out;
```

### Line 95
**Comment:** Use full width as provided by RecyclerView layout

**Context:**
```java
      91|            String path = action.type.equals("renameFile") ? action.newPath : action.path;
      92|            textFileName.setText(path);
      93|
>>>   94|            Context context = itemView.getContext();
      95|            // Use full width as provided by RecyclerView layout
      96|            int changeColor;
      97|            int changeTextColor;
      98|            String changeLabel;
```

### Line 138
**Comment:** No status label in the new UI

**Context:**
```java
     134|                background.setColor(changeColor);
     135|            }
     136|            textChangeLabel.setTextColor(changeTextColor);
>>>  137|
     138|            // No status label in the new UI
     139|
     140|            // Diff badges (+ added / - removed lines)
     141|            int added = 0;
```

### Line 140
**Comment:** Diff badges (+ added / - removed lines)

**Context:**
```java
     136|            textChangeLabel.setTextColor(changeTextColor);
     137|
     138|            // No status label in the new UI
>>>  139|
     140|            // Diff badges (+ added / - removed lines)
     141|            int added = 0;
     142|            int removed = 0;
     143|
```

### Line 161
**Comment:** Configure + badge

**Context:**
```java
     157|                added = counts[0];
     158|                removed = counts[1];
     159|            }
>>>  160|
     161|            // Configure + badge
     162|            if (added > 0) {
     163|                textAddedBadge.setVisibility(View.VISIBLE);
     164|                textAddedBadge.setText("+" + added);
```

### Line 165
**Comment:** Plain colored text; no pill background

**Context:**
```java
     161|            // Configure + badge
     162|            if (added > 0) {
     163|                textAddedBadge.setVisibility(View.VISIBLE);
>>>  164|                textAddedBadge.setText("+" + added);
     165|                // Plain colored text; no pill background
     166|            } else {
     167|                textAddedBadge.setVisibility(View.GONE);
     168|            }
```

### Line 170
**Comment:** Configure - badge

**Context:**
```java
     166|            } else {
     167|                textAddedBadge.setVisibility(View.GONE);
     168|            }
>>>  169|
     170|            // Configure - badge
     171|            if (removed > 0) {
     172|                textRemovedBadge.setVisibility(View.VISIBLE);
     173|                textRemovedBadge.setText("-" + removed);
```

### Line 174
**Comment:** Plain colored text; no pill background

**Context:**
```java
     170|            // Configure - badge
     171|            if (removed > 0) {
     172|                textRemovedBadge.setVisibility(View.VISIBLE);
>>>  173|                textRemovedBadge.setText("-" + removed);
     174|                // Plain colored text; no pill background
     175|            } else {
     176|                textRemovedBadge.setVisibility(View.GONE);
     177|            }
```

### Line 191
**Comment:** Removed duplicate unified diff parser; using DiffUtils.countAddRemove

**Context:**
```java
     187|            }
     188|            return lines;
     189|        }
>>>  190|
     191|        // Removed duplicate unified diff parser; using DiffUtils.countAddRemove
     192|    }
     193|}
```

---

## com/codex/apk/FileItem.java

**Location:** `app/src/main/java/com/codex/apk/FileItem.java`

### Line 8
**Comment:** private final FileItem parent; // Not strictly needed by adapter if tree is flat list

**Context:**
```java
       4|
       5|public class FileItem {
       6|    private final File file;
>>>    7|    private final int level;
       8|    // private final FileItem parent; // Not strictly needed by adapter if tree is flat list
       9|    private boolean expanded;
      10|
      11|    public FileItem(File file, int level, FileItem parent) {
```

### Line 14
**Comment:** this.parent = parent;

**Context:**
```java
      10|
      11|    public FileItem(File file, int level, FileItem parent) {
      12|        this.file = file;
>>>   13|        this.level = level;
      14|        // this.parent = parent;
      15|        this.expanded = (level == 0 && file.isDirectory());
      16|    }
      17|
```

---

## com/codex/apk/FileManager.java

**Location:** `app/src/main/java/com/codex/apk/FileManager.java`

### Line 26
**Comment:** File change listener

**Context:**
```java
      22|	private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
      23|	private final Context context;
      24|	private final File projectDir;
>>>   25|
      26|	// File change listener
      27|	public interface FileChangeListener {
      28|		void onFileCreated(File file);
      29|		void onFileModified(File file);
```

---

## com/codex/apk/GeminiFreeApiClient.java

**Location:** `app/src/main/java/com/codex/apk/GeminiFreeApiClient.java`

### Line 71
**Comment:** Load cached 1psidts if missing

**Context:**
```java
      67|            try {
      68|                if (actionListener != null) actionListener.onAiRequestStarted();
      69|                String psid = SettingsActivity.getSecure1PSID(context);
>>>   70|                String psidts = SettingsActivity.getSecure1PSIDTS(context);
      71|                // Load cached 1psidts if missing
      72|                if ((psidts == null || psidts.isEmpty()) && psid != null && !psid.isEmpty()) {
      73|                    String cached = SettingsActivity.getCached1psidts(context, psid);
      74|                    if (cached != null && !cached.isEmpty()) {
```

### Line 95
**Comment:** Start periodic refresh if not running

**Context:**
```java
      91|                    if (actionListener != null) actionListener.onAiError("Failed to retrieve access token from Gemini INIT page");
      92|                    return;
      93|                }
>>>   94|
      95|                // Start periodic refresh if not running
      96|                startAutoRefresh(psid, cookies);
      97|
      98|                // Optionally rotate 1PSIDTS immediately once
```

### Line 98
**Comment:** Optionally rotate 1PSIDTS immediately once

**Context:**
```java
      94|
      95|                // Start periodic refresh if not running
      96|                startAutoRefresh(psid, cookies);
>>>   97|
      98|                // Optionally rotate 1PSIDTS immediately once
      99|                rotate1psidtsIfPossible(cookies);
     100|                // Persist refreshed __Secure-1PSIDTS if present
     101|                if (cookies.containsKey("__Secure-1PSIDTS")) {
```

### Line 100
**Comment:** Persist refreshed __Secure-1PSIDTS if present

**Context:**
```java
      96|                startAutoRefresh(psid, cookies);
      97|
      98|                // Optionally rotate 1PSIDTS immediately once
>>>   99|                rotate1psidtsIfPossible(cookies);
     100|                // Persist refreshed __Secure-1PSIDTS if present
     101|                if (cookies.containsKey("__Secure-1PSIDTS")) {
     102|                    SettingsActivity.setCached1psidts(context, psid, cookies.get("__Secure-1PSIDTS"));
     103|                }
```

### Line 108
**Comment:** Load prior conversation metadata if any, else derive from history if present

**Context:**
```java
     104|
     105|                String modelId = model != null ? model.getModelId() : "gemini-2.5-flash";
     106|                Headers requestHeaders = buildGeminiHeaders(modelId);
>>>  107|
     108|                // Load prior conversation metadata if any, else derive from history if present
     109|                String priorMeta = SettingsActivity.getFreeConversationMetadata(context, modelId);
     110|                String chatMeta = null;
     111|                if (priorMeta != null && !priorMeta.isEmpty()) {
```

### Line 114
**Comment:** Try to derive minimal metadata from last assistant message raw response if available

**Context:**
```java
     110|                String chatMeta = null;
     111|                if (priorMeta != null && !priorMeta.isEmpty()) {
     112|                    chatMeta = priorMeta; // Stored as JSON array string like [cid, rid, rcid]
>>>  113|                } else {
     114|                    // Try to derive minimal metadata from last assistant message raw response if available
     115|                    try {
     116|                        for (int i = history.size() - 1; i >= 0; i--) {
     117|                            ChatMessage m = history.get(i);
```

### Line 126
**Comment:** Upload attachments minimally and build files array entries

**Context:**
```java
     122|                        }
     123|                    } catch (Exception ignore) {}
     124|                }
>>>  125|
     126|                // Upload attachments minimally and build files array entries
     127|                List<File> imageFiles = attachments != null ? attachments : new ArrayList<>();
     128|                List<UploadedRef> uploaded = new ArrayList<>();
     129|                for (File f : imageFiles) {
```

### Line 171
**Comment:** Normalize JSON (plan/file ops) for model-agnostic handlers

**Context:**
```java
     167|                                String body2 = resp2.body().string();
     168|                                ParsedOutput parsed2 = parseOutputFromStream(body2);
     169|                                persistConversationMetaIfAvailable(modelId, body2);
>>>  170|                                String explanation2 = deriveHumanExplanation(parsed2.text, parsed2.thoughts);
     171|                                // Normalize JSON (plan/file ops) for model-agnostic handlers
     172|                                String normalized2 = normalizeJsonIfPresent(parsed2.text);
     173|                                List<String> suggestions2 = new ArrayList<>();
     174|                                List<ChatMessage.FileActionDetail> files2 = new ArrayList<>();
```

### Line 175
**Comment:** Route via richer callback so thinking is separate

**Context:**
```java
     171|                                // Normalize JSON (plan/file ops) for model-agnostic handlers
     172|                                String normalized2 = normalizeJsonIfPresent(parsed2.text);
     173|                                List<String> suggestions2 = new ArrayList<>();
>>>  174|                                List<ChatMessage.FileActionDetail> files2 = new ArrayList<>();
     175|                                // Route via richer callback so thinking is separate
     176|                                notifyAiActionsProcessed(
     177|                                        body2,
     178|                                        explanation2,
```

### Line 185
**Comment:** Cache metadata onto the last chat message raw response to help derive context later

**Context:**
```java
     181|                                        model != null ? model.getDisplayName() : "Gemini (Free)",
     182|                                        parsed2.thoughts,
     183|                                        new ArrayList<>()
>>>  184|                                );
     185|                                // Cache metadata onto the last chat message raw response to help derive context later
     186|                                // (UI manager will receive this via onAiActionsProcessed).
     187|
     188|                                return;
```

### Line 186
**Comment:** (UI manager will receive this via onAiActionsProcessed).

**Context:**
```java
     182|                                        parsed2.thoughts,
     183|                                        new ArrayList<>()
     184|                                );
>>>  185|                                // Cache metadata onto the last chat message raw response to help derive context later
     186|                                // (UI manager will receive this via onAiActionsProcessed).
     187|
     188|                                return;
     189|                            }
```

### Line 196
**Comment:** Stream parse lines for partial updates

**Context:**
```java
     192|                        if (actionListener != null) actionListener.onAiError("Gemini request failed: " + resp.code() + (errBody != null ? ": " + errBody : ""));
     193|                        return;
     194|                    }
>>>  195|
     196|                    // Stream parse lines for partial updates
     197|                    BufferedSource source = resp.body().source();
     198|                    StringBuilder full = new StringBuilder();
     199|                    while (!source.exhausted()) {
```

### Line 203
**Comment:** emit incremental thinking/text when possible

**Context:**
```java
     199|                    while (!source.exhausted()) {
     200|                        String line = source.readUtf8Line();
     201|                        if (line == null) break;
>>>  202|                        full.append(line).append("\n");
     203|                        // emit incremental thinking/text when possible
     204|                        try {
     205|                            String[] parts = full.toString().split("\n");
     206|                            if (parts.length >= 3) {
```

### Line 208
**Comment:** naive partial: look at last part for candidate delta

**Context:**
```java
     204|                        try {
     205|                            String[] parts = full.toString().split("\n");
     206|                            if (parts.length >= 3) {
>>>  207|                                com.google.gson.JsonArray responseJson = JsonParser.parseString(parts[2]).getAsJsonArray();
     208|                                // naive partial: look at last part for candidate delta
     209|                                for (int i = 0; i < responseJson.size(); i++) {
     210|                                    try {
     211|                                        com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
```

### Line 230
**Comment:** Store actual raw response for long-press, but show only the derived explanation

**Context:**
```java
     226|                    ParsedOutput parsed = parseOutputFromStream(body);
     227|                    persistConversationMetaIfAvailable(modelId, body);
     228|                    String explanation = deriveHumanExplanation(parsed.text, parsed.thoughts);
>>>  229|                    if (actionListener != null) {
     230|                        // Store actual raw response for long-press, but show only the derived explanation
     231|                        // Normalize JSON for model-agnostic downstream parsing (plan/file ops)
     232|                        String normalized = normalizeJsonIfPresent(parsed.text);
     233|                        notifyAiActionsProcessed(
```

### Line 231
**Comment:** Normalize JSON for model-agnostic downstream parsing (plan/file ops)

**Context:**
```java
     227|                    persistConversationMetaIfAvailable(modelId, body);
     228|                    String explanation = deriveHumanExplanation(parsed.text, parsed.thoughts);
     229|                    if (actionListener != null) {
>>>  230|                        // Store actual raw response for long-press, but show only the derived explanation
     231|                        // Normalize JSON for model-agnostic downstream parsing (plan/file ops)
     232|                        String normalized = normalizeJsonIfPresent(parsed.text);
     233|                        notifyAiActionsProcessed(
     234|                                body,
```

### Line 255
**Comment:** Static supported list for COOKIES provider (cookie-based Gemini)

**Context:**
```java
     251|    }
     252|
     253|    @Override
>>>  254|    public List<AIModel> fetchModels() {
     255|        // Static supported list for COOKIES provider (cookie-based Gemini)
     256|        List<AIModel> list = new ArrayList<>();
     257|        list.add(new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.COOKIES, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)));
     258|        list.add(new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.COOKIES, new com.codex.apk.ai.ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)));
```

### Line 272
**Comment:** Merge Set-Cookie from INIT into cookies map

**Context:**
```java
     268|                .header("Cookie", buildCookieHeader(cookies))
     269|                .build();
     270|        try (Response resp = httpClient.newCall(init).execute()) {
>>>  271|            if (!resp.isSuccessful() || resp.body() == null) return null;
     272|            // Merge Set-Cookie from INIT into cookies map
     273|            if (resp.headers("Set-Cookie") != null) {
     274|                for (String c : resp.headers("Set-Cookie")) {
     275|                    String[] parts = c.split(";", 2);
```

### Line 281
**Comment:** Extract "SNlM0e":"<token>"

**Context:**
```java
     277|                    if (kv.length == 2) cookies.put(kv[0], kv[1]);
     278|                }
     279|            }
>>>  280|            String html = resp.body().string();
     281|            // Extract "SNlM0e":"<token>"
     282|            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"SNlM0e\":\"(.*?)\"").matcher(html);
     283|            if (m.find()) {
     284|                return m.group(1);
```

### Line 376
**Comment:** Per-model header similar to x-goog-ext-525001261-jspb in reference; minimal without it may still work for flash.

**Context:**
```java
     372|        headers.put("Referer", "https://gemini.google.com/");
     373|        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
     374|        headers.put("X-Same-Domain", "1");
>>>  375|        headers.put("Accept", "*/*");
     376|        // Per-model header similar to x-goog-ext-525001261-jspb in reference; minimal without it may still work for flash.
     377|        if ("gemini-2.5-flash".equals(modelId)) {
     378|            headers.put("x-goog-ext-525001261-jspb", "[1,null,null,null,\"71c2d248d3b102ff\"]");
     379|        } else if ("gemini-2.5-pro".equals(modelId)) {
```

### Line 390
**Comment:** files payload: [ prompt, 0, null, [ [ [id], name ], ... ] ]

**Context:**
```java
     386|
     387|    private RequestBody buildGenerateForm(String accessToken, String prompt, String chatMetadataJsonArray, List<UploadedRef> uploaded) {
     388|        JsonArray inner = new JsonArray();
>>>  389|        if (uploaded != null && !uploaded.isEmpty()) {
     390|            // files payload: [ prompt, 0, null, [ [ [id], name ], ... ] ]
     391|            JsonArray filesEntry = new JsonArray();
     392|            filesEntry.add(prompt);
     393|            filesEntry.add(0);
```

### Line 400
**Comment:** Expected shape per python client: [ [id], name ]

**Context:**
```java
     396|            for (UploadedRef ur : uploaded) {
     397|                JsonArray item = new JsonArray();
     398|                JsonArray idArr = new JsonArray();
>>>  399|                idArr.add(ur.id);
     400|                // Expected shape per python client: [ [id], name ]
     401|                item.add(idArr);
     402|                item.add(ur.name);
     403|                filesArray.add(item);
```

### Line 412
**Comment:** second element must be null placeholder

**Context:**
```java
     408|            JsonArray promptArray = new JsonArray();
     409|            promptArray.add(prompt);
     410|            inner.add(promptArray);
>>>  411|        }
     412|        // second element must be null placeholder
     413|        inner.add(com.google.gson.JsonNull.INSTANCE);
     414|        // third element: chat metadata array (e.g., [cid, rid, rcid])
     415|        if (chatMetadataJsonArray != null && !chatMetadataJsonArray.isEmpty()) {
```

### Line 414
**Comment:** third element: chat metadata array (e.g., [cid, rid, rcid])

**Context:**
```java
     410|            inner.add(promptArray);
     411|        }
     412|        // second element must be null placeholder
>>>  413|        inner.add(com.google.gson.JsonNull.INSTANCE);
     414|        // third element: chat metadata array (e.g., [cid, rid, rcid])
     415|        if (chatMetadataJsonArray != null && !chatMetadataJsonArray.isEmpty()) {
     416|            try {
     417|                inner.add(JsonParser.parseString(chatMetadataJsonArray).getAsJsonArray());
```

### Line 430
**Comment:** Gemini expects normal form encoding (not URL double-encoded JSON). Use add instead of addEncoded.

**Context:**
```java
     426|        outer.add(com.google.gson.JsonNull.INSTANCE);
     427|        outer.add(jsonInner);
     428|        String fReq = outer.toString();
>>>  429|
     430|        // Gemini expects normal form encoding (not URL double-encoded JSON). Use add instead of addEncoded.
     431|        return new FormBody.Builder()
     432|                .add("at", accessToken)
     433|                .add("f.req", fReq)
```

### Line 437
**Comment:** Python-style parsing adapted: returns text and thoughts when available

**Context:**
```java
     433|                .add("f.req", fReq)
     434|                .build();
     435|    }
>>>  436|
     437|    // Python-style parsing adapted: returns text and thoughts when available
     438|    private ParsedOutput parseOutputFromStream(String responseText) throws IOException {
     439|        try {
     440|            String[] lines = responseText.split("\n");
```

### Line 461
**Comment:** First candidate

**Context:**
```java
     457|
     458|            com.google.gson.JsonArray candidates = body.get(4).getAsJsonArray();
     459|            if (candidates.size() == 0) throw new IOException("No candidates");
>>>  460|
     461|            // First candidate
     462|            com.google.gson.JsonArray cand = candidates.get(0).getAsJsonArray();
     463|            String text = cand.get(1).getAsJsonArray().get(0).getAsString();
     464|            if (text.matches("^http://googleusercontent\\.com/card_content/\\d+")) {
```

### Line 500
**Comment:** Unknown JSON: do not echo raw JSON in the bubble

**Context:**
```java
     496|                        return expl;
     497|                    }
     498|                }
>>>  499|            } catch (Exception ignore) {}
     500|            // Unknown JSON: do not echo raw JSON in the bubble
     501|            return "";
     502|        }
     503|        // Non-JSON text: return as-is (thinking is shown separately by UI)
```

### Line 503
**Comment:** Non-JSON text: return as-is (thinking is shown separately by UI)

**Context:**
```java
     499|            } catch (Exception ignore) {}
     500|            // Unknown JSON: do not echo raw JSON in the bubble
     501|            return "";
>>>  502|        }
     503|        // Non-JSON text: return as-is (thinking is shown separately by UI)
     504|        return text.trim();
     505|    }
     506|
```

### Line 510
**Comment:** Strip leading code fence markers or 'json\n'

**Context:**
```java
     506|
     507|    private String normalizeJsonIfPresent(String text) {
     508|        if (text == null) return null;
>>>  509|        String t = text.trim();
     510|        // Strip leading code fence markers or 'json\n'
     511|        if (t.startsWith("```") ) {
     512|            int firstBrace = t.indexOf('{');
     513|            int lastBrace = t.lastIndexOf('}');
```

### Line 520
**Comment:** Extract object substring if present

**Context:**
```java
     516|            }
     517|        } else if (t.startsWith("json\n")) {
     518|            t = t.substring(5);
>>>  519|        }
     520|        // Extract object substring if present
     521|        int start = t.indexOf('{');
     522|        int end = t.lastIndexOf('}');
     523|        if (start >= 0 && end > start) {
```

### Line 562
**Comment:** body structure has metadata at [1] -> [cid, rid, rcid] possibly

**Context:**
```java
     558|            com.google.gson.JsonArray responseJson = JsonParser.parseString(lines[2]).getAsJsonArray();
     559|            for (int i = 0; i < responseJson.size(); i++) {
     560|                try {
>>>  561|                    com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
     562|                    // body structure has metadata at [1] -> [cid, rid, rcid] possibly
     563|                    if (part.size() > 1 && part.get(1).isJsonArray()) {
     564|                        String meta = part.get(1).toString();
     565|                        if (meta != null && meta.length() > 2) { // simple validity check
```

### Line 585
**Comment:** Return JSON string of [cid, rid, rcid] (can be shorter)

**Context:**
```java
     581|                try {
     582|                    com.google.gson.JsonArray part = JsonParser.parseString(responseJson.get(i).getAsJsonArray().get(2).getAsString()).getAsJsonArray();
     583|                    if (part.size() > 1 && part.get(1).isJsonArray()) {
>>>  584|                        com.google.gson.JsonArray metaArr = part.get(1).getAsJsonArray();
     585|                        // Return JSON string of [cid, rid, rcid] (can be shorter)
     586|                        return metaArr.toString();
     587|                    }
     588|                } catch (Exception ignore) {}
```

---

## com/codex/apk/GeminiOfficialApiClient.java

**Location:** `app/src/main/java/com/codex/apk/GeminiOfficialApiClient.java`

### Line 38
**Comment:** API key is optional at construction and can be set later.

**Context:**
```java
      34|    private final Context context;
      35|    private final AIAssistant.AIActionListener actionListener;
      36|    private final OkHttpClient http;
>>>   37|
      38|    // API key is optional at construction and can be set later.
      39|    private volatile String apiKey;
      40|
      41|    public GeminiOfficialApiClient(Context context, AIAssistant.AIActionListener actionListener, String apiKey) {
```

### Line 69
**Comment:** Fallback to Settings if not set via setter

**Context:**
```java
      65|            if (actionListener != null) actionListener.onAiRequestStarted();
      66|            try {
      67|                String key = apiKey;
>>>   68|                if (key == null || key.isEmpty()) {
      69|                    // Fallback to Settings if not set via setter
      70|                    key = SettingsActivity.getGeminiApiKey(context);
      71|                }
      72|                if (key == null || key.isEmpty()) {
```

### Line 80
**Comment:** Build request JSON. We prepend the system prompt to the user content (simple and robust).

**Context:**
```java
      76|
      77|                String modelId = model != null ? model.getModelId() : "gemini-1.5-flash";
      78|                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId + ":generateContent?key=" + key;
>>>   79|
      80|                // Build request JSON. We prepend the system prompt to the user content (simple and robust).
      81|                JsonObject req = new JsonObject();
      82|                JsonArray contents = new JsonArray();
      83|                JsonObject userMsg = new JsonObject();
```

### Line 106
**Comment:** No web sources for now; could parse citations later.

**Context:**
```java
     102|                    String body = resp.body().string();
     103|                    Parsed parsed = parseGenerateContent(body);
     104|
>>>  105|                    String explanation = deriveHumanExplanation(parsed.text);
     106|                    // No web sources for now; could parse citations later.
     107|                    List<String> suggestions = new ArrayList<>();
     108|                    List<ChatMessage.FileActionDetail> files = new ArrayList<>();
     109|
```

### Line 131
**Comment:** Return static models for GOOGLE provider from AIModel registry.

**Context:**
```java
     127|    }
     128|
     129|    @Override
>>>  130|    public List<AIModel> fetchModels() {
     131|        // Return static models for GOOGLE provider from AIModel registry.
     132|        List<AIModel> all = com.codex.apk.ai.AIModel.values();
     133|        List<AIModel> google = new ArrayList<>();
     134|        for (AIModel m : all) {
```

### Line 149
**Comment:** Candidates -> [ { content: { parts: [ {text: ...}, ... ] } } ]

**Context:**
```java
     145|
     146|    private Parsed parseGenerateContent(String body) throws IOException {
     147|        try {
>>>  148|            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
     149|            // Candidates -> [ { content: { parts: [ {text: ...}, ... ] } } ]
     150|            StringBuilder sb = new StringBuilder();
     151|            if (root.has("candidates") && root.get("candidates").isJsonArray()) {
     152|                JsonArray cands = root.getAsJsonArray("candidates");
```

---

## com/codex/apk/GeminiParser.java

**Location:** `app/src/main/java/com/codex/apk/GeminiParser.java`

### Line 31
**Comment:** Enhanced JSON extraction

**Context:**
```java
      27|					JsonArray responseParts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
      28|					if (responseParts.size() > 0 && responseParts.get(0).getAsJsonObject().has("text")) {
      29|						String responseText = responseParts.get(0).getAsJsonObject().get("text").getAsString();
>>>   30|
      31|						// Enhanced JSON extraction
      32|						if (responseText.contains("\"actions\"") && responseText.contains("\"explanation\"")) {
      33|							int start = responseText.indexOf("{");
      34|							int end = responseText.lastIndexOf("}");
```

---

## com/codex/apk/GitManager.java

**Location:** `app/src/main/java/com/codex/apk/GitManager.java`

### Line 40
**Comment:** Validate URL

**Context:**
```java
      36|
      37|    public void cloneRepository(String repositoryUrl, String projectName, GitCloneCallback callback) {
      38|        new Thread(() -> {
>>>   39|            try {
      40|                // Validate URL
      41|                if (!isValidGitUrl(repositoryUrl)) {
      42|                    callback.onError(context.getString(R.string.invalid_repository_url));
      43|                    return;
```

### Line 46
**Comment:** Check if project already exists

**Context:**
```java
      42|                    callback.onError(context.getString(R.string.invalid_repository_url));
      43|                    return;
      44|                }
>>>   45|
      46|                // Check if project already exists
      47|                File projectDir = new File(projectsDir, projectName);
      48|                if (projectDir.exists()) {
      49|                    callback.onError(context.getString(R.string.project_with_this_name_already_exists));
```

### Line 55
**Comment:** Clone the repository

**Context:**
```java
      51|                }
      52|
      53|                callback.onProgress(context.getString(R.string.cloning_repository), 0);
>>>   54|
      55|                // Clone the repository
      56|                Git.cloneRepository()
      57|                    .setURI(repositoryUrl)
      58|                    .setDirectory(projectDir)
```

### Line 85
**Comment:** Reset current task counters

**Context:**
```java
      81|                        }
      82|
      83|                        @Override
>>>   84|                        public void beginTask(String title, int totalWork) {
      85|                            // Reset current task counters
      86|                            currentTaskTotal = totalWork > 0 ? totalWork : 0;
      87|                            currentTaskDone = 0;
      88|                            int pct = computeProgressPercent();
```

### Line 94
**Comment:** Increment current task progress; avoid overflow

**Context:**
```java
      90|                        }
      91|
      92|                        @Override
>>>   93|                        public void update(int completed) {
      94|                            // Increment current task progress; avoid overflow
      95|                            if (completed > 0 && currentTaskTotal > 0) {
      96|                                currentTaskDone = Math.min(currentTaskTotal, currentTaskDone + completed);
      97|                            }
```

### Line 104
**Comment:** Mark current task as completed

**Context:**
```java
     100|                        }
     101|
     102|                        @Override
>>>  103|                        public void endTask() {
     104|                            // Mark current task as completed
     105|                            if (tasksCompleted < totalTasks) {
     106|                                tasksCompleted++;
     107|                            }
```

### Line 120
**Comment:** Verify the clone was successful

**Context:**
```java
     116|                    .call();
     117|
     118|                callback.onProgress("Finalizing...", 90);
>>>  119|
     120|                // Verify the clone was successful
     121|                if (projectDir.exists() && new File(projectDir, ".git").exists()) {
     122|                    callback.onProgress("Clone completed!", 100);
     123|                    callback.onSuccess(projectDir.getAbsolutePath(), projectName);
```

### Line 146
**Comment:** Compute overall percentage based on tasks and current task progress

**Context:**
```java
     142|    private int clamp(int value, int min, int max) {
     143|        return Math.max(min, Math.min(max, value));
     144|    }
>>>  145|
     146|    // Compute overall percentage based on tasks and current task progress
     147|    private int computePercentInternal(int totalTasks, int tasksCompleted, int currentTaskDone, int currentTaskTotal) {
     148|        if (totalTasks <= 0) return clamp(currentTaskTotal > 0 ? (int) (100.0 * currentTaskDone / Math.max(1, currentTaskTotal)) : 0, 0, 99);
     149|        double perTask = 100.0 / totalTasks;
```

### Line 157
**Comment:** Wrapper to compute using the instance fields within ProgressMonitor

**Context:**
```java
     153|        }
     154|        return clamp((int) progress, 0, 99);
     155|    }
>>>  156|
     157|    // Wrapper to compute using the instance fields within ProgressMonitor
     158|    private int computeProgressPercent() {
     159|        // This method body will be overridden at runtime by the anonymous ProgressMonitor's context,
     160|        // but the compiler requires it here. Not used outside the monitor.
```

### Line 159
**Comment:** This method body will be overridden at runtime by the anonymous ProgressMonitor's context,

**Context:**
```java
     155|    }
     156|
     157|    // Wrapper to compute using the instance fields within ProgressMonitor
>>>  158|    private int computeProgressPercent() {
     159|        // This method body will be overridden at runtime by the anonymous ProgressMonitor's context,
     160|        // but the compiler requires it here. Not used outside the monitor.
     161|        return 0;
     162|    }
```

### Line 160
**Comment:** but the compiler requires it here. Not used outside the monitor.

**Context:**
```java
     156|
     157|    // Wrapper to compute using the instance fields within ProgressMonitor
     158|    private int computeProgressPercent() {
>>>  159|        // This method body will be overridden at runtime by the anonymous ProgressMonitor's context,
     160|        // but the compiler requires it here. Not used outside the monitor.
     161|        return 0;
     162|    }
     163|
```

### Line 169
**Comment:** Check for common Git URL patterns

**Context:**
```java
     165|        if (url == null || url.trim().isEmpty()) {
     166|            return false;
     167|        }
>>>  168|
     169|        // Check for common Git URL patterns
     170|        String trimmedUrl = url.trim();
     171|
     172|        // HTTPS URLs
```

### Line 172
**Comment:** HTTPS URLs

**Context:**
```java
     168|
     169|        // Check for common Git URL patterns
     170|        String trimmedUrl = url.trim();
>>>  171|
     172|        // HTTPS URLs
     173|        if (trimmedUrl.startsWith("https://")) {
     174|            return trimmedUrl.contains("github.com") ||
     175|                   trimmedUrl.contains("gitlab.com") ||
```

### Line 180
**Comment:** SSH URLs

**Context:**
```java
     176|                   trimmedUrl.contains("bitbucket.org") ||
     177|                   trimmedUrl.endsWith(".git");
     178|        }
>>>  179|
     180|        // SSH URLs
     181|        if (trimmedUrl.startsWith("git@")) {
     182|            return trimmedUrl.contains("github.com") ||
     183|                   trimmedUrl.contains("gitlab.com") ||
```

### Line 188
**Comment:** Git protocol URLs

**Context:**
```java
     184|                   trimmedUrl.contains("bitbucket.org") ||
     185|                   trimmedUrl.endsWith(".git");
     186|        }
>>>  187|
     188|        // Git protocol URLs
     189|        if (trimmedUrl.startsWith("git://")) {
     190|            return trimmedUrl.endsWith(".git");
     191|        }
```

### Line 200
**Comment:** Remove .git extension if present

**Context:**
```java
     196|    public String extractProjectNameFromUrl(String repositoryUrl) {
     197|        try {
     198|            String url = repositoryUrl.trim();
>>>  199|
     200|            // Remove .git extension if present
     201|            if (url.endsWith(".git")) {
     202|                url = url.substring(0, url.length() - 4);
     203|            }
```

### Line 205
**Comment:** Extract the last part of the URL path

**Context:**
```java
     201|            if (url.endsWith(".git")) {
     202|                url = url.substring(0, url.length() - 4);
     203|            }
>>>  204|
     205|            // Extract the last part of the URL path
     206|            URI uri = new URI(url);
     207|            String path = uri.getPath();
     208|            if (path != null && !path.isEmpty()) {
```

### Line 215
**Comment:** Fallback: use a default name

**Context:**
```java
     211|                    return pathParts[pathParts.length - 1];
     212|                }
     213|            }
>>>  214|
     215|            // Fallback: use a default name
     216|            return "cloned-project";
     217|
     218|        } catch (URISyntaxException e) {
```

---

## com/codex/apk/InlineDiffAdapter.java

**Location:** `app/src/main/java/com/codex/apk/InlineDiffAdapter.java`

### Line 47
**Comment:** Reset defaults

**Context:**
```java
      43|    @Override
      44|    public void onBindViewHolder(@NonNull DiffViewHolder holder, int position) {
      45|        DiffUtils.DiffLine line = lines.get(position);
>>>   46|
      47|        // Reset defaults
      48|        holder.itemView.setVisibility(View.VISIBLE);
      49|        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
      50|        if (lp != null) {
```

### Line 62
**Comment:** Headers (e.g., @@ -a,+b @@ or ---/+++) are excluded via filterDisplayable();

**Context:**
```java
      58|        holder.itemView.setBackgroundColor(0x00000000); // transparent
      59|
      60|        switch (line.type) {
>>>   61|            case HEADER:
      62|                // Headers (e.g., @@ -a,+b @@ or ---/+++) are excluded via filterDisplayable();
      63|                // This branch should rarely be hit, but render as invisible safety net.
      64|                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
      65|                holder.itemView.setVisibility(View.GONE);
```

### Line 63
**Comment:** This branch should rarely be hit, but render as invisible safety net.

**Context:**
```java
      59|
      60|        switch (line.type) {
      61|            case HEADER:
>>>   62|                // Headers (e.g., @@ -a,+b @@ or ---/+++) are excluded via filterDisplayable();
      63|                // This branch should rarely be hit, but render as invisible safety net.
      64|                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
      65|                holder.itemView.setVisibility(View.GONE);
      66|                break;
```

### Line 111
**Comment:** Remove header lines like @@ hunk headers and ---/+++ file markers from display

**Context:**
```java
     107|            tvContent = itemView.findViewById(R.id.tv_content);
     108|        }
     109|    }
>>>  110|
     111|    // Remove header lines like @@ hunk headers and ---/+++ file markers from display
     112|    private static List<DiffUtils.DiffLine> filterDisplayable(List<DiffUtils.DiffLine> src) {
     113|        List<DiffUtils.DiffLine> out = new ArrayList<>();
     114|        if (src == null) return out;
```

---

## com/codex/apk/LocalServerManager.java

**Location:** `app/src/main/java/com/codex/apk/LocalServerManager.java`

### Line 98
**Comment:** Important: close the socket from a different thread than the accept loop to unblock accept()

**Context:**
```java
      94|            callback.onServerStopped();
      95|            return;
      96|        }
>>>   97|
      98|        // Important: close the socket from a different thread than the accept loop to unblock accept()
      99|        new Thread(() -> {
     100|            try {
     101|                isRunning = false;
```

### Line 129
**Comment:** Consume remaining headers

**Context:**
```java
     125|            String[] parts = requestLine.split(" ");
     126|            String method = parts.length > 0 ? parts[0] : "";
     127|            String path = parts.length > 1 ? parts[1] : "/";
>>>  128|
     129|            // Consume remaining headers
     130|            String line;
     131|            while ((line = in.readLine()) != null && !line.isEmpty()) {}
     132|
```

### Line 161
**Comment:** SPA fallback

**Context:**
```java
     157|                serveFile(out, target);
     158|                return;
     159|            }
>>>  160|
     161|            // SPA fallback
     162|            if (fallbackIndexFile != null && fallbackIndexFile.exists()) {
     163|                serveFile(out, fallbackIndexFile);
     164|                return;
```

### Line 227
**Comment:** Determine serve root and fallback index based on project type and common build outputs

**Context:**
```java
     223|        if (!projectDir.exists()) {
     224|            throw new IllegalStateException("Project directory does not exist: " + projectPath);
     225|        }
>>>  226|
     227|        // Determine serve root and fallback index based on project type and common build outputs
     228|        serveRootDir = determineServeRootDir(projectDir, projectType);
     229|        fallbackIndexFile = determineFallbackIndexFile(serveRootDir, projectDir, projectType);
     230|    }
```

### Line 233
**Comment:** Prefer built assets for Tailwind if present; otherwise serve project root

**Context:**
```java
     229|        fallbackIndexFile = determineFallbackIndexFile(serveRootDir, projectDir, projectType);
     230|    }
     231|
>>>  232|    private File determineServeRootDir(File projectDir, String projectType) {
     233|        // Prefer built assets for Tailwind if present; otherwise serve project root
     234|        switch (projectType) {
     235|            case "tailwind":
     236|                if (new File(projectDir, "dist").exists()) return new File(projectDir, "dist");
```

### Line 245
**Comment:** Find the most likely index.html to serve for basic static projects

**Context:**
```java
     241|        }
     242|    }
     243|
>>>  244|    private File determineFallbackIndexFile(File serveRoot, File projectDir, String projectType) {
     245|        // Find the most likely index.html to serve for basic static projects
     246|        File candidate = new File(serveRoot, "index.html");
     247|        if (candidate.exists()) return candidate;
     248|        candidate = new File(projectDir, "index.html");
```

### Line 271
**Comment:** Use 127.0.0.1 for reliable loopback in WebView

**Context:**
```java
     267|    }
     268|
     269|    public String getServerUrl() {
>>>  270|        if (isRunning) {
     271|            // Use 127.0.0.1 for reliable loopback in WebView
     272|            return "http://127.0.0.1:" + currentPort + "/";
     273|        }
     274|        return null;
```

---

## com/codex/apk/MainActivity.java

**Location:** `app/src/main/java/com/codex/apk/MainActivity.java`

### Line 59
**Comment:** Initialize Views

**Context:**
```java
      55|        super.onCreate(savedInstanceState);
      56|        ThemeManager.setupTheme(this);
      57|        setContentView(R.layout.main);
>>>   58|
      59|        // Initialize Views
      60|        MaterialToolbar toolbar = findViewById(R.id.toolbar);
      61|        setSupportActionBar(toolbar);
      62|        projectsRecyclerView = findViewById(R.id.listview_projects);
```

### Line 68
**Comment:** Initialize Managers

**Context:**
```java
      64|        textEmptyProjects = findViewById(R.id.text_empty_projects);
      65|        layoutEmptyState = findViewById(R.id.layout_empty_state);
      66|        layoutRecentProjects = findViewById(R.id.layout_recent_projects);
>>>   67|
      68|        // Initialize Managers
      69|        projectsList = new ArrayList<>();
      70|        recentProjectsList = new ArrayList<>();
      71|        projectsAdapter = new ProjectsAdapter(this, projectsList, this);
```

### Line 78
**Comment:** Setup UI

**Context:**
```java
      74|        permissionManager = new PermissionManager(this);
      75|        importExportManager = new ProjectImportExportManager(this);
      76|        gitManager = new GitManager(this);
>>>   77|
      78|        // Setup UI
      79|        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
      80|        projectsRecyclerView.setAdapter(projectsAdapter);
      81|        projectsRecyclerView.setNestedScrollingEnabled(false);
```

### Line 86
**Comment:** Setup Listeners

**Context:**
```java
      82|
      83|        recentProjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
      84|        recentProjectsRecyclerView.setAdapter(recentProjectsAdapter);
>>>   85|
      86|        // Setup Listeners
      87|        findViewById(R.id.button_filter_projects).setOnClickListener(v -> showFilterDialog());
      88|        fabQuickActions = findViewById(R.id.fab_quick_actions);
      89|        fabDeleteSelection = findViewById(R.id.fab_delete_selection);
```

### Line 92
**Comment:** Selection listener to toggle FABs

**Context:**
```java
      88|        fabQuickActions = findViewById(R.id.fab_quick_actions);
      89|        fabDeleteSelection = findViewById(R.id.fab_delete_selection);
      90|        fabQuickActions.setOnClickListener(v -> showQuickActionsMenu());
>>>   91|
      92|        // Selection listener to toggle FABs
      93|        projectsAdapter.setSelectionListener((selectedCount, selectionMode) -> {
      94|            if (selectionMode) {
      95|                if (fabQuickActions.getVisibility() == View.VISIBLE) fabQuickActions.setVisibility(View.GONE);
```

### Line 111
**Comment:** Do nothing, this is a background refresh

**Context:**
```java
     107|
     108|        aiAssistant = new AIAssistant(this, null, null);
     109|        if (SettingsActivity.getOpenRouterApiKey(this) != null && !SettingsActivity.getOpenRouterApiKey(this).isEmpty()) {
>>>  110|            aiAssistant.refreshModelsForProvider(AIProvider.OPENROUTER, (success, message) -> {
     111|                // Do nothing, this is a background refresh
     112|            });
     113|        }
     114|    }
```

### Line 226
**Comment:** First, populate recent projects, always sorted by date

**Context:**
```java
     222|
     223|    private void loadProjects() {
     224|        projectManager.loadProjectsList();
>>>  225|
     226|        // First, populate recent projects, always sorted by date
     227|        ArrayList<HashMap<String, Object>> sortedForRecent = new ArrayList<>(projectsList);
     228|        Collections.sort(sortedForRecent, (p1, p2) -> Long.compare((long) p2.get("lastModifiedTimestamp"), (long) p1.get("lastModifiedTimestamp")));
     229|        recentProjectsList.clear();
```

### Line 237
**Comment:** Now, sort the main list according to the user's preference

**Context:**
```java
     233|            recentProjectsList.addAll(sortedForRecent);
     234|        }
     235|        recentProjectsAdapter.notifyDataSetChanged();
>>>  236|
     237|        // Now, sort the main list according to the user's preference
     238|        sortProjects();
     239|        projectsAdapter.notifyDataSetChanged();
     240|
```

### Line 276
**Comment:** This could also be moved to a UIManager class in a larger app

**Context:**
```java
     272|        Collections.sort(projectsList, comparator);
     273|    }
     274|
>>>  275|    private void showQuickActionsMenu() {
     276|        // This could also be moved to a UIManager class in a larger app
     277|        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
     278|            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
     279|        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_quick_actions, null);
```

### Line 327
**Comment:** Show progress

**Context:**
```java
     323|                Toast.makeText(this, "Please enter a valid Git repository URL", Toast.LENGTH_SHORT).show();
     324|                return;
     325|            }
>>>  326|
     327|            // Show progress
     328|            progressLayout.setVisibility(View.VISIBLE);
     329|            cloneButton.setEnabled(false);
     330|            urlInput.setEnabled(false);
```

### Line 332
**Comment:** Start cloning

**Context:**
```java
     328|            progressLayout.setVisibility(View.VISIBLE);
     329|            cloneButton.setEnabled(false);
     330|            urlInput.setEnabled(false);
>>>  331|
     332|            // Start cloning
     333|            String projectName = gitManager.extractProjectNameFromUrl(url);
     334|            gitManager.cloneRepository(url, projectName, new GitManager.GitCloneCallback() {
     335|                @Override
```

### Line 373
**Comment:** Getter methods for managers to access activity context or other managers if needed

**Context:**
```java
     369|
     370|        dialog.show();
     371|    }
>>>  372|
     373|    // Getter methods for managers to access activity context or other managers if needed
     374|    public ProjectManager getProjectManager() {
     375|        return projectManager;
     376|    }
```

---

## com/codex/apk/MarkdownFormatter.java

**Location:** `app/src/main/java/com/codex/apk/MarkdownFormatter.java`

### Line 24
**Comment:** Create main markwon instance for AI messages

**Context:**
```java
      20|    private final Markwon markwon;
      21|    private final Markwon thinkingMarkwon;
      22|
>>>   23|    private MarkdownFormatter(Context context) {
      24|        // Create main markwon instance for AI messages
      25|        markwon = Markwon.builder(context)
      26|                .usePlugin(StrikethroughPlugin.create())
      27|                .usePlugin(TablePlugin.create(context))
```

### Line 34
**Comment:** Create simplified markwon instance for thinking content (no images, simpler formatting)

**Context:**
```java
      30|                .usePlugin(ImagesPlugin.create())
      31|                .usePlugin(LinkifyPlugin.create())
      32|                .build();
>>>   33|
      34|        // Create simplified markwon instance for thinking content (no images, simpler formatting)
      35|        thinkingMarkwon = Markwon.builder(context)
      36|                .usePlugin(StrikethroughPlugin.create())
      37|                .usePlugin(LinkifyPlugin.create())
```

### Line 83
**Comment:** Heuristic: if looks like raw/minified HTML and not already fenced, wrap and pretty-print

**Context:**
```java
      79|    public String preprocessMarkdown(String markdown) {
      80|        if (markdown == null) return "";
      81|        String s = markdown;
>>>   82|
      83|        // Heuristic: if looks like raw/minified HTML and not already fenced, wrap and pretty-print
      84|        String trimmed = s.trim();
      85|        boolean alreadyFenced = trimmed.contains("```");
      86|        boolean looksHtml = !alreadyFenced && trimmed.startsWith("<") && trimmed.contains(">") && (
```

### Line 92
**Comment:** Insert newlines between adjacent tags to improve readability

**Context:**
```java
      88|                trimmed.contains("<body") || trimmed.contains("<div") || trimmed.contains("<span") ||
      89|                trimmed.contains("<p ") || trimmed.contains("<p>") || trimmed.matches("(?s).*<([a-zA-Z][a-zA-Z0-9-]*)([^>]*)>.*</\\1>.*")
      90|        );
>>>   91|        if (looksHtml) {
      92|            // Insert newlines between adjacent tags to improve readability
      93|            String pretty = trimmed.replace("><", ">\n<");
      94|            return "```html\n" + pretty + "\n```";
      95|        }
```

### Line 97
**Comment:** Handle code blocks with language specification (normalize line endings after language)

**Context:**
```java
      93|            String pretty = trimmed.replace("><", ">\n<");
      94|            return "```html\n" + pretty + "\n```";
      95|        }
>>>   96|
      97|        // Handle code blocks with language specification (normalize line endings after language)
      98|        s = s.replaceAll("```(\\w+)\\r?\\n", "```$1\n");
      99|
     100|        // Ensure proper line breaks for lists
```

### Line 100
**Comment:** Ensure proper line breaks for lists

**Context:**
```java
      96|
      97|        // Handle code blocks with language specification (normalize line endings after language)
      98|        s = s.replaceAll("```(\\w+)\\r?\\n", "```$1\n");
>>>   99|
     100|        // Ensure proper line breaks for lists
     101|        s = s.replaceAll("(?<!\\n)\\n([*+-]\\s)", "\n\n$1");
     102|
     103|        // Handle numbered lists
```

### Line 103
**Comment:** Handle numbered lists

**Context:**
```java
      99|
     100|        // Ensure proper line breaks for lists
     101|        s = s.replaceAll("(?<!\\n)\\n([*+-]\\s)", "\n\n$1");
>>>  102|
     103|        // Handle numbered lists
     104|        s = s.replaceAll("(?<!\\n)\\n(\\d+\\.\\s)", "\n\n$1");
     105|
     106|        // Normalize citations spacing: [[n]] -> [[n]] with surrounding spaces ensured by renderer
```

### Line 106
**Comment:** Normalize citations spacing: [[n]] -> [[n]] with surrounding spaces ensured by renderer

**Context:**
```java
     102|
     103|        // Handle numbered lists
     104|        s = s.replaceAll("(?<!\\n)\\n(\\d+\\.\\s)", "\n\n$1");
>>>  105|
     106|        // Normalize citations spacing: [[n]] -> [[n]] with surrounding spaces ensured by renderer
     107|        s = s.replaceAll("\\[\\[(\\d+)\\]\\]", "[[$1]]");
     108|
     109|        return s;
```

---

## com/codex/apk/ModelAdapter.java

**Location:** `app/src/main/java/com/codex/apk/ModelAdapter.java`

### Line 40
**Comment:** Initialize checked states from SharedPreferences

**Context:**
```java
      36|        this.context = context;
      37|        this.prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
      38|        this.modelsByProvider = new LinkedHashMap<>();
>>>   39|
      40|        // Initialize checked states from SharedPreferences
      41|        for (AIModel model : models) {
      42|            String key = "model_" + model.getProvider().name() + "_" + model.getModelId() + "_enabled";
      43|            checkedStates.put(model.getProvider().name() + "_" + model.getModelId(), prefs.getBoolean(key, true));
```

### Line 47
**Comment:** Group models by provider

**Context:**
```java
      43|            checkedStates.put(model.getProvider().name() + "_" + model.getModelId(), prefs.getBoolean(key, true));
      44|        }
      45|
>>>   46|
      47|        // Group models by provider
      48|        for (AIModel model : models) {
      49|            if (!modelsByProvider.containsKey(model.getProvider())) {
      50|                modelsByProvider.put(model.getProvider(), new ArrayList<>());
```

### Line 55
**Comment:** Create a flat list for the adapter

**Context:**
```java
      51|            }
      52|            modelsByProvider.get(model.getProvider()).add(model);
      53|        }
>>>   54|
      55|        // Create a flat list for the adapter
      56|        for (Map.Entry<AIProvider, List<AIModel>> entry : modelsByProvider.entrySet()) {
      57|            items.add(entry.getKey());
      58|            items.addAll(entry.getValue());
```

### Line 175
**Comment:** Long press on header invokes callback if set

**Context:**
```java
     171|                    notifyDataSetChanged();
     172|                }
     173|            });
>>>  174|
     175|            // Long press on header invokes callback if set
     176|            itemView.setOnLongClickListener(v -> {
     177|                if (headerLongClickListener != null) {
     178|                    headerLongClickListener.onProviderHeaderLongClick(provider);
```

---

## com/codex/apk/ModelsActivity.java

**Location:** `app/src/main/java/com/codex/apk/ModelsActivity.java`

### Line 42
**Comment:** Show add model dialog

**Context:**
```java
      38|        recyclerView.setLayoutManager(new LinearLayoutManager(this));
      39|
      40|        fab = findViewById(R.id.fab_add_model);
>>>   41|        fab.setOnClickListener(v -> {
      42|            // Show add model dialog
      43|            showAddModelDialog();
      44|        });
      45|
```

---

## com/codex/apk/PreviewActivity.java

**Location:** `app/src/main/java/com/codex/apk/PreviewActivity.java`

### Line 45
**Comment:** Intent extras

**Context:**
```java
      41|public class PreviewActivity extends AppCompatActivity {
      42|
      43|    private static final String TAG = "PreviewActivity";
>>>   44|
      45|    // Intent extras
      46|    public static final String EXTRA_PROJECT_PATH = "project_path";
      47|    public static final String EXTRA_PROJECT_NAME = "project_name";
      48|    public static final String EXTRA_HTML_CONTENT = "html_content";
```

### Line 51
**Comment:** UI Components

**Context:**
```java
      47|    public static final String EXTRA_PROJECT_NAME = "project_name";
      48|    public static final String EXTRA_HTML_CONTENT = "html_content";
      49|    public static final String EXTRA_FILE_NAME = "file_name";
>>>   50|
      51|    // UI Components
      52|    private WebView webViewPreview;
      53|    private TextView textConsoleOutput;
      54|    private ScrollView scrollViewConsole;
```

### Line 61
**Comment:** Data

**Context:**
```java
      57|    private Toolbar toolbar;
      58|    private String originalUserAgent;
      59|    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0 Safari/537.36 CodeXDesktop";
>>>   60|
      61|    // Data
      62|    private File projectDir;
      63|    private String projectPath;
      64|    private String projectName;
```

### Line 70
**Comment:** Performance optimizations

**Context:**
```java
      66|    private String fileName;
      67|    private boolean isConsoleVisible = false;
      68|    private boolean isDesktopModeEnabled = false;
>>>   69|
      70|    // Performance optimizations
      71|    private Map<String, byte[]> fileCache = new HashMap<>();
      72|    private static final long MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB cache limit
      73|    private long currentCacheSize = 0;
```

### Line 75
**Comment:** Local server management

**Context:**
```java
      71|    private Map<String, byte[]> fileCache = new HashMap<>();
      72|    private static final long MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB cache limit
      73|    private long currentCacheSize = 0;
>>>   74|
      75|    // Local server management
      76|    private LocalServerManager localServerManager;
      77|    private boolean isLocalServerRunning = false;
      78|    private View loadingOverlay;
```

### Line 87
**Comment:** Get intent data

**Context:**
```java
      83|        super.onCreate(savedInstanceState);
      84|        ThemeManager.setupTheme(this);
      85|        setContentView(R.layout.activity_preview);
>>>   86|
      87|        // Get intent data
      88|        extractIntentData();
      89|
      90|        // Initialize views
```

### Line 90
**Comment:** Initialize views

**Context:**
```java
      86|
      87|        // Get intent data
      88|        extractIntentData();
>>>   89|
      90|        // Initialize views
      91|        initializeViews();
      92|
      93|        // Setup toolbar
```

### Line 93
**Comment:** Setup toolbar

**Context:**
```java
      89|
      90|        // Initialize views
      91|        initializeViews();
>>>   92|
      93|        // Setup toolbar
      94|        setupToolbar();
      95|
      96|        // Initialize local server manager
```

### Line 96
**Comment:** Initialize local server manager

**Context:**
```java
      92|
      93|        // Setup toolbar
      94|        setupToolbar();
>>>   95|
      96|        // Initialize local server manager
      97|        localServerManager = new LocalServerManager(this);
      98|
      99|        // Setup WebView with full capabilities
```

### Line 99
**Comment:** Setup WebView with full capabilities

**Context:**
```java
      95|
      96|        // Initialize local server manager
      97|        localServerManager = new LocalServerManager(this);
>>>   98|
      99|        // Setup WebView with full capabilities
     100|        setupWebView();
     101|
     102|
```

### Line 103
**Comment:** Wire console header buttons

**Context:**
```java
      99|        // Setup WebView with full capabilities
     100|        setupWebView();
     101|
>>>  102|
     103|        // Wire console header buttons
     104|        View clearHeader = findViewById(R.id.btn_clear_console_header);
     105|        if (clearHeader != null) clearHeader.setOnClickListener(v -> clearConsole());
     106|        View closeConsole = findViewById(R.id.btn_close_console);
```

### Line 109
**Comment:** Load initial content or start environment based on project type

**Context:**
```java
     105|        if (clearHeader != null) clearHeader.setOnClickListener(v -> clearConsole());
     106|        View closeConsole = findViewById(R.id.btn_close_console);
     107|        if (closeConsole != null) closeConsole.setOnClickListener(v -> toggleConsole());
>>>  108|
     109|        // Load initial content or start environment based on project type
     110|        initializeEnvironmentAndLoad();
     111|    }
     112|
```

### Line 153
**Comment:** Get WebView settings

**Context:**
```java
     149|        }
     150|    }
     151|
>>>  152|    private void setupWebView() {
     153|        // Get WebView settings
     154|        WebSettings webSettings = webViewPreview.getSettings();
     155|
     156|        // Enable all JavaScript capabilities
```

### Line 156
**Comment:** Enable all JavaScript capabilities

**Context:**
```java
     152|    private void setupWebView() {
     153|        // Get WebView settings
     154|        WebSettings webSettings = webViewPreview.getSettings();
>>>  155|
     156|        // Enable all JavaScript capabilities
     157|        webSettings.setJavaScriptEnabled(true);
     158|        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
     159|
```

### Line 160
**Comment:** Enable DOM storage

**Context:**
```java
     156|        // Enable all JavaScript capabilities
     157|        webSettings.setJavaScriptEnabled(true);
     158|        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
>>>  159|
     160|        // Enable DOM storage
     161|        webSettings.setDomStorageEnabled(true);
     162|        webSettings.setDatabaseEnabled(true);
     163|
```

### Line 164
**Comment:** Enable local storage

**Context:**
```java
     160|        // Enable DOM storage
     161|        webSettings.setDomStorageEnabled(true);
     162|        webSettings.setDatabaseEnabled(true);
>>>  163|
     164|        // Enable local storage
     165|        webSettings.setAllowFileAccess(true);
     166|        webSettings.setAllowContentAccess(true);
     167|        webSettings.setAllowFileAccessFromFileURLs(true);
```

### Line 170
**Comment:** Performance optimizations

**Context:**
```java
     166|        webSettings.setAllowContentAccess(true);
     167|        webSettings.setAllowFileAccessFromFileURLs(true);
     168|        webSettings.setAllowUniversalAccessFromFileURLs(true);
>>>  169|
     170|        // Performance optimizations
     171|        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
     172|        // Note: setAppCacheEnabled is deprecated in API 33+, but we keep it for backward compatibility
     173|        webSettings.setLoadsImagesAutomatically(true);
```

### Line 172
**Comment:** Note: setAppCacheEnabled is deprecated in API 33+, but we keep it for backward compatibility

**Context:**
```java
     168|        webSettings.setAllowUniversalAccessFromFileURLs(true);
     169|
     170|        // Performance optimizations
>>>  171|        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
     172|        // Note: setAppCacheEnabled is deprecated in API 33+, but we keep it for backward compatibility
     173|        webSettings.setLoadsImagesAutomatically(true);
     174|        webSettings.setBlockNetworkImage(false);
     175|        webSettings.setBlockNetworkLoads(false);
```

### Line 177
**Comment:** Media and rendering optimizations

**Context:**
```java
     173|        webSettings.setLoadsImagesAutomatically(true);
     174|        webSettings.setBlockNetworkImage(false);
     175|        webSettings.setBlockNetworkLoads(false);
>>>  176|
     177|        // Media and rendering optimizations
     178|        webSettings.setMediaPlaybackRequiresUserGesture(false);
     179|        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
     180|
```

### Line 181
**Comment:** Performance settings for low-end devices

**Context:**
```java
     177|        // Media and rendering optimizations
     178|        webSettings.setMediaPlaybackRequiresUserGesture(false);
     179|        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
>>>  180|
     181|        // Performance settings for low-end devices
     182|        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
     183|        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
     184|
```

### Line 185
**Comment:** Disable zoom controls but allow pinch zoom

**Context:**
```java
     181|        // Performance settings for low-end devices
     182|        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
     183|        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
>>>  184|
     185|        // Disable zoom controls but allow pinch zoom
     186|        webSettings.setSupportZoom(true);
     187|        webSettings.setBuiltInZoomControls(true);
     188|        webSettings.setDisplayZoomControls(false);
```

### Line 190
**Comment:** User agent for better compatibility

**Context:**
```java
     186|        webSettings.setSupportZoom(true);
     187|        webSettings.setBuiltInZoomControls(true);
     188|        webSettings.setDisplayZoomControls(false);
>>>  189|
     190|        // User agent for better compatibility
     191|        webSettings.setUserAgentString(webSettings.getUserAgentString() + " CodeX/1.0");
     192|        originalUserAgent = webSettings.getUserAgentString();
     193|
```

### Line 194
**Comment:** Setup WebViewClient with local file serving

**Context:**
```java
     190|        // User agent for better compatibility
     191|        webSettings.setUserAgentString(webSettings.getUserAgentString() + " CodeX/1.0");
     192|        originalUserAgent = webSettings.getUserAgentString();
>>>  193|
     194|        // Setup WebViewClient with local file serving
     195|        webViewPreview.setWebViewClient(new OptimizedWebViewClient());
     196|
     197|        // Setup WebChromeClient for console output, progress, and title updates
```

### Line 197
**Comment:** Setup WebChromeClient for console output, progress, and title updates

**Context:**
```java
     193|
     194|        // Setup WebViewClient with local file serving
     195|        webViewPreview.setWebViewClient(new OptimizedWebViewClient());
>>>  196|
     197|        // Setup WebChromeClient for console output, progress, and title updates
     198|        webViewPreview.setWebChromeClient(new OptimizedWebChromeClient());
     199|
     200|        // Enable hardware acceleration for better performance
```

### Line 200
**Comment:** Enable hardware acceleration for better performance

**Context:**
```java
     196|
     197|        // Setup WebChromeClient for console output, progress, and title updates
     198|        webViewPreview.setWebChromeClient(new OptimizedWebChromeClient());
>>>  199|
     200|        // Enable hardware acceleration for better performance
     201|        webViewPreview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
     202|    }
     203|
```

### Line 216
**Comment:** Try to load index.html or the specified file

**Context:**
```java
     212|                "UTF-8",
     213|                null
     214|            );
>>>  215|        } else if (projectDir != null) {
     216|            // Try to load index.html or the specified file
     217|            File htmlFile = new File(projectDir, fileName);
     218|            if (!htmlFile.exists()) {
     219|                htmlFile = new File(projectDir, "index.html");
```

### Line 226
**Comment:** Load a default HTML template

**Context:**
```java
     222|            if (htmlFile.exists()) {
     223|                String fileUrl = "file://" + htmlFile.getAbsolutePath();
     224|                webViewPreview.loadUrl(fileUrl);
>>>  225|            } else {
     226|                // Load a default HTML template
     227|                loadDefaultTemplate();
     228|            }
     229|        } else {
```

### Line 237
**Comment:** Start local server by default and load from it when available

**Context:**
```java
     233|
     234|    private void initializeEnvironmentAndLoad() {
     235|        String projectType = detectProjectType();
>>>  236|        addConsoleMessage("Detected project type: " + projectType);
     237|        // Start local server by default and load from it when available
     238|        autoLoadOnServerStart = true;
     239|        startLocalServerWithUi(projectType);
     240|    }
```

### Line 249
**Comment:** Optionally update a loading overlay message if present

**Context:**
```java
     245|        return rootIdx.exists() ? rootIdx : null;
     246|    }
     247|
>>>  248|    private void setLoadingOverlayText(String text) {
     249|        // Optionally update a loading overlay message if present
     250|        // Keeping no-op for now as UI may not include a text view
     251|    }
     252|
```

### Line 250
**Comment:** Keeping no-op for now as UI may not include a text view

**Context:**
```java
     246|    }
     247|
     248|    private void setLoadingOverlayText(String text) {
>>>  249|        // Optionally update a loading overlay message if present
     250|        // Keeping no-op for now as UI may not include a text view
     251|    }
     252|
     253|    private void startLocalServerWithUi(String projectType) {
```

### Line 298
**Comment:** Enhanced version of the HTML processing from PreviewConsoleFragment

**Context:**
```java
     294|        });
     295|    }
     296|
>>>  297|    private String enhanceHtmlContent(String originalHtml) {
     298|        // Enhanced version of the HTML processing from PreviewConsoleFragment
     299|        String htmlLower = originalHtml.toLowerCase(Locale.ROOT);
     300|        StringBuilder resultHtml = new StringBuilder();
     301|
```

### Line 302
**Comment:** Ensure DOCTYPE and HTML structure

**Context:**
```java
     298|        // Enhanced version of the HTML processing from PreviewConsoleFragment
     299|        String htmlLower = originalHtml.toLowerCase(Locale.ROOT);
     300|        StringBuilder resultHtml = new StringBuilder();
>>>  301|
     302|        // Ensure DOCTYPE and HTML structure
     303|        if (!htmlLower.contains("<!doctype")) {
     304|            resultHtml.append("<!DOCTYPE html>\n");
     305|        }
```

### Line 311
**Comment:** Enhanced head section with performance optimizations

**Context:**
```java
     307|        if (!htmlLower.contains("<html")) {
     308|            resultHtml.append("<html lang=\"en\">\n");
     309|        }
>>>  310|
     311|        // Enhanced head section with performance optimizations
     312|        if (!htmlLower.contains("<head")) {
     313|            resultHtml.append("<head>\n");
     314|            resultHtml.append("<meta charset=\"UTF-8\">\n");
```

### Line 318
**Comment:** Performance hints

**Context:**
```java
     314|            resultHtml.append("<meta charset=\"UTF-8\">\n");
     315|            resultHtml.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=yes\">\n");
     316|            resultHtml.append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
>>>  317|
     318|            // Performance hints
     319|            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
     320|            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n");
     321|            resultHtml.append("<link rel=\"preconnect\" href=\"https://cdn.tailwindcss.com\">\n");
```

### Line 323
**Comment:** Default fonts and styles

**Context:**
```java
     319|            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
     320|            resultHtml.append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>\n");
     321|            resultHtml.append("<link rel=\"preconnect\" href=\"https://cdn.tailwindcss.com\">\n");
>>>  322|
     323|            // Default fonts and styles
     324|            resultHtml.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
     325|            resultHtml.append("<script src=\"https://cdn.tailwindcss.com\"></script>\n");
     326|
```

### Line 327
**Comment:** Performance and development enhancements

**Context:**
```java
     323|            // Default fonts and styles
     324|            resultHtml.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
     325|            resultHtml.append("<script src=\"https://cdn.tailwindcss.com\"></script>\n");
>>>  326|
     327|            // Performance and development enhancements
     328|            resultHtml.append("<style>\n");
     329|            resultHtml.append("* { box-sizing: border-box; }\n");
     330|            resultHtml.append("body { font-family: 'Inter', sans-serif; margin: 0; padding: 0; }\n");
```

### Line 343
**Comment:** Close tags if needed

**Context:**
```java
     339|        }
     340|
     341|        resultHtml.append(originalHtml);
>>>  342|
     343|        // Close tags if needed
     344|        if (!htmlLower.contains("</body>")) {
     345|            resultHtml.append("\n</body>");
     346|        }
```

### Line 449
**Comment:** Toggle local server actions visibility

**Context:**
```java
     445|        MenuItem desktopModeMenuItem = menu.findItem(R.id.action_desktop_mode);
     446|        if (desktopModeMenuItem != null) {
     447|            desktopModeMenuItem.setChecked(isDesktopModeEnabled);
>>>  448|        }
     449|        // Toggle local server actions visibility
     450|        MenuItem startServer = menu.findItem(R.id.action_start_local_server);
     451|        MenuItem stopServer = menu.findItem(R.id.action_stop_local_server);
     452|        boolean running = localServerManager != null && localServerManager.isServerRunning();
```

### Line 470
**Comment:** Toolbar back should exit the activity

**Context:**
```java
     466|    public boolean onOptionsItemSelected(MenuItem item) {
     467|        int id = item.getItemId();
     468|
>>>  469|        if (id == android.R.id.home) {
     470|            // Toolbar back should exit the activity
     471|            finish();
     472|            return true;
     473|        } else if (id == R.id.action_refresh) {
```

### Line 519
**Comment:** Prefer local server URL if running

**Context:**
```java
     515|    }
     516|
     517|    private void openInBrowser() {
>>>  518|        try {
     519|            // Prefer local server URL if running
     520|            if (localServerManager != null && localServerManager.isServerRunning()) {
     521|                String serverUrl = localServerManager.getServerUrl();
     522|                if (serverUrl != null) {
```

### Line 533
**Comment:** If server isn't running, start it and open once ready

**Context:**
```java
     529|                    }
     530|                }
     531|            }
>>>  532|
     533|            // If server isn't running, start it and open once ready
     534|            if (localServerManager != null && projectDir != null && !localServerManager.isServerRunning()) {
     535|                addConsoleMessage("Starting local server for external browser...");
     536|                localServerManager.startServer(projectDir.getAbsolutePath(), detectProjectType(), 8080,
```

### Line 559
**Comment:** Fallback to file URL

**Context:**
```java
     555|                    });
     556|                return;
     557|            }
>>>  558|
     559|            // Fallback to file URL
     560|            String filePath = null;
     561|            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
     562|                File tempFile = new File(projectDir, "temp_preview.html");
```

### Line 596
**Comment:** Tailwind detection via CDN in index.html

**Context:**
```java
     592|    }
     593|
     594|    private String detectProjectType() {
>>>  595|        if (projectDir == null) return "html";
     596|        // Tailwind detection via CDN in index.html
     597|        try {
     598|            File idx = new File(projectDir, "index.html");
     599|            if (idx.exists()) {
```

### Line 608
**Comment:** If htmlContent is provided, inspect it too

**Context:**
```java
     604|                reader.close();
     605|                if (sb.toString().contains("cdn.tailwindcss.com")) return "tailwind";
     606|            }
>>>  607|        } catch (Exception ignore) {}
     608|        // If htmlContent is provided, inspect it too
     609|        try {
     610|            if (htmlContent != null && htmlContent.toLowerCase(Locale.ROOT).contains("cdn.tailwindcss.com")) {
     611|                return "tailwind";
```

### Line 614
**Comment:** Default to HTML/CSS/JS

**Context:**
```java
     610|            if (htmlContent != null && htmlContent.toLowerCase(Locale.ROOT).contains("cdn.tailwindcss.com")) {
     611|                return "tailwind";
     612|            }
>>>  613|        } catch (Exception ignore) {}
     614|        // Default to HTML/CSS/JS
     615|        return "html";
     616|    }
     617|
```

### Line 622
**Comment:** Stop local server if running

**Context:**
```java
     618|    @Override
     619|    protected void onDestroy() {
     620|        super.onDestroy();
>>>  621|
     622|        // Stop local server if running
     623|        if (isLocalServerRunning && localServerManager != null) {
     624|            try {
     625|                localServerManager.stopServer(new LocalServerManager.ServerCallback() {
```

### Line 651
**Comment:** Clear cache

**Context:**
```java
     647|        if (webViewPreview != null) {
     648|            webViewPreview.removeAllViews();
     649|            webViewPreview.destroy();
>>>  650|        }
     651|        // Clear cache
     652|        fileCache.clear();
     653|    }
     654|
```

### Line 664
**Comment:** Optimized WebViewClient for better performance

**Context:**
```java
     660|            super.onBackPressed();
     661|        }
     662|    }
>>>  663|
     664|    // Optimized WebViewClient for better performance
     665|    private class OptimizedWebViewClient extends WebViewClient {
     666|
     667|        @Override
```

### Line 671
**Comment:** Handle local file URLs

**Context:**
```java
     667|        @Override
     668|        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
     669|            String url = request.getUrl().toString();
>>>  670|
     671|            // Handle local file URLs
     672|            if (url.startsWith("file://")) {
     673|                return false; // Let WebView handle it
     674|            }
```

### Line 676
**Comment:** Handle external URLs

**Context:**
```java
     672|            if (url.startsWith("file://")) {
     673|                return false; // Let WebView handle it
     674|            }
>>>  675|
     676|            // Handle external URLs
     677|            if (url.startsWith("http://") || url.startsWith("https://")) {
     678|                // For now, allow external URLs for CDNs and resources
     679|                return false;
```

### Line 678
**Comment:** For now, allow external URLs for CDNs and resources

**Context:**
```java
     674|            }
     675|
     676|            // Handle external URLs
>>>  677|            if (url.startsWith("http://") || url.startsWith("https://")) {
     678|                // For now, allow external URLs for CDNs and resources
     679|                return false;
     680|            }
     681|
```

### Line 689
**Comment:** Cache local files for better performance

**Context:**
```java
     685|        @Override
     686|        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
     687|            String url = request.getUrl().toString();
>>>  688|
     689|            // Cache local files for better performance
     690|            if (url.startsWith("file://") && projectDir != null) {
     691|                try {
     692|                    String filePath = url.replace("file://", "");
```

### Line 696
**Comment:** Check cache first

**Context:**
```java
     692|                    String filePath = url.replace("file://", "");
     693|                    File file = new File(filePath);
     694|
>>>  695|                    if (file.exists() && file.isFile()) {
     696|                        // Check cache first
     697|                        byte[] cachedData = fileCache.get(filePath);
     698|                        if (cachedData != null) {
     699|                            String mimeType = getMimeType(filePath);
```

### Line 704
**Comment:** Read and cache file if cache size allows

**Context:**
```java
     700|                            return new WebResourceResponse(mimeType, "UTF-8",
     701|                                new java.io.ByteArrayInputStream(cachedData));
     702|                        }
>>>  703|
     704|                        // Read and cache file if cache size allows
     705|                        if (file.length() < 5 * 1024 * 1024 && // Max 5MB per file
     706|                            currentCacheSize + file.length() < MAX_CACHE_SIZE) {
     707|
```

### Line 753
**Comment:** Optimized WebChromeClient

**Context:**
```java
     749|            addConsoleMessage("Error loading " + failingUrl + ": " + description);
     750|        }
     751|    }
>>>  752|
     753|    // Optimized WebChromeClient
     754|    private class OptimizedWebChromeClient extends WebChromeClient {
     755|
     756|        @Override
```

### Line 781
**Comment:** Enable file upload capability

**Context:**
```java
     777|                getSupportActionBar().setTitle(title != null && !title.isEmpty() ? title : projectName);
     778|            }
     779|        }
>>>  780|
     781|        // Enable file upload capability
     782|        @Override
     783|        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
     784|                                       FileChooserParams fileChooserParams) {
```

### Line 785
**Comment:** For a local development app, we can implement file picker if needed

**Context:**
```java
     781|        // Enable file upload capability
     782|        @Override
     783|        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
>>>  784|                                       FileChooserParams fileChooserParams) {
     785|            // For a local development app, we can implement file picker if needed
     786|            // For now, just return false to use default behavior
     787|            return false;
     788|        }
```

### Line 786
**Comment:** For now, just return false to use default behavior

**Context:**
```java
     782|        @Override
     783|        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
     784|                                       FileChooserParams fileChooserParams) {
>>>  785|            // For a local development app, we can implement file picker if needed
     786|            // For now, just return false to use default behavior
     787|            return false;
     788|        }
     789|    }
```

---

## com/codex/apk/ProjectManager.java

**Location:** `app/src/main/java/com/codex/apk/ProjectManager.java`

### Line 157
**Comment:** Set up project type selection

**Context:**
```java
     153|        ChipGroup templateStyleChipGroup = dialogView.findViewById(R.id.chip_group_template_style);
     154|        View templateStyleLabel = dialogView.findViewById(R.id.text_template_style_label);
     155|        View templateStyleScroll = dialogView.findViewById(R.id.scroll_template_style);
>>>  156|
     157|        // Set up project type selection
     158|        projectTypeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
     159|            if (checkedId == R.id.chip_empty) {
     160|                templateStyleLabel.setVisibility(View.GONE);
```

### Line 185
**Comment:** Get selected project type (only: empty, html_css_js, tailwind)

**Context:**
```java
     181|                    editTextProjectName.setError(context.getString(R.string.project_name_cannot_be_empty));
     182|                    return;
     183|                }
>>>  184|
     185|                // Get selected project type (only: empty, html_css_js, tailwind)
     186|                int selectedProjectTypeId = projectTypeChipGroup.getCheckedChipId();
     187|                String projectType = "empty";
     188|                if (selectedProjectTypeId == R.id.chip_html_css_js) {
```

### Line 194
**Comment:** Get selected template style

**Context:**
```java
     190|                } else if (selectedProjectTypeId == R.id.chip_tailwind) {
     191|                    projectType = "tailwind";
     192|                }
>>>  193|
     194|                // Get selected template style
     195|                String templateStyle = "basic";
     196|                if (templateStyleLabel.getVisibility() == View.VISIBLE) {
     197|                    int selectedTemplateStyleId = templateStyleChipGroup.getCheckedChipId();
```

### Line 284
**Comment:** no files

**Context:**
```java
     280|    private void createTemplateFiles(File projectDir, String projectName, String projectType, String templateStyle) throws IOException {
     281|        FileManager fileManager = new FileManager(context, projectDir);
     282|        switch (projectType) {
>>>  283|            case "empty":
     284|                // no files
     285|                break;
     286|            case "html_css_js":
     287|                createHtmlCssJsProject(projectDir, projectName, templateStyle, fileManager);
```

### Line 317
**Comment:** Tailwind via CDN, single index.html and optional script.js

**Context:**
```java
     313|        }
     314|    }
     315|
>>>  316|    private void createTailwindProject(File projectDir, String projectName, String templateStyle, FileManager fileManager) throws IOException {
     317|        // Tailwind via CDN, single index.html and optional script.js
     318|        fileManager.writeFileContent(new File(projectDir, "index.html"),
     319|                templateManager.getTailwindCdnHtmlTemplate(projectName));
     320|        fileManager.writeFileContent(new File(projectDir, "script.js"),
```

---

## com/codex/apk/ProjectsAdapter.java

**Location:** `app/src/main/java/com/codex/apk/ProjectsAdapter.java`

### Line 33
**Comment:** Multi-select state

**Context:**
```java
      29|    private final Context context;
      30|    private final ArrayList<HashMap<String, Object>> projectsList;
      31|    private final MainActivity mainActivity;
>>>   32|
      33|    // Multi-select state
      34|    private boolean selectionMode = false;
      35|    private final Set<Integer> selectedPositions = new HashSet<>();
      36|
```

### Line 138
**Comment:** Selection UI state

**Context:**
```java
     134|        void bind(final HashMap<String, Object> project, final int position) {
     135|            textProjectName.setText((String) project.get("name"));
     136|            textProjectDate.setText((String) project.get("lastModified"));
>>>  137|
     138|            // Selection UI state
     139|            boolean isSelected = selectedPositions.contains(position);
     140|            checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
     141|            imageMore.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
```

---

## com/codex/apk/QwenApiClient.java

**Location:** `app/src/main/java/com/codex/apk/QwenApiClient.java`

### Line 167
**Comment:** If this is the first message of a conversation, add the system prompt.

**Context:**
```java
     163|        requestBody.addProperty("parent_id", state.getLastParentId()); // Use last parent ID
     164|        requestBody.addProperty("timestamp", System.currentTimeMillis());
     165|
>>>  166|        JsonArray messages = new JsonArray();
     167|        // If this is the first message of a conversation, add the system prompt.
     168|        if (state.getLastParentId() == null) {
     169|            messages.add(createSystemMessage(enabledTools));
     170|        }
```

### Line 172
**Comment:** Add the current user message

**Context:**
```java
     168|        if (state.getLastParentId() == null) {
     169|            messages.add(createSystemMessage(enabledTools));
     170|        }
>>>  171|
     172|        // Add the current user message
     173|        JsonObject userMsg = createUserMessage(userMessage, model, thinkingModeEnabled, webSearchEnabled);
     174|        // Optional parity: set per-message parentId to match top-level
     175|        userMsg.addProperty("parentId", state.getLastParentId());
```

### Line 174
**Comment:** Optional parity: set per-message parentId to match top-level

**Context:**
```java
     170|        }
     171|
     172|        // Add the current user message
>>>  173|        JsonObject userMsg = createUserMessage(userMessage, model, thinkingModeEnabled, webSearchEnabled);
     174|        // Optional parity: set per-message parentId to match top-level
     175|        userMsg.addProperty("parentId", state.getLastParentId());
     176|        messages.add(userMsg);
     177|
```

### Line 212
**Comment:** Capture complete raw response for raw API response dialog (all lines)

**Context:**
```java
     208|        Set<String> seenWebUrls = new HashSet<>();
     209|
     210|        String line;
>>>  211|        while ((line = response.body().source().readUtf8Line()) != null) {
     212|            // Capture complete raw response for raw API response dialog (all lines)
     213|            completeRawResponse.append(line).append("\n");
     214|
     215|            String t = line.trim();
```

### Line 229
**Comment:** Check for conversation state updates

**Context:**
```java
     225|
     226|                try {
     227|                    JsonObject data = JsonParser.parseString(jsonData).getAsJsonObject();
>>>  228|
     229|                    // Check for conversation state updates
     230|                    if (data.has("response.created")) {
     231|                        JsonObject created = data.getAsJsonObject("response.created");
     232|                        if (created.has("chat_id")) state.setConversationId(created.get("chat_id").getAsString());
```

### Line 234
**Comment:** Persist state ASAP

**Context:**
```java
     230|                    if (data.has("response.created")) {
     231|                        JsonObject created = data.getAsJsonObject("response.created");
     232|                        if (created.has("chat_id")) state.setConversationId(created.get("chat_id").getAsString());
>>>  233|                        if (created.has("response_id")) state.setLastParentId(created.get("response_id").getAsString());
     234|                        // Persist state ASAP
     235|                        if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
     236|                        continue; // This line doesn't contain choices, so we skip to the next
     237|                    }
```

### Line 248
**Comment:** Accumulate per-phase content and signals

**Context:**
```java
     244|                            String status = delta.has("status") ? delta.get("status").getAsString() : "";
     245|                            String content = delta.has("content") ? delta.get("content").getAsString() : "";
     246|                            String phase = delta.has("phase") ? delta.get("phase").getAsString() : "";
>>>  247|
     248|                            // Accumulate per-phase content and signals
     249|                            if ("think".equals(phase)) {
     250|                                thinkingContent.append(content);
     251|                                if (actionListener != null) actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
```

### Line 254
**Comment:** Stream answer tokens too

**Context:**
```java
     250|                                thinkingContent.append(content);
     251|                                if (actionListener != null) actionListener.onAiStreamUpdate(thinkingContent.toString(), true);
     252|                            } else if ("answer".equals(phase)) {
>>>  253|                                answerContent.append(content);
     254|                                // Stream answer tokens too
     255|                                if (actionListener != null) actionListener.onAiStreamUpdate(answerContent.toString(), false);
     256|                            } else if ("web_search".equals(phase)) {
     257|                                // Harvest web search sources from function delta extras when available
```

### Line 257
**Comment:** Harvest web search sources from function delta extras when available

**Context:**
```java
     253|                                answerContent.append(content);
     254|                                // Stream answer tokens too
     255|                                if (actionListener != null) actionListener.onAiStreamUpdate(answerContent.toString(), false);
>>>  256|                            } else if ("web_search".equals(phase)) {
     257|                                // Harvest web search sources from function delta extras when available
     258|                                if (delta.has("extra") && delta.get("extra").isJsonObject()) {
     259|                                    JsonObject extra = delta.getAsJsonObject("extra");
     260|                                    if (extra.has("web_search_info") && extra.get("web_search_info").isJsonArray()) {
```

### Line 278
**Comment:** Only finalize when the ANSWER phase reports finished

**Context:**
```java
     274|                                    }
     275|                                }
     276|                            }
>>>  277|
     278|                            // Only finalize when the ANSWER phase reports finished
     279|                            if ("finished".equals(status) && "answer".equals(phase)) {
     280|                                String finalContent = answerContent.toString();
     281|                                String jsonToParse = extractJsonFromCodeBlock(finalContent);
```

### Line 288
**Comment:** Check for tool_call envelope

**Context:**
```java
     284|                                }
     285|
     286|                                if (jsonToParse != null) {
>>>  287|                                    try {
     288|                                        // Check for tool_call envelope
     289|                                        JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
     290|                                        if (maybe.has("action") && "tool_call".equals(maybe.get("action").getAsString())) {
     291|                                            // Execute tool calls, then continue in-loop by synthesizing a tool_result follow-up content
```

### Line 291
**Comment:** Execute tool calls, then continue in-loop by synthesizing a tool_result follow-up content

**Context:**
```java
     287|                                    try {
     288|                                        // Check for tool_call envelope
     289|                                        JsonObject maybe = JsonParser.parseString(jsonToParse).getAsJsonObject();
>>>  290|                                        if (maybe.has("action") && "tool_call".equals(maybe.get("action").getAsString())) {
     291|                                            // Execute tool calls, then continue in-loop by synthesizing a tool_result follow-up content
     292|                                            JsonArray calls = maybe.getAsJsonArray("tool_calls");
     293|                                            JsonArray results = new JsonArray();
     294|                                            for (int i = 0; i < calls.size(); i++) {
```

### Line 305
**Comment:** Synthesize a continuation request by posting the tool_result back as a user message

**Context:**
```java
     301|                                                try { res.add("result", JsonParser.parseString(toolResult)); }
     302|                                                catch (Exception ex) { res.addProperty("result", toolResult); }
     303|                                                results.add(res);
>>>  304|                                            }
     305|                                            // Synthesize a continuation request by posting the tool_result back as a user message
     306|                                            String continuation = buildToolResultContinuation(results);
     307|                                            // Swap out answerContent and restart completion using the same chat
     308|                                            performContinuation(state, model, continuation);
```

### Line 307
**Comment:** Swap out answerContent and restart completion using the same chat

**Context:**
```java
     303|                                                results.add(res);
     304|                                            }
     305|                                            // Synthesize a continuation request by posting the tool_result back as a user message
>>>  306|                                            String continuation = buildToolResultContinuation(results);
     307|                                            // Swap out answerContent and restart completion using the same chat
     308|                                            performContinuation(state, model, continuation);
     309|                                            continue;
     310|                                        }
```

### Line 336
**Comment:** Notify listener to save the updated state (final)

**Context:**
```java
     332|                                } else {
     333|                                    if (actionListener != null) notifyAiActionsProcessed(completeRawResponse.toString(), finalContent, new ArrayList<>(), new ArrayList<>(), model.getDisplayName(), thinkingContent.toString(), webSources);
     334|                                }
>>>  335|
     336|                                // Notify listener to save the updated state (final)
     337|                                if (actionListener != null) actionListener.onQwenConversationStateUpdated(state);
     338|                                break;
     339|                            }
```

### Line 357
**Comment:** Continue the same conversation by sending tool_result as a user message

**Context:**
```java
     353|        return payload.toString();
     354|    }
     355|
>>>  356|    private void performContinuation(QwenConversationState state, AIModel model, String toolResultJson) throws IOException {
     357|        // Continue the same conversation by sending tool_result as a user message
     358|        JsonObject requestBody = new JsonObject();
     359|        requestBody.addProperty("stream", true);
     360|        requestBody.addProperty("incremental_output", true);
```

### Line 383
**Comment:** Optional parity: set per-message parentId too

**Context:**
```java
     379|        featureConfig.addProperty("thinking_enabled", false);
     380|        featureConfig.addProperty("output_schema", "phase");
     381|        msg.add("feature_config", featureConfig);
>>>  382|        msg.addProperty("fid", java.util.UUID.randomUUID().toString());
     383|        // Optional parity: set per-message parentId too
     384|        msg.addProperty("parentId", state.getLastParentId());
     385|        msg.add("childrenIds", new JsonArray());
     386|        messages.add(msg);
```

### Line 429
**Comment:** parentId is set at the request level, not in individual messages (following StormX approach)

**Context:**
```java
     425|            featureConfig.addProperty("thinking_budget", 38912);
     426|        }
     427|        messageObj.add("feature_config", featureConfig);
>>>  428|        messageObj.addProperty("fid", java.util.UUID.randomUUID().toString());
     429|        // parentId is set at the request level, not in individual messages (following StormX approach)
     430|        messageObj.add("childrenIds", new JsonArray());
     431|        return messageObj;
     432|    }
```

### Line 449
**Comment:** Removed x-accel-buffering header as it may interfere with streaming

**Context:**
```java
     445|                .add("Sec-Fetch-Mode", "cors")
     446|                .add("Sec-Fetch-Site", "same-origin")
     447|                .add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
>>>  448|                .add("Source", "web");
     449|                // Removed x-accel-buffering header as it may interfere with streaming
     450|
     451|        if (conversationId != null) {
     452|            builder.add("Referer", "https://chat.qwen.ai/c/" + conversationId);
```

### Line 484
**Comment:** Save to SharedPreferences

**Context:**
```java
     480|            }
     481|            midToken = m.group(1);
     482|            midTokenUses = 1;
>>>  483|
     484|            // Save to SharedPreferences
     485|            sharedPreferences.edit().putString(QWEN_MIDTOKEN_KEY, midToken).apply();
     486|            Log.i(TAG, "Obtained and saved new midtoken. Use count: 1");
     487|            return midToken;
```

### Line 613
**Comment:** Truncate overly large responses to keep UI responsive

**Context:**
```java
     609|                    try (Response resp = httpClient.newCall(request).execute()) {
     610|                        if (resp.isSuccessful() && resp.body() != null) {
     611|                            String content = resp.body().string();
>>>  612|                            String type = resp.header("Content-Type", "");
     613|                            // Truncate overly large responses to keep UI responsive
     614|                            int max = 200_000;
     615|                            if (content.length() > max) content = content.substring(0, max);
     616|                            result.addProperty("ok", true);
```

### Line 663
**Comment:** Helper methods to support grepSearch

**Context:**
```java
     659|        }
     660|        return result.toString();
     661|    }
>>>  662|
     663|    // Helper methods to support grepSearch
     664|    private void grepWalk(java.io.File file, java.io.File projectRoot, java.util.regex.Pattern pattern,
     665|                          com.google.gson.JsonArray outMatches, int[] count, int maxMatches) {
     666|        if (count[0] >= maxMatches || file == null || !file.exists()) return;
```

### Line 679
**Comment:** Skip large/binary files

**Context:**
```java
     675|            }
     676|            return;
     677|        }
>>>  678|
     679|        // Skip large/binary files
     680|        long maxSize = 2_000_000; // 2MB
     681|        if (file.length() > maxSize || looksBinary(file)) return;
     682|
```

### Line 693
**Comment:** Limit line length in output

**Context:**
```java
     689|                if (pattern.matcher(line).find()) {
     690|                    com.google.gson.JsonObject m = new com.google.gson.JsonObject();
     691|                    m.addProperty("file", relPath(projectRoot, file));
>>>  692|                    m.addProperty("line", lineNo);
     693|                    // Limit line length in output
     694|                    String text = line;
     695|                    int maxLen = 500;
     696|                    if (text.length() > maxLen) text = text.substring(0, maxLen);
```

### Line 715
**Comment:** Heuristic: read first 4096 bytes; if there are NULs or many non-text chars, treat as binary

**Context:**
```java
     711|               n.equals(".nuxt") || n.equals("target");
     712|    }
     713|
>>>  714|    private boolean looksBinary(java.io.File f) {
     715|        // Heuristic: read first 4096 bytes; if there are NULs or many non-text chars, treat as binary
     716|        int sample = 4096;
     717|        byte[] buf = new byte[sample];
     718|        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
```

### Line 725
**Comment:** Allow common text control chars: tab, CR, LF, FF

**Context:**
```java
     721|            int nonText = 0;
     722|            for (int i = 0; i < read; i++) {
     723|                int b = buf[i] & 0xFF;
>>>  724|                if (b == 0) return true; // NUL byte
     725|                // Allow common text control chars: tab, CR, LF, FF
     726|                if (b < 0x09 || (b > 0x0D && b < 0x20)) nonText++;
     727|            }
     728|            return nonText > read * 0.3; // >30% suspicious
```

### Line 753
**Comment:** Look for ```json ... ``` pattern

**Context:**
```java
     749|        if (content == null || content.trim().isEmpty()) {
     750|            return null;
     751|        }
>>>  752|
     753|        // Look for ```json ... ``` pattern
     754|        String jsonPattern = "```json\\s*([\\s\\S]*?)```";
     755|        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(jsonPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
     756|        java.util.regex.Matcher matcher = pattern.matcher(content);
```

### Line 762
**Comment:** Also check for ``` ... ``` pattern (without json specifier)

**Context:**
```java
     758|        if (matcher.find()) {
     759|            return matcher.group(1).trim();
     760|        }
>>>  761|
     762|        // Also check for ``` ... ``` pattern (without json specifier)
     763|        String genericPattern = "```\\s*([\\s\\S]*?)```";
     764|        pattern = java.util.regex.Pattern.compile(genericPattern);
     765|        matcher = pattern.matcher(content);
```

### Line 769
**Comment:** Check if the extracted content looks like JSON

**Context:**
```java
     765|        matcher = pattern.matcher(content);
     766|
     767|        if (matcher.find()) {
>>>  768|            String extracted = matcher.group(1).trim();
     769|            // Check if the extracted content looks like JSON
     770|            if (QwenResponseParser.looksLikeJson(extracted)) {
     771|                return extracted;
     772|            }
```

### Line 837
**Comment:** Handle the case where 'data' is a single object

**Context:**
```java
     833|                                models.add(model);
     834|                            }
     835|                        }
>>>  836|                    } else if (responseJson.get("data").isJsonObject()) {
     837|                        // Handle the case where 'data' is a single object
     838|                        JsonObject modelData = responseJson.getAsJsonObject("data");
     839|                        AIModel model = parseModelData(modelData);
     840|                        if (model != null) {
```

### Line 862
**Comment:** Parse basic capabilities

**Context:**
```java
     858|            JsonObject info = modelData.getAsJsonObject("info");
     859|            JsonObject meta = info.getAsJsonObject("meta");
     860|            JsonObject capabilitiesJson = meta.getAsJsonObject("capabilities");
>>>  861|
     862|            // Parse basic capabilities
     863|            boolean supportsThinking = capabilitiesJson.has("thinking") && capabilitiesJson.get("thinking").getAsBoolean();
     864|            boolean supportsThinkingBudget = capabilitiesJson.has("thinking_budget") && capabilitiesJson.get("thinking_budget").getAsBoolean();
     865|            boolean supportsVision = capabilitiesJson.has("vision") && capabilitiesJson.get("vision").getAsBoolean();
```

### Line 871
**Comment:** Parse chat types and check for web search

**Context:**
```java
     867|            boolean supportsVideo = capabilitiesJson.has("video") && capabilitiesJson.get("video").getAsBoolean();
     868|            boolean supportsAudio = capabilitiesJson.has("audio") && capabilitiesJson.get("audio").getAsBoolean();
     869|            boolean supportsCitations = capabilitiesJson.has("citations") && capabilitiesJson.get("citations").getAsBoolean();
>>>  870|
     871|            // Parse chat types and check for web search
     872|            JsonArray chatTypes = meta.has("chat_type") ? meta.get("chat_type").getAsJsonArray() : new JsonArray();
     873|            boolean supportsWebSearch = false;
     874|            List<String> supportedChatTypes = new ArrayList<>();
```

### Line 883
**Comment:** Parse MCP tools

**Context:**
```java
     879|                    supportsWebSearch = true;
     880|                }
     881|            }
>>>  882|
     883|            // Parse MCP tools
     884|            List<String> mcpTools = new ArrayList<>();
     885|            if (meta.has("mcp")) {
     886|                JsonArray mcpArray = meta.get("mcp").getAsJsonArray();
```

### Line 893
**Comment:** Parse modalities

**Context:**
```java
     889|                }
     890|            }
     891|            boolean supportsMCP = !mcpTools.isEmpty();
>>>  892|
     893|            // Parse modalities
     894|            List<String> supportedModalities = new ArrayList<>();
     895|            if (meta.has("modality")) {
     896|                JsonArray modalityArray = meta.get("modality").getAsJsonArray();
```

### Line 902
**Comment:** Parse context and generation limits

**Context:**
```java
     898|                    supportedModalities.add(modalityArray.get(j).getAsString());
     899|                }
     900|            }
>>>  901|
     902|            // Parse context and generation limits
     903|            int maxContextLength = meta.has("max_context_length") ? meta.get("max_context_length").getAsInt() : 0;
     904|            int maxGenerationLength = meta.has("max_generation_length") ? meta.get("max_generation_length").getAsInt() : 0;
     905|            int maxThinkingGenerationLength = meta.has("max_thinking_generation_length") ? meta.get("max_thinking_generation_length").getAsInt() : 0;
```

### Line 908
**Comment:** Parse file limits

**Context:**
```java
     904|            int maxGenerationLength = meta.has("max_generation_length") ? meta.get("max_generation_length").getAsInt() : 0;
     905|            int maxThinkingGenerationLength = meta.has("max_thinking_generation_length") ? meta.get("max_thinking_generation_length").getAsInt() : 0;
     906|            int maxSummaryGenerationLength = meta.has("max_summary_generation_length") ? meta.get("max_summary_generation_length").getAsInt() : 0;
>>>  907|
     908|            // Parse file limits
     909|            Map<String, Integer> fileLimits = new HashMap<>();
     910|            if (meta.has("file_limits")) {
     911|                JsonObject fileLimitsJson = meta.getAsJsonObject("file_limits");
```

### Line 917
**Comment:** Parse abilities (numeric levels)

**Context:**
```java
     913|                    fileLimits.put(key, fileLimitsJson.get(key).getAsInt());
     914|                }
     915|            }
>>>  916|
     917|            // Parse abilities (numeric levels)
     918|            Map<String, Integer> abilities = new HashMap<>();
     919|            if (meta.has("abilities")) {
     920|                JsonObject abilitiesJson = meta.getAsJsonObject("abilities");
```

### Line 926
**Comment:** Parse single round flag

**Context:**
```java
     922|                    abilities.put(key, abilitiesJson.get(key).getAsInt());
     923|                }
     924|            }
>>>  925|
     926|            // Parse single round flag
     927|            boolean isSingleRound = meta.has("is_single_round") ? meta.get("is_single_round").getAsInt() == 1 : false;
     928|
     929|            // Create enhanced capabilities
```

### Line 929
**Comment:** Create enhanced capabilities

**Context:**
```java
     925|
     926|            // Parse single round flag
     927|            boolean isSingleRound = meta.has("is_single_round") ? meta.get("is_single_round").getAsInt() == 1 : false;
>>>  928|
     929|            // Create enhanced capabilities
     930|            ModelCapabilities capabilities = new ModelCapabilities(
     931|                supportsThinking, supportsWebSearch, supportsVision, supportsDocument,
     932|                supportsVideo, supportsAudio, supportsCitations, supportsThinkingBudget,
```

### Line 955
**Comment:** Prefer the richer handler in AiAssistantManager when available

**Context:**
```java
     951|                                          List<ChatMessage.FileActionDetail> fileActions,
     952|                                          String modelDisplayName,
     953|                                          String thinking,
>>>  954|                                          List<WebSource> sources) {
     955|        // Prefer the richer handler in AiAssistantManager when available
     956|        if (actionListener instanceof AiAssistantManager) {
     957|            ((AiAssistantManager) actionListener).onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, fileActions, modelDisplayName, thinking, sources);
     958|        } else {
```

### Line 959
**Comment:** Fallback to legacy interface without separate thinking/sources

**Context:**
```java
     955|        // Prefer the richer handler in AiAssistantManager when available
     956|        if (actionListener instanceof AiAssistantManager) {
     957|            ((AiAssistantManager) actionListener).onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, fileActions, modelDisplayName, thinking, sources);
>>>  958|        } else {
     959|            // Fallback to legacy interface without separate thinking/sources
     960|            String fallback = ResponseUtils.buildExplanationWithThinking(explanation, thinking);
     961|            actionListener.onAiActionsProcessed(rawAiResponseJson, fallback, suggestions, fileActions, modelDisplayName);
     962|        }
```

### Line 965
**Comment:** Enrich proposed file actions with old/new content to enable accurate diff previews

**Context:**
```java
     961|            actionListener.onAiActionsProcessed(rawAiResponseJson, fallback, suggestions, fileActions, modelDisplayName);
     962|        }
     963|    }
>>>  964|
     965|    // Enrich proposed file actions with old/new content to enable accurate diff previews
     966|    private void enrichFileActionDetails(List<ChatMessage.FileActionDetail> details) {
     967|        if (details == null || projectDir == null) return;
     968|        for (ChatMessage.FileActionDetail d : details) {
```

### Line 1005
**Comment:** full or unknown

**Context:**
```java
    1001|                            String pattern = d.searchPattern != null ? d.searchPattern : d.search;
    1002|                            String repl = d.replaceWith != null ? d.replaceWith : d.replace;
    1003|                            d.newContent = com.codex.apk.util.FileOps.applySearchReplace(old, pattern, repl);
>>> 1004|                        } else {
    1005|                            // full or unknown
    1006|                            if (d.newContent == null || d.newContent.isEmpty()) d.newContent = d.replaceWith != null ? d.replaceWith : "";
    1007|                        }
    1008|                        break;
```

### Line 1031
**Comment:** Applying a unified diff is non-trivial; leave newContent empty to rely on diffPatch

**Context:**
```java
    1027|                    }
    1028|                    case "patchFile": {
    1029|                        String old = com.codex.apk.util.FileOps.readFileSafe(new File(projectDir, d.path));
>>> 1030|                        d.oldContent = old;
    1031|                        // Applying a unified diff is non-trivial; leave newContent empty to rely on diffPatch
    1032|                        break;
    1033|                    }
    1034|                    default: {
```

### Line 1035
**Comment:** No-op

**Context:**
```java
    1031|                        // Applying a unified diff is non-trivial; leave newContent empty to rely on diffPatch
    1032|                        break;
    1033|                    }
>>> 1034|                    default: {
    1035|                        // No-op
    1036|                        break;
    1037|                    }
    1038|                }
```

### Line 1047
**Comment:** Sanitize the path to make it a valid preferences key

**Context:**
```java
    1043|    }
    1044|
    1045|    private String getProjectStateKey() {
>>> 1046|        if (projectDir == null) return null;
    1047|        // Sanitize the path to make it a valid preferences key
    1048|        return QWEN_CONVERSATION_STATE_KEY_PREFIX + projectDir.getAbsolutePath().replaceAll("[^a-zA-Z0-9_-]", "_");
    1049|    }
    1050|
```

### Line 1075
**Comment:** Fallback to a new state if parsing fails

**Context:**
```java
    1071|                Log.i(TAG, "Loaded conversation state for project: " + projectDir.getName());
    1072|                return gson.fromJson(jsonState, QwenConversationState.class);
    1073|            } catch (Exception e) {
>>> 1074|                Log.e(TAG, "Failed to load/parse conversation state.", e);
    1075|                // Fallback to a new state if parsing fails
    1076|                return new QwenConversationState();
    1077|            }
    1078|        }
```

---

## com/codex/apk/QwenConversationState.java

**Location:** `app/src/main/java/com/codex/apk/QwenConversationState.java`

### Line 35
**Comment:** Serialize to JSON string

**Context:**
```java
      31|        this.conversationId = null;
      32|        this.lastParentId = null;
      33|    }
>>>   34|
      35|    // Serialize to JSON string
      36|    public String toJson() {
      37|        return new Gson().toJson(this);
      38|    }
```

### Line 40
**Comment:** Deserialize from JSON string

**Context:**
```java
      36|    public String toJson() {
      37|        return new Gson().toJson(this);
      38|    }
>>>   39|
      40|    // Deserialize from JSON string
      41|    public static QwenConversationState fromJson(String json) {
      42|        if (json == null || json.isEmpty()) {
      43|            return new QwenConversationState();
```

---

## com/codex/apk/QwenResponseParser.java

**Location:** `app/src/main/java/com/codex/apk/QwenResponseParser.java`

### Line 43
**Comment:** Line-edit fields

**Context:**
```java
      39|        public final String oldPath;
      40|        public final String newPath;
      41|        public final String search;
>>>   42|        public final String replace;
      43|        // Line-edit fields
      44|        public final Integer startLine;
      45|        public final Integer deleteCount;
      46|        public final List<String> insertLines;
```

### Line 47
**Comment:** Advanced fields

**Context:**
```java
      43|        // Line-edit fields
      44|        public final Integer startLine;
      45|        public final Integer deleteCount;
>>>   46|        public final List<String> insertLines;
      47|        // Advanced fields
      48|        public final String updateType;
      49|        public final String searchPattern;
      50|        public final String replaceWith;
```

### Line 116
**Comment:** Plan response (more flexible: any JSON with a "steps" array is considered a plan)

**Context:**
```java
     112|        try {
     113|            Log.d(TAG, "Parsing response: " + responseText.substring(0, Math.min(200, responseText.length())) + "...");
     114|            JsonObject jsonObj = JsonParser.parseString(responseText).getAsJsonObject();
>>>  115|
     116|            // Plan response (more flexible: any JSON with a "steps" array is considered a plan)
     117|            if (jsonObj.has("steps") && jsonObj.get("steps").isJsonArray()) {
     118|                List<PlanStep> steps = new ArrayList<>();
     119|                JsonArray arr = jsonObj.getAsJsonArray("steps");
```

### Line 131
**Comment:** Check for multi-operation format first

**Context:**
```java
     127|                String explanation = jsonObj.has("goal") ? ("Plan for: " + jsonObj.get("goal").getAsString()) : "Plan";
     128|                return new ParsedResponse("plan", new ArrayList<>(), steps, explanation, true);
     129|            }
>>>  130|
     131|            // Check for multi-operation format first
     132|            if (jsonObj.has("operations") && jsonObj.get("operations").isJsonArray()) {
     133|                Log.d(TAG, "Detected 'operations' array, parsing as multi-operation response");
     134|                return parseFileOperationResponse(jsonObj);
```

### Line 137
**Comment:** Fallback to single-operation if 'operations' is not present

**Context:**
```java
     133|                Log.d(TAG, "Detected 'operations' array, parsing as multi-operation response");
     134|                return parseFileOperationResponse(jsonObj);
     135|            }
>>>  136|
     137|            // Fallback to single-operation if 'operations' is not present
     138|            if (jsonObj.has("action") && isSingleFileAction(jsonObj.get("action").getAsString())) {
     139|                Log.d(TAG, "Detected single-operation response with action: " + jsonObj.get("action").getAsString());
     140|                List<FileOperation> operations = new ArrayList<>();
```

### Line 149
**Comment:** Advanced fields

**Context:**
```java
     145|                String newPath = jsonObj.has("newPath") ? jsonObj.get("newPath").getAsString() : "";
     146|                String search = jsonObj.has("search") ? jsonObj.get("search").getAsString() : "";
     147|                String replace = jsonObj.has("replace") ? jsonObj.get("replace").getAsString() : "";
>>>  148|
     149|                // Advanced fields
     150|                String updateType = jsonObj.has("updateType") ? jsonObj.get("updateType").getAsString() : null;
     151|                String searchPattern = jsonObj.has("searchPattern") ? jsonObj.get("searchPattern").getAsString() : null;
     152|                String replaceWith = jsonObj.has("replaceWith") ? jsonObj.get("replaceWith").getAsString() : null;
```

### Line 179
**Comment:** Fallback: check if the root object itself is a single file operation

**Context:**
```java
     175|                String explanation = jsonObj.has("explanation") ? jsonObj.get("explanation").getAsString() : "";
     176|                return new ParsedResponse(type, operations, new ArrayList<>(), explanation, true);
     177|            }
>>>  178|
     179|            // Fallback: check if the root object itself is a single file operation
     180|            if (jsonObj.has("type") && isSingleFileAction(jsonObj.get("type").getAsString())) {
     181|                Log.d(TAG, "Detected root object as single file operation with type: " + jsonObj.get("type").getAsString());
     182|                List<FileOperation> operations = new ArrayList<>();
```

### Line 191
**Comment:** Advanced fields

**Context:**
```java
     187|                String newPath = jsonObj.has("newPath") ? jsonObj.get("newPath").getAsString() : "";
     188|                String search = jsonObj.has("search") ? jsonObj.get("search").getAsString() : "";
     189|                String replace = jsonObj.has("replace") ? jsonObj.get("replace").getAsString() : "";
>>>  190|
     191|                // Advanced fields
     192|                String updateType = jsonObj.has("updateType") ? jsonObj.get("updateType").getAsString() : null;
     193|                String searchPattern = jsonObj.has("searchPattern") ? jsonObj.get("searchPattern").getAsString() : null;
     194|                String replaceWith = jsonObj.has("replaceWith") ? jsonObj.get("replaceWith").getAsString() : null;
```

### Line 222
**Comment:** Fallback for non-file operation JSON

**Context:**
```java
     218|                return new ParsedResponse(type, operations, new ArrayList<>(), explanation, true);
     219|            }
     220|
>>>  221|            Log.d(TAG, "Not a recognized file operation response, treating as regular JSON");
     222|            // Fallback for non-file operation JSON
     223|            return parseRegularJsonResponse(jsonObj);
     224|        } catch (JsonParseException e) {
     225|            Log.w(TAG, "Failed to parse JSON response: " + responseText, e);
```

### Line 247
**Comment:** If modifyLines is present with search/replace pairs, expand into multiple searchAndReplace operations

**Context:**
```java
     243|
     244|                String type = operation.get("type").getAsString();
     245|                String path = operation.has("path") ? operation.get("path").getAsString() : "";
>>>  246|
     247|                // If modifyLines is present with search/replace pairs, expand into multiple searchAndReplace operations
     248|                if (operation.has("modifyLines") && operation.get("modifyLines").isJsonArray()) {
     249|                    JsonArray hunks = operation.getAsJsonArray("modifyLines");
     250|                    for (int j = 0; j < hunks.size(); j++) {
```

### Line 275
**Comment:** Advanced fields

**Context:**
```java
     271|                String newPath = operation.has("newPath") ? operation.get("newPath").getAsString() : "";
     272|                String search = operation.has("search") ? operation.get("search").getAsString() : "";
     273|                String replace = operation.has("replace") ? operation.get("replace").getAsString() : "";
>>>  274|
     275|                // Advanced fields
     276|                String updateType = operation.has("updateType") ? operation.get("updateType").getAsString() : null;
     277|                String searchPattern = operation.has("searchPattern") ? operation.get("searchPattern").getAsString() : null;
     278|                String replaceWith = operation.has("replaceWith") ? operation.get("replaceWith").getAsString() : null;
```

### Line 311
**Comment:** For regular JSON responses, we don't have specific operations

**Context:**
```java
     307|    /**
     308|     * Parses a regular JSON response (non-file operation)
     309|     */
>>>  310|    private static ParsedResponse parseRegularJsonResponse(JsonObject jsonObj) {
     311|        // For regular JSON responses, we don't have specific operations
     312|        return new ParsedResponse("json_response", new ArrayList<>(), new ArrayList<>(),
     313|                                jsonObj.toString(), true);
     314|    }
```

### Line 354
**Comment:** Map advanced fields

**Context:**
```java
     350|                op.deleteCount != null ? op.deleteCount : 0,
     351|                op.insertLines,
     352|                op.search, op.replace
>>>  353|            );
     354|            // Map advanced fields
     355|            if (op.updateType != null) detail.updateType = op.updateType;
     356|            if (op.searchPattern != null) detail.searchPattern = op.searchPattern;
     357|            if (op.replaceWith != null) detail.replaceWith = op.replaceWith;
```

---

## com/codex/apk/SettingsActivity.java

**Location:** `app/src/main/java/com/codex/apk/SettingsActivity.java`

### Line 37
**Comment:** Set up theme based on user preferences

**Context:**
```java
      33|
      34|	@Override
      35|	protected void onCreate(Bundle savedInstanceState) {
>>>   36|		super.onCreate(savedInstanceState);
      37|		// Set up theme based on user preferences
      38|		ThemeManager.setupTheme(this);
      39|
      40|		try {
```

### Line 43
**Comment:** Initialize toolbar

**Context:**
```java
      39|
      40|		try {
      41|			setContentView(R.layout.settings);
>>>   42|
      43|			// Initialize toolbar
      44|			toolbar = findViewById(R.id.toolbar);
      45|			if (toolbar != null) {
      46|				setSupportActionBar(toolbar);
```

### Line 53
**Comment:** Settings UI is directly in the layout, no fragment needed

**Context:**
```java
      49|					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      50|				}
      51|			}
>>>   52|
      53|			// Settings UI is directly in the layout, no fragment needed
      54|			// Initialize settings controls
      55|			initializeSettings();
      56|		} catch (Exception e) {
```

### Line 54
**Comment:** Initialize settings controls

**Context:**
```java
      50|				}
      51|			}
      52|
>>>   53|			// Settings UI is directly in the layout, no fragment needed
      54|			// Initialize settings controls
      55|			initializeSettings();
      56|		} catch (Exception e) {
      57|			Toast.makeText(this, "Error loading settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
```

### Line 72
**Comment:** Initialize settings controls from the layout

**Context:**
```java
      68|		return super.onOptionsItemSelected(item);
      69|	}
      70|
>>>   71|	private void initializeSettings() {
      72|		// Initialize settings controls from the layout
      73|		com.google.android.material.textfield.TextInputEditText apiKeyEditText = findViewById(R.id.edit_text_api_key);
      74|		com.google.android.material.textfield.TextInputEditText cookie1psidEditText = findViewById(R.id.edit_text_secure_1psid);
      75|		com.google.android.material.textfield.TextInputEditText cookie1psidtsEditText = findViewById(R.id.edit_text_secure_1psidts);
```

### Line 86
**Comment:** Load saved settings

**Context:**
```java
      82|		com.google.android.material.materialswitch.MaterialSwitch wrapSwitch = findViewById(R.id.switch_wrap);
      83|		com.google.android.material.materialswitch.MaterialSwitch readOnlySwitch = findViewById(R.id.switch_read_only);
      84|		com.google.android.material.slider.Slider fontSizeSlider = findViewById(R.id.slider_font_size);
>>>   85|
      86|		// Load saved settings
      87|		SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
      88|		SharedPreferences defaultPrefs = getPreferences(this);
      89|		String savedApiKey = prefs.getString("gemini_api_key", "");
```

### Line 104
**Comment:** Clicks

**Context:**
```java
     100|		if (fontSizeSlider != null) {
     101|			fontSizeSlider.setValue(getFontSize(this));
     102|		}
>>>  103|
     104|		// Clicks
     105|		if (themeSelectorLayout != null) themeSelectorLayout.setOnClickListener(v -> showThemeSelector());
     106|		if (modelsCard != null) modelsCard.setOnClickListener(v -> {
     107|			startActivity(new Intent(this, ModelsActivity.class));
```

### Line 119
**Comment:** Switch Listeners

**Context:**
```java
     115|		if (promptsCard != null) promptsCard.setOnClickListener(v -> {
     116|			startActivity(new Intent(this, PromptsActivity.class));
     117|		});
>>>  118|
     119|		// Switch Listeners
     120|		if (wrapSwitch != null) {
     121|			wrapSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
     122|				getPreferences(this).edit().putBoolean("default_word_wrap", isChecked).apply();
```

### Line 178
**Comment:** Save the theme preference using default preferences

**Context:**
```java
     174|				if (selectedThemeText != null) {
     175|					selectedThemeText.setText(selectedThemeDisplay);
     176|				}
>>>  177|
     178|				// Save the theme preference using default preferences
     179|				getPreferences(this)
     180|					.edit()
     181|					.putString("app_theme", selectedTheme)
```

### Line 184
**Comment:** Apply theme immediately

**Context:**
```java
     180|					.edit()
     181|					.putString("app_theme", selectedTheme)
     182|					.apply();
>>>  183|
     184|				// Apply theme immediately
     185|				ThemeManager.switchTheme(this, selectedTheme);
     186|
     187|				dialog.dismiss();
```

### Line 205
**Comment:** Helper method to get preferences

**Context:**
```java
     201|		}
     202|		return "Light"; // Default
     203|	}
>>>  204|
     205|	// Helper method to get preferences
     206|	public static SharedPreferences getPreferences(android.content.Context context) {
     207|		return context.getSharedPreferences("settings", MODE_PRIVATE);
     208|	}
```

### Line 210
**Comment:** Helper methods to get specific settings

**Context:**
```java
     206|	public static SharedPreferences getPreferences(android.content.Context context) {
     207|		return context.getSharedPreferences("settings", MODE_PRIVATE);
     208|	}
>>>  209|
     210|	// Helper methods to get specific settings
     211|	public static String getGeminiApiKey(android.content.Context context) {
     212|		return getPreferences(context).getString("gemini_api_key", "");
     213|	}
```

### Line 266
**Comment:** Custom prompt settings

**Context:**
```java
     262|	public static String getSecure1PSIDTS(Context context) {
     263|		return getPreferences(context).getString("secure_1psidts", "");
     264|	}
>>>  265|
     266|	// Custom prompt settings
     267|	public static String getCustomFileOpsPrompt(Context context) {
     268|		return getPreferences(context).getString("custom_fileops_prompt", "");
     269|	}
```

### Line 311
**Comment:** Cache helpers for __Secure-1PSIDTS keyed by the 1PSID value

**Context:**
```java
     307|		}
     308|		getPreferences(context).edit().putString("custom_agents", arr.toString()).apply();
     309|	}
>>>  310|
     311|	// Cache helpers for __Secure-1PSIDTS keyed by the 1PSID value
     312|	public static String getCached1psidts(Context context, String psid) {
     313|		if (psid == null || psid.isEmpty()) return "";
     314|		return getPreferences(context).getString("cached_1psidts_" + psid, "");
```

### Line 321
**Comment:** Free provider chat metadata cache (per model id). Value is JSON array string like [cid, rid, rcid]

**Context:**
```java
     317|		if (psid == null || psid.isEmpty() || psidts == null || psidts.isEmpty()) return;
     318|		getPreferences(context).edit().putString("cached_1psidts_" + psid, psidts).apply();
     319|	}
>>>  320|
     321|	// Free provider chat metadata cache (per model id). Value is JSON array string like [cid, rid, rcid]
     322|	public static String getFreeConversationMetadata(Context context, String modelId) {
     323|		if (modelId == null) return "";
     324|		return getPreferences(context).getString("free_conv_meta_" + modelId, "");
```

---

## com/codex/apk/SimpleSoraTabAdapter.java

**Location:** `app/src/main/java/com/codex/apk/SimpleSoraTabAdapter.java`

### Line 49
**Comment:** LRU cache for parsed diffs per tabId with content hash to avoid re-parsing

**Context:**
```java
      45|    private final List<TabItem> openTabs;
      46|    private final TabActionListener tabActionListener;
      47|    private final FileManager fileManager;
>>>   48|    private final Map<Integer, ViewHolder> holders = new HashMap<>();
      49|    // LRU cache for parsed diffs per tabId with content hash to avoid re-parsing
      50|    private static final int MAX_DIFF_CACHE = 16;
      51|
      52|    private static class DiffCacheEntry {
```

### Line 65
**Comment:** Current active tab position

**Context:**
```java
      61|        }
      62|    };
      63|
>>>   64|
      65|    // Current active tab position
      66|    private int activeTabPosition = 0;
      67|
      68|    /**
```

### Line 150
**Comment:** Not an SVG, or SVG parsing failed. Try to load as a bitmap.

**Context:**
```java
     146|                if (svg.getDocumentWidth() != -1) {
     147|                    android.graphics.drawable.PictureDrawable drawable = new android.graphics.drawable.PictureDrawable(svg.renderToPicture());
     148|                    imageViewHolder.imageView.setImageDrawable(drawable);
>>>  149|                } else {
     150|                    // Not an SVG, or SVG parsing failed. Try to load as a bitmap.
     151|                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(tabItem.getFile().getAbsolutePath());
     152|                    imageViewHolder.imageView.setImageBitmap(bitmap);
     153|                }
```

### Line 155
**Comment:** Not an SVG, or SVG parsing failed. Try to load as a bitmap.

**Context:**
```java
     151|                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(tabItem.getFile().getAbsolutePath());
     152|                    imageViewHolder.imageView.setImageBitmap(bitmap);
     153|                }
>>>  154|            } catch (Exception e) {
     155|                // Not an SVG, or SVG parsing failed. Try to load as a bitmap.
     156|                try {
     157|                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(tabItem.getFile().getAbsolutePath());
     158|                    imageViewHolder.imageView.setImageBitmap(bitmap);
```

### Line 179
**Comment:** Only reconfigure if this is a different tab

**Context:**
```java
     175|        String tabId = tabItem.getFile().getAbsolutePath();
     176|        CodeEditor codeEditor = editorViewHolder.codeEditor;
     177|        boolean isDiffTab = tabItem.getFile().getName().startsWith("DIFF_");
>>>  178|
     179|        // Only reconfigure if this is a different tab
     180|        if (!tabId.equals(editorViewHolder.currentTabId)) {
     181|            editorViewHolder.currentTabId = tabId;
     182|
```

### Line 183
**Comment:** Configure the editor only for new tabs (skip heavy setup for DIFF_ tabs)

**Context:**
```java
     179|        // Only reconfigure if this is a different tab
     180|        if (!tabId.equals(editorViewHolder.currentTabId)) {
     181|            editorViewHolder.currentTabId = tabId;
>>>  182|
     183|            // Configure the editor only for new tabs (skip heavy setup for DIFF_ tabs)
     184|            if (!isDiffTab) {
     185|                configureEditor(codeEditor, tabItem);
     186|                // Apply persistent flags
```

### Line 186
**Comment:** Apply persistent flags

**Context:**
```java
     182|
     183|            // Configure the editor only for new tabs (skip heavy setup for DIFF_ tabs)
     184|            if (!isDiffTab) {
>>>  185|                configureEditor(codeEditor, tabItem);
     186|                // Apply persistent flags
     187|                codeEditor.setWordwrap(tabItem.isWrapEnabled());
     188|                codeEditor.setEditable(!tabItem.isReadOnly());
     189|            } else {
```

### Line 190
**Comment:** For diff tabs, keep editor lightweight & disabled

**Context:**
```java
     186|                // Apply persistent flags
     187|                codeEditor.setWordwrap(tabItem.isWrapEnabled());
     188|                codeEditor.setEditable(!tabItem.isReadOnly());
>>>  189|            } else {
     190|                // For diff tabs, keep editor lightweight & disabled
     191|                codeEditor.setText("");
     192|                codeEditor.setEditable(false);
     193|            }
```

### Line 195
**Comment:** Set up content change listener only once per tab

**Context:**
```java
     191|                codeEditor.setText("");
     192|                codeEditor.setEditable(false);
     193|            }
>>>  194|
     195|            // Set up content change listener only once per tab
     196|            if (!editorViewHolder.isListenerAttached) {
     197|                codeEditor.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent.class, (event, unsubscribe) -> {
     198|                    // Get the current tab item for this holder
```

### Line 198
**Comment:** Get the current tab item for this holder

**Context:**
```java
     194|
     195|            // Set up content change listener only once per tab
     196|            if (!editorViewHolder.isListenerAttached) {
>>>  197|                codeEditor.subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent.class, (event, unsubscribe) -> {
     198|                    // Get the current tab item for this holder
     199|                    int currentPos = editorViewHolder.getAdapterPosition();
     200|                    if (currentPos != RecyclerView.NO_POSITION && currentPos < openTabs.size()) {
     201|                        TabItem currentTabItem = openTabs.get(currentPos);
```

### Line 204
**Comment:** Only update if content actually changed

**Context:**
```java
     200|                    if (currentPos != RecyclerView.NO_POSITION && currentPos < openTabs.size()) {
     201|                        TabItem currentTabItem = openTabs.get(currentPos);
     202|                        String newContent = codeEditor.getText().toString();
>>>  203|
     204|                        // Only update if content actually changed
     205|                        if (!currentTabItem.getContent().equals(newContent)) {
     206|                            // Update content immediately for responsive typing
     207|                            currentTabItem.setContent(newContent);
```

### Line 206
**Comment:** Update content immediately for responsive typing

**Context:**
```java
     202|                        String newContent = codeEditor.getText().toString();
     203|
     204|                        // Only update if content actually changed
>>>  205|                        if (!currentTabItem.getContent().equals(newContent)) {
     206|                            // Update content immediately for responsive typing
     207|                            currentTabItem.setContent(newContent);
     208|                            currentTabItem.setModified(true);
     209|                            if (tabActionListener != null) {
```

### Line 225
**Comment:** Toggle between editor and diff view every bind to reflect latest state/content

**Context:**
```java
     221|                codeEditor.setText(tabItem.getContent());
     222|            }
     223|        }
>>>  224|
     225|        // Toggle between editor and diff view every bind to reflect latest state/content
     226|        if (isDiffTab) {
     227|            // Show diff view
     228|            codeEditor.setVisibility(View.GONE);
```

### Line 227
**Comment:** Show diff view

**Context:**
```java
     223|        }
     224|
     225|        // Toggle between editor and diff view every bind to reflect latest state/content
>>>  226|        if (isDiffTab) {
     227|            // Show diff view
     228|            codeEditor.setVisibility(View.GONE);
     229|            if (editorViewHolder.diffRecycler != null) {
     230|                editorViewHolder.diffRecycler.setVisibility(View.VISIBLE);
```

### Line 236
**Comment:** Parse and bind diff lines with LRU caching

**Context:**
```java
     232|                    editorViewHolder.diffRecycler.setLayoutManager(new LinearLayoutManager(context));
     233|                    editorViewHolder.diffRecycler.setHasFixedSize(true);
     234|                    editorViewHolder.diffRecycler.setItemViewCacheSize(64);
>>>  235|                }
     236|                // Parse and bind diff lines with LRU caching
     237|                String key = tabId;
     238|                String content = tabItem.getContent();
     239|                int h = content != null ? content.hashCode() : 0;
```

### Line 259
**Comment:** Show normal editor

**Context:**
```java
     255|                    editorViewHolder.diffAdapter.updateLines(lines);
     256|                }
     257|            }
>>>  258|        } else {
     259|            // Show normal editor
     260|            codeEditor.setVisibility(View.VISIBLE);
     261|            if (editorViewHolder.diffRecycler != null) {
     262|                editorViewHolder.diffRecycler.setVisibility(View.GONE);
```

### Line 268
**Comment:** Apply active tab styling if this is the active tab

**Context:**
```java
     264|                editorViewHolder.diffAdapter = null;
     265|            }
     266|        }
>>>  267|
     268|        // Apply active tab styling if this is the active tab
     269|        if (position == activeTabPosition) {
     270|            codeEditor.requestFocus();
     271|            if (tabActionListener != null) {
```

### Line 292
**Comment:** Choose language scope based on file extension

**Context:**
```java
     288|    private void configureEditor(CodeEditor codeEditor, TabItem tabItem) {
     289|        String fileName = tabItem.getFileName();
     290|        ensureTextMateInitialized(codeEditor.getContext());
>>>  291|
     292|        // Choose language scope based on file extension
     293|        String scope = resolveScopeForFile(fileName);
     294|        try {
     295|            if (scope != null) {
```

### Line 296
**Comment:** Apply TextMate color scheme and language (with completion enabled)

**Context:**
```java
     292|        // Choose language scope based on file extension
     293|        String scope = resolveScopeForFile(fileName);
     294|        try {
>>>  295|            if (scope != null) {
     296|                // Apply TextMate color scheme and language (with completion enabled)
     297|                codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
     298|                codeEditor.setEditorLanguage(TextMateLanguage.create(scope, true));
     299|            } else {
```

### Line 300
**Comment:** Fallback if not supported

**Context:**
```java
     296|                // Apply TextMate color scheme and language (with completion enabled)
     297|                codeEditor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
     298|                codeEditor.setEditorLanguage(TextMateLanguage.create(scope, true));
>>>  299|            } else {
     300|                // Fallback if not supported
     301|                codeEditor.setEditorLanguage(new EmptyLanguage());
     302|                codeEditor.setColorScheme(new EditorColorScheme());
     303|            }
```

### Line 310
**Comment:** Configure indentation (block) guide colors to ensure they are visible

**Context:**
```java
     306|            codeEditor.setEditorLanguage(new EmptyLanguage());
     307|            codeEditor.setColorScheme(new EditorColorScheme());
     308|        }
>>>  309|
     310|        // Configure indentation (block) guide colors to ensure they are visible
     311|        // Uses app palette: file tree indent color for normal guides and primary_light for current block
     312|        EditorColorScheme scheme = codeEditor.getColorScheme();
     313|        int indentColor = ContextCompat.getColor(codeEditor.getContext(), R.color.file_tree_indent_color);
```

### Line 311
**Comment:** Uses app palette: file tree indent color for normal guides and primary_light for current block

**Context:**
```java
     307|            codeEditor.setColorScheme(new EditorColorScheme());
     308|        }
     309|
>>>  310|        // Configure indentation (block) guide colors to ensure they are visible
     311|        // Uses app palette: file tree indent color for normal guides and primary_light for current block
     312|        EditorColorScheme scheme = codeEditor.getColorScheme();
     313|        int indentColor = ContextCompat.getColor(codeEditor.getContext(), R.color.file_tree_indent_color);
     314|        int currentIndentColor = ContextCompat.getColor(codeEditor.getContext(), R.color.primary_light);
```

### Line 320
**Comment:** Configure editor appearance & ergonomics using Settings defaults and tab state

**Context:**
```java
     316|        scheme.setColor(EditorColorScheme.SIDE_BLOCK_LINE, indentColor);
     317|        scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, currentIndentColor);
     318|        codeEditor.invalidate();
>>>  319|
     320|        // Configure editor appearance & ergonomics using Settings defaults and tab state
     321|        float textSizeSp = SettingsActivity.getFontSize(codeEditor.getContext());
     322|        codeEditor.setTextSize(textSizeSp);
     323|        codeEditor.setLineNumberEnabled(SettingsActivity.isLineNumbersEnabled(codeEditor.getContext()));
```

### Line 335
**Comment:** Apply read-only from tab state or default setting

**Context:**
```java
     331|        codeEditor.setCursorAnimationEnabled(true);
     332|        codeEditor.setTabWidth(2);
     333|        codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);
>>>  334|
     335|        // Apply read-only from tab state or default setting
     336|        boolean readOnly = tabItem.isReadOnly() || SettingsActivity.isDefaultReadOnly(codeEditor.getContext());
     337|        codeEditor.setEditable(!readOnly);
     338|        // Performance tweaks
```

### Line 338
**Comment:** Performance tweaks

**Context:**
```java
     334|
     335|        // Apply read-only from tab state or default setting
     336|        boolean readOnly = tabItem.isReadOnly() || SettingsActivity.isDefaultReadOnly(codeEditor.getContext());
>>>  337|        codeEditor.setEditable(!readOnly);
     338|        // Performance tweaks
     339|        codeEditor.setInterceptParentHorizontalScrollIfNeeded(true);
     340|        codeEditor.setBasicDisplayMode(false);
     341|
```

### Line 342
**Comment:** Reduce scrollbars for a cleaner, mobile-friendly UI

**Context:**
```java
     338|        // Performance tweaks
     339|        codeEditor.setInterceptParentHorizontalScrollIfNeeded(true);
     340|        codeEditor.setBasicDisplayMode(false);
>>>  341|
     342|        // Reduce scrollbars for a cleaner, mobile-friendly UI
     343|        codeEditor.setScrollBarEnabled(false);
     344|        codeEditor.setVerticalScrollBarEnabled(false);
     345|        codeEditor.setHorizontalScrollBarEnabled(false);
```

### Line 363
**Comment:** Register assets resolver for TextMate

**Context:**
```java
     359|    private static synchronized void ensureTextMateInitialized(Context context) {
     360|        if (textMateInitialized) return;
     361|        try {
>>>  362|            Context appContext = context.getApplicationContext();
     363|            // Register assets resolver for TextMate
     364|            FileProviderRegistry.getInstance().addFileProvider(
     365|                    new AssetsFileResolver(appContext.getAssets())
     366|            );
```

### Line 368
**Comment:** Load and activate theme

**Context:**
```java
     364|            FileProviderRegistry.getInstance().addFileProvider(
     365|                    new AssetsFileResolver(appContext.getAssets())
     366|            );
>>>  367|
     368|            // Load and activate theme
     369|            String themePath = TEXTMATE_THEME_PATH;
     370|            IThemeSource source = IThemeSource.fromInputStream(
     371|                    FileProviderRegistry.getInstance().tryGetInputStream(themePath),
```

### Line 379
**Comment:** Load grammars from assets

**Context:**
```java
     375|            ThemeModel model = new ThemeModel(source, TEXTMATE_THEME_NAME);
     376|            ThemeRegistry.getInstance().loadTheme(model);
     377|            ThemeRegistry.getInstance().setTheme(TEXTMATE_THEME_NAME);
>>>  378|
     379|            // Load grammars from assets
     380|            GrammarRegistry.getInstance().loadGrammars(TEXTMATE_LANG_INDEX);
     381|            textMateInitialized = true;
     382|        } catch (Throwable t) {
```

### Line 422
**Comment:** Notify both old and new positions for a more precise update

**Context:**
```java
     418|        if (position >= 0 && position < openTabs.size()) {
     419|            int oldPosition = activeTabPosition;
     420|            activeTabPosition = position;
>>>  421|
     422|            // Notify both old and new positions for a more precise update
     423|            notifyItemChanged(oldPosition);
     424|            notifyItemChanged(activeTabPosition);
     425|
```

### Line 445
**Comment:** Try to find a RecyclerView parent to query ViewHolder

**Context:**
```java
     441|     */
     442|    public void setWrapForPosition(int position, boolean enable) {
     443|        RecyclerView recycler = null;
>>>  444|        try {
     445|            // Try to find a RecyclerView parent to query ViewHolder
     446|            // This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.
     447|            // Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.
     448|            // For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.
```

### Line 446
**Comment:** This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.

**Context:**
```java
     442|    public void setWrapForPosition(int position, boolean enable) {
     443|        RecyclerView recycler = null;
     444|        try {
>>>  445|            // Try to find a RecyclerView parent to query ViewHolder
     446|            // This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.
     447|            // Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.
     448|            // For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.
     449|            if (position >= 0 && position < openTabs.size()) {
```

### Line 447
**Comment:** Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.

**Context:**
```java
     443|        RecyclerView recycler = null;
     444|        try {
     445|            // Try to find a RecyclerView parent to query ViewHolder
>>>  446|            // This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.
     447|            // Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.
     448|            // For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.
     449|            if (position >= 0 && position < openTabs.size()) {
     450|                TabItem tab = openTabs.get(position);
```

### Line 448
**Comment:** For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.

**Context:**
```java
     444|        try {
     445|            // Try to find a RecyclerView parent to query ViewHolder
     446|            // This adapter is attached to the ViewPager2's internal RecyclerView, but we can't access it directly here.
>>>  447|            // Instead, request a re-bind and apply in onBindViewHolder by storing the preference on TabItem temporarily if needed.
     448|            // For simplicity, just notifyItemChanged to rebind and then apply in onBindViewHolder via a flag on TabItem.
     449|            if (position >= 0 && position < openTabs.size()) {
     450|                TabItem tab = openTabs.get(position);
     451|                tab.setWrapEnabled(enable);
```

### Line 485
**Comment:** Detach diff adapter to help GC

**Context:**
```java
     481|    public void onViewRecycled(@NonNull RecyclerView.ViewHolder rawHolder) {
     482|        if (rawHolder instanceof ViewHolder) {
     483|            ViewHolder holder = (ViewHolder) rawHolder;
>>>  484|            holders.remove(holder.getAdapterPosition());
     485|            // Detach diff adapter to help GC
     486|            if (holder.diffRecycler != null) {
     487|                holder.diffRecycler.setAdapter(null);
     488|            }
```

### Line 518
**Comment:** Detach adapters and clear holder references

**Context:**
```java
     514|    /**
     515|     * Clean up resources
     516|     */
>>>  517|    public void cleanup() {
     518|        // Detach adapters and clear holder references
     519|        for (ViewHolder vh : holders.values()) {
     520|            if (vh != null && vh.diffRecycler != null) {
     521|                vh.diffRecycler.setAdapter(null);
```

---

## com/codex/apk/TabItem.java

**Location:** `app/src/main/java/com/codex/apk/TabItem.java`

### Line 33
**Comment:** Log the error or handle it as needed

**Context:**
```java
      29|            setModified(false); // After reloading, it's no longer modified
      30|            setLastNotifiedModifiedState(false);
      31|            return true;
>>>   32|        } catch (Exception e) {
      33|            // Log the error or handle it as needed
      34|            return false;
      35|        }
      36|    }
```

---

## com/codex/apk/ThemeManager.java

**Location:** `app/src/main/java/com/codex/apk/ThemeManager.java`

### Line 72
**Comment:** Save the new theme

**Context:**
```java
      68|     * @param activity The current activity
      69|     * @param newTheme The new theme to apply
      70|     */
>>>   71|    public static void switchTheme(Activity activity, String newTheme) {
      72|        // Save the new theme
      73|        SettingsActivity.getPreferences(activity)
      74|            .edit()
      75|            .putString("app_theme", newTheme)
```

### Line 78
**Comment:** Apply the theme

**Context:**
```java
      74|            .edit()
      75|            .putString("app_theme", newTheme)
      76|            .apply();
>>>   77|
      78|        // Apply the theme
      79|        applyTheme(activity, newTheme);
      80|
      81|        // Recreate the activity to apply theme changes
```

### Line 81
**Comment:** Recreate the activity to apply theme changes

**Context:**
```java
      77|
      78|        // Apply the theme
      79|        applyTheme(activity, newTheme);
>>>   80|
      81|        // Recreate the activity to apply theme changes
      82|        activity.recreate();
      83|    }
      84|}
```

---

## com/codex/apk/ToolExecutor.java

**Location:** `app/src/main/java/com/codex/apk/ToolExecutor.java`

### Line 158
**Comment:** Use existing offset/snippet search; no extension filter, cap results

**Context:**
```java
     154|                    boolean isRegex = args.has("isRegex") && args.get("isRegex").getAsBoolean();
     155|                    boolean caseInsensitive = args.has("caseInsensitive") && args.get("caseInsensitive").getAsBoolean();
     156|                    boolean caseSensitive = !caseInsensitive;
>>>  157|                    File root = new File(projectDir, path);
     158|                    // Use existing offset/snippet search; no extension filter, cap results
     159|                    JsonArray results = FileOps.searchInFilesOffsets(root, query, caseSensitive, isRegex, new java.util.ArrayList<>(), 500);
     160|                    result.addProperty("ok", true);
     161|                    result.add("results", results);
```

---

## com/codex/apk/ToolSpec.java

**Location:** `app/src/main/java/com/codex/apk/ToolSpec.java`

### Line 60
**Comment:** createFile

**Context:**
```java
      56|     * ------------------------------------------------------------ */
      57|    public static java.util.List<ToolSpec> defaultFileTools() {
      58|        java.util.List<ToolSpec> tools = new java.util.ArrayList<>();
>>>   59|
      60|        // createFile
      61|        tools.add(new ToolSpec(
      62|                "createFile",
      63|                "Create a new file with the provided content (UTF-8). The file will be created in the project workspace.",
```

### Line 70
**Comment:** updateFile

**Context:**
```java
      66|                        new String[]{"string", "string"},
      67|                        new String[]{"Relative path to the file to create", "Content to write to the file"}
      68|                )));
>>>   69|
      70|        // updateFile
      71|        tools.add(new ToolSpec(
      72|                "updateFile",
      73|                "Overwrite an existing file with new content. The file must exist in the project workspace.",
```

### Line 80
**Comment:** deleteFile

**Context:**
```java
      76|                        new String[]{"string", "string"},
      77|                        new String[]{"Relative path to the file to update", "New content to write to the file"}
      78|                )));
>>>   79|
      80|        // deleteFile
      81|        tools.add(new ToolSpec(
      82|                "deleteFile",
      83|                "Delete a file or an empty directory from the project workspace.",
```

### Line 90
**Comment:** renameFile

**Context:**
```java
      86|                        new String[]{"string"},
      87|                        new String[]{"Relative path to the file or directory to delete"}
      88|                )));
>>>   89|
      90|        // renameFile
      91|        tools.add(new ToolSpec(
      92|                "renameFile",
      93|                "Rename or move a file or directory within the project workspace.",
```

### Line 100
**Comment:** readFile

**Context:**
```java
      96|                        new String[]{"string", "string"},
      97|                        new String[]{"Current path of the file or directory", "New path for the file or directory"}
      98|                )));
>>>   99|
     100|        // readFile
     101|        tools.add(new ToolSpec(
     102|                "readFile",
     103|                "Read the contents of a file from the project workspace.",
```

### Line 110
**Comment:** listFiles

**Context:**
```java
     106|                        new String[]{"string"},
     107|                        new String[]{"Relative path to the file to read"}
     108|                )));
>>>  109|
     110|        // listFiles
     111|        tools.add(new ToolSpec(
     112|                "listFiles",
     113|                "List files and directories in a directory within the project workspace.",
```

### Line 120
**Comment:** searchAndReplace

**Context:**
```java
     116|                        new String[]{"string"},
     117|                        new String[]{"Relative path to the directory to list (use '.' for root)"}
     118|                )));
>>>  119|
     120|        // searchAndReplace
     121|        tools.add(new ToolSpec(
     122|                "searchAndReplace",
     123|                "Search a file by pattern and replace occurrences. Supports simple regex.",
```

### Line 130
**Comment:** patchFile

**Context:**
```java
     126|                        new String[]{"string", "string", "string"},
     127|                        new String[]{"Relative path to the file", "Regex or plain text to search", "Replacement text"}
     128|                )));
>>>  129|
     130|        // patchFile
     131|        tools.add(new ToolSpec(
     132|                "patchFile",
     133|                "Apply a unified diff patch to a file in the project workspace.",
```

### Line 140
**Comment:** listProjectTree

**Context:**
```java
     136|                        new String[]{"string", "string"},
     137|                        new String[]{"Relative path to the file", "Unified diff patch content"}
     138|                )));
>>>  139|
     140|        // listProjectTree
     141|        tools.add(new ToolSpec(
     142|                "listProjectTree",
     143|                "List the project tree from a path with depth and entry limits.",
```

### Line 150
**Comment:** searchInProject

**Context:**
```java
     146|                        new String[]{"string", "integer", "integer"},
     147|                        new String[]{"Relative path ('.' for root)", "Max depth (0-5)", "Max entries (10-1000)"}
     148|                )));
>>>  149|
     150|        // searchInProject
     151|        tools.add(new ToolSpec(
     152|                "searchInProject",
     153|                "Search project files for a query. Supports regex when enabled.",
```

### Line 160
**Comment:** fixLint

**Context:**
```java
     156|                        new String[]{"string", "integer", "boolean"},
     157|                        new String[]{"Search query or regex pattern", "Maximum number of results", "Treat query as regex"}
     158|                )));
>>>  159|
     160|        // fixLint
     161|        tools.add(new ToolSpec(
     162|                "fixLint",
     163|                "Apply simple auto-fixes for common HTML/CSS/JS lint issues (adds missing doctype, alt/type, balances brackets).",
```

### Line 239
**Comment:** required

**Context:**
```java
     235|            props.add(keys[i], field);
     236|        }
     237|        schema.add("properties", props);
>>>  238|
     239|        // required
     240|        JsonArray req = new JsonArray();
     241|        for (String k : keys) req.add(k);
     242|        schema.add("required", req);
```

---

## com/codex/apk/WeWordleApiClient.java

**Location:** `app/src/main/java/com/codex/apk/WeWordleApiClient.java`

### Line 59
**Comment:** Handle rate limiting with backoff

**Context:**
```java
      55|                    if (response.isSuccessful() && response.body() != null) {
      56|                        break; // Success
      57|                    }
>>>   58|
      59|                    // Handle rate limiting with backoff
      60|                    if (response.code() == 429) {
      61|                        android.util.Log.w("WeWordleApiClient", "Rate limited. Retrying in " + backoff + "ms... (Attempt " + (i + 1) + ")");
      62|                        try { response.close(); } catch (Exception ignore) {}
```

### Line 73
**Comment:** If not rate-limited or retries exhausted, fail permanently

**Context:**
```java
      69|                            continue;
      70|                        }
      71|                    }
>>>   72|
      73|                    // If not rate-limited or retries exhausted, fail permanently
      74|                    String errBody = null;
      75|                    try { if (response != null && response.body() != null) errBody = response.body().string(); } catch (Exception ignore) {}
      76|                    String snippet = errBody != null ? (errBody.length() > 400 ? errBody.substring(0, 400) + "..." : errBody) : null;
```

### Line 97
**Comment:** Not a JSON response, or not in the expected format. Use the raw body.

**Context:**
```java
      93|                            content = messageObj.get("content").getAsString();
      94|                        }
      95|                    }
>>>   96|                } catch (Exception e) {
      97|                    // Not a JSON response, or not in the expected format. Use the raw body.
      98|                }
      99|                if (actionListener != null) {
     100|                    actionListener.onAiActionsProcessed(responseBody, content, new java.util.ArrayList<>(), new java.util.ArrayList<>(), model.getDisplayName());
```

---

## com/codex/apk/WebSourcesAdapter.java

**Location:** `app/src/main/java/com/codex/apk/WebSourcesAdapter.java`

### Line 68
**Comment:** TODO: Load favicon from source.favicon if available

**Context:**
```java
      64|            textTitle.setText(source.title);
      65|            textSnippet.setText(source.snippet);
      66|            textUrl.setText(source.url);
>>>   67|
      68|            // TODO: Load favicon from source.favicon if available
      69|            // For now, using default icon
      70|        }
      71|    }
```

### Line 69
**Comment:** For now, using default icon

**Context:**
```java
      65|            textSnippet.setText(source.snippet);
      66|            textUrl.setText(source.url);
      67|
>>>   68|            // TODO: Load favicon from source.favicon if available
      69|            // For now, using default icon
      70|        }
      71|    }
      72|}
```

---

## com/codex/apk/ZhipuApiClient.java

**Location:** `app/src/main/java/com/codex/apk/ZhipuApiClient.java`

### Line 229
**Comment:** This regex will remove all HTML tags.

**Context:**
```java
     225|    }
     226|
     227|    private String extractContentFromHtml(String html) {
>>>  228|        if (html == null) return "";
     229|        // This regex will remove all HTML tags.
     230|        return html.replaceAll("<[^>]*>", "").trim();
     231|    }
     232|}
```

---

## com/codex/apk/ai/AIModel.java

**Location:** `app/src/main/java/com/codex/apk/ai/AIModel.java`

### Line 17
**Comment:** Store models in a thread-safe map, keyed by provider for efficient lookup.

**Context:**
```java
      13|    private final String displayName;
      14|    private final AIProvider provider;
      15|    private final ModelCapabilities capabilities;
>>>   16|
      17|    // Store models in a thread-safe map, keyed by provider for efficient lookup.
      18|    private static final Map<AIProvider, List<AIModel>> modelsByProvider = new ConcurrentHashMap<>();
      19|    private static final List<AIModel> customModels = new ArrayList<>();
      20|
```

### Line 21
**Comment:** Static initializer to populate the initial set of models

**Context:**
```java
      17|    // Store models in a thread-safe map, keyed by provider for efficient lookup.
      18|    private static final Map<AIProvider, List<AIModel>> modelsByProvider = new ConcurrentHashMap<>();
      19|    private static final List<AIModel> customModels = new ArrayList<>();
>>>   20|
      21|    // Static initializer to populate the initial set of models
      22|    static {
      23|        loadCustomModels();
      24|        List<AIModel> initialModels = new ArrayList<>(Arrays.asList(
```

### Line 25
**Comment:** Google Models

**Context:**
```java
      21|    // Static initializer to populate the initial set of models
      22|    static {
      23|        loadCustomModels();
>>>   24|        List<AIModel> initialModels = new ArrayList<>(Arrays.asList(
      25|            // Google Models
      26|            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
      27|            new AIModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
      28|            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
```

### Line 33
**Comment:** Updated Alibaba/Qwen Models (from the new JSON data)

**Context:**
```java
      29|            new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
      30|            new AIModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
      31|            new AIModel("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
>>>   32|            new AIModel("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking", AIProvider.GOOGLE, new ModelCapabilities(true, false, true, true, true, true, true, 1048576, 8192)),
      33|            // Updated Alibaba/Qwen Models (from the new JSON data)
      34|            new AIModel("qwen3-235b-a22b", "Qwen3-235B-A22B-2507", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 81920)),
      35|            new AIModel("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA, new ModelCapabilities(false, true, true, true, true, true, true, 1048576, 65536)),
      36|            new AIModel("qwen3-30b-a3b", "Qwen3-30B-A3B-2507", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 32768)),
```

### Line 49
**Comment:** DeepInfra (OpenAI-compatible chat)

**Context:**
```java
      45|            new AIModel("qwen2.5-14b-instruct-1m", "Qwen2.5-14B-Instruct-1M", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 1000000, 8192)),
      46|            new AIModel("qwen2.5-coder-32b-instruct", "Qwen2.5-Coder-32B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),
      47|            new AIModel("qwen2.5-72b-instruct", "Qwen2.5-72B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, false, true, 131072, 8192)),
>>>   48|
      49|            // DeepInfra (OpenAI-compatible chat)
      50|            new AIModel("deepseek-v3", "DeepInfra DeepSeek V3", AIProvider.DEEPINFRA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
      51|
      52|            // Cookies Provider (Gemini reverse-engineered via cookies)
```

### Line 52
**Comment:** Cookies Provider (Gemini reverse-engineered via cookies)

**Context:**
```java
      48|
      49|            // DeepInfra (OpenAI-compatible chat)
      50|            new AIModel("deepseek-v3", "DeepInfra DeepSeek V3", AIProvider.DEEPINFRA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
>>>   51|
      52|            // Cookies Provider (Gemini reverse-engineered via cookies)
      53|            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
      54|            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
      55|
```

### Line 56
**Comment:** Zhipu Provider (default model)

**Context:**
```java
      52|            // Cookies Provider (Gemini reverse-engineered via cookies)
      53|            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
      54|            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.COOKIES, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
>>>   55|
      56|            // Zhipu Provider (default model)
      57|            new AIModel("GLM-4.5", "Zhipu GLM-4.5", AIProvider.ZHIPU, new ModelCapabilities(true, false, false, false, false, false, false, false, false, false, 0, 0, 0, 0, new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>())),
      58|
      59|            // OIVSCodeSer0501 Provider
```

### Line 59
**Comment:** OIVSCodeSer0501 Provider

**Context:**
```java
      55|
      56|            // Zhipu Provider (default model)
      57|            new AIModel("GLM-4.5", "Zhipu GLM-4.5", AIProvider.ZHIPU, new ModelCapabilities(true, false, false, false, false, false, false, false, false, false, 0, 0, 0, 0, new HashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>())),
>>>   58|
      59|            // OIVSCodeSer0501 Provider
      60|            new AIModel("gpt-4.1-mini", "OIVSCodeSer0501 gpt-4.1-mini", AIProvider.OIVSCodeSer0501, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
      61|
      62|            // OIVSCodeSer2 Provider
```

### Line 62
**Comment:** OIVSCodeSer2 Provider

**Context:**
```java
      58|
      59|            // OIVSCodeSer0501 Provider
      60|            new AIModel("gpt-4.1-mini", "OIVSCodeSer0501 gpt-4.1-mini", AIProvider.OIVSCodeSer0501, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
>>>   61|
      62|            // OIVSCodeSer2 Provider
      63|            new AIModel("gpt-4o-mini", "OIVSCodeSer2 gpt-4o-mini", AIProvider.OIVSCodeSer2, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
      64|
      65|            // WeWordle Provider
```

### Line 65
**Comment:** WeWordle Provider

**Context:**
```java
      61|
      62|            // OIVSCodeSer2 Provider
      63|            new AIModel("gpt-4o-mini", "OIVSCodeSer2 gpt-4o-mini", AIProvider.OIVSCodeSer2, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
>>>   64|
      65|            // WeWordle Provider
      66|            new AIModel("gpt-4", "WeWordle GPT-4", AIProvider.WEWORDLE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
      67|
      68|            // OpenRouter Models
```

### Line 68
**Comment:** OpenRouter Models

**Context:**
```java
      64|
      65|            // WeWordle Provider
      66|            new AIModel("gpt-4", "WeWordle GPT-4", AIProvider.WEWORDLE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
>>>   67|
      68|            // OpenRouter Models
      69|            new AIModel("deepseek/deepseek-v3.1", "DeepSeek V3.1", AIProvider.OPENROUTER, new ModelCapabilities(true, false, false, true, false, false, false, 128000, 8192)),
      70|            new AIModel("deepseek/deepseek-r1", "DeepSeek R1", AIProvider.OPENROUTER, new ModelCapabilities(true, false, false, true, false, false, false, 128000, 8192)),
      71|            new AIModel("deepseek/deepseek-v3", "DeepSeek V3", AIProvider.OPENROUTER, new ModelCapabilities(true, false, false, true, false, false, false, 128000, 8192)),
```

### Line 96
**Comment:** Reflect immediately in in-memory map to show in UI without duplication

**Context:**
```java
      92|    public boolean supportsFunctionCalling() { return true; }
      93|
      94|    public static void addCustomModel(AIModel model) {
>>>   95|        customModels.add(model);
      96|        // Reflect immediately in in-memory map to show in UI without duplication
      97|        upsertModel(model);
      98|        saveCustomModels();
      99|    }
```

### Line 130
**Comment:** Load previously fetched models per provider and replace in-memory lists where present

**Context:**
```java
     126|        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
     127|        if (context == null) return;
     128|        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
>>>  129|        com.google.gson.Gson gson = new com.google.gson.Gson();
     130|        // Load previously fetched models per provider and replace in-memory lists where present
     131|        try {
     132|            for (AIProvider p : AIProvider.values()) {
     133|                String key = "fetched_models_" + p.name();
```

### Line 140
**Comment:** Preserve existing caps if known, else default to chat-only

**Context:**
```java
     136|                    SimpleModel[] arr = gson.fromJson(fetchedJson, SimpleModel[].class);
     137|                    if (arr != null && arr.length > 0) {
     138|                        java.util.List<AIModel> restored = new java.util.ArrayList<>();
>>>  139|                        for (SimpleModel sm : arr) {
     140|                            // Preserve existing caps if known, else default to chat-only
     141|                            AIModel existing = findByDisplayName(sm.displayName);
     142|                            ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
     143|                            restored.add(new AIModel(sm.modelId, sm.displayName, AIProvider.valueOf(sm.provider), caps));
```

### Line 150
**Comment:** Deletions

**Context:**
```java
     146|                    }
     147|                }
     148|            }
>>>  149|        } catch (Exception ignored) {}
     150|        // Deletions
     151|        String deletedJson = prefs.getString("deleted_models", null);
     152|        java.util.Set<String> deleted = new java.util.HashSet<>();
     153|        if (deletedJson != null) {
```

### Line 159
**Comment:** Remove deleted from map

**Context:**
```java
     155|                String[] arr = gson.fromJson(deletedJson, String[].class);
     156|                if (arr != null) deleted.addAll(java.util.Arrays.asList(arr));
     157|            } catch (Exception ignored) {}
>>>  158|        }
     159|        // Remove deleted from map
     160|        if (!deleted.isEmpty()) {
     161|            for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
     162|                e.getValue().removeIf(m -> deleted.contains(m.getDisplayName()));
```

### Line 165
**Comment:** Overrides

**Context:**
```java
     161|            for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
     162|                e.getValue().removeIf(m -> deleted.contains(m.getDisplayName()));
     163|            }
>>>  164|        }
     165|        // Overrides
     166|        String overridesJson = prefs.getString("model_overrides", null);
     167|        if (overridesJson != null) {
     168|            try {
```

### Line 180
**Comment:** Add custom models into map (they are separate storage)

**Context:**
```java
     176|                    }
     177|                }
     178|            } catch (Exception ignored) {}
>>>  179|        }
     180|        // Add custom models into map (they are separate storage)
     181|        for (AIModel cm : customModels) {
     182|            upsertModel(cm);
     183|        }
```

### Line 196
**Comment:** Remove any existing entry with same display name across providers

**Context:**
```java
     192|        return null;
     193|    }
     194|
>>>  195|    private static void upsertModel(AIModel model) {
     196|        // Remove any existing entry with same display name across providers
     197|        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
     198|            e.getValue().removeIf(m -> m.getDisplayName().equals(model.getDisplayName()));
     199|        }
```

### Line 204
**Comment:** Record deletion

**Context:**
```java
     200|        modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
     201|    }
     202|
>>>  203|    public static void removeModelByDisplayName(String displayName) {
     204|        // Record deletion
     205|        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
     206|        if (context == null) return;
     207|        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
```

### Line 216
**Comment:** Apply in-memory removal

**Context:**
```java
     212|            try { String[] arr = gson.fromJson(deletedJson, String[].class); if (arr != null) deleted.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
     213|        }
     214|        deleted.add(displayName);
>>>  215|        prefs.edit().putString("deleted_models", gson.toJson(deleted.toArray(new String[0]))).apply();
     216|        // Apply in-memory removal
     217|        for (Map.Entry<AIProvider, List<AIModel>> e : modelsByProvider.entrySet()) {
     218|            e.getValue().removeIf(m -> m.getDisplayName().equals(displayName));
     219|        }
```

### Line 228
**Comment:** Persist override

**Context:**
```java
     224|        if (context == null) return;
     225|        AIModel existing = findByDisplayName(oldDisplayName);
     226|        ModelCapabilities caps = existing != null ? existing.getCapabilities() : new ModelCapabilities(false, false, false, true, false, false, false, 0, 0);
>>>  227|        AIModel updated = new AIModel(newModelId, newDisplayName, provider, caps);
     228|        // Persist override
     229|        android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", android.content.Context.MODE_PRIVATE);
     230|        com.google.gson.Gson gson = new com.google.gson.Gson();
     231|        String overridesJson = prefs.getString("model_overrides", null);
```

### Line 236
**Comment:** Remove old if exists

**Context:**
```java
     232|        java.util.List<SimpleModel> overrides = new java.util.ArrayList<>();
     233|        if (overridesJson != null) {
     234|            try { SimpleModel[] arr = gson.fromJson(overridesJson, SimpleModel[].class); if (arr != null) overrides.addAll(java.util.Arrays.asList(arr)); } catch (Exception ignored) {}
>>>  235|        }
     236|        // Remove old if exists
     237|        overrides.removeIf(sm -> sm.displayName.equals(oldDisplayName));
     238|        overrides.add(new SimpleModel(updated.getModelId(), updated.getDisplayName(), updated.getProvider().name()));
     239|        prefs.edit().putString("model_overrides", gson.toJson(overrides)).apply();
```

### Line 240
**Comment:** Apply in-memory

**Context:**
```java
     236|        // Remove old if exists
     237|        overrides.removeIf(sm -> sm.displayName.equals(oldDisplayName));
     238|        overrides.add(new SimpleModel(updated.getModelId(), updated.getDisplayName(), updated.getProvider().name()));
>>>  239|        prefs.edit().putString("model_overrides", gson.toJson(overrides)).apply();
     240|        // Apply in-memory
     241|        upsertModel(updated);
     242|    }
     243|
```

### Line 276
**Comment:** Persist a lightweight list so models survive app restarts

**Context:**
```java
     272|    }
     273|
     274|    public static void updateModelsForProvider(AIProvider provider, List<AIModel> newModels) {
>>>  275|        modelsByProvider.put(provider, new ArrayList<>(newModels));
     276|        // Persist a lightweight list so models survive app restarts
     277|        android.content.Context context = com.codex.apk.CodeXApplication.getAppContext();
     278|        if (context != null) {
     279|            try {
```

---

## com/codex/apk/ai/ModelCapabilities.java

**Location:** `app/src/main/java/com/codex/apk/ai/ModelCapabilities.java`

### Line 9
**Comment:** Legacy boolean capabilities

**Context:**
```java
       5|import java.util.Map;
       6|import java.util.HashMap;
       7|
>>>    8|public class ModelCapabilities {
       9|    // Legacy boolean capabilities
      10|    public final boolean supportsThinking;
      11|    public final boolean supportsWebSearch;
      12|    public final boolean supportsVision;
```

### Line 18
**Comment:** New capabilities

**Context:**
```java
      14|    public final boolean supportsVideo;
      15|    public final boolean supportsAudio;
      16|    public final boolean supportsCitations;
>>>   17|
      18|    // New capabilities
      19|    public final boolean supportsThinkingBudget;
      20|    public final boolean supportsMCP;
      21|    public final boolean isSingleRound;
```

### Line 23
**Comment:** Context and generation limits

**Context:**
```java
      19|    public final boolean supportsThinkingBudget;
      20|    public final boolean supportsMCP;
      21|    public final boolean isSingleRound;
>>>   22|
      23|    // Context and generation limits
      24|    public final int maxContextLength;
      25|    public final int maxGenerationLength;
      26|    public final int maxThinkingGenerationLength;
```

### Line 29
**Comment:** File limits

**Context:**
```java
      25|    public final int maxGenerationLength;
      26|    public final int maxThinkingGenerationLength;
      27|    public final int maxSummaryGenerationLength;
>>>   28|
      29|    // File limits
      30|    public final Map<String, Integer> fileLimits;
      31|
      32|    // Modality support
```

### Line 32
**Comment:** Modality support

**Context:**
```java
      28|
      29|    // File limits
      30|    public final Map<String, Integer> fileLimits;
>>>   31|
      32|    // Modality support
      33|    public final List<String> supportedModalities;
      34|
      35|    // Chat types
```

### Line 35
**Comment:** Chat types

**Context:**
```java
      31|
      32|    // Modality support
      33|    public final List<String> supportedModalities;
>>>   34|
      35|    // Chat types
      36|    public final List<String> supportedChatTypes;
      37|
      38|    // MCP tools
```

### Line 38
**Comment:** MCP tools

**Context:**
```java
      34|
      35|    // Chat types
      36|    public final List<String> supportedChatTypes;
>>>   37|
      38|    // MCP tools
      39|    public final List<String> mcpTools;
      40|
      41|    // Numeric ability levels (0=disabled, 1=enabled, 2=limited, 4=advanced)
```

### Line 41
**Comment:** Numeric ability levels (0=disabled, 1=enabled, 2=limited, 4=advanced)

**Context:**
```java
      37|
      38|    // MCP tools
      39|    public final List<String> mcpTools;
>>>   40|
      41|    // Numeric ability levels (0=disabled, 1=enabled, 2=limited, 4=advanced)
      42|    public final Map<String, Integer> abilities;
      43|
      44|    // Legacy constructor for backward compatibility
```

### Line 44
**Comment:** Legacy constructor for backward compatibility

**Context:**
```java
      40|
      41|    // Numeric ability levels (0=disabled, 1=enabled, 2=limited, 4=advanced)
      42|    public final Map<String, Integer> abilities;
>>>   43|
      44|    // Legacy constructor for backward compatibility
      45|    public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch,
      46|                            boolean supportsVision, boolean supportsDocument,
      47|                            boolean supportsVideo, boolean supportsAudio,
```

### Line 60
**Comment:** Initialize new fields with defaults

**Context:**
```java
      56|        this.supportsCitations = supportsCitations;
      57|        this.maxContextLength = maxContextLength;
      58|        this.maxGenerationLength = maxGenerationLength;
>>>   59|
      60|        // Initialize new fields with defaults
      61|        this.supportsThinkingBudget = false;
      62|        this.supportsMCP = false;
      63|        this.isSingleRound = false;
```

### Line 73
**Comment:** Enhanced constructor with all new capabilities

**Context:**
```java
      69|        this.mcpTools = new ArrayList<>();
      70|        this.abilities = new HashMap<>();
      71|    }
>>>   72|
      73|    // Enhanced constructor with all new capabilities
      74|    public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch,
      75|                            boolean supportsVision, boolean supportsDocument,
      76|                            boolean supportsVideo, boolean supportsAudio,
```

### Line 105
**Comment:** Helper method to check if a capability is supported at a specific level

**Context:**
```java
     101|        this.mcpTools = mcpTools != null ? mcpTools : new ArrayList<>();
     102|        this.abilities = abilities != null ? abilities : new HashMap<>();
     103|    }
>>>  104|
     105|    // Helper method to check if a capability is supported at a specific level
     106|    public boolean hasAbility(String capability, int minLevel) {
     107|        Integer level = abilities.get(capability);
     108|        return level != null && level >= minLevel;
```

### Line 111
**Comment:** Helper method to check if a modality is supported

**Context:**
```java
     107|        Integer level = abilities.get(capability);
     108|        return level != null && level >= minLevel;
     109|    }
>>>  110|
     111|    // Helper method to check if a modality is supported
     112|    public boolean supportsModality(String modality) {
     113|        return supportedModalities.contains(modality);
     114|    }
```

### Line 116
**Comment:** Helper method to check if a chat type is supported

**Context:**
```java
     112|    public boolean supportsModality(String modality) {
     113|        return supportedModalities.contains(modality);
     114|    }
>>>  115|
     116|    // Helper method to check if a chat type is supported
     117|    public boolean supportsChatType(String chatType) {
     118|        return supportedChatTypes.contains(chatType);
     119|    }
```

### Line 121
**Comment:** Helper method to check if an MCP tool is supported

**Context:**
```java
     117|    public boolean supportsChatType(String chatType) {
     118|        return supportedChatTypes.contains(chatType);
     119|    }
>>>  120|
     121|    // Helper method to check if an MCP tool is supported
     122|    public boolean supportsMCPTool(String tool) {
     123|        return mcpTools.contains(tool);
     124|    }
```

### Line 126
**Comment:** Helper method to get file limit

**Context:**
```java
     122|    public boolean supportsMCPTool(String tool) {
     123|        return mcpTools.contains(tool);
     124|    }
>>>  125|
     126|    // Helper method to get file limit
     127|    public int getFileLimit(String limitType) {
     128|        return fileLimits.getOrDefault(limitType, 0);
     129|    }
```

### Line 131
**Comment:** Get a summary of capabilities

**Context:**
```java
     127|    public int getFileLimit(String limitType) {
     128|        return fileLimits.getOrDefault(limitType, 0);
     129|    }
>>>  130|
     131|    // Get a summary of capabilities
     132|    public String getCapabilitySummary() {
     133|        StringBuilder summary = new StringBuilder();
     134|
```

### Line 152
**Comment:** Get context length in a human-readable format

**Context:**
```java
     148|
     149|        return summary.toString();
     150|    }
>>>  151|
     152|    // Get context length in a human-readable format
     153|    public String getContextLengthDisplay() {
     154|        if (maxContextLength >= 1000000) {
     155|            return String.format("%.1fM", maxContextLength / 1000000.0);
```

---

## com/codex/apk/core/config/ProviderConfig.java

**Location:** `app/src/main/java/com/codex/apk/core/config/ProviderConfig.java`

### Line 39
**Comment:** Getters

**Context:**
```java
      35|        this.providerSpecificConfig = new HashMap<>(builder.providerSpecificConfig);
      36|        this.enabled = builder.enabled;
      37|    }
>>>   38|
      39|    // Getters
      40|    public AIProvider getProviderType() { return providerType; }
      41|    public String getBaseUrl() { return baseUrl; }
      42|    public String getApiKey() { return apiKey; }
```

### Line 50
**Comment:** Convenience methods

**Context:**
```java
      46|    public Map<String, String> getCustomHeaders() { return new HashMap<>(customHeaders); }
      47|    public Map<String, Object> getProviderSpecificConfig() { return new HashMap<>(providerSpecificConfig); }
      48|    public boolean isEnabled() { return enabled; }
>>>   49|
      50|    // Convenience methods
      51|    public boolean hasApiKey() { return apiKey != null && !apiKey.trim().isEmpty(); }
      52|    public boolean hasBaseUrl() { return baseUrl != null && !baseUrl.trim().isEmpty(); }
      53|
```

### Line 104
**Comment:** Validate URLs if present

**Context:**
```java
     100|        if (providerType == null) {
     101|            result.addError("Provider type is required");
     102|        }
>>>  103|
     104|        // Validate URLs if present
     105|        if (hasBaseUrl()) {
     106|            if (!isValidUrl(baseUrl)) {
     107|                result.addError("Invalid base URL: " + baseUrl);
```

### Line 111
**Comment:** Provider-specific validations

**Context:**
```java
     107|                result.addError("Invalid base URL: " + baseUrl);
     108|            }
     109|        }
>>>  110|
     111|        // Provider-specific validations
     112|        switch (providerType) {
     113|            case GOOGLE:
     114|                if (!hasApiKey() && !hasProviderSpecificConfig("cookies")) {
```

### Line 119
**Comment:** Qwen might require specific configuration

**Context:**
```java
     115|                    result.addError("Google provider requires either API key or cookies");
     116|                }
     117|                break;
>>>  118|            case ALIBABA:
     119|                // Qwen might require specific configuration
     120|                break;
     121|            case DEEPINFRA:
     122|                if (!hasBaseUrl()) {
```

### Line 128
**Comment:** Validate timeouts

**Context:**
```java
     124|                }
     125|                break;
     126|        }
>>>  127|
     128|        // Validate timeouts
     129|        ValidationResult timeoutValidation = timeouts.validate();
     130|        result.merge(timeoutValidation);
     131|
```

### Line 132
**Comment:** Validate retry policy

**Context:**
```java
     128|        // Validate timeouts
     129|        ValidationResult timeoutValidation = timeouts.validate();
     130|        result.merge(timeoutValidation);
>>>  131|
     132|        // Validate retry policy
     133|        ValidationResult retryValidation = retryPolicy.validate();
     134|        result.merge(retryValidation);
     135|
```

### Line 261
**Comment:** Set provider-specific defaults

**Context:**
```java
     257|     */
     258|    public static ProviderConfig defaults(AIProvider providerType) {
     259|        Builder builder = builder(providerType);
>>>  260|
     261|        // Set provider-specific defaults
     262|        switch (providerType) {
     263|            case GOOGLE:
     264|                builder.withBaseUrl("https://generativelanguage.googleapis.com/v1beta/models/");
```

---

## com/codex/apk/core/migration/LegacyAIAssistantAdapter.java

**Location:** `app/src/main/java/com/codex/apk/core/migration/LegacyAIAssistantAdapter.java`

### Line 43
**Comment:** Convert legacy parameters to new format

**Context:**
```java
      39|
      40|    @Override
      41|    public void sendMessage(String message, List<ChatMessage> history, QwenConversationState qwenState, List<File> attachments) {
>>>   42|        try {
      43|            // Convert legacy parameters to new format
      44|            AIRequest request = converter.convertToAIRequest(
      45|                message, getCurrentModel(), history, qwenState,
      46|                isThinkingModeEnabled(), isWebSearchEnabled(),
```

### Line 50
**Comment:** Execute through new architecture

**Context:**
```java
      46|                isThinkingModeEnabled(), isWebSearchEnabled(),
      47|                getEnabledTools(), attachments
      48|            );
>>>   49|
      50|            // Execute through new architecture
      51|            serviceManager.executeRequest(request, this::handleResponse, this::handleError)
      52|                    .thenRun(this::handleComplete);
      53|
```

### Line 70
**Comment:** Convert back to legacy format

**Context:**
```java
      66|            listener.onAiStreamUpdate(response.getContent(), response.hasThinking());
      67|        }
      68|
>>>   69|        if (response.isComplete()) {
      70|            // Convert back to legacy format
      71|            LegacyResponse legacyResponse = converter.convertToLegacyResponse(response);
      72|            listener.onAiActionsProcessed(
      73|                legacyResponse.rawJson,
```

### Line 94
**Comment:** Override other methods as needed to maintain compatibility

**Context:**
```java
      90|            getActionListener().onAiRequestCompleted();
      91|        }
      92|    }
>>>   93|
      94|    // Override other methods as needed to maintain compatibility
      95|    @Override
      96|    public void refreshModelsForProvider(AIProvider provider, RefreshCallback callback) {
      97|        // Use new service manager for model fetching
```

### Line 97
**Comment:** Use new service manager for model fetching

**Context:**
```java
      93|
      94|    // Override other methods as needed to maintain compatibility
      95|    @Override
>>>   96|    public void refreshModelsForProvider(AIProvider provider, RefreshCallback callback) {
      97|        // Use new service manager for model fetching
      98|        try {
      99|            serviceManager.getService(provider)
     100|                .getModels()
```

### Line 140
**Comment:** Convert message history

**Context:**
```java
     136|
     137|            AIRequest.Builder builder = AIRequest.builder()
     138|                .withModel(model != null ? model.getModelId() : null);
>>>  139|
     140|            // Convert message history
     141|            if (history != null) {
     142|                for (ChatMessage chatMsg : history) {
     143|                    Message.MessageRole role = chatMsg.getSender() == ChatMessage.SENDER_USER ?
```

### Line 152
**Comment:** Add current message

**Context:**
```java
     148|                        .build());
     149|                }
     150|            }
>>>  151|
     152|            // Add current message
     153|            builder.addMessage(Message.user(message));
     154|
     155|            // Set parameters
```

### Line 155
**Comment:** Set parameters

**Context:**
```java
     151|
     152|            // Add current message
     153|            builder.addMessage(Message.user(message));
>>>  154|
     155|            // Set parameters
     156|            builder.withParameters(com.codex.apk.core.model.RequestParameters.builder()
     157|                .withStream(true) // Default to streaming for better UX
     158|                .build());
```

### Line 160
**Comment:** Set required capabilities

**Context:**
```java
     156|            builder.withParameters(com.codex.apk.core.model.RequestParameters.builder()
     157|                .withStream(true) // Default to streaming for better UX
     158|                .build());
>>>  159|
     160|            // Set required capabilities
     161|            com.codex.apk.core.model.RequiredCapabilities capabilities =
     162|                new com.codex.apk.core.model.RequiredCapabilities(
     163|                    true,     // streaming
```

### Line 172
**Comment:** Add tools

**Context:**
```java
     168|                    false     // multimodal
     169|                );
     170|            builder.requireCapabilities(capabilities);
>>>  171|
     172|            // Add tools
     173|            if (tools != null) {
     174|                builder.withTools(tools);
     175|            }
```

### Line 177
**Comment:** Add metadata for legacy compatibility

**Context:**
```java
     173|            if (tools != null) {
     174|                builder.withTools(tools);
     175|            }
>>>  176|
     177|            // Add metadata for legacy compatibility
     178|            builder.addMetadata("legacy_qwen_state", qwenState);
     179|            builder.addMetadata("thinking_mode", thinkingMode);
     180|            builder.addMetadata("web_search", webSearch);
```

### Line 211
**Comment:** Helper methods for accessing private fields from parent class

**Context:**
```java
     207|        List<ChatMessage.FileActionDetail> fileActions;
     208|        String modelName;
     209|    }
>>>  210|
     211|    // Helper methods for accessing private fields from parent class
     212|    private AIActionListener getActionListener() {
     213|        // Would need to expose this in parent class or use reflection
     214|        return null; // Placeholder
```

### Line 213
**Comment:** Would need to expose this in parent class or use reflection

**Context:**
```java
     209|    }
     210|
     211|    // Helper methods for accessing private fields from parent class
>>>  212|    private AIActionListener getActionListener() {
     213|        // Would need to expose this in parent class or use reflection
     214|        return null; // Placeholder
     215|    }
     216|
```

### Line 218
**Comment:** Would need to expose this in parent class

**Context:**
```java
     214|        return null; // Placeholder
     215|    }
     216|
>>>  217|    private List<ToolSpec> getEnabledTools() {
     218|        // Would need to expose this in parent class
     219|        return java.util.Collections.emptyList(); // Placeholder
     220|    }
     221|}
```

---

## com/codex/apk/core/model/AIRequest.java

**Location:** `app/src/main/java/com/codex/apk/core/model/AIRequest.java`

### Line 43
**Comment:** Getters

**Context:**
```java
      39|        this.context = builder.context;
      40|        this.timestamp = Instant.now();
      41|    }
>>>   42|
      43|    // Getters
      44|    public String getId() { return id; }
      45|    public List<Message> getMessages() { return new ArrayList<>(messages); }
      46|    public String getModel() { return model; }
```

### Line 55
**Comment:** Convenience methods

**Context:**
```java
      51|    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
      52|    public ExecutionContext getContext() { return context; }
      53|    public Instant getTimestamp() { return timestamp; }
>>>   54|
      55|    // Convenience methods
      56|    public boolean isStreaming() {
      57|        return parameters.isStream();
      58|    }
```

### Line 88
**Comment:** Validate messages

**Context:**
```java
      84|        if (model == null || model.trim().isEmpty()) {
      85|            result.addError("Model must be specified");
      86|        }
>>>   87|
      88|        // Validate messages
      89|        for (int i = 0; i < messages.size(); i++) {
      90|            Message msg = messages.get(i);
      91|            if (msg.getContent() == null || msg.getContent().trim().isEmpty()) {
```

### Line 98
**Comment:** Validate attachments if present

**Context:**
```java
      94|                }
      95|            }
      96|        }
>>>   97|
      98|        // Validate attachments if present
      99|        for (Attachment attachment : attachments) {
     100|            if (attachment.getType() == null) {
     101|                result.addError("Attachment missing type");
```

### Line 105
**Comment:** Validate parameters

**Context:**
```java
     101|                result.addError("Attachment missing type");
     102|            }
     103|        }
>>>  104|
     105|        // Validate parameters
     106|        ValidationResult paramValidation = parameters.validate();
     107|        result.merge(paramValidation);
     108|
```

### Line 119
**Comment:** This could be extended to apply provider-specific optimizations

**Context:**
```java
     115|     * @param providerType The target provider type
     116|     * @return Optimized request copy
     117|     */
>>>  118|    public AIRequest optimizeForProvider(com.codex.apk.ai.AIProvider providerType) {
     119|        // This could be extended to apply provider-specific optimizations
     120|        try {
     121|            return new Builder(this).build();
     122|        } catch (ValidationException e) {
```

### Line 123
**Comment:** Log the error and return the original request

**Context:**
```java
     119|        // This could be extended to apply provider-specific optimizations
     120|        try {
     121|            return new Builder(this).build();
>>>  122|        } catch (ValidationException e) {
     123|            // Log the error and return the original request
     124|            System.err.println("Failed to optimize request for provider " + providerType + ": " + e.getMessage());
     125|            return this;
     126|        }
```

---

## com/codex/apk/core/model/AIResponse.java

**Location:** `app/src/main/java/com/codex/apk/core/model/AIResponse.java`

### Line 48
**Comment:** Getters

**Context:**
```java
      44|        this.isComplete = builder.isComplete;
      45|        this.error = builder.error;
      46|    }
>>>   47|
      48|    // Getters
      49|    public String getId() { return id; }
      50|    public String getRequestId() { return requestId; }
      51|    public String getContent() { return content != null ? content : ""; }
```

### Line 64
**Comment:** Convenience methods

**Context:**
```java
      60|    public boolean isStreaming() { return isStreaming; }
      61|    public boolean isComplete() { return isComplete; }
      62|    public AIError getError() { return error; }
>>>   63|
      64|    // Convenience methods
      65|    public boolean hasError() {
      66|        return error != null;
      67|    }
```

---

## com/codex/apk/core/model/Message.java

**Location:** `app/src/main/java/com/codex/apk/core/model/Message.java`

### Line 30
**Comment:** Getters

**Context:**
```java
      26|        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
      27|        this.name = builder.name;
      28|    }
>>>   29|
      30|    // Getters
      31|    public MessageRole getRole() { return role; }
      32|    public String getContent() { return content != null ? content : ""; }
      33|    public List<Attachment> getAttachments() { return new ArrayList<>(attachments); }
```

### Line 38
**Comment:** Convenience methods

**Context:**
```java
      34|    public List<ToolCall> getToolCalls() { return new ArrayList<>(toolCalls); }
      35|    public Instant getTimestamp() { return timestamp; }
      36|    public String getName() { return name; }
>>>   37|
      38|    // Convenience methods
      39|    public boolean hasContent() {
      40|        return content != null && !content.trim().isEmpty();
      41|    }
```

### Line 64
**Comment:** Rough approximation: 1 token per 4 characters for English text

**Context:**
```java
      60|     */
      61|    public int getEstimatedTokenCount() {
      62|        int tokens = 0;
>>>   63|
      64|        // Rough approximation: 1 token per 4 characters for English text
      65|        if (content != null) {
      66|            tokens += content.length() / 4;
      67|        }
```

### Line 69
**Comment:** Add overhead for role and structure

**Context:**
```java
      65|        if (content != null) {
      66|            tokens += content.length() / 4;
      67|        }
>>>   68|
      69|        // Add overhead for role and structure
      70|        tokens += 10;
      71|
      72|        // Add tokens for attachments (rough estimate)
```

### Line 72
**Comment:** Add tokens for attachments (rough estimate)

**Context:**
```java
      68|
      69|        // Add overhead for role and structure
      70|        tokens += 10;
>>>   71|
      72|        // Add tokens for attachments (rough estimate)
      73|        tokens += attachments.size() * 100;
      74|
      75|        // Add tokens for tool calls
```

### Line 75
**Comment:** Add tokens for tool calls

**Context:**
```java
      71|
      72|        // Add tokens for attachments (rough estimate)
      73|        tokens += attachments.size() * 100;
>>>   74|
      75|        // Add tokens for tool calls
      76|        for (ToolCall toolCall : toolCalls) {
      77|            tokens += toolCall.getEstimatedTokenCount();
      78|        }
```

---

## com/codex/apk/core/model/ProviderCapabilities.java

**Location:** `app/src/main/java/com/codex/apk/core/model/ProviderCapabilities.java`

### Line 44
**Comment:** Basic optimization - could be extended per provider

**Context:**
```java
      40|        return required.isCompatibleWith(this);
      41|    }
      42|
>>>   43|    public AIRequest optimizeRequest(AIRequest request) {
      44|        // Basic optimization - could be extended per provider
      45|        return request;
      46|    }
      47|}
```

---

## com/codex/apk/core/model/RequestParameters.java

**Location:** `app/src/main/java/com/codex/apk/core/model/RequestParameters.java`

### Line 37
**Comment:** Default constructor with sensible defaults

**Context:**
```java
      33|        this.seed = builder.seed;
      34|        this.responseFormat = builder.responseFormat;
      35|    }
>>>   36|
      37|    // Default constructor with sensible defaults
      38|    public RequestParameters() {
      39|        this.temperature = null;  // Let provider decide
      40|        this.maxTokens = null;    // Let provider decide
```

### Line 51
**Comment:** Getters

**Context:**
```java
      47|        this.seed = null;
      48|        this.responseFormat = null;
      49|    }
>>>   50|
      51|    // Getters
      52|    public Double getTemperature() { return temperature; }
      53|    public Integer getMaxTokens() { return maxTokens; }
      54|    public Double getTopP() { return topP; }
```

### Line 63
**Comment:** Convenience methods

**Context:**
```java
      59|    public boolean isStream() { return stream; }
      60|    public Integer getSeed() { return seed; }
      61|    public String getResponseFormat() { return responseFormat; }
>>>   62|
      63|    // Convenience methods
      64|    public boolean hasTemperature() { return temperature != null; }
      65|    public boolean hasMaxTokens() { return maxTokens != null; }
      66|    public boolean hasTopP() { return topP != null; }
```

---

## com/codex/apk/core/model/SupportingModels.java

**Location:** `app/src/main/java/com/codex/apk/core/model/SupportingModels.java`

### Line 3
**Comment:** This file intentionally left empty.

**Context:**
```java
       0|// All previously bundled classes were split into individual files in this package.
       1|package com.codex.apk.core.model;
>>>    2|
       3|// This file intentionally left empty.
       4|// All previously bundled classes were split into individual files in this package.
```

### Line 4
**Comment:** All previously bundled classes were split into individual files in this package.

**Context:**
```java
       0|// All previously bundled classes were split into individual files in this package.
       1|package com.codex.apk.core.model;
       2|
>>>    3|// This file intentionally left empty.
       4|// All previously bundled classes were split into individual files in this package.
```

---

## com/codex/apk/core/providers/GeminiFreeService.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/GeminiFreeService.java`

### Line 47
**Comment:** Ensure we have a valid session

**Context:**
```java
      43|
      44|    @Override
      45|    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
>>>   46|        try {
      47|            // Ensure we have a valid session
      48|            if (accessToken == null) {
      49|                initializeSession();
      50|                if (accessToken == null) {
```

### Line 57
**Comment:** Build form data for Gemini's expected format

**Context:**
```java
      53|            }
      54|
      55|            String modelId = request.getModel() != null ? request.getModel() : "gemini-2.5-flash";
>>>   56|
      57|            // Build form data for Gemini's expected format
      58|            FormBody.Builder formBuilder = new FormBody.Builder();
      59|            formBuilder.add("at", accessToken);
      60|
```

### Line 61
**Comment:** Build the request payload in Gemini's expected format

**Context:**
```java
      57|            // Build form data for Gemini's expected format
      58|            FormBody.Builder formBuilder = new FormBody.Builder();
      59|            formBuilder.add("at", accessToken);
>>>   60|
      61|            // Build the request payload in Gemini's expected format
      62|            JsonObject requestData = buildGeminiFreeRequestData(request);
      63|            formBuilder.add("f.req", requestData.toString());
      64|
```

### Line 98
**Comment:** Simple implementation - parse the whole response and send as one chunk

**Context:**
```java
      94|                                                              Consumer<Throwable> onError) {
      95|        return CompletableFuture.runAsync(() -> {
      96|            try {
>>>   97|                String responseBody = response.body().string();
      98|                // Simple implementation - parse the whole response and send as one chunk
      99|                AIResponse aiResponse = parseResponse(response, requestId);
     100|                onResponse.accept(aiResponse);
     101|            } catch (Exception e) {
```

### Line 109
**Comment:** Return static models for COOKIES provider from AIModel registry

**Context:**
```java
     105|    }
     106|
     107|    @Override
>>>  108|    protected List<AIModel> fetchAvailableModels() throws Exception {
     109|        // Return static models for COOKIES provider from AIModel registry
     110|        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
     111|        List<AIModel> geminiModels = new ArrayList<>();
     112|
```

### Line 125
**Comment:** Check if we can get access token

**Context:**
```java
     121|
     122|    @Override
     123|    protected boolean performHealthCheck() throws Exception {
>>>  124|        try {
     125|            // Check if we can get access token
     126|            if (accessToken == null) {
     127|                initializeSession();
     128|            }
```

### Line 137
**Comment:** Get cookies from configuration

**Context:**
```java
     133|    }
     134|
     135|    private void initializeSession() {
>>>  136|        try {
     137|            // Get cookies from configuration
     138|            String psid = configuration.getProviderSpecificConfig("psid", "");
     139|            String psidts = configuration.getProviderSpecificConfig("psidts", "");
     140|
```

### Line 152
**Comment:** Warmup session and fetch access token

**Context:**
```java
     148|            if (psidts != null && !psidts.isEmpty()) {
     149|                sessionCookies.put("__Secure-1PSIDTS", psidts);
     150|            }
>>>  151|
     152|            // Warmup session and fetch access token
     153|            warmupSession();
     154|            fetchAccessToken();
     155|
```

### Line 162
**Comment:** Visit Google.com to establish session

**Context:**
```java
     158|        }
     159|    }
     160|
>>>  161|    private void warmupSession() throws Exception {
     162|        // Visit Google.com to establish session
     163|        Request warmupRequest = new Request.Builder()
     164|            .url("https://www.google.com")
     165|            .header("Cookie", buildCookieHeader(sessionCookies))
```

### Line 172
**Comment:** Visit Gemini app to initialize

**Context:**
```java
     168|        try (Response response = httpClient.newCall(warmupRequest).execute()) {
     169|            updateCookiesFromResponse(response);
     170|        }
>>>  171|
     172|        // Visit Gemini app to initialize
     173|        Request initRequest = new Request.Builder()
     174|            .url(INIT_URL)
     175|            .header("Cookie", buildCookieHeader(sessionCookies))
```

### Line 199
**Comment:** Extract access token from HTML using various patterns

**Context:**
```java
     195|        }
     196|    }
     197|
>>>  198|    private String extractAccessTokenFromHtml(String html) {
     199|        // Extract access token from HTML using various patterns
     200|        String[] patterns = {
     201|            "\"SNlM0e\":\"([^\"]+)\"",
     202|            "'SNlM0e':'([^']+)'",
```

### Line 221
**Comment:** Build the complex nested structure Gemini expects

**Context:**
```java
     217|
     218|    private JsonObject buildGeminiFreeRequestData(AIRequest request) {
     219|        JsonArray requestArray = new JsonArray();
>>>  220|
     221|        // Build the complex nested structure Gemini expects
     222|        JsonArray innerArray = new JsonArray();
     223|
     224|        // Message content
```

### Line 224
**Comment:** Message content

**Context:**
```java
     220|
     221|        // Build the complex nested structure Gemini expects
     222|        JsonArray innerArray = new JsonArray();
>>>  223|
     224|        // Message content
     225|        String messageContent = buildMessageContent(request.getMessages());
     226|        innerArray.add(messageContent);
     227|
```

### Line 228
**Comment:** Conversation metadata (if available)

**Context:**
```java
     224|        // Message content
     225|        String messageContent = buildMessageContent(request.getMessages());
     226|        innerArray.add(messageContent);
>>>  227|
     228|        // Conversation metadata (if available)
     229|        String conversationMeta = configuration.getProviderSpecificConfig("conversation_meta", "[]");
     230|        try {
     231|            JsonArray metaArray = JsonParser.parseString(conversationMeta).getAsJsonArray();
```

### Line 237
**Comment:** Additional parameters

**Context:**
```java
     233|        } catch (Exception e) {
     234|            innerArray.add(new JsonArray()); // Empty array as fallback
     235|        }
>>>  236|
     237|        // Additional parameters
     238|        JsonArray paramsArray = new JsonArray();
     239|        paramsArray.add(JsonNull.INSTANCE); // Usually null
     240|        paramsArray.add(JsonNull.INSTANCE); // Usually null
```

### Line 256
**Comment:** Prepend system message as context

**Context:**
```java
     252|
     253|        for (Message message : messages) {
     254|            switch (message.getRole()) {
>>>  255|                case SYSTEM:
     256|                    // Prepend system message as context
     257|                    if (content.length() > 0) content.append("\n\n");
     258|                    content.append(message.getContent());
     259|                    break;
```

### Line 265
**Comment:** Skip assistant messages in request building

**Context:**
```java
     261|                    if (content.length() > 0) content.append("\n\n");
     262|                    content.append(message.getContent());
     263|                    break;
>>>  264|                case ASSISTANT:
     265|                    // Skip assistant messages in request building
     266|                    break;
     267|            }
     268|        }
```

### Line 314
**Comment:** Parse Gemini's complex response format

**Context:**
```java
     310|            .isStreaming(isStreaming)
     311|            .isComplete(isComplete);
     312|
>>>  313|        try {
     314|            // Parse Gemini's complex response format
     315|            ParsedOutput parsed = parseOutputFromStream(responseBody);
     316|
     317|            if (parsed.text != null && !parsed.text.isEmpty()) {
```

### Line 337
**Comment:** This would need specific implementation for Gemini's streaming format

**Context:**
```java
     333|        return builder.build();
     334|    }
     335|
>>>  336|    private AIResponse parseStreamingLine(String line, String requestId, StringBuilder contentBuffer) {
     337|        // This would need specific implementation for Gemini's streaming format
     338|        // For now, return null to indicate no delta content
     339|        return null;
     340|    }
```

### Line 338
**Comment:** For now, return null to indicate no delta content

**Context:**
```java
     334|    }
     335|
     336|    private AIResponse parseStreamingLine(String line, String requestId, StringBuilder contentBuffer) {
>>>  337|        // This would need specific implementation for Gemini's streaming format
     338|        // For now, return null to indicate no delta content
     339|        return null;
     340|    }
     341|
```

### Line 343
**Comment:** Simplified parsing - would need full implementation of Gemini's response format

**Context:**
```java
     339|        return null;
     340|    }
     341|
>>>  342|    private ParsedOutput parseOutputFromStream(String responseBody) {
     343|        // Simplified parsing - would need full implementation of Gemini's response format
     344|        ParsedOutput output = new ParsedOutput();
     345|
     346|        try {
```

### Line 347
**Comment:** Look for text content in various response patterns

**Context:**
```java
     343|        // Simplified parsing - would need full implementation of Gemini's response format
     344|        ParsedOutput output = new ParsedOutput();
     345|
>>>  346|        try {
     347|            // Look for text content in various response patterns
     348|            if (responseBody.contains("\"text\":")) {
     349|                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
     350|                java.util.regex.Matcher matcher = pattern.matcher(responseBody);
```

### Line 359
**Comment:** Look for thinking content

**Context:**
```java
     355|                        .replace("\\\\", "\\");
     356|                }
     357|            }
>>>  358|
     359|            // Look for thinking content
     360|            if (responseBody.contains("\"thinking\":")) {
     361|                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"thinking\"\\s*:\\s*\"([^\"]+)\"");
     362|                java.util.regex.Matcher matcher = pattern.matcher(responseBody);
```

---

## com/codex/apk/core/providers/GeminiOfficialService.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/GeminiOfficialService.java`

### Line 43
**Comment:** Add API key as query parameter

**Context:**
```java
      39|            String modelId = request.getModel() != null ? request.getModel() : "gemini-1.5-flash";
      40|            String endpoint = request.isStreaming() ? ":streamGenerateContent" : ":generateContent";
      41|            String url = BASE_URL + "/models/" + modelId + endpoint;
>>>   42|
      43|            // Add API key as query parameter
      44|            if (configuration.hasApiKey()) {
      45|                url += "?key=" + configuration.getApiKey();
      46|            } else {
```

### Line 99
**Comment:** Emit final complete response

**Context:**
```java
      95|                    if (line.startsWith("data: ")) {
      96|                        String data = line.substring(6).trim();
      97|
>>>   98|                        if ("[DONE]".equals(data)) {
      99|                            // Emit final complete response
     100|                            AIResponse finalResponse = AIResponse.builder()
     101|                                .withRequestId(requestId)
     102|                                .withContent(contentBuffer.toString())
```

### Line 133
**Comment:** Return static models for GOOGLE provider from AIModel registry

**Context:**
```java
     129|    }
     130|
     131|    @Override
>>>  132|    protected List<AIModel> fetchAvailableModels() throws Exception {
     133|        // Return static models for GOOGLE provider from AIModel registry
     134|        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
     135|        List<AIModel> geminiModels = new ArrayList<>();
     136|
```

### Line 148
**Comment:** Simple health check by making a minimal request

**Context:**
```java
     144|    }
     145|
     146|    @Override
>>>  147|    protected boolean performHealthCheck() throws Exception {
     148|        // Simple health check by making a minimal request
     149|        try {
     150|            String url = BASE_URL + "/models/gemini-1.5-flash:generateContent";
     151|            if (configuration.hasApiKey()) {
```

### Line 167
**Comment:** Add generation config to limit response

**Context:**
```java
     163|            userMessage.add("parts", parts);
     164|            contents.add(userMessage);
     165|            healthRequest.add("contents", contents);
>>>  166|
     167|            // Add generation config to limit response
     168|            JsonObject generationConfig = new JsonObject();
     169|            generationConfig.addProperty("maxOutputTokens", 1);
     170|            healthRequest.add("generationConfig", generationConfig);
```

### Line 189
**Comment:** Build contents array

**Context:**
```java
     185|
     186|    private JsonObject buildGeminiRequestBody(AIRequest request) {
     187|        JsonObject body = new JsonObject();
>>>  188|
     189|        // Build contents array
     190|        JsonArray contents = new JsonArray();
     191|
     192|        for (Message message : request.getMessages()) {
```

### Line 198
**Comment:** Add text content

**Context:**
```java
     194|            content.addProperty("role", mapMessageRole(message.getRole()));
     195|
     196|            JsonArray parts = new JsonArray();
>>>  197|
     198|            // Add text content
     199|            if (message.hasContent()) {
     200|                JsonObject textPart = new JsonObject();
     201|                textPart.addProperty("text", message.getContent());
```

### Line 205
**Comment:** Add attachments (images, documents)

**Context:**
```java
     201|                textPart.addProperty("text", message.getContent());
     202|                parts.add(textPart);
     203|            }
>>>  204|
     205|            // Add attachments (images, documents)
     206|            for (Attachment attachment : message.getAttachments()) {
     207|                if ("image".equals(attachment.getType())) {
     208|                    JsonObject imagePart = new JsonObject();
```

### Line 213
**Comment:** Base64 encode image data

**Context:**
```java
     209|                    JsonObject inlineData = new JsonObject();
     210|                    inlineData.addProperty("mimeType", attachment.getMimeType());
     211|
>>>  212|                    if (attachment.getData() != null) {
     213|                        // Base64 encode image data
     214|                        String base64Data = java.util.Base64.getEncoder().encodeToString(attachment.getData());
     215|                        inlineData.addProperty("data", base64Data);
     216|                    } else if (attachment.getUrl() != null) {
```

### Line 217
**Comment:** For URL-based images, we'd need to fetch and encode

**Context:**
```java
     213|                        // Base64 encode image data
     214|                        String base64Data = java.util.Base64.getEncoder().encodeToString(attachment.getData());
     215|                        inlineData.addProperty("data", base64Data);
>>>  216|                    } else if (attachment.getUrl() != null) {
     217|                        // For URL-based images, we'd need to fetch and encode
     218|                        // For now, skip URL-based images
     219|                        continue;
     220|                    }
```

### Line 218
**Comment:** For now, skip URL-based images

**Context:**
```java
     214|                        String base64Data = java.util.Base64.getEncoder().encodeToString(attachment.getData());
     215|                        inlineData.addProperty("data", base64Data);
     216|                    } else if (attachment.getUrl() != null) {
>>>  217|                        // For URL-based images, we'd need to fetch and encode
     218|                        // For now, skip URL-based images
     219|                        continue;
     220|                    }
     221|
```

### Line 233
**Comment:** Add generation configuration

**Context:**
```java
     229|        }
     230|
     231|        body.add("contents", contents);
>>>  232|
     233|        // Add generation configuration
     234|        JsonObject generationConfig = new JsonObject();
     235|        RequestParameters params = request.getParameters();
     236|
```

### Line 257
**Comment:** Add tools if present

**Context:**
```java
     253|        }
     254|
     255|        body.add("generationConfig", generationConfig);
>>>  256|
     257|        // Add tools if present
     258|        if (request.hasTools()) {
     259|            JsonArray tools = new JsonArray();
     260|            for (ToolSpec toolSpec : request.getTools()) {
```

### Line 272
**Comment:** Add safety settings for permissive content

**Context:**
```java
     268|            }
     269|            body.add("tools", tools);
     270|        }
>>>  271|
     272|        // Add safety settings for permissive content
     273|        JsonArray safetySettings = new JsonArray();
     274|        String[] categories = {
     275|            "HARM_CATEGORY_HATE_SPEECH",
```

### Line 299
**Comment:** Gemini doesn't have explicit system role, prepend to first user message

**Context:**
```java
     295|                return "user";
     296|            case ASSISTANT:
     297|                return "model";
>>>  298|            case SYSTEM:
     299|                // Gemini doesn't have explicit system role, prepend to first user message
     300|                return "user";
     301|            case TOOL:
     302|                return "function";
```

### Line 314
**Comment:** Parse candidates

**Context:**
```java
     310|            .withRequestId(requestId)
     311|            .isStreaming(isStreaming)
     312|            .isComplete(isComplete);
>>>  313|
     314|        // Parse candidates
     315|        if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isJsonArray()) {
     316|            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
     317|            if (candidates.size() > 0) {
```

### Line 320
**Comment:** Parse content

**Context:**
```java
     316|            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
     317|            if (candidates.size() > 0) {
     318|                JsonObject candidate = candidates.get(0).getAsJsonObject();
>>>  319|
     320|                // Parse content
     321|                if (candidate.has("content")) {
     322|                    JsonObject content = candidate.getAsJsonObject("content");
     323|                    StringBuilder textContent = new StringBuilder();
```

### Line 334
**Comment:** Parse function calls

**Context:**
```java
     330|                            if (part.has("text")) {
     331|                                textContent.append(part.get("text").getAsString());
     332|                            }
>>>  333|
     334|                            // Parse function calls
     335|                            if (part.has("functionCall")) {
     336|                                JsonObject functionCall = part.getAsJsonObject("functionCall");
     337|                                String name = functionCall.get("name").getAsString();
```

### Line 354
**Comment:** Parse finish reason

**Context:**
```java
     350|
     351|                    builder.withContent(textContent.toString());
     352|                }
>>>  353|
     354|                // Parse finish reason
     355|                if (candidate.has("finishReason")) {
     356|                    String finishReason = candidate.get("finishReason").getAsString();
     357|                    builder.withFinishReason(mapFinishReason(finishReason));
```

### Line 362
**Comment:** Parse usage metadata

**Context:**
```java
     358|                }
     359|            }
     360|        }
>>>  361|
     362|        // Parse usage metadata
     363|        if (jsonResponse.has("usageMetadata")) {
     364|            JsonObject usage = jsonResponse.getAsJsonObject("usageMetadata");
     365|            TokenUsage tokenUsage = new TokenUsage(
```

---

## com/codex/apk/core/providers/GeminiServiceFactory.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/GeminiServiceFactory.java`

### Line 65
**Comment:** Official Gemini API requires API key

**Context:**
```java
      61|        ValidationResult.Builder result = ValidationResult.builder();
      62|
      63|        switch (providerType) {
>>>   64|            case GOOGLE:
      65|                // Official Gemini API requires API key
      66|                if (!config.hasApiKey()) {
      67|                    result.addError("API key is required for Gemini Official API");
      68|                }
```

### Line 70
**Comment:** Validate base URL if provided

**Context:**
```java
      66|                if (!config.hasApiKey()) {
      67|                    result.addError("API key is required for Gemini Official API");
      68|                }
>>>   69|
      70|                // Validate base URL if provided
      71|                if (config.hasBaseUrl()) {
      72|                    String baseUrl = config.getBaseUrl();
      73|                    if (!baseUrl.contains("generativelanguage.googleapis.com")) {
```

### Line 80
**Comment:** Cookie-based implementation requires PSID cookie

**Context:**
```java
      76|                }
      77|                break;
      78|
>>>   79|            case COOKIES:
      80|                // Cookie-based implementation requires PSID cookie
      81|                String psid = config.getProviderSpecificConfig("psid", "");
      82|                if (psid == null || psid.isEmpty()) {
      83|                    result.addError("__Secure-1PSID cookie is required for Gemini Free (cookie-based) access");
```

### Line 86
**Comment:** PSIDTS is optional but recommended

**Context:**
```java
      82|                if (psid == null || psid.isEmpty()) {
      83|                    result.addError("__Secure-1PSID cookie is required for Gemini Free (cookie-based) access");
      84|                }
>>>   85|
      86|                // PSIDTS is optional but recommended
      87|                String psidts = config.getProviderSpecificConfig("psidts", "");
      88|                if (psidts == null || psidts.isEmpty()) {
      89|                    result.addWarning("__Secure-1PSIDTS cookie is recommended for better session stability");
```

---

## com/codex/apk/core/providers/OpenAICompatibleService.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/OpenAICompatibleService.java`

### Line 51
**Comment:** Add authorization header if API key is available

**Context:**
```java
      47|                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
      48|                .addHeader("Content-Type", "application/json")
      49|                .addHeader("Accept", request.isStreaming() ? "text/event-stream" : "application/json");
>>>   50|
      51|            // Add authorization header if API key is available
      52|            if (configuration.hasApiKey()) {
      53|                builder.addHeader("Authorization", "Bearer " + configuration.getApiKey());
      54|            }
```

### Line 56
**Comment:** Add provider-specific headers

**Context:**
```java
      52|            if (configuration.hasApiKey()) {
      53|                builder.addHeader("Authorization", "Bearer " + configuration.getApiKey());
      54|            }
>>>   55|
      56|            // Add provider-specific headers
      57|            addProviderSpecificHeaders(builder, request);
      58|
      59|            return builder.build();
```

### Line 86
**Comment:** Simple implementation - parse the whole response and send as one chunk

**Context:**
```java
      82|                                                              Consumer<Throwable> onError) {
      83|        return CompletableFuture.runAsync(() -> {
      84|            try {
>>>   85|                String responseBody = response.body().string();
      86|                // Simple implementation - parse the whole response and send as one chunk
      87|                AIResponse aiResponse = parseResponse(response, requestId);
      88|                onResponse.accept(aiResponse);
      89|            } catch (Exception e) {
```

### Line 124
**Comment:** Simple health check by trying to fetch models or making a minimal request

**Context:**
```java
     120|    }
     121|
     122|    @Override
>>>  123|    protected boolean performHealthCheck() throws Exception {
     124|        // Simple health check by trying to fetch models or making a minimal request
     125|        try {
     126|            List<AIModel> models = fetchAvailableModels();
     127|            return models != null;
```

### Line 129
**Comment:** If models endpoint fails, try a simple chat request

**Context:**
```java
     125|        try {
     126|            List<AIModel> models = fetchAvailableModels();
     127|            return models != null;
>>>  128|        } catch (Exception e) {
     129|            // If models endpoint fails, try a simple chat request
     130|            return performSimpleHealthCheck();
     131|        }
     132|    }
```

### Line 168
**Comment:** Model

**Context:**
```java
     164|
     165|    private JsonObject buildRequestBody(AIRequest request) {
     166|        JsonObject body = new JsonObject();
>>>  167|
     168|        // Model
     169|        body.addProperty("model", request.getModel());
     170|
     171|        // Messages
```

### Line 171
**Comment:** Messages

**Context:**
```java
     167|
     168|        // Model
     169|        body.addProperty("model", request.getModel());
>>>  170|
     171|        // Messages
     172|        JsonArray messages = new JsonArray();
     173|        for (Message message : request.getMessages()) {
     174|            JsonObject msgObj = new JsonObject();
```

### Line 178
**Comment:** Add name if present (for tool messages)

**Context:**
```java
     174|            JsonObject msgObj = new JsonObject();
     175|            msgObj.addProperty("role", message.getRole().name().toLowerCase());
     176|            msgObj.addProperty("content", message.getContent());
>>>  177|
     178|            // Add name if present (for tool messages)
     179|            if (message.getName() != null) {
     180|                msgObj.addProperty("name", message.getName());
     181|            }
```

### Line 183
**Comment:** Add tool calls if present

**Context:**
```java
     179|            if (message.getName() != null) {
     180|                msgObj.addProperty("name", message.getName());
     181|            }
>>>  182|
     183|            // Add tool calls if present
     184|            if (message.hasToolCalls()) {
     185|                JsonArray toolCalls = new JsonArray();
     186|                for (ToolCall toolCall : message.getToolCalls()) {
```

### Line 205
**Comment:** Parameters

**Context:**
```java
     201|            messages.add(msgObj);
     202|        }
     203|        body.add("messages", messages);
>>>  204|
     205|        // Parameters
     206|        RequestParameters params = request.getParameters();
     207|        if (params.hasTemperature()) {
     208|            body.addProperty("temperature", params.getTemperature());
```

### Line 228
**Comment:** Streaming

**Context:**
```java
     224|            params.getStopSequences().forEach(stop::add);
     225|            body.add("stop", stop);
     226|        }
>>>  227|
     228|        // Streaming
     229|        body.addProperty("stream", params.isStream());
     230|
     231|        // Tools
```

### Line 231
**Comment:** Tools

**Context:**
```java
     227|
     228|        // Streaming
     229|        body.addProperty("stream", params.isStream());
>>>  230|
     231|        // Tools
     232|        if (request.hasTools()) {
     233|            JsonArray tools = ToolSpec.toJsonArray(request.getTools());
     234|            body.add("tools", tools);
```

### Line 237
**Comment:** Provider-specific parameters

**Context:**
```java
     233|            JsonArray tools = ToolSpec.toJsonArray(request.getTools());
     234|            body.add("tools", tools);
     235|        }
>>>  236|
     237|        // Provider-specific parameters
     238|        addProviderSpecificParameters(body, request);
     239|
     240|        return body;
```

### Line 262
**Comment:** Pollinations-specific parameters

**Context:**
```java
     258|        AIProvider provider = configuration.getProviderType();
     259|
     260|        switch (provider) {
>>>  261|            case FREE:
     262|                // Pollinations-specific parameters
     263|                body.addProperty("seed", System.currentTimeMillis() % Integer.MAX_VALUE);
     264|                body.addProperty("referrer", "https://github.com/NikitHamal/CodeZ");
     265|                break;
```

### Line 275
**Comment:** Extract choices

**Context:**
```java
     271|            .withRequestId(requestId)
     272|            .isStreaming(isStreaming)
     273|            .isComplete(isComplete);
>>>  274|
     275|        // Extract choices
     276|        if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
     277|            JsonArray choices = jsonResponse.getAsJsonArray("choices");
     278|            if (choices.size() > 0) {
```

### Line 287
**Comment:** Parse tool calls

**Context:**
```java
     283|                    if (message.has("content") && !message.get("content").isJsonNull()) {
     284|                        builder.withContent(message.get("content").getAsString());
     285|                    }
>>>  286|
     287|                    // Parse tool calls
     288|                    if (message.has("tool_calls")) {
     289|                        List<ToolCall> toolCalls = parseToolCalls(message.getAsJsonArray("tool_calls"));
     290|                        builder.withToolCalls(toolCalls);
```

### Line 294
**Comment:** Parse finish reason

**Context:**
```java
     290|                        builder.withToolCalls(toolCalls);
     291|                    }
     292|                }
>>>  293|
     294|                // Parse finish reason
     295|                if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
     296|                    String finishReason = choice.get("finish_reason").getAsString();
     297|                    builder.withFinishReason(mapFinishReason(finishReason));
```

### Line 302
**Comment:** Extract usage

**Context:**
```java
     298|                }
     299|            }
     300|        }
>>>  301|
     302|        // Extract usage
     303|        if (jsonResponse.has("usage")) {
     304|            JsonObject usage = jsonResponse.getAsJsonObject("usage");
     305|            TokenUsage tokenUsage = new TokenUsage(
```

### Line 313
**Comment:** Extract model

**Context:**
```java
     309|            );
     310|            builder.withUsage(tokenUsage);
     311|        }
>>>  312|
     313|        // Extract model
     314|        if (jsonResponse.has("model")) {
     315|            builder.withModel(jsonResponse.get("model").getAsString());
     316|        }
```

### Line 384
**Comment:** Create basic capabilities for OpenAI-compatible models

**Context:**
```java
     380|            for (JsonElement element : data) {
     381|                JsonObject modelObj = element.getAsJsonObject();
     382|                String id = modelObj.get("id").getAsString();
>>>  383|
     384|                // Create basic capabilities for OpenAI-compatible models
     385|                com.codex.apk.ai.ModelCapabilities capabilities = new com.codex.apk.ai.ModelCapabilities(
     386|                    false, false, false, true, false, false, false, 4096, 2048
     387|                );
```

---

## com/codex/apk/core/providers/OpenAICompatibleServiceFactory.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/OpenAICompatibleServiceFactory.java`

### Line 56
**Comment:** Provider-specific validations

**Context:**
```java
      52|        if (!config.hasBaseUrl()) {
      53|            result.addError("Base URL is required for OpenAI-compatible providers");
      54|        }
>>>   55|
      56|        // Provider-specific validations
      57|        switch (providerType) {
      58|            case DEEPINFRA:
      59|                if (!config.hasApiKey()) {
```

### Line 64
**Comment:** Free endpoints typically don't require API keys

**Context:**
```java
      60|                    result.addError("API key is required for DeepInfra");
      61|                }
      62|                break;
>>>   63|            case FREE:
      64|                // Free endpoints typically don't require API keys
      65|                break;
      66|        }
      67|
```

---

## com/codex/apk/core/providers/QwenService.java

**Location:** `app/src/main/java/com/codex/apk/core/providers/QwenService.java`

### Line 52
**Comment:** Get or create conversation state

**Context:**
```java
      48|
      49|    @Override
      50|    protected Request buildHttpRequest(AIRequest request) throws RequestBuildException {
>>>   51|        try {
      52|            // Get or create conversation state
      53|            QwenConversationState state = getOrCreateConversationState(request);
      54|
      55|            String modelId = request.getModel() != null ? request.getModel() : "qwen3-coder-plus";
```

### Line 57
**Comment:** Ensure conversation exists

**Context:**
```java
      53|            QwenConversationState state = getOrCreateConversationState(request);
      54|
      55|            String modelId = request.getModel() != null ? request.getModel() : "qwen3-coder-plus";
>>>   56|
      57|            // Ensure conversation exists
      58|            if (state.getConversationId() == null) {
      59|                String conversationId = createConversation(modelId, request.requiresCapability("websearch"));
      60|                if (conversationId == null) {
```

### Line 101
**Comment:** Simple implementation - parse the whole response and send as one chunk

**Context:**
```java
      97|                                                              Consumer<Throwable> onError) {
      98|        return CompletableFuture.runAsync(() -> {
      99|            try {
>>>  100|                String responseBody = response.body().string();
     101|                // Simple implementation - parse the whole response and send as one chunk
     102|                AIResponse aiResponse = parseResponse(response, requestId);
     103|                onResponse.accept(aiResponse);
     104|            } catch (Exception e) {
```

### Line 112
**Comment:** Return static models for ALIBABA provider from AIModel registry

**Context:**
```java
     108|    }
     109|
     110|    @Override
>>>  111|    protected List<AIModel> fetchAvailableModels() throws Exception {
     112|        // Return static models for ALIBABA provider from AIModel registry
     113|        List<AIModel> allModels = com.codex.apk.ai.AIModel.values();
     114|        List<AIModel> qwenModels = new ArrayList<>();
     115|
```

### Line 128
**Comment:** Simple health check by trying to create a conversation

**Context:**
```java
     124|
     125|    @Override
     126|    protected boolean performHealthCheck() throws Exception {
>>>  127|        try {
     128|            // Simple health check by trying to create a conversation
     129|            String testConversationId = createConversation("qwen3-coder-plus", false);
     130|            return testConversationId != null;
     131|        } catch (Exception e) {
```

### Line 186
**Comment:** Build messages array

**Context:**
```java
     182|        body.addProperty("model", request.getModel());
     183|        body.addProperty("parent_id", state.getLastParentId());
     184|        body.addProperty("timestamp", System.currentTimeMillis());
>>>  185|
     186|        // Build messages array
     187|        JsonArray messages = new JsonArray();
     188|
     189|        // Add system message if this is the first message
```

### Line 189
**Comment:** Add system message if this is the first message

**Context:**
```java
     185|
     186|        // Build messages array
     187|        JsonArray messages = new JsonArray();
>>>  188|
     189|        // Add system message if this is the first message
     190|        if (state.getLastParentId() == null && request.hasTools()) {
     191|            JsonObject systemMessage = createSystemMessage(request.getTools());
     192|            messages.add(systemMessage);
```

### Line 195
**Comment:** Convert universal messages to Qwen format

**Context:**
```java
     191|            JsonObject systemMessage = createSystemMessage(request.getTools());
     192|            messages.add(systemMessage);
     193|        }
>>>  194|
     195|        // Convert universal messages to Qwen format
     196|        for (Message message : request.getMessages()) {
     197|            if (message.getRole() == Message.MessageRole.USER) {
     198|                JsonObject qwenMessage = createQwenUserMessage(message, request);
```

### Line 206
**Comment:** Add tools if present

**Context:**
```java
     202|        }
     203|
     204|        body.add("messages", messages);
>>>  205|
     206|        // Add tools if present
     207|        if (request.hasTools()) {
     208|            JsonArray tools = ToolSpec.toJsonArray(request.getTools());
     209|            body.add("tools", tools);
```

### Line 247
**Comment:** Add thinking mode if enabled

**Context:**
```java
     243|        JsonObject qwenMessage = new JsonObject();
     244|        qwenMessage.addProperty("role", "user");
     245|        qwenMessage.addProperty("content", message.getContent());
>>>  246|
     247|        // Add thinking mode if enabled
     248|        if (request.requiresCapability("thinking")) {
     249|            qwenMessage.addProperty("thinking_enabled", true);
     250|        }
```

### Line 252
**Comment:** Add web search if enabled

**Context:**
```java
     248|        if (request.requiresCapability("thinking")) {
     249|            qwenMessage.addProperty("thinking_enabled", true);
     250|        }
>>>  251|
     252|        // Add web search if enabled
     253|        if (request.requiresCapability("websearch")) {
     254|            qwenMessage.addProperty("enable_search", true);
     255|        }
```

### Line 285
**Comment:** Fetch new midtoken

**Context:**
```java
     281|            midTokenUses++;
     282|            return midToken;
     283|        }
>>>  284|
     285|        // Fetch new midtoken
     286|        midToken = fetchMidToken();
     287|        midTokenUses = 1;
     288|
```

### Line 322
**Comment:** Parse Qwen's response format

**Context:**
```java
     318|            .isStreaming(isStreaming)
     319|            .isComplete(isComplete);
     320|
>>>  321|        try {
     322|            // Parse Qwen's response format
     323|            StringBuilder content = new StringBuilder();
     324|            StringBuilder thinking = new StringBuilder();
     325|            List<ToolCall> toolCalls = new ArrayList<>();
```

### Line 336
**Comment:** Continue processing other lines

**Context:**
```java
     332|                        try {
     333|                            JsonObject deltaObject = JsonParser.parseString(data).getAsJsonObject();
     334|                            parseQwenDelta(deltaObject, content, thinking, toolCalls);
>>>  335|                        } catch (Exception e) {
     336|                            // Continue processing other lines
     337|                        }
     338|                    }
     339|                }
```

---

## com/codex/apk/core/service/AIServiceManager.java

**Location:** `app/src/main/java/com/codex/apk/core/service/AIServiceManager.java`

### Line 158
**Comment:** If a specific provider is set, try to use it first

**Context:**
```java
     154|        activeServices.clear();
     155|    }
     156|
>>>  157|    private ProviderSelectionResult selectProvider(AIRequest request) {
     158|        // If a specific provider is set, try to use it first
     159|        if (currentProvider != null) {
     160|            try {
     161|                AIService service = getService(currentProvider);
```

### Line 166
**Comment:** Fall through to automatic selection

**Context:**
```java
     162|                if (service.canHandle(request)) {
     163|                    return ProviderSelectionResult.success(service, currentProvider);
     164|                }
>>>  165|            } catch (Exception e) {
     166|                // Fall through to automatic selection
     167|            }
     168|        }
     169|
```

### Line 170
**Comment:** Use provider selector to find the best match

**Context:**
```java
     166|                // Fall through to automatic selection
     167|            }
     168|        }
>>>  169|
     170|        // Use provider selector to find the best match
     171|        return providerSelector.selectOptimal(request, registry.getAvailableProviders());
     172|    }
     173|
```

### Line 189
**Comment:** Try to find a fallback provider

**Context:**
```java
     185|    }
     186|
     187|    private void handleError(AIRequest request, Throwable error,
>>>  188|                           Consumer<AIResponse> onResponse, Consumer<Throwable> onError) {
     189|        // Try to find a fallback provider
     190|        List<AIProvider> availableProviders = List.copyOf(registry.getAvailableProviders());
     191|        for (AIProvider fallback : availableProviders) {
     192|            if (fallback.equals(currentProvider)) continue;
```

### Line 203
**Comment:** Continue to next fallback

**Context:**
```java
     199|                    pipeline.execute(request, service, onResponse, onError);
     200|                    return;
     201|                }
>>>  202|            } catch (Exception e) {
     203|                // Continue to next fallback
     204|            }
     205|        }
     206|
```

### Line 207
**Comment:** No fallback available, return original error

**Context:**
```java
     203|                // Continue to next fallback
     204|            }
     205|        }
>>>  206|
     207|        // No fallback available, return original error
     208|        onError.accept(error);
     209|    }
     210|
```

### Line 269
**Comment:** Score providers based on capabilities and preferences

**Context:**
```java
     265|        this.configuration = configuration;
     266|    }
     267|
>>>  268|    public AIServiceManager.ProviderSelectionResult selectOptimal(AIRequest request, java.util.Set<AIProvider> availableProviders) {
     269|        // Score providers based on capabilities and preferences
     270|        AIProvider bestProvider = null;
     271|        int bestScore = -1;
     272|
```

### Line 284
**Comment:** Skip problematic providers

**Context:**
```java
     280|                        bestProvider = provider;
     281|                    }
     282|                }
>>>  283|            } catch (Exception e) {
     284|                // Skip problematic providers
     285|            }
     286|        }
     287|
```

### Line 304
**Comment:** Base score

**Context:**
```java
     300|
     301|    private int calculateScore(AIProvider provider, AIRequest request, ProviderInfo info) {
     302|        int score = 0;
>>>  303|
     304|        // Base score
     305|        score += 100;
     306|
     307|        // Prefer providers with exact capability matches
```

### Line 307
**Comment:** Prefer providers with exact capability matches

**Context:**
```java
     303|
     304|        // Base score
     305|        score += 100;
>>>  306|
     307|        // Prefer providers with exact capability matches
     308|        if (request.requiresCapability("streaming") && info.getCapabilities().supportsStreaming()) {
     309|            score += 50;
     310|        }
```

### Line 318
**Comment:** Apply configuration preferences

**Context:**
```java
     314|        if (request.requiresCapability("tools") && info.getCapabilities().supportsTools()) {
     315|            score += 50;
     316|        }
>>>  317|
     318|        // Apply configuration preferences
     319|        if (configuration.getPreferredProviders().contains(provider)) {
     320|            score += 200;
     321|        }
```

### Line 342
**Comment:** Start periodic health checks

**Context:**
```java
     338|        this.lastHealthStatus = new ConcurrentHashMap<>();
     339|    }
     340|
>>>  341|    public void startMonitoring(AIProvider provider) {
     342|        // Start periodic health checks
     343|        scheduler.scheduleAtFixedRate(() -> {
     344|            checkService(provider).thenAccept(status -> {
     345|                lastHealthStatus.put(provider, status);
```

---

## com/codex/apk/core/service/BaseAIService.java

**Location:** `app/src/main/java/com/codex/apk/core/service/BaseAIService.java`

### Line 111
**Comment:** Apply provider-specific optimizations

**Context:**
```java
     107|    @Override
     108|    public AIRequest optimizeRequest(AIRequest request) {
     109|        AIRequest.Builder builder = new AIRequest.Builder(request);
>>>  110|
     111|        // Apply provider-specific optimizations
     112|        RequestParameters optimized = optimizeParameters(request.getParameters());
     113|        builder.withParameters(optimized);
     114|
```

### Line 115
**Comment:** Optimize messages if needed

**Context:**
```java
     111|        // Apply provider-specific optimizations
     112|        RequestParameters optimized = optimizeParameters(request.getParameters());
     113|        builder.withParameters(optimized);
>>>  114|
     115|        // Optimize messages if needed
     116|        List<Message> optimizedMessages = optimizeMessages(request.getMessages());
     117|        builder.withMessages(optimizedMessages);
     118|
```

### Line 144
**Comment:** Abstract methods that concrete implementations must provide

**Context:**
```java
     140|            logError("Error during shutdown", e);
     141|        }
     142|    }
>>>  143|
     144|    // Abstract methods that concrete implementations must provide
     145|
     146|    /**
     147|     * Builds a provider-specific HTTP request from the universal AI request.
```

### Line 194
**Comment:** Template method for request execution

**Context:**
```java
     190|     * @throws Exception if health check fails
     191|     */
     192|    protected abstract boolean performHealthCheck() throws Exception;
>>>  193|
     194|    // Template method for request execution
     195|    private CompletableFuture<Void> executeRequest(AIRequest request,
     196|                                                  Consumer<AIResponse> onResponse,
     197|                                                  Consumer<Throwable> onError) {
```

### Line 252
**Comment:** Helper methods

**Context:**
```java
     248|            return null;
     249|        });
     250|    }
>>>  251|
     252|    // Helper methods
     253|
     254|    private OkHttpClient createHttpClient() {
     255|        OkHttpClient.Builder builder = new OkHttpClient.Builder()
```

### Line 260
**Comment:** Add custom headers

**Context:**
```java
     256|            .connectTimeout(configuration.getTimeouts().getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
     257|            .readTimeout(configuration.getTimeouts().getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
     258|            .writeTimeout(configuration.getTimeouts().getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS);
>>>  259|
     260|        // Add custom headers
     261|        if (!configuration.getCustomHeaders().isEmpty()) {
     262|            builder.addInterceptor(chain -> {
     263|                Request.Builder requestBuilder = chain.request().newBuilder();
```

### Line 269
**Comment:** Add retry interceptor if configured

**Context:**
```java
     265|                return chain.proceed(requestBuilder.build());
     266|            });
     267|        }
>>>  268|
     269|        // Add retry interceptor if configured
     270|        if (configuration.getRetryPolicy().getMaxRetries() > 0) {
     271|            builder.addInterceptor(new RetryInterceptor(configuration.getRetryPolicy()));
     272|        }
```

### Line 292
**Comment:** This could be enhanced to check against a list of supported models

**Context:**
```java
     288|    }
     289|
     290|    private boolean supportsModel(String modelId) {
>>>  291|        if (modelId == null) return true; // Provider will use default
     292|        // This could be enhanced to check against a list of supported models
     293|        return true;
     294|    }
     295|
```

### Line 297
**Comment:** Estimate token count and check against limits

**Context:**
```java
     293|        return true;
     294|    }
     295|
>>>  296|    private boolean validateRequestSize(AIRequest request) {
     297|        // Estimate token count and check against limits
     298|        int estimatedTokens = request.getMessages().stream()
     299|            .mapToInt(Message::getEstimatedTokenCount)
     300|            .sum();
```

### Line 310
**Comment:** Base implementation - can be overridden by providers

**Context:**
```java
     306|        return capabilities.supportsStreaming();
     307|    }
     308|
>>>  309|    protected RequestParameters optimizeParameters(RequestParameters params) {
     310|        // Base implementation - can be overridden by providers
     311|        return params;
     312|    }
     313|
```

### Line 315
**Comment:** Base implementation - can be overridden by providers

**Context:**
```java
     311|        return params;
     312|    }
     313|
>>>  314|    protected List<Message> optimizeMessages(List<Message> messages) {
     315|        // Base implementation - can be overridden by providers
     316|        return messages;
     317|    }
     318|
```

### Line 335
**Comment:** Override in subclasses if additional cleanup is needed

**Context:**
```java
     331|        return new ServiceException("Unexpected error: " + e.getMessage(), e);
     332|    }
     333|
>>>  334|    protected void performCleanup() {
     335|        // Override in subclasses if additional cleanup is needed
     336|    }
     337|
     338|    protected void logInfo(String message) {
```

### Line 346
**Comment:** Exception classes

**Context:**
```java
     342|    protected void logError(String message, Throwable error) {
     343|        android.util.Log.e(getClass().getSimpleName(), message, error);
     344|    }
>>>  345|
     346|    // Exception classes
     347|    public static class RequestBuildException extends Exception {
     348|        public RequestBuildException(String message) { super(message); }
     349|        public RequestBuildException(String message, Throwable cause) { super(message, cause); }
```

### Line 397
**Comment:** Wait before retry

**Context:**
```java
     393|                    break;
     394|                }
     395|            }
>>>  396|
     397|            // Wait before retry
     398|            if (attempt < retryPolicy.getMaxRetries()) {
     399|                try {
     400|                    Thread.sleep(retryPolicy.getBackoffDelay().toMillis() * (attempt + 1));
```

---

## com/codex/apk/core/test/ModularArchitectureTest.java

**Location:** `app/src/main/java/com/codex/apk/core/test/ModularArchitectureTest.java`

### Line 24
**Comment:** Create provider registry

**Context:**
```java
      20|     * Tests the basic service registration and request flow.
      21|     */
      22|    public static void testBasicServiceFlow() {
>>>   23|        try {
      24|            // Create provider registry
      25|            ProviderRegistry registry = new ProviderRegistry();
      26|
      27|            // Register OpenAI-compatible providers
```

### Line 27
**Comment:** Register OpenAI-compatible providers

**Context:**
```java
      23|        try {
      24|            // Create provider registry
      25|            ProviderRegistry registry = new ProviderRegistry();
>>>   26|
      27|            // Register OpenAI-compatible providers
      28|            registry.register(AIProvider.DEEPINFRA, OpenAICompatibleServiceFactory.create(AIProvider.DEEPINFRA));
      29|            registry.register(AIProvider.FREE, OpenAICompatibleServiceFactory.create(AIProvider.FREE));
      30|
```

### Line 31
**Comment:** Create service configuration

**Context:**
```java
      27|            // Register OpenAI-compatible providers
      28|            registry.register(AIProvider.DEEPINFRA, OpenAICompatibleServiceFactory.create(AIProvider.DEEPINFRA));
      29|            registry.register(AIProvider.FREE, OpenAICompatibleServiceFactory.create(AIProvider.FREE));
>>>   30|
      31|            // Create service configuration
      32|            ServiceConfiguration config = ServiceConfiguration.builder()
      33|                .withProviderConfig(AIProvider.DEEPINFRA,
      34|                    ProviderConfig.builder(AIProvider.DEEPINFRA)
```

### Line 43
**Comment:** Create request pipeline

**Context:**
```java
      39|                    ProviderConfig.defaults(AIProvider.FREE))
      40|                .withRequestTimeout(Duration.ofMinutes(2))
      41|                .build();
>>>   42|
      43|            // Create request pipeline
      44|            RequestPipeline pipeline = new DefaultRequestPipeline();
      45|
      46|            // Create service manager
```

### Line 46
**Comment:** Create service manager

**Context:**
```java
      42|
      43|            // Create request pipeline
      44|            RequestPipeline pipeline = new DefaultRequestPipeline();
>>>   45|
      46|            // Create service manager
      47|            AIServiceManager serviceManager = new AIServiceManager(registry, pipeline, config);
      48|
      49|            // Test provider switching
```

### Line 49
**Comment:** Test provider switching

**Context:**
```java
      45|
      46|            // Create service manager
      47|            AIServiceManager serviceManager = new AIServiceManager(registry, pipeline, config);
>>>   48|
      49|            // Test provider switching
      50|            serviceManager.switchProvider(AIProvider.FREE);
      51|            assert serviceManager.getCurrentProvider() == AIProvider.FREE;
      52|
```

### Line 53
**Comment:** Test request building

**Context:**
```java
      49|            // Test provider switching
      50|            serviceManager.switchProvider(AIProvider.FREE);
      51|            assert serviceManager.getCurrentProvider() == AIProvider.FREE;
>>>   52|
      53|            // Test request building
      54|            AIRequest request = AIRequest.builder()
      55|                .withModel("gpt-3.5-turbo")
      56|                .addMessage(Message.user("Hello, world!"))
```

### Line 64
**Comment:** Validate request

**Context:**
```java
      60|                    .withStream(false)
      61|                    .build())
      62|                .build();
>>>   63|
      64|            // Validate request
      65|            ValidationResult validation = request.validate();
      66|            assert validation.isValid() : "Request should be valid";
      67|
```

### Line 68
**Comment:** Test service capability checking

**Context:**
```java
      64|            // Validate request
      65|            ValidationResult validation = request.validate();
      66|            assert validation.isValid() : "Request should be valid";
>>>   67|
      68|            // Test service capability checking
      69|            AIService service = serviceManager.getService(AIProvider.FREE);
      70|            assert service.canHandle(request) : "Service should be able to handle the request";
      71|
```

### Line 72
**Comment:** Test health check

**Context:**
```java
      68|            // Test service capability checking
      69|            AIService service = serviceManager.getService(AIProvider.FREE);
      70|            assert service.canHandle(request) : "Service should be able to handle the request";
>>>   71|
      72|            // Test health check
      73|            CompletableFuture<HealthStatus> healthFuture = service.healthCheck();
      74|            HealthStatus health = healthFuture.get();
      75|            System.out.println("Health check result: " + health.isHealthy());
```

### Line 90
**Comment:** Test valid configuration

**Context:**
```java
      86|     * Tests configuration validation and provider capabilities.
      87|     */
      88|    public static void testConfigurationValidation() {
>>>   89|        try {
      90|            // Test valid configuration
      91|            ProviderConfig validConfig = ProviderConfig.builder(AIProvider.DEEPINFRA)
      92|                .withBaseUrl("https://api.deepinfra.com/v1/openai")
      93|                .withApiKey("test-key")
```

### Line 99
**Comment:** Test invalid configuration

**Context:**
```java
      95|
      96|            ValidationResult validation = validConfig.validate();
      97|            assert validation.isValid() : "Valid configuration should pass validation";
>>>   98|
      99|            // Test invalid configuration
     100|            try {
     101|                ProviderConfig invalidConfig = ProviderConfig.builder(AIProvider.DEEPINFRA)
     102|                    .withBaseUrl("invalid-url")
```

### Line 107
**Comment:** Expected

**Context:**
```java
     103|                    .build(); // Missing required API key
     104|
     105|                assert false : "Invalid configuration should throw exception";
>>>  106|            } catch (IllegalArgumentException e) {
     107|                // Expected
     108|            }
     109|
     110|            // Test configuration merging
```

### Line 110
**Comment:** Test configuration merging

**Context:**
```java
     106|            } catch (IllegalArgumentException e) {
     107|                // Expected
     108|            }
>>>  109|
     110|            // Test configuration merging
     111|            ProviderConfig base = ProviderConfig.defaults(AIProvider.FREE);
     112|            ProviderConfig override = ProviderConfig.builder(AIProvider.FREE)
     113|                .withApiKey("new-key")
```

### Line 132
**Comment:** Test message creation

**Context:**
```java
     128|     * Tests data model creation and validation.
     129|     */
     130|    public static void testDataModels() {
>>>  131|        try {
     132|            // Test message creation
     133|            Message userMessage = Message.user("Hello");
     134|            assert userMessage.getRole() == Message.MessageRole.USER;
     135|            assert "Hello".equals(userMessage.getContent());
```

### Line 140
**Comment:** Test request parameters

**Context:**
```java
     136|
     137|            Message systemMessage = Message.system("You are a helpful assistant");
     138|            assert systemMessage.getRole() == Message.MessageRole.SYSTEM;
>>>  139|
     140|            // Test request parameters
     141|            RequestParameters params = RequestParameters.builder()
     142|                .withTemperature(0.8)
     143|                .withMaxTokens(1000)
```

### Line 150
**Comment:** Test invalid parameters

**Context:**
```java
     146|
     147|            ValidationResult paramValidation = params.validate();
     148|            assert paramValidation.isValid() : "Valid parameters should pass validation";
>>>  149|
     150|            // Test invalid parameters
     151|            RequestParameters invalidParams = RequestParameters.builder()
     152|                .withTemperature(-1.0) // Invalid temperature
     153|                .build();
```

### Line 158
**Comment:** Test request building

**Context:**
```java
     154|
     155|            ValidationResult invalidValidation = invalidParams.validate();
     156|            assert invalidValidation.hasErrors() : "Invalid parameters should have errors";
>>>  157|
     158|            // Test request building
     159|            AIRequest request = AIRequest.builder()
     160|                .withModel("test-model")
     161|                .addMessage(userMessage)
```

### Line 184
**Comment:** Test registration

**Context:**
```java
     180|    public static void testProviderRegistry() {
     181|        try {
     182|            ProviderRegistry registry = new ProviderRegistry();
>>>  183|
     184|            // Test registration
     185|            AIServiceFactory factory = OpenAICompatibleServiceFactory.create(AIProvider.FREE);
     186|            registry.register(AIProvider.FREE, factory);
     187|
```

### Line 191
**Comment:** Test retrieval

**Context:**
```java
     187|
     188|            assert registry.isRegistered(AIProvider.FREE) : "Provider should be registered";
     189|            assert registry.size() == 1 : "Registry should have one provider";
>>>  190|
     191|            // Test retrieval
     192|            AIServiceFactory retrieved = registry.getFactory(AIProvider.FREE);
     193|            assert retrieved != null : "Factory should be retrievable";
     194|            assert retrieved.getProviderType() == AIProvider.FREE;
```

### Line 196
**Comment:** Test provider info

**Context:**
```java
     192|            AIServiceFactory retrieved = registry.getFactory(AIProvider.FREE);
     193|            assert retrieved != null : "Factory should be retrievable";
     194|            assert retrieved.getProviderType() == AIProvider.FREE;
>>>  195|
     196|            // Test provider info
     197|            ProviderInfo info = registry.getProviderInfo(AIProvider.FREE);
     198|            assert info != null : "Provider info should be available";
     199|            assert info.getType() == AIProvider.FREE;
```

### Line 201
**Comment:** Test unregistration

**Context:**
```java
     197|            ProviderInfo info = registry.getProviderInfo(AIProvider.FREE);
     198|            assert info != null : "Provider info should be available";
     199|            assert info.getType() == AIProvider.FREE;
>>>  200|
     201|            // Test unregistration
     202|            AIServiceFactory unregistered = registry.unregister(AIProvider.FREE);
     203|            assert unregistered != null : "Unregistered factory should be returned";
     204|            assert !registry.isRegistered(AIProvider.FREE) : "Provider should no longer be registered";
```

### Line 244
**Comment:** Simple implementation that just forwards to the service

**Context:**
```java
     240|    @Override
     241|    public CompletableFuture<Void> execute(AIRequest request, AIService service,
     242|                                          java.util.function.Consumer<AIResponse> onResponse,
>>>  243|                                          java.util.function.Consumer<Throwable> onError) {
     244|        // Simple implementation that just forwards to the service
     245|        return service.sendMessage(request, onResponse, onError);
     246|    }
     247|
```

---

## com/codex/apk/core/tools/FileSystemToolExecutor.java

**Location:** `app/src/main/java/com/codex/apk/core/tools/FileSystemToolExecutor.java`

### Line 25
**Comment:** Very basic argument handling: args is expected to be a simple JSON-like string.

**Context:**
```java
      21|    @Override
      22|    public ToolResult execute(ToolCall call, ExecutionContext context) throws Exception {
      23|        String name = call.getName();
>>>   24|        String args = call.getArguments();
      25|        // Very basic argument handling: args is expected to be a simple JSON-like string.
      26|        // For compile/unblock purposes, we support a minimal subset.
      27|        try {
      28|            switch (name) {
```

### Line 26
**Comment:** For compile/unblock purposes, we support a minimal subset.

**Context:**
```java
      22|    public ToolResult execute(ToolCall call, ExecutionContext context) throws Exception {
      23|        String name = call.getName();
      24|        String args = call.getArguments();
>>>   25|        // Very basic argument handling: args is expected to be a simple JSON-like string.
      26|        // For compile/unblock purposes, we support a minimal subset.
      27|        try {
      28|            switch (name) {
      29|                case "createFile": {
```

### Line 104
**Comment:** extremely naive key extractor from a json-ish string: "key":"value"

**Context:**
```java
     100|        if (rel == null) rel = "";
     101|        return new File(base, rel).toPath();
     102|    }
>>>  103|
     104|    // extremely naive key extractor from a json-ish string: "key":"value"
     105|    private String extract(String json, String key) {
     106|        if (json == null) return null;
     107|        String needle = "\"" + key + "\":";
```

---

## com/codex/apk/core/tools/ToolRegistry.java

**Location:** `app/src/main/java/com/codex/apk/core/tools/ToolRegistry.java`

### Line 153
**Comment:** Validate call against definition

**Context:**
```java
     149|        if (executor == null) {
     150|            throw new ToolExecutionException("Executor not found for tool: " + toolName);
     151|        }
>>>  152|
     153|        // Validate call against definition
     154|        ValidationResult callValidation = definition.validateCall(call);
     155|        if (callValidation.hasErrors()) {
     156|            throw new ToolExecutionException("Tool call validation failed: " + callValidation.getErrors());
```

### Line 159
**Comment:** Check permissions

**Context:**
```java
     155|        if (callValidation.hasErrors()) {
     156|            throw new ToolExecutionException("Tool call validation failed: " + callValidation.getErrors());
     157|        }
>>>  158|
     159|        // Check permissions
     160|        if (!securityManager.hasPermissions(context, executor.getRequiredPermissions())) {
     161|            throw new ToolExecutionException("Insufficient permissions for tool: " + toolName);
     162|        }
```

### Line 164
**Comment:** Execute the tool

**Context:**
```java
     160|        if (!securityManager.hasPermissions(context, executor.getRequiredPermissions())) {
     161|            throw new ToolExecutionException("Insufficient permissions for tool: " + toolName);
     162|        }
>>>  163|
     164|        // Execute the tool
     165|        try {
     166|            return executor.execute(call, context);
     167|        } catch (Exception e) {
```

### Line 230
**Comment:** Register file system tools

**Context:**
```java
     226|        executorService.shutdown();
     227|    }
     228|
>>>  229|    private void registerDefaultTools() {
     230|        // Register file system tools
     231|        FileSystemToolExecutor fileExecutor = new FileSystemToolExecutor();
     232|
     233|        registerTool(ToolDefinition.createFile(), fileExecutor);
```

### Line 240
**Comment:** Register web search tool if available

**Context:**
```java
     236|        registerTool(ToolDefinition.deleteFile(), fileExecutor);
     237|        registerTool(ToolDefinition.listFiles(), fileExecutor);
     238|        registerTool(ToolDefinition.searchFiles(), fileExecutor);
>>>  239|
     240|        // Register web search tool if available
     241|        try {
     242|            WebSearchToolExecutor webExecutor = new WebSearchToolExecutor();
     243|            registerTool(ToolDefinition.webSearch(), webExecutor);
```

### Line 245
**Comment:** Web search not available

**Context:**
```java
     241|        try {
     242|            WebSearchToolExecutor webExecutor = new WebSearchToolExecutor();
     243|            registerTool(ToolDefinition.webSearch(), webExecutor);
>>>  244|        } catch (Exception e) {
     245|            // Web search not available
     246|        }
     247|    }
     248|
```

### Line 294
**Comment:** Could add JSON schema validation here

**Context:**
```java
     290|        if (!name.equals(call.getName())) {
     291|            result.addError("Tool name mismatch");
     292|        }
>>>  293|
     294|        // Could add JSON schema validation here
     295|
     296|        return result.build();
     297|    }
```

### Line 317
**Comment:** Factory methods for common tools

**Context:**
```java
     313|    public ToolSpec toToolSpec() {
     314|        return new ToolSpec(name, description, parametersSchema);
     315|    }
>>>  316|
     317|    // Factory methods for common tools
     318|    public static ToolDefinition createFile() {
     319|        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
     320|        // Add schema definition for createFile parameters
```

### Line 320
**Comment:** Add schema definition for createFile parameters

**Context:**
```java
     316|
     317|    // Factory methods for common tools
     318|    public static ToolDefinition createFile() {
>>>  319|        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
     320|        // Add schema definition for createFile parameters
     321|
     322|        return new ToolDefinition(
     323|            "createFile",
```

### Line 333
**Comment:** Add schema definition

**Context:**
```java
     329|    }
     330|
     331|    public static ToolDefinition readFile() {
>>>  332|        com.google.gson.JsonObject schema = new com.google.gson.JsonObject();
     333|        // Add schema definition
     334|
     335|        return new ToolDefinition(
     336|            "readFile",
```

### Line 483
**Comment:** For now, allow all permissions - could be enhanced with actual permission checking

**Context:**
```java
     479| */
     480|class DefaultSecurityManager implements SecurityManager {
     481|    @Override
>>>  482|    public boolean hasPermissions(ExecutionContext context, Set<Permission> requiredPermissions) {
     483|        // For now, allow all permissions - could be enhanced with actual permission checking
     484|        return true;
     485|    }
     486|}
```

---

## com/codex/apk/core/tools/WebSearchToolExecutor.java

**Location:** `app/src/main/java/com/codex/apk/core/tools/WebSearchToolExecutor.java`

### Line 43
**Comment:** extremely naive key extractor from a json-ish string: "key":"value"

**Context:**
```java
      39|
      40|    private String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
      41|    private String safe(String s) { return s == null ? "" : s; }
>>>   42|
      43|    // extremely naive key extractor from a json-ish string: "key":"value"
      44|    private String extract(String json, String key) {
      45|        if (json == null) return null;
      46|        String needle = "\"" + key + "\":";
```

---

## com/codex/apk/editor/AiAssistantManager.java

**Location:** `app/src/main/java/com/codex/apk/editor/AiAssistantManager.java`

### Line 51
**Comment:** Track current streaming AI message position

**Context:**
```java
      47|    private final AiProcessor aiProcessor; // AiProcessor instance
      48|    private final ExecutorService executorService;
      49|    private PlanExecutor planExecutor; // The new PlanExecutor instance
>>>   50|
      51|    // Track current streaming AI message position
      52|    private Integer currentStreamingMessagePosition = null;
      53|
      54|    public AiAssistantManager(EditorActivity activity, File projectDir, String projectName,
```

### Line 66
**Comment:** Model selection: prefer per-project last-used, else global default, else fallback

**Context:**
```java
      62|        String apiKey = SettingsActivity.getGeminiApiKey(activity);
      63|        this.aiAssistant = new AIAssistant(activity, apiKey, projectDir, projectName, executorService, this);
      64|        this.aiAssistant.setEnabledTools(com.codex.apk.ToolSpec.defaultFileToolsPlusSearchNet());
>>>   65|
      66|        // Model selection: prefer per-project last-used, else global default, else fallback
      67|        SharedPreferences settingsPrefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
      68|        SharedPreferences modelPrefs = activity.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
      69|        String projectKey = "project_" + (projectName != null ? projectName : "default") + "_last_model";
```

### Line 120
**Comment:** Persist per-project last used model

**Context:**
```java
     116|        }
     117|
     118|        try {
>>>  119|            aiAssistant.sendPrompt(userPrompt, chatHistory, qwenState, currentFileName, currentFileContent);
     120|            // Persist per-project last used model
     121|            String projectKey = "project_" + (activity.getProjectName() != null ? activity.getProjectName() : "default") + "_last_model";
     122|            activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
     123|                    .edit()
```

### Line 149
**Comment:** Track changed files to refresh them

**Context:**
```java
     145|                    List<File> changedFiles = new ArrayList<>();
     146|                    for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
     147|                        String summary = aiProcessor.applyFileAction(detail);
>>>  148|                        appliedSummaries.add(summary);
     149|                        // Track changed files to refresh them
     150|                        File fileToRefresh = new File(activity.getProjectDirectory(), detail.path);
     151|                        if (fileToRefresh.exists()) {
     152|                            changedFiles.add(fileToRefresh);
```

### Line 169
**Comment:** Refresh tabs and file tree

**Context:**
```java
     165|                        AIChatFragment aiChatFragment = activity.getAiChatFragment();
     166|                        if (aiChatFragment != null) {
     167|                            aiChatFragment.updateMessage(messagePosition, message);
>>>  168|                        }
     169|                        // Refresh tabs and file tree
     170|                        activity.tabManager.refreshOpenTabsAfterAi();
     171|                        activity.loadFileTree();
     172|                    });
```

### Line 181
**Comment:** Agent mode: auto-apply without additional approval

**Context:**
```java
     177|            });
     178|            return;
     179|        }
>>>  180|
     181|        // Agent mode: auto-apply without additional approval
     182|        executorService.execute(() -> {
     183|            List<String> appliedSummaries = new ArrayList<>();
     184|            List<ChatMessage.FileActionDetail> steps = message.getProposedFileChanges();
```

### Line 234
**Comment:** Look for ```json ... ``` pattern

**Context:**
```java
     230|    private String extractJsonFromCodeBlock(String content) {
     231|        if (content == null || content.trim().isEmpty()) {
     232|            return null;
>>>  233|        }
     234|        // Look for ```json ... ``` pattern
     235|        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```json\\s*([\\s\\S]*?)```", java.util.regex.Pattern.CASE_INSENSITIVE);
     236|        java.util.regex.Matcher matcher = pattern.matcher(content);
     237|        if (matcher.find()) {
```

### Line 240
**Comment:** Also check for ``` ... ``` pattern (without json specifier)

**Context:**
```java
     236|        java.util.regex.Matcher matcher = pattern.matcher(content);
     237|        if (matcher.find()) {
     238|            return matcher.group(1).trim();
>>>  239|        }
     240|        // Also check for ``` ... ``` pattern (without json specifier)
     241|        pattern = java.util.regex.Pattern.compile("```\\s*([\\s\\S]*?)```");
     242|        matcher = pattern.matcher(content);
     243|        if (matcher.find()) {
```

### Line 286
**Comment:** Public API required by EditorActivity and UI

**Context:**
```java
     282|        }
     283|        return -1;
     284|    }
>>>  285|
     286|    // Public API required by EditorActivity and UI
     287|    public void onAiDiscardActions(int messagePosition, ChatMessage message) {
     288|        Log.d(TAG, "User discarded AI actions for message at position: " + messagePosition);
     289|        message.setStatus(ChatMessage.STATUS_DISCARDED);
```

### Line 325
**Comment:** Prefer provided diffPatch if it looks like a valid unified diff; otherwise, generate fallback

**Context:**
```java
     321|
     322|        String oldFileContent = fileActionDetail.oldContent != null ? fileActionDetail.oldContent : "";
     323|        String newFileContent = fileActionDetail.newContent != null ? fileActionDetail.newContent : "";
>>>  324|
     325|        // Prefer provided diffPatch if it looks like a valid unified diff; otherwise, generate fallback
     326|        String providedPatch = fileActionDetail.diffPatch != null ? fileActionDetail.diffPatch.trim() : "";
     327|        boolean looksUnified = false;
     328|        if (!providedPatch.isEmpty()) {
```

### Line 329
**Comment:** Heuristics: presence of @@ hunk headers or ---/+++ file markers

**Context:**
```java
     325|        // Prefer provided diffPatch if it looks like a valid unified diff; otherwise, generate fallback
     326|        String providedPatch = fileActionDetail.diffPatch != null ? fileActionDetail.diffPatch.trim() : "";
     327|        boolean looksUnified = false;
>>>  328|        if (!providedPatch.isEmpty()) {
     329|            // Heuristics: presence of @@ hunk headers or ---/+++ file markers
     330|            looksUnified = providedPatch.contains("@@ ") || (providedPatch.startsWith("--- ") && providedPatch.contains("\n+++ "));
     331|        }
     332|
```

### Line 337
**Comment:** Fallback: generate unified diff from contents with appropriate file marker paths

**Context:**
```java
     333|        String diffContent;
     334|        if (!providedPatch.isEmpty() && looksUnified) {
     335|            diffContent = providedPatch;
>>>  336|        } else {
     337|            // Fallback: generate unified diff from contents with appropriate file marker paths
     338|            if ("createFile".equals(fileActionDetail.type)) {
     339|                diffContent = DiffGenerator.generateDiff("", newFileContent, "unified", "/dev/null", "b/" + fileNameToOpen);
     340|            } else if ("deleteFile".equals(fileActionDetail.type)) {
```

### Line 356
**Comment:** --- Implement AIAssistant.AIActionListener methods ---

**Context:**
```java
     352|    }
     353|
     354|    public void shutdown() { if (aiAssistant != null) aiAssistant.shutdown(); }
>>>  355|
     356|    // --- Implement AIAssistant.AIActionListener methods ---
     357|    @Override
     358|    public void onAiActionsProcessed(String rawAiResponseJson, String explanation, List<String> suggestions, List<ChatMessage.FileActionDetail> proposedFileChanges, String aiModelDisplayName) {
     359|        onAiActionsProcessed(rawAiResponseJson, explanation, suggestions, proposedFileChanges, aiModelDisplayName, null, null);
```

### Line 380
**Comment:** Centralized tool call handling

**Context:**
```java
     376|                uiFrag.hideThinkingMessage();
     377|                currentStreamingMessagePosition = null;
     378|            }
>>>  379|
     380|            // Centralized tool call handling
     381|            String jsonToParseForTools = extractJsonFromCodeBlock(explanation);
     382|            if (jsonToParseForTools == null) {
     383|                // Fallback to check the raw response if nothing is found in the explanation
```

### Line 383
**Comment:** Fallback to check the raw response if nothing is found in the explanation

**Context:**
```java
     379|
     380|            // Centralized tool call handling
     381|            String jsonToParseForTools = extractJsonFromCodeBlock(explanation);
>>>  382|            if (jsonToParseForTools == null) {
     383|                // Fallback to check the raw response if nothing is found in the explanation
     384|                jsonToParseForTools = extractJsonFromCodeBlock(rawAiResponseJson);
     385|            }
     386|            if (jsonToParseForTools == null && looksLikeJson(explanation)) {
```

### Line 391
**Comment:** Log the JSON for debugging instead of showing in UI

**Context:**
```java
     387|                jsonToParseForTools = explanation;
     388|            }
     389|
>>>  390|            if (jsonToParseForTools != null) {
     391|                // Log the JSON for debugging instead of showing in UI
     392|                Log.d(TAG, "Detected tool call JSON: " + jsonToParseForTools);
     393|                try {
     394|                    JsonObject maybe = JsonParser.parseString(jsonToParseForTools).getAsJsonObject();
```

### Line 427
**Comment:** Not a valid tool call, proceed with normal processing.

**Context:**
```java
     423|                        sendAiPrompt(fenced, new java.util.ArrayList<>(), activity.getQwenState(), activity.getActiveTab());
     424|                        return; // Stop further processing
     425|                    }
>>>  426|                } catch (Exception e) {
     427|                    // Not a valid tool call, proceed with normal processing.
     428|                    Log.w(TAG, "Could not execute tool call. Error parsing JSON.", e);
     429|                }
     430|            }
```

### Line 577
**Comment:** When thinking, the main content should be blank or a placeholder

**Context:**
```java
     573|            if (msg == null) return;
     574|
     575|            if (isThinking) {
>>>  576|                msg.setThinkingContent(partialResponse);
     577|                // When thinking, the main content should be blank or a placeholder
     578|                if (msg.getContent() == null || !msg.getContent().equals(activity.getString(com.codex.apk.R.string.ai_is_thinking))) {
     579|                    msg.setContent("");
     580|                }
```

### Line 583
**Comment:** Clear thinking content when we get a final response

**Context:**
```java
     579|                    msg.setContent("");
     580|                }
     581|            } else {
>>>  582|                msg.setContent(partialResponse != null ? partialResponse : "");
     583|                // Clear thinking content when we get a final response
     584|                msg.setThinkingContent(null);
     585|            }
     586|            chatFragment.updateMessage(currentStreamingMessagePosition, msg);
```

---

## com/codex/apk/editor/EditorUiManager.java

**Location:** `app/src/main/java/com/codex/apk/editor/EditorUiManager.java`

### Line 45
**Comment:** UI components

**Context:**
```java
      41|    private final DialogHelper dialogHelper;
      42|    private final ExecutorService executorService;
      43|    private final List<TabItem> openTabs; // Need access to openTabs for preview logic
>>>   44|
      45|    // UI components
      46|    private DrawerLayout drawerLayout;
      47|    private NavigationView navigationView;
      48|    private MaterialToolbar toolbar;
```

### Line 67
**Comment:** Initialize drawer components

**Context:**
```java
      63|     * Initializes the main UI components from the layout.
      64|     */
      65|    public void initializeViews() {
>>>   66|        try {
      67|            // Initialize drawer components
      68|            drawerLayout = activity.findViewById(R.id.drawer_layout);
      69|            navigationView = activity.findViewById(R.id.navigation_drawer);
      70|            toolbar = activity.findViewById(R.id.toolbar);
```

### Line 73
**Comment:** Setup refresh button

**Context:**
```java
      69|            navigationView = activity.findViewById(R.id.navigation_drawer);
      70|            toolbar = activity.findViewById(R.id.toolbar);
      71|            mainViewPager = activity.findViewById(R.id.view_pager);
>>>   72|
      73|            // Setup refresh button
      74|            View refreshButton = activity.findViewById(R.id.btn_refresh_file_tree);
      75|            if (refreshButton != null) {
      76|                refreshButton.setOnClickListener(v -> {
```

### Line 101
**Comment:** Setup drawer toggle

**Context:**
```java
      97|                activity.getSupportActionBar().setTitle(activity.getProjectName()); // Get project name from activity
      98|                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      99|            }
>>>  100|
     101|            // Setup drawer toggle
     102|            if (drawerLayout != null) {
     103|                drawerToggle = new ActionBarDrawerToggle(
     104|                    activity, drawerLayout, toolbar,
```

### Line 154
**Comment:** No drawer to close, go directly to exit check

**Context:**
```java
     150|    /**
     151|     * Handles the back press logic for the activity.
     152|     */
>>>  153|    public void handleBackPressed() {
     154|        // No drawer to close, go directly to exit check
     155|        checkUnsavedChangesBeforeExit();
     156|    }
     157|
```

### Line 241
**Comment:** Preview is now in a separate activity, no need to update here

**Context:**
```java
     237|     * @param content The new content of the active file.
     238|     * @param fileName The name of the active file.
     239|     */
>>>  240|    public void onActiveTabContentChanged(String content, String fileName) {
     241|        // Preview is now in a separate activity, no need to update here
     242|        // This method can be removed or used for other UI updates in the future
     243|    }
     244|
```

### Line 242
**Comment:** This method can be removed or used for other UI updates in the future

**Context:**
```java
     238|     * @param fileName The name of the active file.
     239|     */
     240|    public void onActiveTabContentChanged(String content, String fileName) {
>>>  241|        // Preview is now in a separate activity, no need to update here
     242|        // This method can be removed or used for other UI updates in the future
     243|    }
     244|
     245|    /**
```

### Line 250
**Comment:** Preview is now in a separate activity, no need to update here

**Context:**
```java
     246|     * Handles active tab changes (preview is now in separate activity)
     247|     * @param newFile The File object of the newly active tab.
     248|     */
>>>  249|    public void onActiveTabChanged(File newFile) {
     250|        // Preview is now in a separate activity, no need to update here
     251|        // This method can be removed or used for other UI updates in the future
     252|    }
     253|
```

### Line 251
**Comment:** This method can be removed or used for other UI updates in the future

**Context:**
```java
     247|     * @param newFile The File object of the newly active tab.
     248|     */
     249|    public void onActiveTabChanged(File newFile) {
>>>  250|        // Preview is now in a separate activity, no need to update here
     251|        // This method can be removed or used for other UI updates in the future
     252|    }
     253|
     254|    public ViewPager2 getMainViewPager() {
```

---

## com/codex/apk/editor/ExpandableTreeAdapter.java

**Location:** `app/src/main/java/com/codex/apk/editor/ExpandableTreeAdapter.java`

### Line 70
**Comment:** Indentation guide is now drawn by ItemDecoration; hide per-item guide to avoid gaps

**Context:**
```java
      66|        int base = (int) (12 * density);
      67|        int indent = base + (int) (14 * density) * Math.max(0, node.level);
      68|        holder.itemView.setPadding(indent, (int) (4 * density), holder.itemView.getPaddingRight(), (int) (4 * density));
>>>   69|
      70|        // Indentation guide is now drawn by ItemDecoration; hide per-item guide to avoid gaps
      71|        if (holder.indentGuide != null) {
      72|            holder.indentGuide.setVisibility(View.GONE);
      73|        }
```

### Line 77
**Comment:** Set icon per state

**Context:**
```java
      73|        }
      74|
      75|        holder.textFileName.setText(f.getName());
>>>   76|
      77|        // Set icon per state
      78|        if (f.isDirectory()) {
      79|            holder.imageFileIcon.setImageResource(node.expanded ? R.drawable.ic_folder_open_outline : R.drawable.ic_folder_outline);
      80|            // Hide chevron if folder has no children
```

### Line 80
**Comment:** Hide chevron if folder has no children

**Context:**
```java
      76|
      77|        // Set icon per state
      78|        if (f.isDirectory()) {
>>>   79|            holder.imageFileIcon.setImageResource(node.expanded ? R.drawable.ic_folder_open_outline : R.drawable.ic_folder_outline);
      80|            // Hide chevron if folder has no children
      81|            holder.imageExpandIcon.setVisibility(node.hasVisibleChildren() ? View.VISIBLE : View.INVISIBLE);
      82|            holder.imageExpandIcon.setImageResource(node.expanded ? R.drawable.icon_expand_less_round : R.drawable.icon_expand_more_round);
      83|        } else {
```

### Line 88
**Comment:** Apply consistent tints from color tokens

**Context:**
```java
      84|            holder.imageFileIcon.setImageResource(getFileIconRes(f.getName()));
      85|            holder.imageExpandIcon.setVisibility(View.GONE);
      86|        }
>>>   87|
      88|        // Apply consistent tints from color tokens
      89|        int iconColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_icon_color);
      90|        int textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_text_color);
      91|        int normalBg = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_background);
```

### Line 94
**Comment:** Determine selection based on active tab

**Context:**
```java
      90|        int textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_text_color);
      91|        int normalBg = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_background);
      92|        int selectedIcon = ContextCompat.getColor(holder.itemView.getContext(), R.color.file_tree_selected_icon_color);
>>>   93|
      94|        // Determine selection based on active tab
      95|        com.codex.apk.TabItem active = activity.getActiveTab();
      96|        boolean isSelected = active != null && active.getFile() != null && f.equals(active.getFile());
      97|
```

---

## com/codex/apk/editor/FileTreeManager.java

**Location:** `app/src/main/java/com/codex/apk/editor/FileTreeManager.java`

### Line 39
**Comment:** Ensure no item decorations are present (no indent lines)

**Context:**
```java
      35|        searchEditText = activity.findViewById(R.id.search_edit_text);
      36|        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
      37|        adapter = new ExpandableTreeAdapter(activity, new ArrayList<>());
>>>   38|        recyclerView.setAdapter(adapter);
      39|        // Ensure no item decorations are present (no indent lines)
      40|        for (int i = recyclerView.getItemDecorationCount() - 1; i >= 0; i--) {
      41|            recyclerView.removeItemDecorationAt(i);
      42|        }
```

### Line 70
**Comment:** Do not show the top-level project folder; use its children as roots

**Context:**
```java
      66|        File root = activity.getProjectDirectory();
      67|        List<TreeNode> roots = new ArrayList<>();
      68|        if (root != null && root.exists()) {
>>>   69|            TreeNode rootNode = buildTree(root, 0, null);
      70|            // Do not show the top-level project folder; use its children as roots
      71|            if (!rootNode.children.isEmpty()) {
      72|                roots.addAll(rootNode.children);
      73|                rebaseAsRoots(roots);
```

### Line 87
**Comment:** Show empty only when there are truly no root nodes to display

**Context:**
```java
      83|
      84|    private void updateEmptyState(List<TreeNode> nodes) {
      85|        View emptyStateView = activity.findViewById(R.id.empty_state_view);
>>>   86|        if (emptyStateView != null) {
      87|            // Show empty only when there are truly no root nodes to display
      88|            // After filtering/rebasing, any non-empty roots means we have results
      89|            boolean hasAny = !nodes.isEmpty();
      90|            emptyStateView.setVisibility(hasAny ? View.GONE : View.VISIBLE);
```

### Line 88
**Comment:** After filtering/rebasing, any non-empty roots means we have results

**Context:**
```java
      84|    private void updateEmptyState(List<TreeNode> nodes) {
      85|        View emptyStateView = activity.findViewById(R.id.empty_state_view);
      86|        if (emptyStateView != null) {
>>>   87|            // Show empty only when there are truly no root nodes to display
      88|            // After filtering/rebasing, any non-empty roots means we have results
      89|            boolean hasAny = !nodes.isEmpty();
      90|            emptyStateView.setVisibility(hasAny ? View.GONE : View.VISIBLE);
      91|            TextView emptyStateText = activity.findViewById(R.id.empty_state_text);
```

### Line 128
**Comment:** Fix isLast flags post-filter

**Context:**
```java
     124|        for (TreeNode n : nodes) {
     125|            TreeNode copy = n.copyPruned(query, null);
     126|            if (copy != null) result.add(copy);
>>>  127|        }
     128|        // Fix isLast flags post-filter
     129|        for (TreeNode r : result) fixSiblingsFlagsRecursive(r);
     130|        return result;
     131|    }
```

### Line 137
**Comment:** Reset isLast correctly at each level

**Context:**
```java
     133|    private void rebaseAsRoots(List<TreeNode> roots) {
     134|        for (TreeNode r : roots) {
     135|            rebaseLevelsRecursive(r, 0, null);
>>>  136|        }
     137|        // Reset isLast correctly at each level
     138|        for (TreeNode r : roots) fixSiblingsFlagsRecursive(r);
     139|    }
     140|
```

### Line 259
**Comment:** TreeNode model

**Context:**
```java
     255|            });
     256|        }
     257|    }
>>>  258|
     259|    // TreeNode model
     260|    static class TreeNode {
     261|        final File file;
     262|        int level;
```

### Line 280
**Comment:** Set isLast for children

**Context:**
```java
     276|            for (TreeNode c : children) {
     277|                TreeNode pruned = c.copyPruned(query, copy);
     278|                if (pruned != null) copy.children.add(pruned);
>>>  279|            }
     280|            // Set isLast for children
     281|            for (int i = 0; i < copy.children.size(); i++) {
     282|                copy.children.get(i).isLast = (i == copy.children.size() - 1);
     283|            }
```

---

## com/codex/apk/editor/PlanExecutor.java

**Location:** `app/src/main/java/com/codex/apk/editor/PlanExecutor.java`

### Line 118
**Comment:** We must update the UI to show the failed state

**Context:**
```java
     114|            if (planStepRetryCount > 4) {
     115|                Log.e(TAG, "AI did not produce file ops after 5 prompts; marking step failed and continuing.");
     116|                setCurrentRunningPlanStepStatus("failed");
>>>  117|                planStepRetryCount = 0;
     118|                // We must update the UI to show the failed state
     119|                AIChatFragment frag = activity.getAiChatFragment();
     120|                if (lastPlanMessagePosition != null && frag != null) {
     121|                    ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
```

### Line 124
**Comment:** Don't halt, just move to the next step

**Context:**
```java
     120|                if (lastPlanMessagePosition != null && frag != null) {
     121|                    ChatMessage planMsg = frag.getMessageAt(lastPlanMessagePosition);
     122|                    if (planMsg != null) frag.updateMessage(lastPlanMessagePosition, planMsg);
>>>  123|                }
     124|                // Don't halt, just move to the next step
     125|                sendNextPlanStepFollowUp();
     126|            } else {
     127|                Log.w(TAG, "AI did not return file operations. Retrying step (attempt " + planStepRetryCount + ")");
```

---

## com/codex/apk/editor/TabManager.java

**Location:** `app/src/main/java/com/codex/apk/editor/TabManager.java`

### Line 66
**Comment:** If file is already open, switch to it

**Context:**
```java
      62|        }
      63|
      64|        for (int i = 0; i < openTabs.size(); i++) {
>>>   65|            if (openTabs.get(i).getFile().equals(file)) {
      66|                // If file is already open, switch to it
      67|                activity.getCodeEditorFragment().setFileViewPagerCurrentItem(i, true);
      68|                // Also ensure we are on the Code tab (position 1)
      69|                activity.getMainViewPager().setCurrentItem(1, true);
```

### Line 68
**Comment:** Also ensure we are on the Code tab (position 1)

**Context:**
```java
      64|        for (int i = 0; i < openTabs.size(); i++) {
      65|            if (openTabs.get(i).getFile().equals(file)) {
      66|                // If file is already open, switch to it
>>>   67|                activity.getCodeEditorFragment().setFileViewPagerCurrentItem(i, true);
      68|                // Also ensure we are on the Code tab (position 1)
      69|                activity.getMainViewPager().setCurrentItem(1, true);
      70|                return;
      71|            }
```

### Line 81
**Comment:** Initialize tab defaults from Settings

**Context:**
```java
      77|                return;
      78|            }
      79|            String content = fileManager.readFileContent(file);
>>>   80|            TabItem tabItem = new TabItem(file, content);
      81|            // Initialize tab defaults from Settings
      82|            tabItem.setWrapEnabled(SettingsActivity.isDefaultWordWrap(activity));
      83|            tabItem.setReadOnly(SettingsActivity.isDefaultReadOnly(activity));
      84|            openTabs.add(tabItem);
```

### Line 86
**Comment:** Switch to Code tab (position 1) and then to the newly opened file

**Context:**
```java
      82|            tabItem.setWrapEnabled(SettingsActivity.isDefaultWordWrap(activity));
      83|            tabItem.setReadOnly(SettingsActivity.isDefaultReadOnly(activity));
      84|            openTabs.add(tabItem);
>>>   85|            activity.getCodeEditorFragment().addFileTab(tabItem); // Add to fragment's adapter
      86|            // Switch to Code tab (position 1) and then to the newly opened file
      87|            activity.getMainViewPager().setCurrentItem(1, false);
      88|            activity.getCodeEditorFragment().setFileViewPagerCurrentItem(openTabs.size() - 1, true);
      89|            activity.getCodeEditorFragment().refreshFileTabLayout();
```

### Line 108
**Comment:** Create a unique file object for the diff tab.

**Context:**
```java
     104|            activity.getMainViewPager().setCurrentItem(1, true); // Switch to editor view
     105|            return;
     106|        }
>>>  107|
     108|        // Create a unique file object for the diff tab.
     109|        File diffFile = new File(activity.getProjectDirectory(), "DIFF_" + fileName); // Use projectDir from activity
     110|
     111|        // Check if a diff tab for this file is already open
```

### Line 111
**Comment:** Check if a diff tab for this file is already open

**Context:**
```java
     107|
     108|        // Create a unique file object for the diff tab.
     109|        File diffFile = new File(activity.getProjectDirectory(), "DIFF_" + fileName); // Use projectDir from activity
>>>  110|
     111|        // Check if a diff tab for this file is already open
     112|        for (int i = 0; i < openTabs.size(); i++) {
     113|            TabItem existingTab = openTabs.get(i);
     114|            if (existingTab.getFile().equals(diffFile)) {
```

### Line 115
**Comment:** Update content and switch to existing diff tab

**Context:**
```java
     111|        // Check if a diff tab for this file is already open
     112|        for (int i = 0; i < openTabs.size(); i++) {
     113|            TabItem existingTab = openTabs.get(i);
>>>  114|            if (existingTab.getFile().equals(diffFile)) {
     115|                // Update content and switch to existing diff tab
     116|                existingTab.setContent(diffContent);
     117|                existingTab.setModified(false); // Diff tabs are not "modified" in the save sense
     118|                activity.getCodeEditorFragment().setFileViewPagerCurrentItem(i, true);
```

### Line 125
**Comment:** Create a new TabItem for the diff

**Context:**
```java
     121|                return;
     122|            }
     123|        }
>>>  124|
     125|        // Create a new TabItem for the diff
     126|        TabItem diffTabItem = new TabItem(diffFile, diffContent);
     127|        diffTabItem.setModified(false); // Diffs are not user-editable in this context
     128|
```

### Line 146
**Comment:** Prevent saving for diff tabs

**Context:**
```java
     142|        if (tabItem == null || tabItem.getFile() == null) {
     143|            Log.e(TAG, "Cannot save, TabItem or its file is null");
     144|            return;
>>>  145|        }
     146|        // Prevent saving for diff tabs
     147|        if (tabItem.getFile().getName().startsWith("DIFF_")) {
     148|            activity.showToast("Diff tabs cannot be saved.");
     149|            tabItem.setModified(false); // Ensure it's not marked as modified
```

### Line 191
**Comment:** Don't refresh diff tabs

**Context:**
```java
     187|
     188|        TabItem tabItem = openTabs.get(position);
     189|        File file = tabItem.getFile();
>>>  190|
     191|        // Don't refresh diff tabs
     192|        if (file.getName().startsWith("DIFF_")) {
     193|            activity.showToast("Cannot refresh a diff tab.");
     194|            return;
```

### Line 199
**Comment:** Optionally, close the tab

**Context:**
```java
     195|        }
     196|
     197|        if (!file.exists()) {
>>>  198|            activity.showToast("File no longer exists.");
     199|            // Optionally, close the tab
     200|            removeTabAtPosition(position);
     201|            return;
     202|        }
```

### Line 221
**Comment:** Prevent saving for diff tabs

**Context:**
```java
     217|        if (tabItem == null || tabItem.getFile() == null) {
     218|            Log.e(TAG, "Cannot save, TabItem or its file is null");
     219|            return;
>>>  220|        }
     221|        // Prevent saving for diff tabs
     222|        if (tabItem.getFile().getName().startsWith("DIFF_")) {
     223|            if (showToast) {
     224|                activity.showToast("Diff tabs cannot be saved.");
```

### Line 261
**Comment:** If it's a diff tab, just close it without confirmation or saving

**Context:**
```java
     257|        if (position < 0 || position >= openTabs.size()) return;
     258|
     259|        TabItem tabItem = openTabs.get(position);
>>>  260|
     261|        // If it's a diff tab, just close it without confirmation or saving
     262|        if (tabItem.getFile().getName().startsWith("DIFF_")) {
     263|            removeTabAtPosition(position);
     264|            return;
```

### Line 291
**Comment:** Purge diff cache for the tab being removed

**Context:**
```java
     287|     * @param position The position of the tab to remove.
     288|     */
     289|    private void removeTabAtPosition(int position) {
>>>  290|        if (position >= 0 && position < openTabs.size()) {
     291|            // Purge diff cache for the tab being removed
     292|            TabItem removed = openTabs.get(position);
     293|            if (activity.getCodeEditorFragment() != null) {
     294|                SimpleSoraTabAdapter adapter = activity.getCodeEditorFragment().getFileTabAdapter();
```

### Line 349
**Comment:** Clear diff caches as many tabs are being closed at once

**Context:**
```java
     345|    private void performCloseOtherTabs(int keepPosition) {
     346|        if (keepPosition < 0 || keepPosition >= openTabs.size()) return;
     347|
>>>  348|        TabItem tabToKeep = openTabs.get(keepPosition);
     349|        // Clear diff caches as many tabs are being closed at once
     350|        if (activity.getCodeEditorFragment() != null && activity.getCodeEditorFragment().getFileTabAdapter() != null) {
     351|            activity.getCodeEditorFragment().getFileTabAdapter().clearDiffCaches();
     352|        }
```

### Line 399
**Comment:** Clear diff caches as all tabs are being closed

**Context:**
```java
     395|    /**
     396|     * Performs the actual closing of all open tabs.
     397|     */
>>>  398|    private void performCloseAllTabs() {
     399|        // Clear diff caches as all tabs are being closed
     400|        if (activity.getCodeEditorFragment() != null && activity.getCodeEditorFragment().getFileTabAdapter() != null) {
     401|            activity.getCodeEditorFragment().getFileTabAdapter().clearDiffCaches();
     402|        }
```

### Line 456
**Comment:** Skip diff tabs as they are not real files and don't need content refresh from disk

**Context:**
```java
     452|            return;
     453|        }
     454|
>>>  455|        for (TabItem tab : currentOpenTabs) {
     456|            // Skip diff tabs as they are not real files and don't need content refresh from disk
     457|            if (tab.getFile().getName().startsWith("DIFF_")) {
     458|                continue;
     459|            }
```

### Line 484
**Comment:** Check if the file path itself changed (e.g., due to rename)

**Context:**
```java
     480|                        tab.setModified(false); // Mark as not modified as content is synced
     481|                        tabsChanged = true;
     482|                        Log.d(TAG, "Tab content for " + tab.getFileName() + " updated by AI.");
>>>  483|                    }
     484|                    // Check if the file path itself changed (e.g., due to rename)
     485|                    if (!tab.getFile().getAbsolutePath().equals(currentFileInProjectDir.getAbsolutePath())) {
     486|                        Log.d(TAG, "Tab file path updated for " + tab.getFileName() + " from " + tab.getFile().getAbsolutePath() + " to " + currentFileInProjectDir.getAbsolutePath());
     487|                        tab.setFile(currentFileInProjectDir);
```

---

## com/codex/apk/lint/CssLinter.java

**Location:** `app/src/main/java/com/codex/apk/lint/CssLinter.java`

### Line 14
**Comment:** Warn on !important usage

**Context:**
```java
      10|        int close = count(content, '}');
      11|        if (open != close) {
      12|            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.ERROR, "Unbalanced braces in CSS"));
>>>   13|        }
      14|        // Warn on !important usage
      15|        if (content.contains("!important")) {
      16|            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.INFO, "Avoid !important where possible"));
      17|        }
```

---

## com/codex/apk/lint/HtmlLinter.java

**Location:** `app/src/main/java/com/codex/apk/lint/HtmlLinter.java`

### Line 20
**Comment:** Simple check for unclosed tags by angle bracket balance

**Context:**
```java
      16|        }
      17|        if (!lower.contains("<body")) {
      18|            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Missing <body> section"));
>>>   19|        }
      20|        // Simple check for unclosed tags by angle bracket balance
      21|        int lt = count(content, '<');
      22|        int gt = count(content, '>');
      23|        if (gt < lt) {
```

### Line 27
**Comment:** Tag stack validation (basic): detect unclosed closing mismatch

**Context:**
```java
      23|        if (gt < lt) {
      24|            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.ERROR, "Unbalanced angle brackets; possible unclosed tag(s)"));
      25|        }
>>>   26|
      27|        // Tag stack validation (basic): detect unclosed closing mismatch
      28|        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
      29|        int line = 1, col = 1;
      30|        for (int i = 0; i < content.length();) {
```

### Line 60
**Comment:** Accessibility nudges

**Context:**
```java
      56|        }
      57|        if (!stack.isEmpty()) {
      58|            issues.add(new LintIssue(path, line, col, LintIssue.Severity.ERROR, "Unclosed tag(s): " + String.join(", ", stack)));
>>>   59|        }
      60|        // Accessibility nudges
      61|        if (lower.contains("<img ") && !lower.contains(" alt=")) {
      62|            issues.add(new LintIssue(path, 1, 1, LintIssue.Severity.WARNING, "Images should include alt attributes"));
      63|        }
```

### Line 73
**Comment:** Common container tags to track; void tags omitted

**Context:**
```java
      69|
      70|    private int count(String s, char ch) { int c=0; for (int i=0;i<s.length();i++) if (s.charAt(i)==ch) c++; return c; }
      71|
>>>   72|    private boolean isContainerTag(String name) {
      73|        // Common container tags to track; void tags omitted
      74|        switch (name) {
      75|            case "html": case "head": case "body": case "div": case "section": case "header": case "footer":
      76|            case "main": case "nav": case "article": case "aside": case "ul": case "ol": case "li": case "span":
```

---

## com/codex/apk/lint/JsLinter.java

**Location:** `app/src/main/java/com/codex/apk/lint/JsLinter.java`

### Line 9
**Comment:** Tokenize and track line/col with a simple scanner; ignore strings/comments for balance

**Context:**
```java
       5|
       6|public class JsLinter {
       7|    public List<LintIssue> lint(String path, String content) {
>>>    8|        List<LintIssue> issues = new ArrayList<>();
       9|        // Tokenize and track line/col with a simple scanner; ignore strings/comments for balance
      10|        int line = 1, col = 1;
      11|        boolean inSgl = false, inDbl = false, inTpl = false, inLineCmt = false, inBlkCmt = false;
      12|        int balParen = 0, balBrace = 0, balBracket = 0;
```

### Line 31
**Comment:** Not in string/comment → track brackets

**Context:**
```java
      27|            if (!inSgl && !inTpl && ch == '"' ) { inDbl = !inDbl; col++; continue; }
      28|            if (!inSgl && !inDbl && ch == '`' ) { inTpl = !inTpl; col++; continue; }
      29|            if (ch == '\n') { line++; col = 1; continue; }
>>>   30|            if (inSgl || inDbl || inTpl) { col++; continue; }
      31|            // Not in string/comment → track brackets
      32|            if (ch == '(') balParen++; else if (ch == ')') balParen--;
      33|            if (ch == '{') balBrace++; else if (ch == '}') balBrace--;
      34|            if (ch == '[') balBracket++; else if (ch == ']') balBracket--;
```

### Line 46
**Comment:** intentionally no general balance helper; handled by scanner to ignore strings/comments

**Context:**
```java
      42|        }
      43|        return issues;
      44|    }
>>>   45|
      46|    // intentionally no general balance helper; handled by scanner to ignore strings/comments
      47|}
      48|
      49|
```

---

## com/codex/apk/util/FileOps.java

**Location:** `app/src/main/java/com/codex/apk/util/FileOps.java`

### Line 56
**Comment:** Backward-compatible flavor: restrict to common web file extensions and return offsets/snippets

**Context:**
```java
      52|        }
      53|    }
      54|
>>>   55|    public static JsonArray searchInProject(File root, String query, int maxResults, boolean regex) {
      56|        // Backward-compatible flavor: restrict to common web file extensions and return offsets/snippets
      57|        List<String> exts = Arrays.asList("html", "htm", "css", "js", "json", "md");
      58|        return searchInFilesOffsets(root, query, true, regex, exts, maxResults);
      59|    }
```

### Line 125
**Comment:** ===== Consolidated search/file listing helpers (migrated from FileSearchHelper) =====

**Context:**
```java
     121|        }
     122|        return String.join("\n", out);
     123|    }
>>>  124|
     125|    // ===== Consolidated search/file listing helpers (migrated from FileSearchHelper) =====
     126|
     127|    // Line-number oriented search result flavor
     128|    public static class LineSearchResult {
```

### Line 127
**Comment:** Line-number oriented search result flavor

**Context:**
```java
     123|    }
     124|
     125|    // ===== Consolidated search/file listing helpers (migrated from FileSearchHelper) =====
>>>  126|
     127|    // Line-number oriented search result flavor
     128|    public static class LineSearchResult {
     129|        private final File file;
     130|        private final String fileName;
```

### Line 150
**Comment:** Public: search by file name (simple contains match respecting case sensitivity)

**Context:**
```java
     146|        public String getLineContent() { return lineContent; }
     147|        public String getMatchedText() { return matchedText; }
     148|    }
>>>  149|
     150|    // Public: search by file name (simple contains match respecting case sensitivity)
     151|    public static List<File> searchFilesByName(File projectDir, String pattern, boolean caseSensitive) {
     152|        List<File> results = new ArrayList<>();
     153|        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory() || pattern == null) return results;
```

### Line 176
**Comment:** Public: search in files and return line-number oriented results

**Context:**
```java
     172|            }
     173|        }
     174|    }
>>>  175|
     176|    // Public: search in files and return line-number oriented results
     177|    public static List<LineSearchResult> searchInFiles(File projectDir, String searchText, boolean caseSensitive,
     178|                                                       boolean useRegex, List<String> fileExtensions, int maxResults) {
     179|        List<LineSearchResult> results = new ArrayList<>();
```

### Line 241
**Comment:** Public: search in files and return offset/snippet oriented results

**Context:**
```java
     237|            }
     238|        } catch (IOException ignored) {}
     239|    }
>>>  240|
     241|    // Public: search in files and return offset/snippet oriented results
     242|    public static JsonArray searchInFilesOffsets(File projectDir, String searchText, boolean caseSensitive,
     243|                                                 boolean useRegex, List<String> fileExtensions, int maxResults) {
     244|        JsonArray out = new JsonArray();
```

### Line 305
**Comment:** Recent files helper

**Context:**
```java
     301|        }
     302|        return out;
     303|    }
>>>  304|
     305|    // Recent files helper
     306|    public static List<File> getRecentFiles(File projectDir, int maxFiles) {
     307|        List<File> files = new ArrayList<>();
     308|        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory()) return files;
```

### Line 327
**Comment:** Convenience helpers using projectDir and relative paths

**Context:**
```java
     323|            }
     324|        }
     325|    }
>>>  326|
     327|    // Convenience helpers using projectDir and relative paths
     328|    public static void createFile(File projectDir, String relativePath, String content) throws java.io.IOException {
     329|        File file = new File(projectDir, relativePath);
     330|        File parent = file.getParentFile();
```

---

## com/codex/apk/util/ResponseUtils.java

**Location:** `app/src/main/java/com/codex/apk/util/ResponseUtils.java`

### Line 6
**Comment:** Build final explanation including thinking content if available

**Context:**
```java
       2|
       3|public final class ResponseUtils {
       4|    private ResponseUtils() {}
>>>    5|
       6|    // Build final explanation including thinking content if available
       7|    public static String buildExplanationWithThinking(String baseExplanation, String thinking) {
       8|        if (thinking == null || thinking.trim().isEmpty()) return baseExplanation != null ? baseExplanation : "";
       9|        StringBuilder sb = new StringBuilder();
```

---

