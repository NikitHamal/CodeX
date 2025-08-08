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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.animation.AlphaAnimation;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.app.AlertDialog;
import com.codex.apk.ai.WebSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

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
            }
            private String capitalize(String x) { return x.length() > 0 ? Character.toUpperCase(x.charAt(0)) + x.substring(1) : x; }
        }
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage; MaterialCardView cardMessage; private final Context context;
        UserMessageViewHolder(View itemView, Context context) { super(itemView); this.context = context; textMessage = itemView.findViewById(R.id.text_message_content); cardMessage = itemView.findViewById(R.id.user_message_card_view); }
        void bind(ChatMessage message) { textMessage.setText(message.getContent()); }
    }

    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage; TextView textAiModelName; RecyclerView fileChangesContainer; LinearLayout layoutThinkingSection; TextView textThinkingContent; ImageView iconThinkingExpand; LinearLayout layoutWebSources; TextView buttonWebSources; LinearLayout layoutTypingIndicator; TextView textTypingIndicator; LinearLayout layoutPlanSteps; RecyclerView recyclerPlanSteps; TextView textAgentThinking;
        LinearLayout layoutPlanActions; MaterialButton buttonAcceptPlan; MaterialButton buttonDiscardPlan;
        private final OnAiActionInteractionListener listener; private final Context context; private MarkdownFormatter markdownFormatter;
        AiMessageViewHolder(View itemView, OnAiActionInteractionListener listener) {
            super(itemView); this.listener = listener; this.context = itemView.getContext();
            textMessage = itemView.findViewById(R.id.text_message);
            textAiModelName = itemView.findViewById(R.id.text_ai_model_name);
            fileChangesContainer = itemView.findViewById(R.id.file_changes_container);
            layoutThinkingSection = itemView.findViewById(R.id.layout_thinking_section);
            textThinkingContent = itemView.findViewById(R.id.text_thinking_content);
            iconThinkingExpand = itemView.findViewById(R.id.icon_thinking_expand);
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
            View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_web_sources, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_web_sources);
            WebSourcesAdapter adapter = new WebSourcesAdapter(webSources);
            recyclerView.setAdapter(adapter);
            BottomSheetDialog dialog = new BottomSheetDialog(context); dialog.setContentView(dialogView); dialog.show();
        }
        
        private void showRawApiResponseDialog(ChatMessage message) {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_raw_api_response, null);
            TextView textRawResponse = dialogView.findViewById(R.id.text_raw_response); MaterialButton buttonCopy = dialogView.findViewById(R.id.button_copy); MaterialButton buttonClose = dialogView.findViewById(R.id.button_close);
            String rawResponse = message.getRawApiResponse(); textRawResponse.setText(rawResponse != null && !rawResponse.isEmpty() ? rawResponse : "No raw API response available.");
            AlertDialog.Builder builder = new AlertDialog.Builder(context); builder.setView(dialogView); final AlertDialog dialog = builder.create();
            buttonCopy.setOnClickListener(v -> { android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE); if (clipboard != null) { android.content.ClipData clip = android.content.ClipData.newPlainText("Raw API Response", rawResponse != null ? rawResponse : ""); clipboard.setPrimaryClip(clip); android.widget.Toast.makeText(context, "Raw response copied", android.widget.Toast.LENGTH_SHORT).show(); } });
            buttonClose.setOnClickListener(v -> dialog.dismiss()); dialog.show();
        }

        void bind(ChatMessage message, int messagePosition) {
            boolean isTyping = message.getContent() != null && message.getContent().equals(context.getString(R.string.ai_is_thinking));
            itemView.setOnLongClickListener(v -> { showRawApiResponseDialog(message); return true; });

            layoutTypingIndicator.setVisibility(isTyping ? View.VISIBLE : View.GONE);
            if (isTyping) {
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
            if (content != null && !content.isEmpty()) {
                String processedContent = markdownFormatter.preprocessMarkdown(content);
                markdownFormatter.setMarkdown(textMessage, processedContent);
            } else {
                textMessage.setText("");
            }

            if (layoutThinkingSection.getVisibility() == View.VISIBLE) {
                String processedThinking = markdownFormatter.preprocessMarkdown(message.getThinkingContent());
                markdownFormatter.setThinkingMarkdown(textThinkingContent, processedThinking);
                textThinkingContent.setVisibility(View.GONE);
                iconThinkingExpand.setRotation(0f);
                View thinkingHeader = layoutThinkingSection.findViewById(R.id.layout_thinking_header);
                if (thinkingHeader != null) {
                    thinkingHeader.setOnClickListener(v -> {
                        boolean expanded = textThinkingContent.getVisibility() == View.VISIBLE;
                        textThinkingContent.setVisibility(expanded ? View.GONE : View.VISIBLE);
                        iconThinkingExpand.animate().rotation(expanded ? 0f : 180f).setDuration(200).start();
                    });
                }
            }

            if (layoutPlanSteps.getVisibility() == View.VISIBLE) {
                recyclerPlanSteps.setAdapter(new PlanStepsAdapter(message.getPlanSteps()));
            }

            if (layoutWebSources.getVisibility() == View.VISIBLE) {
                buttonWebSources.setText("Web sources (" + message.getWebSources().size() + ")");
                buttonWebSources.setOnClickListener(v -> showWebSourcesDialog(message.getWebSources()));
            }

            if (itemView.findViewById(R.id.layout_proposed_file_changes).getVisibility() == View.VISIBLE) {
                FileActionAdapter fileActionAdapter = new FileActionAdapter(message.getProposedFileChanges(), fileActionDetail -> { if (listener != null) listener.onFileChangeClicked(fileActionDetail); });
                fileChangesContainer.setAdapter(fileActionAdapter);
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
