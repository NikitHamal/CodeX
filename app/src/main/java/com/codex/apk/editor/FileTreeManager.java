package com.codex.apk.editor;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.codex.apk.EditorActivity;
import com.codex.apk.FileItem;
import com.codex.apk.FileManager;
import com.codex.apk.R;
import com.codex.apk.TabItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.ViewGroup;

import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

public class FileTreeManager {
    private final EditorActivity activity;
    private final FileManager fileManager;
    private final com.codex.apk.DialogHelper dialogHelper;
    private final List<FileItem> fileItems;
    private final List<FileItem> filteredFileItems;
    private final List<TabItem> openTabs;

    private AndroidTreeView androidTreeView;
    private TreeNode rootNode;
    private View treeContainer;
    private EditText searchEditText;
    private View searchContainer;
    private String currentSearchQuery = "";

    public FileTreeManager(EditorActivity activity, FileManager fileManager, com.codex.apk.DialogHelper dialogHelper, List<FileItem> fileItems, List<TabItem> openTabs) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.dialogHelper = dialogHelper;
        this.fileItems = fileItems;
        this.filteredFileItems = new ArrayList<>();
        this.openTabs = openTabs;
    }

    public void setupFileTree() {
        treeContainer = activity.findViewById(R.id.tree_container);
        searchContainer = activity.findViewById(R.id.search_container);
        searchEditText = activity.findViewById(R.id.search_edit_text);

        setupSearch();
        setupActionButtons();
        loadFileTree();
    }

    private void setupSearch() {
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
    }

    private void setupActionButtons() {
        View btnNewFile = activity.findViewById(R.id.btn_new_file);
        if (btnNewFile != null) btnNewFile.setOnClickListener(v -> showNewFileDialog(activity.getProjectDirectory()));
        View btnNewFolder = activity.findViewById(R.id.btn_new_folder);
        if (btnNewFolder != null) btnNewFolder.setOnClickListener(v -> showNewFolderDialog(activity.getProjectDirectory()));
        View btnRefresh = activity.findViewById(R.id.btn_refresh_file_tree);
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> loadFileTree());
        View btnOpenFromDevice = activity.findViewById(R.id.btn_open_from_device);
        if (btnOpenFromDevice != null) btnOpenFromDevice.setOnClickListener(v -> activity.showToast("Open from device: Not yet implemented"));
    }

    public void loadFileTree() {
        if (treeContainer == null) return;

        // Remove existing tree view from its parent if present, then recreate
        if (androidTreeView != null && androidTreeView.getView() != null) {
            View existing = androidTreeView.getView();
            ViewGroup parent = (ViewGroup) existing.getParent();
            if (parent != null) {
                parent.removeView(existing);
            }
            androidTreeView = null;
            rootNode = null;
        }

        rootNode = TreeNode.root();

        File projectDir = activity.getProjectDirectory();
        TreeNode projectNode = null;
        if (projectDir != null && projectDir.exists()) {
            projectNode = createNodeForFile(projectDir);
            rootNode.addChild(projectNode);
            if (currentSearchQuery.isEmpty()) {
                buildTree(projectNode, projectDir);
            } else {
                buildFilteredTree(projectNode, projectDir, currentSearchQuery);
            }
        }

        androidTreeView = new AndroidTreeView(activity, rootNode);
        androidTreeView.setDefaultContainerStyle(R.style.TreeNodeStyle);
        androidTreeView.setDefaultAnimation(true);
        androidTreeView.setDefaultViewHolder(FileNodeViewHolder.class);
        androidTreeView.setUseAutoToggle(true);

        ViewGroup container = (ViewGroup) treeContainer;
        container.removeAllViews();
        container.addView(androidTreeView.getView());

        // Empty state visibility
        View emptyStateView = activity.findViewById(R.id.empty_state_view);
        if (emptyStateView != null) {
            boolean hasAny = projectNode != null && !projectNode.getChildren().isEmpty();
            emptyStateView.setVisibility(hasAny ? View.GONE : View.VISIBLE);
            TextView emptyStateText = activity.findViewById(R.id.empty_state_text);
            if (emptyStateText != null) {
                if (!currentSearchQuery.isEmpty()) {
                    emptyStateText.setText("No files match your search");
                } else {
                    emptyStateText.setText("No files in this directory");
                }
            }
        }
    }

    private void buildTree(TreeNode parentNode, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        for (File file : files) {
            TreeNode node = createNodeForFile(file);
            parentNode.addChild(node);
            if (file.isDirectory()) {
                buildTree(node, file);
            }
        }
    }

    private void buildFilteredTree(TreeNode parentNode, File dir, String query) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.getName().toLowerCase().contains(query)) {
                parentNode.addChild(createNodeForFile(file));
            }
            if (file.isDirectory()) {
                buildFilteredTree(parentNode, file, query);
            }
        }
    }

    private TreeNode createNodeForFile(File file) {
        return new TreeNode(new FileNode(file)).setViewHolder(new FileNodeViewHolder(activity, fileManager, this));
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
                String fullFileName = extension.isEmpty() ? fileName : fileName + "." + extension;
                File newFile = new File(parentDirectory, fullFileName);
                if (newFile.exists()) {
                    Toast.makeText(activity, "File already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (newFile.createNewFile()) {
                        activity.showToast("File created: " + fullFileName);
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
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    public void rebuildFileTree() {
        loadFileTree();
    }

    public void toggleNode(TreeNode node) {
        if (androidTreeView != null && node != null) {
            boolean expanded = node.isExpanded();
            if (expanded) {
                androidTreeView.collapseNode(node);
            } else {
                androidTreeView.expandNode(node);
            }
        }
    }

    public static class FileNode {
        public final File file;
        public FileNode(File file) { this.file = file; }
    }

    public static class FileNodeViewHolder extends TreeNode.BaseNodeViewHolder<FileNode> {
        private final EditorActivity activity;
        private final FileManager fileManager;
        private final FileTreeManager manager;

        public FileNodeViewHolder(EditorActivity activity, FileManager fileManager, FileTreeManager manager) {
            super(activity);
            this.activity = activity;
            this.fileManager = fileManager;
            this.manager = manager;
        }

        @Override
        public View createNodeView(TreeNode node, FileNode value) {
            View view = activity.getLayoutInflater().inflate(R.layout.item_tree_node, null, false);
            TextView tv = view.findViewById(R.id.text_file_name);
            ImageView icon = view.findViewById(R.id.image_icon);
            ImageView more = view.findViewById(R.id.image_more);

            tv.setText(value.file.getName());
            icon.setImageResource(value.file.isDirectory() ? R.drawable.icon_folder_round : R.drawable.icon_file_round);

            view.setOnClickListener(v -> {
                if (value.file.isDirectory()) {
                    manager.toggleNode(node);
                } else {
                    activity.openFile(value.file);
                }
            });

            more.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(activity, more);
                popup.inflate(R.menu.file_context_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_rename) {
                        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_rename_file, null);
                        com.google.android.material.textfield.TextInputEditText editText = dialogView.findViewById(R.id.edit_text_new_name);
                        editText.setText(value.file.getName());
                        new MaterialAlertDialogBuilder(activity)
                            .setTitle("Rename")
                            .setView(dialogView)
                            .setPositiveButton("Rename", (d, w) -> {
                                String newName = editText.getText().toString().trim();
                                if (!newName.isEmpty()) {
                                    File newFile = new File(value.file.getParentFile(), newName);
                                    manager.renameFileOrDir(value.file, newFile);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                        return true;
                    } else if (id == R.id.action_delete) {
                        new MaterialAlertDialogBuilder(activity)
                            .setTitle("Confirm Delete")
                            .setMessage("Delete " + value.file.getName() + "?")
                            .setPositiveButton("Delete", (d, w) -> manager.deleteFileByPath(value.file))
                            .setNegativeButton("Cancel", null)
                            .show();
                        return true;
                    } else if (id == R.id.action_new_file && value.file.isDirectory()) {
                        manager.showNewFileDialog(value.file);
                        return true;
                    } else if (id == R.id.action_new_folder && value.file.isDirectory()) {
                        manager.showNewFolderDialog(value.file);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });

            return view;
        }
    }
}