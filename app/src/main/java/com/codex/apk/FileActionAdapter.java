package com.codex.apk;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class FileActionAdapter extends RecyclerView.Adapter<FileActionAdapter.ViewHolder> {

    private final List<ChatMessage.FileActionDetail> fileActions;
    private final OnFileActionClickListener listener;

    public interface OnFileActionClickListener {
        void onFileActionClicked(ChatMessage.FileActionDetail fileActionDetail);
    }

    public FileActionAdapter(List<ChatMessage.FileActionDetail> fileActions, OnFileActionClickListener listener) {
        this.fileActions = fileActions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_file_change, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage.FileActionDetail action = fileActions.get(position);
        holder.bind(action, listener);
    }

    @Override
    public int getItemCount() {
        return fileActions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView textFileName;
        private final TextView textChangeLabel;
        private final TextView textAddedBadge;
        private final TextView textRemovedBadge;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            textFileName = itemView.findViewById(R.id.text_file_name);
            textChangeLabel = itemView.findViewById(R.id.text_change_label);
            textAddedBadge = itemView.findViewById(R.id.text_added_badge);
            textRemovedBadge = itemView.findViewById(R.id.text_removed_badge);
        }

        void bind(final ChatMessage.FileActionDetail action, final OnFileActionClickListener listener) {
            String path = action.type.equals("renameFile") ? action.newPath : action.path;
            textFileName.setText(path);

            Context context = itemView.getContext();
            int changeColor;
            int changeTextColor;
            String changeLabel;

            switch (action.type) {
                case "createFile":
                    changeLabel = "new";
                    changeColor = ContextCompat.getColor(context, R.color.success_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_success_container);
                    break;
                case "updateFile":
                case "modifyLines":
                    changeLabel = "updated";
                    changeColor = ContextCompat.getColor(context, R.color.primary_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_primary_container);
                    break;
                case "deleteFile":
                    changeLabel = "deleted";
                    changeColor = ContextCompat.getColor(context, R.color.error_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_error_container);
                    break;
                case "renameFile":
                    changeLabel = "renamed";
                    changeColor = ContextCompat.getColor(context, R.color.warning_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_warning_container);
                    break;
                default:
                    changeLabel = "modified";
                    changeColor = ContextCompat.getColor(context, R.color.surface_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_surface);
                    break;
            }

            textChangeLabel.setText(changeLabel);
            if (textChangeLabel.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) textChangeLabel.getBackground().mutate();
                background.setColor(changeColor);
            }
            textChangeLabel.setTextColor(changeTextColor);

            // No status label in the new UI

            // Diff badges (+ added / - removed lines)
            int added = 0;
            int removed = 0;

            if ("modifyLines".equals(action.type)) {
                added = (action.insertLines != null) ? action.insertLines.size() : 0;
                removed = Math.max(0, action.deleteCount);
            } else if (action.diffPatch != null && !action.diffPatch.isEmpty()) {
                int[] counts = DiffUtils.countAddRemove(action.diffPatch);
                added = counts[0];
                removed = counts[1];
            }

            // Configure + badge
            if (added > 0) {
                textAddedBadge.setVisibility(View.VISIBLE);
                textAddedBadge.setText("+" + added);
                // Plain colored text; no pill background
            } else {
                textAddedBadge.setVisibility(View.GONE);
            }

            // Configure - badge
            if (removed > 0) {
                textRemovedBadge.setVisibility(View.VISIBLE);
                textRemovedBadge.setText("-" + removed);
                // Plain colored text; no pill background
            } else {
                textRemovedBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onFileActionClicked(action));
        }

        // Removed duplicate unified diff parser; using DiffUtils.countAddRemove
    }
}
