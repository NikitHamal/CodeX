package com.codex.apk;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

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
        MaterialCardView cardMessage;
        LinearLayout fileChangesContainer; // Container for proposed file changes

        // New UI elements for indexing progress
        ProgressBar progressBarIndexing;
        TextView textIndexingStatus;
        LinearLayout layoutIndexingProgress; // Container for progress bar and text

        private final OnAiActionInteractionListener listener;
        private final Context context;

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
            cardMessage = itemView.findViewById(R.id.card_message);
            fileChangesContainer = itemView.findViewById(R.id.file_changes_container);

            // Initialize new UI elements
            progressBarIndexing = itemView.findViewById(R.id.progress_bar_indexing);
            textIndexingStatus = itemView.findViewById(R.id.text_indexing_status);
            layoutIndexingProgress = itemView.findViewById(R.id.layout_indexing_progress);
        }

        void bind(ChatMessage message, int messagePosition) {
            // Card background defined via XML for dynamic theming.

            textAiModelName.setText(message.getAiModelName());

            // Handle indexing progress messages
            if (message.getStatus() == ChatMessage.STATUS_INDEXING_PROGRESS) {
                layoutIndexingProgress.setVisibility(View.VISIBLE);
                textMessage.setVisibility(View.GONE); // Hide main message content
                layoutActionButtons.setVisibility(View.GONE);
                layoutActionSummaries.setVisibility(View.GONE);
                layoutSuggestions.setVisibility(View.GONE);
                fileChangesContainer.setVisibility(View.GONE); // Hide file changes section

                progressBarIndexing.setMax(message.getIndexingProgressTotal());
                progressBarIndexing.setProgress(message.getIndexingProgressCurrent());

                String statusText = "Indexing " + message.getIndexingProgressCurrent() + "/" +
                        message.getIndexingProgressTotal();
                if (message.getIndexingCurrentFile() != null && !message.getIndexingCurrentFile().isEmpty()) {
                    statusText += " (" + message.getIndexingCurrentFile() + ")";
                }
                textIndexingStatus.setText(statusText);

            } else {
                layoutIndexingProgress.setVisibility(View.GONE);
                textMessage.setVisibility(View.VISIBLE); // Show main message content
                textMessage.setText(message.getContent());

                // Display proposed file changes
                if (message.getProposedFileChanges() != null && !message.getProposedFileChanges().isEmpty()) {
                    fileChangesContainer.setVisibility(View.VISIBLE);
                    fileChangesContainer.removeAllViews(); // Clear previous views

                    // Add "Proposed File Changes:" header
                    TextView header = new TextView(context);
                    header.setText("Proposed File Changes:");
                    header.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                    header.setTextSize(12); // Use 12sp as defined in XML
                    header.setPadding(0, (int) context.getResources().getDimension(R.dimen.padding_small), 0, (int) context.getResources().getDimension(R.dimen.padding_extra_small));
                    fileChangesContainer.addView(header);

                    for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                        View fileChangeItemView = LayoutInflater.from(context).inflate(R.layout.item_ai_file_change, fileChangesContainer, false);
                        TextView fileNameTextView = fileChangeItemView.findViewById(R.id.text_file_name);
                        TextView fileChangeLabel = fileChangeItemView.findViewById(R.id.text_change_label);
                        MaterialCardView fileChangeCardView = (MaterialCardView) fileChangeItemView; // The root of item_ai_file_change.xml is a MaterialCardView

                        fileNameTextView.setText(detail.path != null ? detail.path : detail.oldPath + " to " + detail.newPath);

                        String labelText = "";
                        int labelBgColorResId;
                        int strokeColorResId;

                        switch (detail.type) {
                            case "createFile":
                                labelText = "New";
                                labelBgColorResId = R.color.success_container;
                                strokeColorResId = R.color.success;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "deleteFile":
                                labelText = "Deleted";
                                labelBgColorResId = R.color.error_container;
                                strokeColorResId = R.color.error;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "modifyLines":
                            case "updateFile":
                                labelText = "Updated";
                                labelBgColorResId = R.color.primary_container;
                                strokeColorResId = R.color.primary;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "renameFile":
                                labelText = "Renamed";
                                labelBgColorResId = R.color.warning_container;
                                strokeColorResId = R.color.warning;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.warning_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            default:
                                labelText = ""; // No label for unknown types
                                labelBgColorResId = android.R.color.transparent;
                                strokeColorResId = R.color.outline;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                        }

                        if (!labelText.isEmpty()) {
                            fileChangeLabel.setText(labelText);
                            fileChangeLabel.setVisibility(View.VISIBLE);
                            // Set background color for the label using GradientDrawable
                            GradientDrawable labelBackground = (GradientDrawable) fileChangeLabel.getBackground();
                            if (labelBackground == null) {
                                labelBackground = new GradientDrawable();
                                labelBackground.setShape(GradientDrawable.RECTANGLE);
                                labelBackground.setCornerRadius(context.getResources().getDimension(R.dimen.corner_radius_small));
                                fileChangeLabel.setBackground(labelBackground);
                            }
                            labelBackground.setColor(ContextCompat.getColor(context, labelBgColorResId));
                            fileChangeLabel.setTextColor(ContextCompat.getColor(context, R.color.white)); // Ensure text color is white for labels
                        } else {
                            fileChangeLabel.setVisibility(View.GONE);
                        }

                        fileChangeItemView.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onFileChangeClicked(detail);
                            }
                        });
                        fileChangesContainer.addView(fileChangeItemView);
                    }
                } else {
                    fileChangesContainer.setVisibility(View.GONE);
                }

                // Display action summaries (Actions Performed)
                if (message.getActionSummaries() != null && !message.getActionSummaries().isEmpty()) {
                    layoutActionSummaries.setVisibility(View.VISIBLE);
                    layoutActionSummaries.removeAllViews(); // Clear previous views

                    TextView header = new TextView(context);
                    header.setText("Actions Performed:");
                    header.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                    header.setTextSize(12); // Use 12sp as defined in XML
                    header.setPadding(0, (int) context.getResources().getDimension(R.dimen.padding_small), 0, (int) context.getResources().getDimension(R.dimen.padding_extra_small));
                    layoutActionSummaries.addView(header);

                    for (ChatMessage.FileActionDetail detail : message.getProposedFileChanges()) {
                        View fileChangeItemView = LayoutInflater.from(context).inflate(R.layout.item_ai_file_change, layoutActionSummaries, false);
                        TextView fileNameTextView = fileChangeItemView.findViewById(R.id.text_file_name);
                        TextView fileChangeLabel = fileChangeItemView.findViewById(R.id.text_change_label);
                        MaterialCardView fileChangeCardView = (MaterialCardView) fileChangeItemView;

                        fileNameTextView.setText(detail.path != null ? detail.path : detail.oldPath + " to " + detail.newPath);

                        String labelText = "";
                        int labelBgColorResId;
                        int strokeColorResId;

                        switch (detail.type) {
                            case "createFile":
                                labelText = "New";
                                labelBgColorResId = R.color.success_container;
                                strokeColorResId = R.color.success;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "deleteFile":
                                labelText = "Deleted";
                                labelBgColorResId = R.color.error_container;
                                strokeColorResId = R.color.error;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "modifyLines":
                            case "updateFile":
                                labelText = "Updated";
                                labelBgColorResId = R.color.primary_container;
                                strokeColorResId = R.color.primary;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            case "renameFile":
                                labelText = "Renamed";
                                labelBgColorResId = R.color.warning_container;
                                strokeColorResId = R.color.warning;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.warning_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                            default:
                                labelText = ""; // No label for unknown types
                                labelBgColorResId = android.R.color.transparent;
                                strokeColorResId = R.color.outline;
                                fileChangeCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container));
                                fileChangeCardView.setStrokeColor(ContextCompat.getColor(context, strokeColorResId));
                                break;
                        }

                        if (!labelText.isEmpty()) {
                            fileChangeLabel.setText(labelText);
                            fileChangeLabel.setVisibility(View.VISIBLE);
                            GradientDrawable labelBackground = (GradientDrawable) fileChangeLabel.getBackground();
                            if (labelBackground == null) {
                                labelBackground = new GradientDrawable();
                                labelBackground.setShape(GradientDrawable.RECTANGLE);
                                labelBackground.setCornerRadius(context.getResources().getDimension(R.dimen.corner_radius_small));
                                fileChangeLabel.setBackground(labelBackground);
                            }
                            labelBackground.setColor(ContextCompat.getColor(context, labelBgColorResId));
                            fileChangeLabel.setTextColor(ContextCompat.getColor(context, R.color.white));
                        } else {
                            fileChangeLabel.setVisibility(View.GONE);
                        }

                        fileChangeItemView.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onFileChangeClicked(detail);
                            }
                        });
                        layoutActionSummaries.addView(fileChangeItemView);
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
}
