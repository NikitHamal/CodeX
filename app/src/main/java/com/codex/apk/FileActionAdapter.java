package com.codex.apk;

import android.content.Context;
import android.graphics.Color;
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

        ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            textFileName = itemView.findViewById(R.id.text_file_name);
            textChangeLabel = itemView.findViewById(R.id.text_change_label);
        }

        void bind(final ChatMessage.FileActionDetail action, final OnFileActionClickListener listener) {
            String path = action.type.equals("renameFile") ? action.newPath : action.path;
            textFileName.setText(path);

            Context context = itemView.getContext();
            int color;
            String label;

            switch (action.type) {
                case "createFile":
                    label = "New";
                    color = ContextCompat.getColor(context, R.color.success_container);
                    break;
                case "updateFile":
                case "modifyLines":
                    label = "Updated";
                    color = ContextCompat.getColor(context, R.color.primary_container);
                    break;
                case "deleteFile":
                    label = "Deleted";
                    color = ContextCompat.getColor(context, R.color.error_container);
                    break;
                case "renameFile":
                    label = "Renamed";
                    color = ContextCompat.getColor(context, R.color.warning_container);
                    break;
                default:
                    label = "Modified";
                    color = ContextCompat.getColor(context, R.color.surface_container);
                    break;
            }

            textChangeLabel.setText(label);

            // Set background color with rounded corners
            if (textChangeLabel.getBackground() instanceof GradientDrawable) {
                GradientDrawable background = (GradientDrawable) textChangeLabel.getBackground().mutate();
                background.setColor(color);
            }


            itemView.setOnClickListener(v -> listener.onFileActionClicked(action));
        }
    }
}
