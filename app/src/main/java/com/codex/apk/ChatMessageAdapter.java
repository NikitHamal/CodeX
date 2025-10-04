package com.codex.apk;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.animation.AlphaAnimation;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.app.AlertDialog;
import com.codex.apk.ai.WebSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ViewTreeObserver;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages;
    private final Context context;
    private OnAiActionInteractionListener aiActionInteractionListener;

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;

    public interface OnAiActionInteractionListener {
        void onAcceptClicked(int messagePosition, ChatMessage message);
        void onDiscardClicked(int messagePosition, ChatMessage message);
        void onReapplyClicked(int messagePosition, ChatMessage message);
        void onFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail);
        void onPlanAcceptClicked(int messagePosition, ChatMessage message);
        void onPlanDiscardClicked(int messagePosition, ChatMessage message);
    }

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    public void setOnAiActionInteractionListener(OnAiActionInteractionListener listener) {
        this.aiActionInteractionListener = listener;
    }

    @Override
    public int getItemViewType(int position) { return messages.get(position).getSender(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view, parent.getContext());
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_message, parent, false);
            return new AiMessageViewHolder(view, aiActionInteractionListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((AiMessageViewHolder) holder).bind(message, position);
        }
        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(24f);
            holder.itemView.animate().alpha(1f).translationY(0f).setDuration(250).start();
            lastAnimatedPosition = position;
        }
    }

    private int lastAnimatedPosition = -1;

    @Override
    public int getItemCount() { return messages.size(); }

    static class PlanStepsAdapter extends RecyclerView.Adapter<PlanStepsAdapter.StepViewHolder> {
        private final List<ChatMessage.PlanStep> steps;
        PlanStepsAdapter(List<ChatMessage.PlanStep> steps) { this.steps = steps != null ? steps : new ArrayList<>(); }
        @NonNull
        @Override
        public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_step, parent, false);
            return new StepViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull StepViewHolder holder, int position) { holder.bind(steps.get(position)); }
        @Override
        public int getItemCount() { return steps.size(); }
        static class StepViewHolder extends RecyclerView.ViewHolder {
            TextView title; TextView status;
            StepViewHolder(View itemView) { super(itemView); title = itemView.findViewById(R.id.text_step_title); status = itemView.findViewById(R.id.text_step_status); }
            void bind(ChatMessage.PlanStep step) {
                title.setText(step.title);
                String s = step.status != null ? step.status : "pending";
                status.setText(capitalize(s));
                int colorId;
                switch (s) { case "running": colorId = R.color.warning_container; break; case "completed": colorId = R.color.success_container; break; case "failed": colorId = R.color.error_container; break; default: colorId = R.color.surface_container; }
                if (status.getBackground() instanceof GradientDrawable) { GradientDrawable bg = (GradientDrawable) status.getBackground().mutate(); bg.setColor(itemView.getResources().getColor(colorId)); }

                // Long-press to show raw response for this step
                itemView.setOnLongClickListener(v -> {
                    Context ctx = itemView.getContext();
                    View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_raw_api_response, null);
                    TextView textRawResponse = dialogView.findViewById(R.id.text_raw_response);
                    com.google.android.material.button.MaterialButton buttonCopy = dialogView.findViewById(R.id.button_copy);
                    com.google.android.material.button.MaterialButton buttonClose = dialogView.findViewById(R.id.button_close);
                    String raw = step.rawResponse;
                    textRawResponse.setText(raw != null && !raw.isEmpty() ? raw : "No raw response captured for this step yet.");
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ctx);
                    builder.setView(dialogView);
                    final androidx.appcompat.app.AlertDialog dialog = builder.create();
                    buttonCopy.setOnClickListener(x -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Plan Step Raw Response", raw != null ? raw : "");
                            clipboard.setPrimaryClip(clip);
                            android.widget.Toast.makeText(ctx, "Raw response copied", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                    buttonClose.setOnClickListener(x -> dialog.dismiss());
                    dialog.show();
                    return true;
                });
            }
            private String capitalize(String x) { return x.length() > 0 ? Character.toUpperCase(x.charAt(0)) + x.substring(1) : x; }
        }
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage; MaterialCardView cardMessage; private final Context context; RecyclerView recyclerAttachments;
        UserMessageViewHolder(View itemView, Context context) { super(itemView); this.context = context; textMessage = itemView.findViewById(R.id.text_message_content); cardMessage = itemView.findViewById(R.id.user_message_card_view); recyclerAttachments = itemView.findViewById(R.id.recycler_user_attachments); }
        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            List<String> paths = message.getUserAttachmentPaths();
            if (paths != null && !paths.isEmpty()) {
                recyclerAttachments.setVisibility(View.VISIBLE);
                recyclerAttachments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
                recyclerAttachments.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        // Vertical: thumbnail/card (64dp) + filename below
                        LinearLayout root = new LinearLayout(context);
                        root.setOrientation(LinearLayout.VERTICAL);
                        root.setPadding(dp(4), dp(4), dp(4), dp(4));

                        com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(context);
                        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(dp(64), dp(64));
                        card.setLayoutParams(cardLp);
                        card.setRadius(dp(8));
                        card.setCardElevation(0f);
                        card.setStrokeWidth(0);

                        ImageView iv = new ImageView(context);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        card.addView(iv);

                        TextView tv = new TextView(context);
                        tv.setTextColor(ContextCompat.getColor(context, R.color.on_primary_container));
                        tv.setTextSize(12);
                        tv.setMaxLines(1);
                        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT);
                        tvLp.topMargin = dp(4);
                        tv.setLayoutParams(tvLp);

                        root.addView(card);
                        root.addView(tv);

                        return new RecyclerView.ViewHolder(root) {};
                    }
                    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        LinearLayout root = (LinearLayout) holder.itemView;
                        com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) root.getChildAt(0);
                        ImageView iv = (ImageView) card.getChildAt(0);
                        TextView tv = (TextView) root.getChildAt(1);
                        String p = paths.get(position);
                        java.io.File f = new java.io.File(p);
                        tv.setText(f.getName());

                        if (isImageFile(f)) {
                            android.graphics.Bitmap bmp = decodeSampledBitmapFromFile(f.getAbsolutePath(), dp(64), dp(64));
                            if (bmp != null) {
                                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                iv.setImageBitmap(bmp);
                            } else {
                                iv.setScaleType(ImageView.ScaleType.CENTER);
                                iv.setImageResource(R.drawable.icon_file_round);
                            }
                        } else {
                            iv.setScaleType(ImageView.ScaleType.CENTER);
                            iv.setImageResource(R.drawable.icon_file_round);
                        }

                        holder.itemView.setOnClickListener(v -> {
                            try {
                                Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", f);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(uri);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                context.startActivity(intent);
                            } catch (Exception ignored) {}
                        });
                    }
                    @Override public int getItemCount() { return paths.size(); }

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
            } else {
                recyclerAttachments.setVisibility(View.GONE);
            }
        }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage; TextView textAiModelName; RecyclerView fileChangesContainer; LinearLayout layoutThinkingSection; TextView textThinkingContent; TextView textThinkingHeaderTitle; LinearLayout layoutWebSources; TextView buttonWebSources; LinearLayout layoutTypingIndicator; TextView textTypingIndicator; LinearLayout layoutPlanSteps; RecyclerView recyclerPlanSteps; TextView textAgentThinking;
        LinearLayout layoutPlanActions; MaterialButton buttonAcceptPlan; MaterialButton buttonDiscardPlan;
        private final OnAiActionInteractionListener listener; private final Context context; private MarkdownFormatter markdownFormatter;
        AiMessageViewHolder(View itemView, OnAiActionInteractionListener listener) {
            super(itemView); this.listener = listener; this.context = itemView.getContext();
            textMessage = itemView.findViewById(R.id.text_message);
            textAiModelName = itemView.findViewById(R.id.text_ai_model_name);
            fileChangesContainer = itemView.findViewById(R.id.file_changes_container);
            layoutThinkingSection = itemView.findViewById(R.id.layout_thinking_section);
            textThinkingContent = itemView.findViewById(R.id.text_thinking_content);
            textThinkingHeaderTitle = itemView.findViewById(R.id.text_thinking_header_title);
            layoutWebSources = itemView.findViewById(R.id.layout_web_sources);
            buttonWebSources = itemView.findViewById(R.id.button_web_sources);
            layoutTypingIndicator = itemView.findViewById(R.id.layout_typing_indicator);
            textTypingIndicator = itemView.findViewById(R.id.text_typing_indicator);
            layoutPlanSteps = itemView.findViewById(R.id.layout_plan_steps);
            recyclerPlanSteps = itemView.findViewById(R.id.recycler_plan_steps);
            textAgentThinking = itemView.findViewById(R.id.text_agent_thinking);
            layoutPlanActions = itemView.findViewById(R.id.layout_plan_actions);
            buttonAcceptPlan = itemView.findViewById(R.id.button_accept_plan);
            buttonDiscardPlan = itemView.findViewById(R.id.button_discard_plan);
            markdownFormatter = MarkdownFormatter.getInstance(context);
            // Long click is set in bind with the bound message to avoid outer messages reference
        }
        
        private void showWebSourcesDialog(List<WebSource> webSources) {
            showWebSourcesDialogAt(webSources, -1);
        }

        private void showWebSourcesDialogAt(List<WebSource> webSources, int initialIndex) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_web_sources, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_web_sources);
            WebSourcesAdapter adapter = new WebSourcesAdapter(webSources);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(adapter);
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            dialog.setContentView(dialogView);
            dialog.setOnShowListener(d -> {
                if (initialIndex >= 0 && initialIndex < webSources.size()) {
                    recyclerView.post(() -> recyclerView.scrollToPosition(initialIndex));
                }
            });
            dialog.show();
        }
        
        private void showRawApiResponseDialog(ChatMessage message) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_raw_api_response, null);
            TextView textRawResponse = dialogView.findViewById(R.id.text_raw_response);
            MaterialButton buttonCopy = dialogView.findViewById(R.id.button_copy);
            MaterialButton buttonClose = dialogView.findViewById(R.id.button_close);

            String rawResponse = message.getRawApiResponse();
            String displayText;

            if (rawResponse != null && !rawResponse.isEmpty()) {
                try {
                    String trimmedResponse = rawResponse.trim();
                    if (trimmedResponse.startsWith("{")) {
                        displayText = new org.json.JSONObject(trimmedResponse).toString(4);
                    } else if (trimmedResponse.startsWith("[")) {
                        displayText = new org.json.JSONArray(trimmedResponse).toString(4);
                    } else {
                        displayText = rawResponse;
                    }
                } catch (org.json.JSONException e) {
                    displayText = rawResponse;
                }
            } else {
                displayText = "No raw API response available.";
            }

            textRawResponse.setText(displayText);
            final String finalResponseToCopy = displayText;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            buttonCopy.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Raw API Response", finalResponseToCopy);
                    clipboard.setPrimaryClip(clip);
                    android.widget.Toast.makeText(context, "Raw response copied", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            buttonClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        private void applyCitationSpans(TextView tv, List<WebSource> sources) {
            if (tv == null || sources == null || sources.isEmpty()) return;
            CharSequence text = tv.getText();
            if (text == null) return;
            // Replace visible [[n]] with (n)
            String visible = text.toString().replaceAll("\\[\\[(\\d+)\\]\\]", "($1)");
            SpannableStringBuilder ssb = new SpannableStringBuilder(visible);
            // Find (n) again to attach spans
            Pattern p = Pattern.compile("\\((\\d+)\\)");
            Matcher m = p.matcher(visible);
            while (m.find()) {
                int start = m.start();
                int end = m.end();
                String numStr = m.group(1);
                int idx;
                try { idx = Integer.parseInt(numStr); } catch (Exception e) { continue; }
                final int targetIndex = Math.max(0, Math.min(sources.size() - 1, idx - 1));
                ssb.setSpan(new ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) {
                        showWebSourcesDialogAt(sources, targetIndex);
                    }
                }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            tv.setText(ssb);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
        }

        void bind(ChatMessage message, int messagePosition) {
            boolean isTyping = message.getContent() != null && message.getContent().equals(context.getString(R.string.ai_is_thinking));
            itemView.setOnLongClickListener(v -> { showRawApiResponseDialog(message); return true; });

            layoutTypingIndicator.setVisibility(isTyping ? View.VISIBLE : View.GONE);
            if (isTyping) {
                // Minimal indicator: keep subtle fade animation
                AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f);
                anim.setDuration(800);
                anim.setRepeatMode(AlphaAnimation.REVERSE);
                anim.setRepeatCount(AlphaAnimation.INFINITE);
                layoutTypingIndicator.startAnimation(anim);
            } else {
                layoutTypingIndicator.clearAnimation();
            }
            textMessage.setVisibility(isTyping ? View.GONE : View.VISIBLE);
            layoutThinkingSection.setVisibility(isTyping ? View.GONE : (message.getThinkingContent() != null && !message.getThinkingContent().trim().isEmpty() ? View.VISIBLE : View.GONE));
            layoutWebSources.setVisibility(isTyping ? View.GONE : (message.getWebSources() != null && !message.getWebSources().isEmpty() ? View.VISIBLE : View.GONE));
            layoutPlanSteps.setVisibility(isTyping ? View.GONE : (message.getPlanSteps() != null && !message.getPlanSteps().isEmpty() ? View.VISIBLE : View.GONE));
            itemView.findViewById(R.id.layout_proposed_file_changes).setVisibility(isTyping ? View.GONE : (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty() ? View.VISIBLE : View.GONE));

            textAiModelName.setText(message.getAiModelName());

            String content = message.getContent();
            String displayContent = content;
            if (message.getPlanSteps() != null && !message.getPlanSteps().isEmpty()) {
                String planTitle = null;
                String raw = message.getRawApiResponse();
                if (raw != null && !raw.trim().isEmpty()) {
                    try {
                        com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                        if (obj.has("action") && "plan".equalsIgnoreCase(obj.get("action").getAsString())) {
                            String goal = obj.has("goal") ? obj.get("goal").getAsString() : "Plan";
                            planTitle = "Plan: " + goal;
                        }
                    } catch (Exception ignore) {}
                }
                if (planTitle != null) {
                    displayContent = planTitle;
                } else if (content != null && content.trim().startsWith("{") && (raw != null && raw.contains("\"action\"") && raw.contains("\"plan\""))) {
                    // Suppress raw JSON echo when plan UI is present
                    displayContent = "";
                } else if (content != null && content.trim().startsWith("{") && com.codex.apk.QwenResponseParser.looksLikeJson(content)) {
                    // Suppress any other JSON content
                    displayContent = "";
                }
            }
            if (message.getThinkingContent() != null && !message.getThinkingContent().trim().isEmpty() && displayContent != null) {
                int idx = displayContent.indexOf("[Thinking]");
                if (idx >= 0) {
                    displayContent = displayContent.substring(0, idx).trim();
                }
            }
            if (displayContent != null && !displayContent.isEmpty()) {
                String processedContent = markdownFormatter.preprocessMarkdown(displayContent);
                markdownFormatter.setMarkdown(textMessage, processedContent);
            } else {
                textMessage.setText("");
            }

            if (layoutThinkingSection.getVisibility() == View.VISIBLE) {
                String processedThinking = markdownFormatter.preprocessMarkdown(message.getThinkingContent());
                markdownFormatter.setThinkingMarkdown(textThinkingContent, processedThinking);
                // Default to collapsed on bind
                textThinkingContent.setVisibility(View.GONE);
                textThinkingHeaderTitle.setRotation(0f);
                View thinkingHeader = layoutThinkingSection.findViewById(R.id.layout_thinking_header);
                if (thinkingHeader != null) {
                    thinkingHeader.setOnClickListener(v -> {
                        boolean expanded = textThinkingContent.getVisibility() == View.VISIBLE;
                        textThinkingContent.setVisibility(expanded ? View.GONE : View.VISIBLE);
                        // Animate the drawable rotation
                        android.graphics.drawable.Drawable[] drawables = textThinkingHeaderTitle.getCompoundDrawables();
                        android.graphics.drawable.Drawable endDrawable = drawables[2]; // 0:left, 1:top, 2:right, 3:bottom
                        if (endDrawable != null) {
                            android.animation.ObjectAnimator.ofInt(endDrawable, "level", expanded ? 10000 : 0, expanded ? 0 : 10000)
                                .setDuration(200)
                                .start();
                        }
                    });
                }
            }

            if (layoutPlanSteps.getVisibility() == View.VISIBLE) {
                recyclerPlanSteps.setAdapter(new PlanStepsAdapter(message.getPlanSteps()));
                recyclerPlanSteps.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            }

            if (layoutWebSources.getVisibility() == View.VISIBLE) {
                buttonWebSources.setText("Web sources (" + message.getWebSources().size() + ")");
                buttonWebSources.setOnClickListener(v -> showWebSourcesDialog(message.getWebSources()));
                // Link (n) citations in the main message to sources
                applyCitationSpans(textMessage, message.getWebSources());
            }

            boolean hasFileChanges = itemView.findViewById(R.id.layout_proposed_file_changes).getVisibility() == View.VISIBLE;
            if (hasFileChanges) {
                FileActionAdapter fileActionAdapter = new FileActionAdapter(message.getProposedFileChanges(), fileActionDetail -> { if (listener != null) listener.onFileChangeClicked(fileActionDetail); });
                fileChangesContainer.setAdapter(fileActionAdapter);
            }

            // Show file actions Accept/Discard only when not in agent mode
            boolean isAgent = false;
            if (context instanceof EditorActivity) {
                AIAssistant assistant = ((EditorActivity) context).aiAssistantManager != null ? ((EditorActivity) context).aiAssistantManager.getAIAssistant() : null;
                isAgent = assistant != null && assistant.isAgentModeEnabled();
            }
            View layoutFileActions = itemView.findViewById(R.id.layout_file_actions);
            if (hasFileChanges && message.getStatus() == ChatMessage.STATUS_PENDING_APPROVAL && !isAgent) {
                layoutFileActions.setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.button_accept_file_actions).setOnClickListener(v -> { if (listener != null) listener.onAcceptClicked(messagePosition, message); });
                itemView.findViewById(R.id.button_discard_file_actions).setOnClickListener(v -> { if (listener != null) listener.onDiscardClicked(messagePosition, message); });
            } else {
                layoutFileActions.setVisibility(View.GONE);
            }

            boolean anyRunning = false;
            if (message.getPlanSteps() != null) {
                for (ChatMessage.PlanStep ps : message.getPlanSteps()) { if ("running".equals(ps.status)) { anyRunning = true; break; } }
            }
            TextView bottom = textAgentThinking;
            if (anyRunning) {
                bottom.setVisibility(View.VISIBLE);
                AlphaAnimation anim = new AlphaAnimation(0.2f, 1.0f); anim.setDuration(800); anim.setRepeatMode(AlphaAnimation.REVERSE); anim.setRepeatCount(AlphaAnimation.INFINITE); bottom.startAnimation(anim);
            } else {
                bottom.clearAnimation(); bottom.setVisibility(View.GONE);
            }

            boolean isPlan = message.getPlanSteps() != null && !message.getPlanSteps().isEmpty();
            if (isPlan && message.getStatus() == ChatMessage.STATUS_PENDING_APPROVAL) {
                layoutPlanActions.setVisibility(View.VISIBLE);
                buttonAcceptPlan.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPlanAcceptClicked(messagePosition, message);
                    }
                });
                buttonDiscardPlan.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPlanDiscardClicked(messagePosition, message);
                    }
                });
            } else {
                layoutPlanActions.setVisibility(View.GONE);
            }
        }
    }
}
