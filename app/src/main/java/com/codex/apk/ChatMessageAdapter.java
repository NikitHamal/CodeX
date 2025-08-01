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
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
        LinearLayout layoutSuggestions; // To display suggestions
        RecyclerView fileChangesContainer; // Container for proposed file changes
        
        // New fields for thinking and web sources
        LinearLayout layoutThinkingSection;
        TextView textThinkingContent;
        ImageView iconThinkingExpand;
        LinearLayout layoutWebSources;
        TextView buttonWebSources;
        
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
            layoutSuggestions = itemView.findViewById(R.id.layout_suggestions);
            fileChangesContainer = itemView.findViewById(R.id.file_changes_container);
            
            // Initialize new thinking and web sources views
            layoutThinkingSection = itemView.findViewById(R.id.layout_thinking_section);
            textThinkingContent = itemView.findViewById(R.id.text_thinking_content);
            iconThinkingExpand = itemView.findViewById(R.id.icon_thinking_expand);
            layoutWebSources = itemView.findViewById(R.id.layout_web_sources);
            buttonWebSources = itemView.findViewById(R.id.button_web_sources);
            
            // Initialize markdown formatter
            markdownFormatter = MarkdownFormatter.getInstance(context);
        }
        
        private void showWebSourcesDialog(List<ChatMessage.WebSource> webSources) {
            // Create and show web sources dialog
            View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_web_sources, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_web_sources);
            
            // Convert ChatMessage.WebSource to AIAssistant.WebSource for adapter compatibility
            java.util.List<AIAssistant.WebSource> aiWebSources = new java.util.ArrayList<>();
            for (ChatMessage.WebSource source : webSources) {
                aiWebSources.add(new AIAssistant.WebSource(
                    source.getUrl(), source.getTitle(), source.getSnippet(), source.getFavicon()));
            }
            
            WebSourcesAdapter adapter = new WebSourcesAdapter(aiWebSources);
            recyclerView.setAdapter(adapter);
            
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            dialog.setContentView(dialogView);
            dialog.show();
        }

        void bind(ChatMessage message, int messagePosition) {
            // Card background defined via XML for dynamic theming.

            textAiModelName.setText(message.getAiModelName());

            // Handle indexing progress messages and apply markdown formatting
            String content = message.getContent();
            if (content != null && !content.isEmpty()) {
                String processedContent = markdownFormatter.preprocessMarkdown(content);
                markdownFormatter.setMarkdown(textMessage, processedContent);
            } else {
                textMessage.setText("");
            }
            
            // Handle thinking content
            if (message.getThinkingContent() != null && !message.getThinkingContent().trim().isEmpty()) {
                layoutThinkingSection.setVisibility(View.VISIBLE);
                String processedThinking = markdownFormatter.preprocessMarkdown(message.getThinkingContent());
                markdownFormatter.setThinkingMarkdown(textThinkingContent, processedThinking);
                
                // Set up thinking section collapse/expand (initially collapsed)
                textThinkingContent.setVisibility(View.GONE);
                iconThinkingExpand.setRotation(0f);
                
                View thinkingHeader = layoutThinkingSection.findViewById(R.id.layout_thinking_header);
                if (thinkingHeader != null) {
                    thinkingHeader.setOnClickListener(v -> {
                        boolean currentlyExpanded = textThinkingContent.getVisibility() == View.VISIBLE;
                        if (currentlyExpanded) {
                            textThinkingContent.setVisibility(View.GONE);
                            iconThinkingExpand.animate().rotation(0f).setDuration(200).start();
                        } else {
                            textThinkingContent.setVisibility(View.VISIBLE);
                            iconThinkingExpand.animate().rotation(180f).setDuration(200).start();
                        }
                    });
                }
            } else {
                layoutThinkingSection.setVisibility(View.GONE);
            }
            
            // Handle web sources
            if (message.getWebSources() != null && !message.getWebSources().isEmpty()) {
                layoutWebSources.setVisibility(View.VISIBLE);
                buttonWebSources.setText("Web sources (" + message.getWebSources().size() + ")");
                buttonWebSources.setOnClickListener(v -> showWebSourcesDialog(message.getWebSources()));
            } else {
                layoutWebSources.setVisibility(View.GONE);
            }

            // Display proposed file changes
            LinearLayout layoutProposedFileChanges = itemView.findViewById(R.id.layout_proposed_file_changes);
            if (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty()) {
                android.util.Log.d("ChatMessageAdapter", "Binding " + message.getProposedFileChanges().size() + " file changes");
                layoutProposedFileChanges.setVisibility(View.VISIBLE);
                FileActionAdapter fileActionAdapter = new FileActionAdapter(message.getProposedFileChanges(), fileActionDetail -> {
                    if (listener != null) {
                        listener.onFileChangeClicked(fileActionDetail);
                    }
                });
                fileChangesContainer.setAdapter(fileActionAdapter);
            } else {
                android.util.Log.d("ChatMessageAdapter", "No file changes to bind");
                layoutProposedFileChanges.setVisibility(View.GONE);
            }

            // Display action summaries (Actions Performed)
            if (message.getActionSummaries() != null && !message.getActionSummaries().isEmpty()) {
                layoutActionSummaries.setVisibility(View.VISIBLE);
                // The RecyclerView is now responsible for displaying the file changes.
                // The summaries can be simple text or a more complex layout if needed.
                // For now, we'll just show a simple text summary.
                layoutActionSummaries.removeAllViews(); // Clear previous views
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

            } else {
                layoutActionSummaries.setVisibility(View.GONE);
            }

                // Display suggestions if available
                if (message.getSuggestions() != null && !message.getSuggestions().isEmpty()) {
                    layoutSuggestions.setVisibility(View.VISIBLE);
                    layoutSuggestions.removeAllViews(); // Clear previous views

                    TextView header = new TextView(context);
                    header.setText("Suggestions:");
                    header.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                    header.setTextSize(12); // Use 12sp as defined in XML
                    header.setPadding(0, (int) context.getResources().getDimension(R.dimen.padding_small), 0, (int) context.getResources().getDimension(R.dimen.padding_extra_small));
                    layoutSuggestions.addView(header);

                    for (String suggestion : message.getSuggestions()) {
                        TextView tv = new TextView(context);
                        tv.setText("• " + suggestion);
                        tv.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                        tv.setTextSize(12); // Use 12sp as defined in XML
                        layoutSuggestions.addView(tv);
                    }
                } else {
                    layoutSuggestions.setVisibility(View.GONE);
                }

                // Handle action buttons visibility based on message status
                if (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty()) {
                    layoutActionButtons.setVisibility(View.VISIBLE);
                    if (message.getStatus() == ChatMessage.STATUS_PENDING_APPROVAL) {
                        buttonAccept.setVisibility(View.VISIBLE);
                        buttonDiscard.setVisibility(View.VISIBLE);
                        buttonReapply.setVisibility(View.GONE);
                    } else if (message.getStatus() == ChatMessage.STATUS_ACCEPTED) {
                        buttonAccept.setVisibility(View.GONE);
                        buttonDiscard.setVisibility(View.GONE);
                        buttonReapply.setVisibility(View.GONE);
                    } else if (message.getStatus() == ChatMessage.STATUS_DISCARDED) {
                        buttonAccept.setVisibility(View.GONE);
                        buttonDiscard.setVisibility(View.GONE);
                        buttonReapply.setVisibility(View.VISIBLE);
                    } else {
                        layoutActionButtons.setVisibility(View.GONE);
                    }

                    // Set click listeners for buttons
                    buttonAccept.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAcceptClicked(messagePosition, message);
                        }
                    });

                    buttonDiscard.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDiscardClicked(messagePosition, message);
                        }
                    });

                    buttonReapply.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onReapplyClicked(messagePosition, message);
                        }
                    });
                } else {
                    layoutActionButtons.setVisibility(View.GONE);
                }
            }
        }
    }
