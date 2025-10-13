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
import java.util.ArrayList;
import java.util.LinkedHashMap;

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
        List<ChatMessage.FileActionDetail> displayActions = getDisplayActions();
        ChatMessage.FileActionDetail action = displayActions.get(position);
        int[] counts = computeAggregatedCountsForDisplayAction(action);
        holder.bind(action, listener, counts[0], counts[1]);
    }

    @Override
    public int getItemCount() {
        return getDisplayActions().size();
    }

    private List<ChatMessage.FileActionDetail> getDisplayActions() {
        List<ChatMessage.FileActionDetail> input = this.fileActions;
        if (input == null || input.isEmpty()) return new ArrayList<>();
        // Iterate from end to keep the latest action for each effective path
        LinkedHashMap<String, ChatMessage.FileActionDetail> map = new LinkedHashMap<>();
        for (int i = input.size() - 1; i >= 0; i--) {
            ChatMessage.FileActionDetail a = input.get(i);
            String key;
            if ("renameFile".equals(a.type)) {
                key = a.newPath != null ? a.newPath : a.path;
            } else {
                key = a.path;
            }
            if (key == null) key = "";
            if (!map.containsKey(key)) {
                map.put(key, a);
            }
        }
        // map preserves insertion order (from end); reverse to retain overall chronological order
        ArrayList<ChatMessage.FileActionDetail> out = new ArrayList<>(map.values());
        java.util.Collections.reverse(out);
        return out;
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

        void bind(final ChatMessage.FileActionDetail action, final OnFileActionClickListener listener, int added, int removed) {
            String path = action.type.equals("renameFile") ? action.newPath : action.path;
            textFileName.setText(path);

            Context context = itemView.getContext();
            // Use full width as provided by RecyclerView layout
            int changeColor;
            int changeTextColor;
            String changeLabel;

            switch (action.type) {
                case "createFile":
                    changeLabel = "New";
                    changeColor = ContextCompat.getColor(context, R.color.success_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_success_container);
                    break;
                case "updateFile":
                case "modifyLines":
                case "smartUpdate":
                case "patchFile":
                    changeLabel = "Modified";
                    changeColor = ContextCompat.getColor(context, R.color.primary_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_primary_container);
                    break;
                case "deleteFile":
                    changeLabel = "Deleted";
                    changeColor = ContextCompat.getColor(context, R.color.error_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_error_container);
                    break;
                case "renameFile":
                    changeLabel = "Renamed";
                    changeColor = ContextCompat.getColor(context, R.color.warning_container);
                    changeTextColor = ContextCompat.getColor(context, R.color.on_warning_container);
                    break;
                default:
                    changeLabel = "Modified";
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

            // Diff badges (+ added / - removed lines) provided by adapter pre-computation

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

        private int countLines(String s) {
            if (s == null || s.isEmpty()) return 0;
            int lines = 1;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\n') lines++;
            }
            return lines;
        }

        // Removed duplicate unified diff parser; using DiffUtils.countAddRemove
    }

    /**
     * Compute aggregated add/remove counts for a displayed action by summing all
     * changes to the same effective file path within this message (handles renames).
     */
    private int[] computeAggregatedCountsForDisplayAction(ChatMessage.FileActionDetail displayed) {
        String displayPath = "renameFile".equals(displayed.type) && displayed.newPath != null && !displayed.newPath.isEmpty()
                ? displayed.newPath
                : displayed.path;
        if (displayPath == null) displayPath = "";

        // Build path alias set by walking rename chain backwards
        java.util.Set<String> relatedPaths = new java.util.LinkedHashSet<>();
        relatedPaths.add(displayPath);
        boolean changed;
        for (int pass = 0; pass < 3; pass++) { // few passes are enough for single-message chains
            changed = false;
            for (ChatMessage.FileActionDetail a : fileActions) {
                if ("renameFile".equals(a.type) && a.newPath != null && relatedPaths.contains(a.newPath) && a.oldPath != null) {
                    if (relatedPaths.add(a.oldPath)) changed = true;
                }
            }
            if (!changed) break;
        }

        int added = 0, removed = 0;
        for (ChatMessage.FileActionDetail a : fileActions) {
            boolean affects = false;
            if (a.path != null && relatedPaths.contains(a.path)) affects = true;
            if (!affects && "renameFile".equals(a.type) && (relatedPaths.contains(a.oldPath) || relatedPaths.contains(a.newPath))) affects = true;
            if (!affects) continue;

            if ("modifyLines".equals(a.type)) {
                added += (a.insertLines != null) ? a.insertLines.size() : 0;
                removed += Math.max(0, a.deleteCount);
            } else if (a.diffPatch != null && !a.diffPatch.isEmpty()) {
                int[] c = DiffUtils.countAddRemove(a.diffPatch);
                added += c[0];
                removed += c[1];
            } else if ("createFile".equals(a.type) && a.newContent != null) {
                added += countLinesStatic(a.newContent);
            } else if ("deleteFile".equals(a.type) && a.oldContent != null) {
                removed += countLinesStatic(a.oldContent);
            } else if (a.oldContent != null || a.newContent != null) {
                int[] c = DiffUtils.countAddRemoveFromContents(a.oldContent, a.newContent);
                added += c[0];
                removed += c[1];
            }
        }
        return new int[]{added, removed};
    }

    private static int countLinesStatic(String s) {
        if (s == null || s.isEmpty()) return 0;
        int lines = 1;
        for (int i = 0; i < s.length(); i++) { if (s.charAt(i) == '\n') lines++; }
        return lines;
    }
}
