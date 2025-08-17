package com.codex.apk.editor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.EditorActivity;
import com.codex.apk.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExpandableTreeAdapter extends RecyclerView.Adapter<ExpandableTreeAdapter.NodeViewHolder> {
    private final EditorActivity activity;
    private final List<FileTreeManager.TreeNode> visibleNodes = new ArrayList<>();
    private final List<FileTreeManager.TreeNode> roots;

    public ExpandableTreeAdapter(EditorActivity activity, List<FileTreeManager.TreeNode> initial) {
        this.activity = activity;
        this.roots = new ArrayList<>(initial);
        rebuildVisible();
    }

    public void setNodes(List<FileTreeManager.TreeNode> nodes) {
        this.roots.clear();
        this.roots.addAll(nodes);
        rebuildVisible();
        notifyDataSetChanged();
    }

    private void rebuildVisible() {
        visibleNodes.clear();
        for (FileTreeManager.TreeNode n : roots) {
            addVisible(n);
        }
    }

    private void addVisible(FileTreeManager.TreeNode node) {
        visibleNodes.add(node);
        if (node.expanded) {
            for (FileTreeManager.TreeNode c : node.children) addVisible(c);
        }
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_tree_enhanced, parent, false);
        return new NodeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
        FileTreeManager.TreeNode node = visibleNodes.get(position);
        File f = node.file;

        float density = holder.itemView.getResources().getDisplayMetrics().density;
        int base = (int) (12 * density);
        int indent = base + (int) (14 * density) * Math.max(0, node.level);
        holder.itemView.setPadding(indent, (int) (4 * density), holder.itemView.getPaddingRight(), (int) (4 * density));

        // Indentation guide
        if (holder.indentGuide != null) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.indentGuide.getLayoutParams();
            int guideOffset = base + (int) (14 * density) * Math.max(0, node.level - 1);
            lp.setMarginStart(guideOffset);
            holder.indentGuide.setLayoutParams(lp);
            holder.indentGuide.setVisibility(node.level > 0 ? View.VISIBLE : View.GONE);
        }

        holder.textFileName.setText(f.getName());
        if (f.isDirectory()) {
            holder.imageFileIcon.setImageResource(node.expanded ? R.drawable.icon_folder_open_round : R.drawable.icon_folder_round);
            holder.imageExpandIcon.setVisibility(View.VISIBLE);
            holder.imageExpandIcon.setImageResource(node.expanded ? R.drawable.icon_expand_less_round : R.drawable.icon_expand_more_round);
        } else {
            holder.imageFileIcon.setImageResource(R.drawable.icon_file_round);
            holder.imageExpandIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (f.isDirectory()) {
                node.expanded = !node.expanded;
                rebuildVisible();
                notifyDataSetChanged();
            } else {
                activity.openFile(f);
            }
        });

        holder.imageExpandIcon.setOnClickListener(v -> {
            node.expanded = !node.expanded;
            rebuildVisible();
            notifyDataSetChanged();
        });

        holder.imageMoreVert.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(activity, holder.imageMoreVert);
            popup.inflate(R.menu.file_context_menu);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_rename) {
                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_rename_file, null);
                    com.google.android.material.textfield.TextInputEditText editText = dialogView.findViewById(R.id.edit_text_new_name);
                    editText.setText(f.getName());
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                            .setTitle("Rename")
                            .setView(dialogView)
                            .setPositiveButton("Rename", (d, w) -> {
                                String newName = editText.getText().toString().trim();
                                if (!newName.isEmpty()) {
                                    File newFile = new File(f.getParentFile(), newName);
                                    ((FileTreeManager) (activity.fileTreeManager)).renameFileOrDir(f, newFile);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                } else if (id == R.id.action_delete) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                            .setTitle("Confirm Delete")
                            .setMessage("Delete " + f.getName() + "?")
                            .setPositiveButton("Delete", (d, w) -> ((FileTreeManager) (activity.fileTreeManager)).deleteFileByPath(f))
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                } else if (id == R.id.action_new_file && f.isDirectory()) {
                    ((FileTreeManager) (activity.fileTreeManager)).showNewFileDialog(f);
                    return true;
                } else if (id == R.id.action_new_folder && f.isDirectory()) {
                    ((FileTreeManager) (activity.fileTreeManager)).showNewFolderDialog(f);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return visibleNodes.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        View indentGuide;
        ImageView imageFileIcon, imageExpandIcon, imageMoreVert;
        TextView textFileName;
        NodeViewHolder(@NonNull View itemView) {
            super(itemView);
            indentGuide = itemView.findViewById(R.id.indent_guide);
            imageFileIcon = itemView.findViewById(R.id.image_file_icon);
            imageExpandIcon = itemView.findViewById(R.id.image_expand_icon);
            imageMoreVert = itemView.findViewById(R.id.image_more_vert);
            textFileName = itemView.findViewById(R.id.text_file_name);
        }
    }
}