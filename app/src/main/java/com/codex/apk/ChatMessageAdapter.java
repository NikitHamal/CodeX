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
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.app.AlertDialog;
import com.codex.apk.ai.WebSource;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages;
    private final Context context;
    private OnAiActionInteractionListener aiActionInteractionListener;

    // View types
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;

    /**
     * Interface for handling interactions with AI action messages (Accept/Discard/Reapply, file clicks).
     */
    public interface OnAiActionInteractionListener {
        void onAcceptClicked(int messagePosition, ChatMessage message);
        void onDiscardClicked(int messagePosition, ChatMessage message);
        void onReapplyClicked(int messagePosition, ChatMessage message);
        void onFileChangeClicked(ChatMessage.FileActionDetail fileActionDetail);
    }

    public ChatMessageAdapter(Context context, List<ChatMessage> messages) {
        this.context = context;
        this.messages = messages;
    }

    public void setOnAiActionInteractionListener(OnAiActionInteractionListener listener) {
        this.aiActionInteractionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSender();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view, parent.getContext());
        } else { // VIEW_TYPE_AI
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

        // Animate newly appearing items for a smoother chat experience
        if (position > lastAnimatedPosition) {
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(24f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
            lastAnimatedPosition = position;
        }
    }

    // Keeps track of the last position that was animated to avoid re-animating while scrolling
    private int lastAnimatedPosition = -1;

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * ViewHolder for user messages.
     */
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        MaterialCardView cardMessage;
        private final Context context;

        UserMessageViewHolder(View itemView, Context context) {
            super(itemView);
            this.context = context;
            textMessage = itemView.findViewById(R.id.text_message_content);
            cardMessage = itemView.findViewById(R.id.user_message_card_view);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
        }
    }

    /**
     * ViewHolder for AI messages.
     */
    static class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textAiModelName;
        LinearLayout layoutActionButtons;
        MaterialButton buttonAccept;
        MaterialButton buttonDiscard;
        MaterialButton buttonReapply;
        LinearLayout layoutActionSummaries; // To display action summaries (Actions Performed)
        RecyclerView fileChangesContainer; // Container for proposed file changes
        
        // New fields for thinking, web sources and typing indicator
        LinearLayout layoutThinkingSection;
        TextView textThinkingContent;
        ImageView iconThinkingExpand;
        LinearLayout layoutWebSources;
        TextView buttonWebSources;
        LinearLayout layoutTypingIndicator;
        TextView textTypingIndicator;
        
        private final OnAiActionInteractionListener listener;
        private final Context context;
        private MarkdownFormatter markdownFormatter;

        AiMessageViewHolder(View itemView, OnAiActionInteractionListener listener) {
            super(itemView);
            this.listener = listener;
            this.context = itemView.getContext();

            textMessage = itemView.findViewById(R.id.text_message);
            textAiModelName = itemView.findViewById(R.id.text_ai_model_name);
            layoutActionButtons = itemView.findViewById(R.id.layout_action_buttons);
            buttonAccept = itemView.findViewById(R.id.button_accept);
            buttonDiscard = itemView.findViewById(R.id.button_discard);
            buttonReapply = itemView.findViewById(R.id.button_reapply);
            layoutActionSummaries = itemView.findViewById(R.id.layout_action_summaries);
            fileChangesContainer = itemView.findViewById(R.id.file_changes_container);
            
            // Initialize new thinking, web sources and typing indicator views
            layoutThinkingSection = itemView.findViewById(R.id.layout_thinking_section);
            textThinkingContent = itemView.findViewById(R.id.text_thinking_content);
            iconThinkingExpand = itemView.findViewById(R.id.icon_thinking_expand);
            layoutWebSources = itemView.findViewById(R.id.layout_web_sources);
            buttonWebSources = itemView.findViewById(R.id.button_web_sources);
            layoutTypingIndicator = itemView.findViewById(R.id.layout_typing_indicator);
            textTypingIndicator = itemView.findViewById(R.id.text_typing_indicator);
            
            // Initialize markdown formatter
            markdownFormatter = MarkdownFormatter.getInstance(context);
            
            // Add long click listener for raw API response
            itemView.setOnLongClickListener(v -> {
                // We'll set this up in the bind method
                return true;
            });
        }
        
        private void showWebSourcesDialog(List<WebSource> webSources) {
            // Create and show web sources dialog
            View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_web_sources, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_web_sources);
            
            WebSourcesAdapter adapter = new WebSourcesAdapter(webSources);
            recyclerView.setAdapter(adapter);
            
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            dialog.setContentView(dialogView);
            dialog.show();
        }
        
        private void showRawApiResponseDialog(ChatMessage message) {
            // Inflate the custom layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_raw_api_response, null);

            // Find views in the custom layout
            TextView textRawResponse = dialogView.findViewById(R.id.text_raw_response);
            MaterialButton buttonCopy = dialogView.findViewById(R.id.button_copy);
            MaterialButton buttonClose = dialogView.findViewById(R.id.button_close);

            // Set the raw response text
            String rawResponse = message.getRawApiResponse();
            if (rawResponse != null && !rawResponse.isEmpty()) {
                textRawResponse.setText(rawResponse);
            } else {
                textRawResponse.setText("No raw API response available.");
            }

            // Create the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            final AlertDialog dialog = builder.create();

            // Set up copy button
            buttonCopy.setOnClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Raw API Response", rawResponse != null ? rawResponse : "");
                    clipboard.setPrimaryClip(clip);
                    android.widget.Toast.makeText(context, "Raw response copied", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            // Set up close button
            buttonClose.setOnClickListener(v -> dialog.dismiss());
            
            // Show the dialog
            dialog.show();
        }

        void bind(ChatMessage message, int messagePosition) {
            String aiThinkingString = context.getString(R.string.ai_is_thinking);
            boolean isTyping = message.getContent() != null && message.getContent().equals(aiThinkingString);
            
            // Set up long click listener for raw API response
            itemView.setOnLongClickListener(v -> {
                showRawApiResponseDialog(message);
                return true;
            });

            // Toggle visibility based on whether the AI is typing
            layoutTypingIndicator.setVisibility(isTyping ? View.VISIBLE : View.GONE);
            textMessage.setVisibility(isTyping ? View.GONE : View.VISIBLE);
            layoutThinkingSection.setVisibility(isTyping ? View.GONE : (message.getThinkingContent() != null && !message.getThinkingContent().trim().isEmpty() ? View.VISIBLE : View.GONE));
            layoutWebSources.setVisibility(isTyping ? View.GONE : (message.getWebSources() != null && !message.getWebSources().isEmpty() ? View.VISIBLE : View.GONE));
            itemView.findViewById(R.id.layout_proposed_file_changes).setVisibility(isTyping ? View.GONE : (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty() ? View.VISIBLE : View.GONE));
            layoutActionSummaries.setVisibility(isTyping ? View.GONE : (message.getActionSummaries() != null && !message.getActionSummaries().isEmpty() ? View.VISIBLE : View.GONE));
            layoutActionButtons.setVisibility(isTyping ? View.GONE : (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty() ? View.VISIBLE : View.GONE));

            if (isTyping) {
                // If AI is typing, we just show the indicator and the model name.
                textAiModelName.setText(message.getAiModelName());
                textTypingIndicator.setText(message.getContent()); // You can update this text dynamically if needed
            } else {
                // If it's a regular message, bind all data as before.
                textAiModelName.setText(message.getAiModelName());

                // Handle main content
                String content = message.getContent();
                if (content != null && !content.isEmpty()) {
                    String processedContent = markdownFormatter.preprocessMarkdown(content);
                    markdownFormatter.setMarkdown(textMessage, processedContent);
                } else {
                    textMessage.setText("");
                }

                // Handle thinking content
                if (layoutThinkingSection.getVisibility() == View.VISIBLE) {
                    String processedThinking = markdownFormatter.preprocessMarkdown(message.getThinkingContent());
                    markdownFormatter.setThinkingMarkdown(textThinkingContent, processedThinking);
                    textThinkingContent.setVisibility(View.GONE);
                    iconThinkingExpand.setRotation(0f);
                    View thinkingHeader = layoutThinkingSection.findViewById(R.id.layout_thinking_header);
                    if (thinkingHeader != null) {
                        thinkingHeader.setOnClickListener(v -> {
                            boolean currentlyExpanded = textThinkingContent.getVisibility() == View.VISIBLE;
                            textThinkingContent.setVisibility(currentlyExpanded ? View.GONE : View.VISIBLE);
                            iconThinkingExpand.animate().rotation(currentlyExpanded ? 0f : 180f).setDuration(200).start();
                        });
                    }
                }

                // Handle web sources
                if (layoutWebSources.getVisibility() == View.VISIBLE) {
                    buttonWebSources.setText("Web sources (" + message.getWebSources().size() + ")");
                    buttonWebSources.setOnClickListener(v -> showWebSourcesDialog(message.getWebSources()));
                }

                // Handle proposed file changes
                if (itemView.findViewById(R.id.layout_proposed_file_changes).getVisibility() == View.VISIBLE) {
                    FileActionAdapter fileActionAdapter = new FileActionAdapter(message.getProposedFileChanges(), fileActionDetail -> {
                        if (listener != null) listener.onFileChangeClicked(fileActionDetail);
                    });
                    fileChangesContainer.setAdapter(fileActionAdapter);
                }

                // Handle action summaries
                if (layoutActionSummaries.getVisibility() == View.VISIBLE) {
                    layoutActionSummaries.removeAllViews();
                    TextView header = new TextView(context);
                    header.setText("Actions Performed:");
                    header.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                    header.setTextSize(12);
                    layoutActionSummaries.addView(header);
                    for(String summary : message.getActionSummaries()) {
                        TextView summaryView = new TextView(context);
                        summaryView.setText("• " + summary);
                        summaryView.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                        summaryView.setTextSize(12);
                        layoutActionSummaries.addView(summaryView);
                    }
                }

                // Handle action buttons
                if (layoutActionButtons.getVisibility() == View.VISIBLE) {
                    buttonAccept.setVisibility(message.getStatus() == ChatMessage.STATUS_PENDING_APPROVAL ? View.VISIBLE : View.GONE);
                    buttonDiscard.setVisibility(message.getStatus() == ChatMessage.STATUS_PENDING_APPROVAL ? View.VISIBLE : View.GONE);
                    buttonReapply.setVisibility(message.getStatus() == ChatMessage.STATUS_DISCARDED ? View.VISIBLE : View.GONE);

                    buttonAccept.setOnClickListener(v -> { if (listener != null) listener.onAcceptClicked(messagePosition, message); });
                    buttonDiscard.setOnClickListener(v -> { if (listener != null) listener.onDiscardClicked(messagePosition, message); });
                    buttonReapply.setOnClickListener(v -> { if (listener != null) listener.onReapplyClicked(messagePosition, message); });
                }
            }
        }
    }
}
