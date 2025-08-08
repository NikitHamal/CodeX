package com.codex.apk;

import android.content.Context;
import android.graphics.Paint;
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
        private final TextView textStatusLabel;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            textFileName = itemView.findViewById(R.id.text_file_name);
            textChangeLabel = itemView.findViewById(R.id.text_change_label);
            textStatusLabel = itemView.findViewById(R.id.text_status_label);
        }

        void bind(final ChatMessage.FileActionDetail action, final OnFileActionClickListener listener) {
            String path = action.type.equals("renameFile") ? action.newPath : action.path;
            textFileName.setText(path);

            Context context = itemView.getContext();
            int changeColor;
            String changeLabel;

            switch (action.type) {
                case "createFile":
                    changeLabel = "New";
                    changeColor = ContextCompat.getColor(context, R.color.success_container);
                    break;
                case "updateFile":
                case "modifyLines":
                    changeLabel = "Updated";
                    changeColor = ContextCompat.getColor(context, R.color.primary_container);
                    break;
                case "deleteFile":
                    changeLabel = "Deleted";
                    changeColor = ContextCompat.getColor(context, R.color.error_container);
                    break;
                case "renameFile":
                    changeLabel = "Renamed";
                    changeColor = ContextCompat.getColor(context, R.color.warning_container);
                    break;
                default:
                    changeLabel = "Modified";
                    changeColor = ContextCompat.getColor(context, R.color.surface_container);
                    break;
            }

            textChangeLabel.setText(changeLabel);
            if (textChangeLabel.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) textChangeLabel.getBackground().mutate();
                background.setColor(changeColor);
            }

            // Status
            String status = action.stepStatus != null ? action.stepStatus : "pending";
            int statusColor;
            String statusText;
            switch (status) {
                case "running":
                    statusText = "Running";
                    statusColor = ContextCompat.getColor(context, R.color.warning_container);
                    break;
                case "completed":
                    statusText = "Completed";
                    statusColor = ContextCompat.getColor(context, R.color.success_container);
                    break;
                case "failed":
                    statusText = "Failed";
                    statusColor = ContextCompat.getColor(context, R.color.error_container);
                    break;
                default:
                    statusText = "Pending";
                    statusColor = ContextCompat.getColor(context, R.color.surface_container);
                    break;
            }
            textStatusLabel.setText(statusText);
            if (textStatusLabel.getBackground() instanceof GradientDrawable) {
                GradientDrawable bg = (GradientDrawable) textStatusLabel.getBackground().mutate();
                bg.setColor(statusColor);
            }

            // Strike-through when completed
            // if ("completed".equals(status)) {
            //     textFileName.setPaintFlags(textFileName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // } else {
            //     textFileName.setPaintFlags(textFileName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            // }

            itemView.setOnClickListener(v -> listener.onFileActionClicked(action));
        }
    }
}
