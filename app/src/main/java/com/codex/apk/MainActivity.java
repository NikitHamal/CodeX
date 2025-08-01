package com.codex.apk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 102;
    private static final int REQUEST_CODE_PICK_ZIP_FILE = 103;
    private static final String PREFS_NAME = "project_prefs";
    private static final String PROJECTS_LIST_KEY = "projects_list";
    private static final String CHAT_HISTORY_FILE_NAME = "chat_history.json";

    private ListView listViewProjects;
    private TextView textEmptyProjects;
    private LinearLayout layoutEmptyState;
    private MaterialToolbar toolbar;

    // New buttons instead of FABs
    private MaterialButton buttonNewProject;
    private MaterialButton buttonImportProject;

    private ArrayList<HashMap<String, Object>> projectsList;
    private ProjectsAdapter projectsAdapter;
    private TemplateManager templateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        		// Set up theme based on user preferences
		ThemeManager.setupTheme(this);
		setTheme(R.style.AppTheme);
        setContentView(R.layout.main);

        // Initialize views
        listViewProjects = findViewById(R.id.listview_projects);
        textEmptyProjects = findViewById(R.id.text_empty_projects);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        toolbar = findViewById(R.id.toolbar);

        // Initialize new buttons (removed from welcome card)
        buttonNewProject = null; // Removed from welcome card
        buttonImportProject = null; // Removed from welcome card

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name)); // Set title as per screenshot
        }

        migrateOldProjects();

        projectsList = new ArrayList<>();
        projectsAdapter = new ProjectsAdapter(this, projectsList, this);
        listViewProjects.setAdapter(projectsAdapter);

        templateManager = new TemplateManager();

        // Set up click listeners for new buttons (removed from welcome card)
        // buttonNewProject and buttonImportProject are now null since buttons were removed
        
        // Additional button listeners
        MaterialButton buttonRefreshProjects = findViewById(R.id.button_refresh_projects);
        if (buttonRefreshProjects != null) {
            buttonRefreshProjects.setOnClickListener(v -> refreshProjectsList());
        }
        
        // Removed button_create_first_project as per requirements

        // Extended FAB click listener
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabQuickActions = findViewById(R.id.fab_quick_actions);
        if (fabQuickActions != null) {
            fabQuickActions.setOnClickListener(v -> showQuickActionsMenu());
        }

        listViewProjects.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, Object> project = projectsList.get(position);
            String projectPath = (String) project.get("path");
            String projectName = (String) project.get("name");
            if (projectPath != null && projectName != null) {
                openProject(projectPath, projectName);
            } else {
                Toast.makeText(this, getString(R.string.error_invalid_project_data), Toast.LENGTH_SHORT).show();
            }
        });

        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjectsList();
        updateEmptyStateVisibility();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu inflation removed to hide search icon as requested
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            } else {
                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE permission already granted.");
                loadProjectsList();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                Log.d(TAG, "READ/WRITE_EXTERNAL_STORAGE permissions already granted.");
                loadProjectsList();
            }
        }
    }

    private void showManageStoragePermissionDialog() {
        new MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
                .setTitle(getString(R.string.permission_required))
                .setMessage(getString(R.string.permission_required_message))
                .setPositiveButton(getString(R.string.grant), (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_CODE_MANAGE_EXTERNAL_STORAGE);
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.could_not_open_settings), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to open manage storage settings", e);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                    updateEmptyStateVisibility();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show();
                loadProjectsList();
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show();
            }
            updateEmptyStateVisibility();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, getString(R.string.all_files_access_granted), Toast.LENGTH_SHORT).show();
                    loadProjectsList();
                } else {
                    Toast.makeText(this, getString(R.string.all_files_access_denied), Toast.LENGTH_LONG).show();
                }
                updateEmptyStateVisibility();
            }
        } else if (requestCode == REQUEST_CODE_PICK_ZIP_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                handleImportZipFile(uri);
            } else {
                Toast.makeText(this, getString(R.string.failed_to_get_file_uri), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadProjectsList() {
        if (!hasStoragePermission()) {
            Log.w(TAG, "Cannot load projects: Storage permission not granted.");
            projectsList.clear();
            if (projectsAdapter != null) {
                projectsAdapter.notifyDataSetChanged();
            }
            updateEmptyStateVisibility();
            return;
        }

        // Sync from filesystem to get the single source of truth.
        syncProjectsFromFilesystem();

        // Sort by last modified timestamp (most recent first)
        Collections.sort(projectsList, (p1, p2) -> {
            Number timestamp1 = (Number) p1.getOrDefault("lastModifiedTimestamp", 0L);
            Number timestamp2 = (Number) p2.getOrDefault("lastModifiedTimestamp", 0L);
            long date1 = timestamp1.longValue();
            long date2 = timestamp2.longValue();
            return Long.compare(date2, date1);
        });
        
        if (projectsAdapter != null) {
            projectsAdapter.notifyDataSetChanged();
        }
        updateEmptyStateVisibility();
    }

    private void saveProjectsList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(projectsList);
        editor.putString(PROJECTS_LIST_KEY, json);
        editor.apply();
        updateEmptyStateVisibility();
    }

    private void updateEmptyStateVisibility() {
        if (projectsList.isEmpty() && hasStoragePermission()) {
            listViewProjects.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            // Text is now set in layout XML as requested
        } else if (!hasStoragePermission()) {
            listViewProjects.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textEmptyProjects.setText(getString(R.string.storage_permission_required));
        }
        else {
            listViewProjects.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void showNewProjectDialog() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, getString(R.string.please_grant_storage_permission), Toast.LENGTH_LONG).show();
            checkAndRequestPermissions();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null);
        TextInputEditText editTextProjectName = dialogView.findViewById(R.id.edittext_project_name);

        final String[] selectedTemplate = {"blank"};


        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
                .setTitle(getString(R.string.create_new_project))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.create), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String projectName = editTextProjectName.getText().toString().trim();

                if (projectName.isEmpty()) {
                    editTextProjectName.setError(getString(R.string.project_name_cannot_be_empty));
                    return;
                }

                if (projectName.contains("/") || projectName.contains("\\") || projectName.contains(":") ||
                        projectName.contains("*") || projectName.contains("?") || projectName.contains("\"") ||
                        projectName.contains("<") || projectName.contains(">") || projectName.contains("|")) {
                    editTextProjectName.setError(getString(R.string.project_name_contains_invalid_characters));
                    return;
                }

                File projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
                if (!projectsDir.exists()) {
                    projectsDir.mkdirs();
                }

                File newProjectDir = new File(projectsDir, projectName);
                if (newProjectDir.exists()) {
                    editTextProjectName.setError(getString(R.string.project_with_this_name_already_exists));
                    return;
                }

                try {
                    if (!newProjectDir.mkdirs()) {
                        throw new IOException(getString(R.string.failed_to_create_project_directory));
                    }

                    createTemplateFiles(newProjectDir, projectName, selectedTemplate[0]);

                    long creationTime = System.currentTimeMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
                    String lastModifiedStr = sdf.format(new Date(creationTime));

                    HashMap<String, Object> newProject = new HashMap<>();
                    newProject.put("name", projectName);
                    newProject.put("path", newProjectDir.getAbsolutePath());
                    newProject.put("lastModified", lastModifiedStr);
                    newProject.put("lastModifiedTimestamp", creationTime);

                    projectsList.add(0, newProject);
                    saveProjectsList();
                    Toast.makeText(MainActivity.this, getString(R.string.project_created, projectName), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    openProject(newProjectDir.getAbsolutePath(), projectName);
                } catch (IOException e) {
                    Log.e(TAG, "Error creating new project", e);
                    Toast.makeText(MainActivity.this, getString(R.string.error_creating_project, e.getMessage()), Toast.LENGTH_LONG).show();
                    if (newProjectDir.exists()) {
                        deleteRecursive(newProjectDir);
                    }
                }
            });
        });
        dialog.show();
    }

    private void createTemplateFiles(File projectDir, String projectName, String templateType) throws IOException {
        FileManager fileManager = new FileManager(this, projectDir);

        if ("blank".equals(templateType)) {
            // Do nothing for a truly blank project
        } else if ("basic".equals(templateType)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getBasicHtmlTemplate(projectName));
            fileManager.writeFileContent(new File(projectDir, "style.css"),
                    templateManager.getBasicCssTemplate());
            fileManager.writeFileContent(new File(projectDir, "script.js"),
                    templateManager.getBasicJsTemplate());
        } else if ("responsive".equals(templateType)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getResponsiveHtmlTemplate(projectName));
            fileManager.writeFileContent(new File(projectDir, "style.css"),
                    templateManager.getResponsiveCssTemplate());
            fileManager.writeFileContent(new File(projectDir, "script.js"),
                    templateManager.getResponsiveJsTemplate());
        }
    }

    public void openProject(String projectPath, String projectName) {
        if (!hasStoragePermission()) {
            Toast.makeText(this, getString(R.string.storage_permission_required_to_open_projects), Toast.LENGTH_LONG).show();
            checkAndRequestPermissions();
            return;
        }
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
        intent.putExtra("projectPath", projectPath);
        intent.putExtra("projectName", projectName);
        startActivity(intent);
    }

    public void deleteProjectDirectory(File projectDir) {
        new Thread(() -> {
            if (!hasStoragePermission()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.storage_permission_required_to_delete_projects), Toast.LENGTH_LONG).show();
                    checkAndRequestPermissions();
                });
                return;
            }
            boolean deleted = deleteRecursive(projectDir);
            runOnUiThread(() -> {
                if (deleted) {
                    projectsList.removeIf(project -> projectDir.getAbsolutePath().equals(project.get("path")));
                    saveProjectsList();
                    Toast.makeText(this, getString(R.string.project_deleted), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.failed_to_delete_project), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * Renames a file or directory on the file system and updates the projects list.
     * @param oldFile The old file or directory.
     * @param newFile The new file or directory.
     * @throws IOException If the rename operation fails.
     */
    public void renameFileOrDir(File oldFile, File newFile) {
        new Thread(() -> {
            try {
                if (!hasStoragePermission()) {
                    throw new IOException(getString(R.string.storage_permission_not_granted_cannot_rename));
                }
                if (!oldFile.exists()) {
                    throw new IOException(getString(R.string.original_file_directory_does_not_exist, oldFile.getAbsolutePath()));
                }
                if (newFile.exists()) {
                    throw new IOException(getString(R.string.file_directory_with_new_name_already_exists, newFile.getAbsolutePath()));
                }

                if (oldFile.renameTo(newFile)) {
                    Log.d(TAG, "Renamed " + oldFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
                    // Update the projectsList entry with the new path and name
                    for (HashMap<String, Object> project : projectsList) {
                        if (oldFile.getAbsolutePath().equals(project.get("path"))) {
                            project.put("name", newFile.getName());
                            project.put("path", newFile.getAbsolutePath());
                            project.put("lastModified", new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault()).format(new Date()));
                            project.put("lastModifiedTimestamp", System.currentTimeMillis());
                            break;
                        }
                    }
                    runOnUiThread(() -> {
                        saveProjectsList(); // Save the updated project list
                        // Also update chat history key if the project was renamed
                        // This is a bit more complex as AIChatFragment manages its own SharedPreferences key.
                        // A simpler approach for now is to rely on the projectPath being updated in the project list
                        // and the AIChatFragment's loadChatHistoryFromPrefs logic to handle migration if needed.
                        // For a robust solution, you might need a dedicated method in AIChatFragment to update its key.
                    });
                } else {
                    throw new IOException(getString(R.string.failed_to_rename, oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, getString(R.string.failed_to_rename, oldFile.getName(), newFile.getName()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    /**
     * Initiates the project export process.
     * This will zip the project directory and its associated chat history.
     * @param projectDir The directory of the project to export.
     * @param projectName The name of the project.
     */
    public void exportProject(File projectDir, String projectName) {
        new Thread(() -> {
            if (!hasStoragePermission()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.storage_permission_required_to_export_projects), Toast.LENGTH_LONG).show();
                    checkAndRequestPermissions();
                });
                return;
            }

            File exportDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
                Log.d(TAG, "Created export directory: " + exportDir.getAbsolutePath());
            }

            File zipFile = new File(exportDir, projectName + ".codex"); // Use .codex extension
            File chatHistoryFile = new File(projectDir, CHAT_HISTORY_FILE_NAME); // Temporary file for chat history
            boolean chatExportedSuccessfully = false;

            Log.d(TAG, "Starting export for project: " + projectName);
            Log.d(TAG, "Project directory: " + projectDir.getAbsolutePath());
            Log.d(TAG, "Target zip file: " + zipFile.getAbsolutePath());

            try {
                // 1. Export chat history to a temporary file within the project directory
                chatExportedSuccessfully = AIChatFragment.exportChatHistoryToJson(this, projectDir.getAbsolutePath(), chatHistoryFile);
                if (chatExportedSuccessfully) {
                    Log.d(TAG, "Chat history exported temporarily to: " + chatHistoryFile.getAbsolutePath());
                } else {
                    Log.d(TAG, "No chat history to export or failed to export chat history.");
                }

                // 2. Zip the project directory including the chat history file
                Log.d(TAG, "Calling zipDirectory for " + projectDir.getAbsolutePath());
                zipDirectory(projectDir, zipFile);
                Log.d(TAG, "zipDirectory completed for " + zipFile.getAbsolutePath());

                // Verify zip file creation
                if (zipFile.exists() && zipFile.length() > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.project_exported_to, projectName, zipFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Zip file created successfully. Size: " + zipFile.length() + " bytes.");
                        // 3. Share the zipped file
                        shareFile(zipFile);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.failed_to_create_exported_project_file), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Zip file was not created or is empty.");
                    });
                }

            } catch (IOException e) {
                Log.e(TAG, "Error exporting project: " + projectName, e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.failed_to_export_project, e.getMessage()), Toast.LENGTH_LONG).show());
            } finally {
                // 4. Clean up the temporary chat history file
                if (chatHistoryFile.exists() && chatExportedSuccessfully) {
                    boolean deleted = chatHistoryFile.delete();
                    Log.d(TAG, "Temporary chat history file deleted: " + deleted);
                }
            }
        }).start();
    }

    /**
     * Zips a directory into a single file.
     * @param directory The directory to zip.
     * @param zipFile The output zip file.
     * @throws IOException If an I/O error occurs.
     */
    private void zipDirectory(File directory, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            zipFile(directory, directory, zos);
            // zos.close() is handled by try-with-resources
            Log.d(TAG, "ZipOutputStream closed successfully.");
        }
    }

    /**
     * Recursively zips files and directories.
     * @param rootDir The root directory being zipped.
     * @param sourceFile The current file or directory to add to the zip.
     * @param zos The ZipOutputStream.
     * @throws IOException If an I/O error occurs.
     */
    private void zipFile(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        if (sourceFile.isDirectory()) {
            Log.d(TAG, "Processing directory: " + sourceFile.getAbsolutePath());
            File[] files = sourceFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    zipFile(rootDir, file, zos); // Recursive call
                }
            } else {
                Log.d(TAG, "Directory is empty or inaccessible: " + sourceFile.getAbsolutePath());
            }
        } else {
            Log.d(TAG, "Processing file: " + sourceFile.getAbsolutePath());
            String relativePath = rootDir.toURI().relativize(sourceFile.toURI()).getPath();
            ZipEntry entry = new ZipEntry(relativePath);
            zos.putNextEntry(entry);
            Log.d(TAG, "Added ZipEntry: " + relativePath);

            try (FileInputStream fis = new FileInputStream(sourceFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                int count;
                while ((count = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, count);
                }
                Log.d(TAG, "Wrote " + sourceFile.length() + " bytes for " + relativePath);
            } // fis and bis are automatically closed here by try-with-resources
            zos.closeEntry();
            Log.d(TAG, "Closed ZipEntry: " + relativePath);
        }
    }

    /**
     * Shares a file using an Intent.
     * @param file The file to share.
     */
    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip"); // Or application/x-codex if you register a custom MIME type
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Project"));
    }

    /**
     * Initiates the project import process by opening a file picker.
     */
    private void importProject() {
        if (!hasStoragePermission()) {
            Toast.makeText(this, getString(R.string.storage_permission_required_to_import_projects), Toast.LENGTH_LONG).show();
            checkAndRequestPermissions();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip"); // Filter for zip files
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_project_to_import)), REQUEST_CODE_PICK_ZIP_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.please_install_a_file_manager), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the selected zip file for import.
     * @param uri The URI of the selected zip file.
     */
    private void handleImportZipFile(Uri uri) {
        new Thread(() -> {
            try {
                File projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
                if (!projectsDir.exists()) {
                    projectsDir.mkdirs();
                }

                // Get the display name of the file to use as project name
                String fileName = getFileNameFromUri(uri);
                String projectNameStr = fileName.endsWith(".codex") ? fileName.substring(0, fileName.length() - ".codex".length()) :
                        fileName.endsWith(".zip") ? fileName.substring(0, fileName.length() - ".zip".length()) :
                                fileName;
                final String projectName = projectNameStr.replaceAll("[^a-zA-Z0-9_.-]", "_"); // Sanitize project name

                File newProjectDir = new File(projectsDir, projectName);
                if (newProjectDir.exists()) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.project_with_name_already_exists, projectName), Toast.LENGTH_LONG).show());
                    return;
                }

                // Unzip the file
                unzipFile(uri, newProjectDir);

                // After unzipping, check for chat history file and import it
                File importedChatHistoryFile = new File(newProjectDir, CHAT_HISTORY_FILE_NAME);
                if (importedChatHistoryFile.exists()) {
                    AIChatFragment.importChatHistoryFromJson(this, newProjectDir.getAbsolutePath(), importedChatHistoryFile);
                    importedChatHistoryFile.delete(); // Delete the temporary chat history file
                }

                long creationTime = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
                String lastModifiedStr = sdf.format(new Date(creationTime));

                HashMap<String, Object> newProject = new HashMap<>();
                newProject.put("name", projectName);
                newProject.put("path", newProjectDir.getAbsolutePath());
                newProject.put("lastModified", lastModifiedStr);
                newProject.put("lastModifiedTimestamp", creationTime);

                String finalProjectName = projectName;
                runOnUiThread(() -> {
                    projectsList.add(0, newProject);
                    saveProjectsList();
                    Toast.makeText(this, getString(R.string.project_imported_successfully, finalProjectName), Toast.LENGTH_SHORT).show();
                    openProject(newProjectDir.getAbsolutePath(), finalProjectName);
                });

            } catch (IOException e) {
                Log.e(TAG, "Error importing project", e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.failed_to_import_project, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Unzips a file from a URI to a target directory.
     * @param zipFileUri The URI of the zip file.
     * @param targetDirectory The directory to extract files to.
     * @throws IOException If an I/O error occurs.
     */
    private void unzipFile(Uri zipFileUri, File targetDirectory) throws IOException {
        try (InputStream is = getContentResolver().openInputStream(zipFileUri);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                // Security check: Prevent Zip Path Traversal Vulnerability
                String canonicalPath = file.getCanonicalPath();
                String canonicalTargetDirPath = targetDirectory.getCanonicalPath();
                if (!canonicalPath.startsWith(canonicalTargetDirPath)) {
                    throw new IOException(getString(R.string.zip_entry_points_outside_target_directory, ze.getName()));
                }

                if (ze.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(file);
                         BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
                        while ((count = zis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, count);
                        }
                        bos.flush();
                    } // fos and bos are automatically closed here
                }
                zis.closeEntry();
            }
        } // is and zis are automatically closed here
    }

    /**
     * Gets the file name from a content URI.
     * @param uri The content URI.
     * @return The display name of the file.
     */
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    
    /**
     * Refresh the projects list with animation
     */
    private void refreshProjectsList() {
        loadProjectsList();
        updateEmptyStateVisibility();
        Toast.makeText(this, getString(R.string.projects_refreshed), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show quick actions menu with modern options
     */
    private void showQuickActionsMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_quick_actions, null);
        
        // Quick action buttons
        view.findViewById(R.id.action_new_project).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showNewProjectDialog();
        });
        
        view.findViewById(R.id.action_import_project).setOnClickListener(v -> {
            bottomSheet.dismiss();
            importProject();
        });
        
        view.findViewById(R.id.action_open_settings).setOnClickListener(v -> {
            bottomSheet.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        
        view.findViewById(R.id.action_open_about).setOnClickListener(v -> {
            bottomSheet.dismiss();
            startActivity(new Intent(this, AboutActivity.class));
        });

        bottomSheet.setContentView(view);
        bottomSheet.show();
    }

    private void migrateOldProjects() {
        File oldProjectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX_Projects");
        File newProjectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");

        if (oldProjectsDir.exists() && oldProjectsDir.isDirectory()) {
            if (!newProjectsDir.exists()) {
                newProjectsDir.mkdirs();
            }
            File[] projects = oldProjectsDir.listFiles();
            if (projects != null) {
                for (File project : projects) {
                    File newProject = new File(newProjectsDir, project.getName());
                    if (!newProject.exists()) {
                        project.renameTo(newProject);
                    }
                }
            }
            deleteRecursive(oldProjectsDir);
        }
    }

    private void syncProjectsFromFilesystem() {
        if (!hasStoragePermission()) {
            return;
        }
        File projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
        if (!projectsDir.exists() || !projectsDir.isDirectory()) {
            return;
        }

        ArrayList<HashMap<String, Object>> filesystemProjects = new ArrayList<>();
        File[] projectDirs = projectsDir.listFiles(File::isDirectory);

        if (projectDirs != null) {
            for (File projectDir : projectDirs) {
                long lastModified = projectDir.lastModified();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault());
                String lastModifiedStr = sdf.format(new Date(lastModified));

                HashMap<String, Object> project = new HashMap<>();
                project.put("name", projectDir.getName());
                project.put("path", projectDir.getAbsolutePath());
                project.put("lastModified", lastModifiedStr);
                project.put("lastModifiedTimestamp", lastModified);
                filesystemProjects.add(project);
            }
        }

        // Create a map of existing projects by path
        Map<String, HashMap<String, Object>> existingProjectsMap = new HashMap<>();
        for (HashMap<String, Object> project : projectsList) {
            String path = (String) project.get("path");
            if (path != null) {
                existingProjectsMap.put(path, project);
            }
        }
        
        // Clear and rebuild the list with filesystem projects
        projectsList.clear();
        projectsList.addAll(filesystemProjects);
        
        // Save the updated list
        saveProjectsList();
    }
}
