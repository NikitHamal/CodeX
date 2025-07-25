package com.codex.apk.editor;

import android.util.Log;
import android.view.MenuItem;
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
import com.codex.apk.DialogHelper; // Added import for DialogHelper
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.view.View; // Added import for View

public class FileTreeManager {
    private static final String TAG = "FileTreeManager";
    private final EditorActivity activity;
    private final FileManager fileManager;
    private final DialogHelper dialogHelper;
    private final List<FileItem> fileItems; // The list of file tree items
    private final List<TabItem> openTabs; // Need access to openTabs for refresh logic

    private RecyclerView recyclerViewFileTree;
    private FileTreeAdapter fileTreeAdapter;

    public FileTreeManager(EditorActivity activity, FileManager fileManager, DialogHelper dialogHelper, List<FileItem> fileItems, List<TabItem> openTabs) {
        this.activity = activity;
        this.fileManager = fileManager;
        this.dialogHelper = dialogHelper;
        this.fileItems = fileItems;
        this.openTabs = openTabs;
    }

    /**
     * Sets up the file tree RecyclerView in the navigation drawer.
     */
    public void setupFileTree() {
        recyclerViewFileTree = activity.findViewById(R.id.recycler_view_file_tree);
        fileTreeAdapter = new FileTreeAdapter(activity, fileItems, null, activity); // No drawer layout in new design
        recyclerViewFileTree.setAdapter(fileTreeAdapter);
        recyclerViewFileTree.setLayoutManager(new LinearLayoutManager(activity));

        TextView projectNameText = activity.findViewById(R.id.text_project_name);
        if (projectNameText != null) {
            projectNameText.setText("Project Files");
        }

        // Handle new file, new folder, and open from device buttons
        View btnNewFile = activity.findViewById(R.id.btn_new_file);
        if (btnNewFile != null) {
            btnNewFile.setOnClickListener(v -> showNewFileDialog(activity.getProjectDirectory()));
        }
        View btnNewFolder = activity.findViewById(R.id.btn_new_folder);
        if (btnNewFolder != null) {
            btnNewFolder.setOnClickListener(v -> showNewFolderDialog(activity.getProjectDirectory()));
        }
        View btnOpenFromDevice = activity.findViewById(R.id.btn_open_from_device);
        if (btnOpenFromDevice != null) {
            btnOpenFromDevice.setOnClickListener(v -> {
                // Show a file picker dialog (simple intent)
                activity.showToast("Open from device: Not yet implemented");
                // TODO: Implement file picker and import logic
            });
        }

        loadFileTree(); // Initial load
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

        if (fileTreeAdapter != null) {
            fileTreeAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Shows a dialog for creating a new file.
     * @param parentDirectory The directory where the new file will be created.
     */
    public void showNewFileDialog(File parentDirectory) {
        if (dialogHelper == null) {
            activity.showToast("Dialog helper not initialized.");
            return;
        }
        dialogHelper.showNewFileDialog(parentDirectory);
    }

    /**
     * Shows a dialog for creating a new folder.
     * @param parentDirectory The directory where the new folder will be created.
     */
    public void showNewFolderDialog(File parentDirectory) {
        if (dialogHelper == null) {
            activity.showToast("Dialog helper not initialized.");
            return;
        }
        dialogHelper.showNewFolderDialog(parentDirectory);
    }

    /**
     * Renames a file or directory and updates the file tree and open tabs.
     * @param oldFile The old file or directory.
     * @param newFile The new file or directory.
     * @throws IOException If the rename operation fails.
     */
    public void renameFileOrDir(File oldFile, File newFile) throws IOException {
        if (fileManager == null) {
            throw new IOException("FileManager not initialized. Cannot rename.");
        }
        fileManager.renameFileOrDir(oldFile, newFile);
        loadFileTree();
        refreshOpenTabsAfterFileOperation(oldFile, newFile);
    }

    /**
     * Deletes a file or directory and updates the file tree and open tabs.
     * @param fileOrDirectory The file or directory to delete.
     * @throws IOException If the delete operation fails.
     */
    public void deleteFileByPath(File fileOrDirectory) throws IOException {
        if (fileManager == null) {
            throw new IOException("FileManager not initialized. Cannot delete.");
        }
        fileManager.deleteFileOrDirectory(fileOrDirectory);
        loadFileTree();
        refreshOpenTabsAfterFileOperation(fileOrDirectory, null);
    }

    /**
     * Refreshes open tabs after a file operation (rename or delete).
     * @param oldFileOrDir The old file or directory involved in the operation.
     * @param newFileOrDir The new file or directory (null if deleted).
     */
    public void refreshOpenTabsAfterFileOperation(File oldFileOrDir, File newFileOrDir) {
        List<TabItem> toRemove = new ArrayList<>();
        List<TabItem> toUpdate = new ArrayList<>();

        for (TabItem tab : openTabs) {
            // Skip diff tabs
            if (tab.getFile().getName().startsWith("DIFF_")) {
                continue;
            }

            String tabFilePath = tab.getFile().getAbsolutePath();
            String oldPath = oldFileOrDir.getAbsolutePath();

            if (oldFileOrDir.isDirectory()) {
                if (tabFilePath.startsWith(oldPath + File.separator) || tabFilePath.equals(oldPath)) {
                    if (newFileOrDir == null) { // Directory deleted
                        toRemove.add(tab);
                    } else { // Directory renamed
                        String relativePathUnderOldDir = tabFilePath.substring(oldPath.length());
                        File updatedFile = new File(newFileOrDir, relativePathUnderOldDir);
                        tab.setFile(updatedFile);
                        toUpdate.add(tab);
                    }
                }
            } else {
                if (tabFilePath.equals(oldPath)) {
                    if (newFileOrDir == null) { // File deleted
                        toRemove.add(tab);
                    } else { // File renamed
                        tab.setFile(newFileOrDir);
                        toUpdate.add(tab);
                    }
                }
            }
        }

        openTabs.removeAll(toRemove);

        // Close removed tabs in fragment
        for (TabItem removedTab : toRemove) {
            int index = activity.getOpenTabsList().indexOf(removedTab); // Get current index in activity's list
            if (index != -1) {
                activity.getCodeEditorFragment().removeFileTab(index);
            }
        }

        if (!toRemove.isEmpty() || !toUpdate.isEmpty()) {
            activity.getCodeEditorFragment().refreshAllFileTabs(); // Refresh all tabs in fragment
            activity.getCodeEditorFragment().refreshFileTabLayout();
        }
    }

	public void rebuildFileTree() {
		if (fileTreeAdapter != null) {
			loadFileTree();
		}
	}
}