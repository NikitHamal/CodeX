package com.codex.apk.editor;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.EditorActivity;
import com.codex.apk.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileTreeManager {
    private final EditorActivity activity;
    private RecyclerView recyclerView;
    private ExpandableTreeAdapter adapter;
    private EditText searchEditText;
    private String currentSearchQuery = "";

    public FileTreeManager(EditorActivity activity, com.codex.apk.FileManager fileManager, com.codex.apk.DialogHelper dialogHelper, List<com.codex.apk.FileItem> fileItems, List<com.codex.apk.TabItem> openTabs) {
        this.activity = activity;
    }

    public void setupFileTree() {
        recyclerView = activity.findViewById(R.id.recycler_view_file_tree);
        searchEditText = activity.findViewById(R.id.search_edit_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new ExpandableTreeAdapter(activity, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase();
                    loadFileTree();
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        View btnNewFile = activity.findViewById(R.id.btn_new_file);
        if (btnNewFile != null) btnNewFile.setOnClickListener(v -> showNewFileDialog(activity.getProjectDirectory()));
        View btnNewFolder = activity.findViewById(R.id.btn_new_folder);
        if (btnNewFolder != null) btnNewFolder.setOnClickListener(v -> showNewFolderDialog(activity.getProjectDirectory()));
        View btnRefresh = activity.findViewById(R.id.btn_refresh_file_tree);
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> loadFileTree());

        loadFileTree();
    }

    public void loadFileTree() {
        File root = activity.getProjectDirectory();
        List<TreeNode> nodes = new ArrayList<>();
        if (root != null && root.exists()) {
            TreeNode rootNode = buildTree(root, 0);
            nodes.add(rootNode);
        }
        if (!currentSearchQuery.isEmpty()) {
            nodes = filterNodes(nodes, currentSearchQuery);
        }
        adapter.setNodes(nodes);
        updateEmptyState(nodes);
    }

    private void updateEmptyState(List<TreeNode> nodes) {
        View emptyStateView = activity.findViewById(R.id.empty_state_view);
        if (emptyStateView != null) {
            boolean hasAny = !nodes.isEmpty() && !nodes.get(0).children.isEmpty();
            emptyStateView.setVisibility(hasAny ? View.GONE : View.VISIBLE);
            TextView emptyStateText = activity.findViewById(R.id.empty_state_text);
            if (emptyStateText != null) {
                emptyStateText.setText(currentSearchQuery.isEmpty() ? "No files in this directory" : "No files match your search");
            }
        }
    }

    private TreeNode buildTree(File file, int level) {
        TreeNode node = new TreeNode(file, level);
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                Arrays.sort(list, new Comparator<File>() {
                    @Override public int compare(File f1, File f2) {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    }
                });
                for (File child : list) {
                    node.children.add(buildTree(child, level + 1));
                }
            }
        }
        return node;
    }

    private List<TreeNode> filterNodes(List<TreeNode> nodes, String query) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode n : nodes) {
            TreeNode copy = n.copyPruned(query);
            if (copy != null) result.add(copy);
        }
        return result;
    }

    public void showNewFileDialog(File parentDirectory) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        EditText fileNameEditText = dialogView.findViewById(R.id.edit_text_file_name);
        EditText fileExtensionEditText = dialogView.findViewById(R.id.edit_text_file_extension);
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Create New File")
            .setView(dialogView)
            .setPositiveButton("Create", (dialog, which) -> {
                String fileName = fileNameEditText.getText().toString().trim();
                String extension = fileExtensionEditText.getText().toString().trim();
                if (fileName.isEmpty()) {
                    Toast.makeText(activity, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                String full = extension.isEmpty() ? fileName : fileName + "." + extension;
                File newFile = new File(parentDirectory, full);
                if (newFile.exists()) {
                    Toast.makeText(activity, "File already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (newFile.createNewFile()) {
                        activity.showToast("File created: " + full);
                        loadFileTree();
                        activity.openFile(newFile);
                    } else {
                        Toast.makeText(activity, "Failed to create file", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(activity, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public void showNewFolderDialog(File parentDirectory) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        EditText folderNameEditText = dialogView.findViewById(R.id.edit_text_folder_name);
        new MaterialAlertDialogBuilder(activity)
            .setTitle("Create New Folder")
            .setView(dialogView)
            .setPositiveButton("Create", (dialog, which) -> {
                String folderName = folderNameEditText.getText().toString().trim();
                if (folderName.isEmpty()) {
                    Toast.makeText(activity, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                File newFolder = new File(parentDirectory, folderName);
                if (newFolder.exists()) {
                    Toast.makeText(activity, "Folder already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newFolder.mkdir()) {
                    activity.showToast("Folder created: " + folderName);
                    loadFileTree();
                } else {
                    Toast.makeText(activity, "Failed to create folder", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public void renameFileOrDir(File oldFile, File newFile) {
        if (oldFile.renameTo(newFile)) {
            activity.showToast("Renamed successfully");
            loadFileTree();
        } else {
            Toast.makeText(activity, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteFileByPath(File fileOrDirectory) {
        if (deleteRecursively(fileOrDirectory)) {
            activity.showToast("Deleted successfully");
            loadFileTree();
        } else {
            Toast.makeText(activity, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }
        return file.delete();
    }

    public void rebuildFileTree() { loadFileTree(); }

    // TreeNode model
    static class TreeNode {
        final File file;
        final int level;
        boolean expanded = true;
        final List<TreeNode> children = new ArrayList<>();

        TreeNode(File file, int level) { this.file = file; this.level = level; }

        TreeNode copyPruned(String query) {
            boolean matches = file.getName().toLowerCase().contains(query);
            TreeNode copy = new TreeNode(file, level);
            for (TreeNode c : children) {
                TreeNode pruned = c.copyPruned(query);
                if (pruned != null) copy.children.add(pruned);
            }
            if (matches || !copy.children.isEmpty()) return copy;
            return null;
        }
    }
}