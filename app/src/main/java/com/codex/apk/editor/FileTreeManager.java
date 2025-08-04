package com.codex.apk.editor;

import android.animation.LayoutTransition;
import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.EditorActivity;
import com.codex.apk.FileItem;
import com.codex.apk.FileManager;
import com.codex.apk.FileTreeAdapter;
import com.codex.apk.R;
import com.codex.apk.TabItem;
import com.codex.apk.DialogHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileTreeManager {
    private static final String TAG = "FileTreeManager";
    private final EditorActivity activity;
    private final FileManager fileManager;
    private final DialogHelper dialogHelper;
    private final List<FileItem> fileItems;
    private final List<FileItem> filteredFileItems;
    private final List<TabItem> openTabs;

    private RecyclerView recyclerViewFileTree;
    private FileTreeAdapter fileTreeAdapter;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private ChipGroup filterChipGroup;
    private View searchContainer;
    private String currentSearchQuery = "";
    private String currentFilter = "all";

    public FileTreeManager(EditorActivity activity, FileManager fileManager, DialogHelper dialogHelper, List<FileItem> fileItems, List<TabItem> openTabs) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.dialogHelper = dialogHelper;
        this.fileItems = fileItems;
        this.filteredFileItems = new ArrayList<>();
        this.openTabs = openTabs;
    }

    /**
     * Sets up the file tree RecyclerView in the navigation drawer with enhanced features.
     */
    public void setupFileTree() {
        recyclerViewFileTree = activity.findViewById(R.id.recycler_view_file_tree);
        searchContainer = activity.findViewById(R.id.search_container);
        searchInputLayout = activity.findViewById(R.id.search_input_layout);
        searchEditText = activity.findViewById(R.id.search_edit_text);
        filterChipGroup = activity.findViewById(R.id.filter_chip_group);

        // Setup search functionality
        setupSearch();
        
        // Setup filter chips
        setupFilterChips();

        fileTreeAdapter = new FileTreeAdapter(activity, filteredFileItems, null, activity);
        recyclerViewFileTree.setAdapter(fileTreeAdapter);
        
        // Enhanced layout manager with smooth scrolling
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerViewFileTree.setLayoutManager(layoutManager);
        
        // Enable item animations
        recyclerViewFileTree.setItemAnimator(null); // Disable default animations for better performance

        TextView projectNameText = activity.findViewById(R.id.text_project_name);
        if (projectNameText != null) {
            projectNameText.setText("Project Files");
        }

        // Enhanced button setup with better visual feedback
        setupActionButtons();

        loadFileTree(); // Initial load
    }

    private void setupSearch() {
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().toLowerCase();
                    filterAndDisplayFiles();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void setupFilterChips() {
        if (filterChipGroup != null) {
            // Clear existing chips
            filterChipGroup.removeAllViews();
            
            // Add filter chips
            addFilterChip("All", "all", true);
            addFilterChip("Files", "files", false);
            addFilterChip("Folders", "folders", false);
            addFilterChip("Code", "code", false);
            addFilterChip("Media", "media", false);
        }
    }

    private void addFilterChip(String text, String tag, boolean selected) {
        Chip chip = new Chip(activity);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(selected);
        chip.setChipBackgroundColorResource(R.color.chip_background_color);
        chip.setTextColor(activity.getResources().getColorStateList(R.color.chip_text_color, null));
        
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = tag;
                filterAndDisplayFiles();
            }
        });
        
        filterChipGroup.addView(chip);
    }

    private void setupActionButtons() {
        // New File Button
        View btnNewFile = activity.findViewById(R.id.btn_new_file);
        if (btnNewFile != null) {
            btnNewFile.setOnClickListener(v -> {
                animateButtonClick(btnNewFile);
                showNewFileDialog(activity.getProjectDirectory());
            });
        }
        
        // New Folder Button
        View btnNewFolder = activity.findViewById(R.id.btn_new_folder);
        if (btnNewFolder != null) {
            btnNewFolder.setOnClickListener(v -> {
                animateButtonClick(btnNewFolder);
                showNewFolderDialog(activity.getProjectDirectory());
            });
        }
        
        // Open from Device Button
        View btnOpenFromDevice = activity.findViewById(R.id.btn_open_from_device);
        if (btnOpenFromDevice != null) {
            btnOpenFromDevice.setOnClickListener(v -> {
                animateButtonClick(btnOpenFromDevice);
                activity.showToast("Open from device: Not yet implemented");
            });
        }
        
        // Refresh Button
        View btnRefresh = activity.findViewById(R.id.btn_refresh_file_tree);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                animateButtonClick(btnRefresh);
                loadFileTree();
            });
        }
    }

    private void animateButtonClick(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    /**
     * Loads the file tree from the project directory and updates the RecyclerView.
     */
    public void loadFileTree() {
        fileItems.clear();
        if (fileManager != null) {
            fileItems.addAll(fileManager.loadFileTree());
        } else {
            Log.e(TAG, "loadFileTree: FileManager not initialized!");
            activity.showToast("Error: Could not load file tree.");
        }

        filterAndDisplayFiles();
    }

    private void filterAndDisplayFiles() {
        filteredFileItems.clear();
        
        for (FileItem item : fileItems) {
            if (matchesSearch(item) && matchesFilter(item)) {
                filteredFileItems.add(item);
            }
        }
        
        if (fileTreeAdapter != null) {
            fileTreeAdapter.notifyDataSetChanged();
        }
        
        updateEmptyState();
    }

    private boolean matchesSearch(FileItem item) {
        if (currentSearchQuery.isEmpty()) {
            return true;
        }
        return item.getName().toLowerCase().contains(currentSearchQuery);
    }

    private boolean matchesFilter(FileItem item) {
        switch (currentFilter) {
            case "files":
                return !item.isDirectory();
            case "folders":
                return item.isDirectory();
            case "code":
                return isCodeFile(item.getName());
            case "media":
                return isMediaFile(item.getName());
            default:
                return true;
        }
    }

    private boolean isCodeFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".html") || lowerName.endsWith(".htm") ||
               lowerName.endsWith(".css") || lowerName.endsWith(".js") ||
               lowerName.endsWith(".json") || lowerName.endsWith(".xml") ||
               lowerName.endsWith(".md") || lowerName.endsWith(".txt");
    }

    private boolean isMediaFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
               lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
               lowerName.endsWith(".svg") || lowerName.endsWith(".webp") ||
               lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
               lowerName.endsWith(".mov") || lowerName.endsWith(".mkv") ||
               lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
               lowerName.endsWith(".ogg") || lowerName.endsWith(".flac");
    }

    private void updateEmptyState() {
        View emptyStateView = activity.findViewById(R.id.empty_state_view);
        if (emptyStateView != null) {
            if (filteredFileItems.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                TextView emptyStateText = activity.findViewById(R.id.empty_state_text);
                if (emptyStateText != null) {
                    if (!currentSearchQuery.isEmpty()) {
                        emptyStateText.setText("No files match your search");
                    } else if (!currentFilter.equals("all")) {
                        emptyStateText.setText("No files match the selected filter");
                    } else {
                        emptyStateText.setText("No files in this directory");
                    }
                }
            } else {
                emptyStateView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Shows a dialog for creating a new file.
     * @param parentDirectory The directory where the new file will be created.
     */
    public void showNewFileDialog(File parentDirectory) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_new_file, null);
        TextInputEditText fileNameEditText = dialogView.findViewById(R.id.edit_text_file_name);
        TextInputEditText fileExtensionEditText = dialogView.findViewById(R.id.edit_text_file_extension);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
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
                        Log.e(TAG, "Error creating file", e);
                        Toast.makeText(activity, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows a dialog for creating a new folder.
     * @param parentDirectory The directory where the new folder will be created.
     */
    public void showNewFolderDialog(File parentDirectory) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        TextInputEditText folderNameEditText = dialogView.findViewById(R.id.edit_text_folder_name);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
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

    /**
     * Renames a file or directory.
     * @param oldFile The file to rename.
     * @param newFile The new file name.
     */
    public void renameFileOrDir(File oldFile, File newFile) {
        if (oldFile.renameTo(newFile)) {
            activity.showToast("Renamed successfully");
            refreshOpenTabsAfterFileOperation(oldFile, newFile);
            loadFileTree();
        } else {
            Toast.makeText(activity, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes a file or directory.
     * @param fileOrDirectory The file or directory to delete.
     */
    public void deleteFileByPath(File fileOrDirectory) {
        if (deleteRecursively(fileOrDirectory)) {
            activity.showToast("Deleted successfully");
            refreshOpenTabsAfterFileOperation(fileOrDirectory, null);
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

    /**
     * Refreshes open tabs after file operations.
     * @param oldFileOrDir The old file or directory.
     * @param newFileOrDir The new file or directory.
     */
    public void refreshOpenTabsAfterFileOperation(File oldFileOrDir, File newFileOrDir) {
        if (oldFileOrDir == null) return;
        
        for (int i = openTabs.size() - 1; i >= 0; i--) {
            TabItem tab = openTabs.get(i);
            if (tab.getFile().equals(oldFileOrDir)) {
                if (newFileOrDir != null) {
                    // File was renamed
                    tab.setFile(newFileOrDir);
                } else {
                    // File was deleted
                    activity.closeTab(i, false);
                }
            }
        }
    }

    /**
     * Rebuilds the file tree with current filters and search.
     */
    public void rebuildFileTree() {
        loadFileTree();
    }
}