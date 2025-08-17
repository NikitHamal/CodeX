package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ProjectManager {

    private static final String TAG = "ProjectManager";
    private static final String PREFS_NAME = "project_prefs";
    private static final String PROJECTS_LIST_KEY = "projects_list";

    private final MainActivity mainActivity;
    private final Context context;
    private ArrayList<HashMap<String, Object>> projectsList;
    private ProjectsAdapter projectsAdapter;
    private final TemplateManager templateManager;

    public ProjectManager(MainActivity mainActivity, ArrayList<HashMap<String, Object>> projectsList, ProjectsAdapter projectsAdapter) {
        this.mainActivity = mainActivity;
        this.context = mainActivity.getApplicationContext();
        this.projectsList = projectsList;
        this.projectsAdapter = projectsAdapter;
        this.templateManager = new TemplateManager();
    }

    public void loadProjectsList() {
        if (!mainActivity.getPermissionManager().hasStoragePermission()) {
            Log.w(TAG, "Cannot load projects: Storage permission not granted.");
            projectsList.clear();
            if (projectsAdapter != null) {
                projectsAdapter.notifyDataSetChanged();
            }
            mainActivity.updateEmptyStateVisibility();
            return;
        }

        syncProjectsFromFilesystem();

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
        mainActivity.updateEmptyStateVisibility();
    }

    public void saveProjectsList() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(projectsList);
        editor.putString(PROJECTS_LIST_KEY, json);
        editor.apply();
        mainActivity.updateEmptyStateVisibility();
    }

    public void deleteProjectDirectory(File projectDir) {
        new Thread(() -> {
            if (!mainActivity.getPermissionManager().hasStoragePermission()) {
                mainActivity.runOnUiThread(() -> {
                    Toast.makeText(context, context.getString(R.string.storage_permission_required_to_delete_projects), Toast.LENGTH_LONG).show();
                    mainActivity.getPermissionManager().checkAndRequestPermissions();
                });
                return;
            }
            String projectPath = projectDir.getAbsolutePath();
            boolean deleted = deleteRecursive(projectDir);
            if (deleted) {
                AIChatHistoryManager.deleteChatStateForProject(context, projectPath);
            }
            mainActivity.runOnUiThread(() -> {
                if (deleted) {
                    Toast.makeText(context, context.getString(R.string.project_deleted), Toast.LENGTH_SHORT).show();
                    loadProjectsList();
                } else {
                    Toast.makeText(context, context.getString(R.string.failed_to_delete_project), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    public void renameFileOrDir(File oldFile, File newFile) {
        new Thread(() -> {
            try {
                if (!mainActivity.getPermissionManager().hasStoragePermission()) {
                    throw new IOException(context.getString(R.string.storage_permission_not_granted_cannot_rename));
                }
                if (!oldFile.exists()) {
                    throw new IOException(context.getString(R.string.original_file_directory_does_not_exist, oldFile.getAbsolutePath()));
                }
                if (newFile.exists()) {
                    throw new IOException(context.getString(R.string.file_directory_with_new_name_already_exists, newFile.getAbsolutePath()));
                }

                if (oldFile.renameTo(newFile)) {
                    for (HashMap<String, Object> project : projectsList) {
                        if (oldFile.getAbsolutePath().equals(project.get("path"))) {
                            project.put("name", newFile.getName());
                            project.put("path", newFile.getAbsolutePath());
                            project.put("lastModified", new SimpleDateFormat("MMM dd,yyyy HH:mm", Locale.getDefault()).format(new Date()));
                            project.put("lastModifiedTimestamp", System.currentTimeMillis());
                            break;
                        }
                    }
                    mainActivity.runOnUiThread(() -> {
                        saveProjectsList();
                        loadProjectsList();
                    });
                } else {
                    throw new IOException(context.getString(R.string.failed_to_rename, oldFile.getAbsolutePath(), newFile.getAbsolutePath()));
                }
            } catch (IOException e) {
                mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.failed_to_rename, oldFile.getName(), newFile.getName()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void showNewProjectDialog() {
        if (!mainActivity.getPermissionManager().hasStoragePermission()) {
            Toast.makeText(context, context.getString(R.string.please_grant_storage_permission), Toast.LENGTH_LONG).show();
            mainActivity.getPermissionManager().checkAndRequestPermissions();
            return;
        }

        View dialogView = LayoutInflater.from(mainActivity).inflate(R.layout.dialog_new_project, null);
        TextInputEditText editTextProjectName = dialogView.findViewById(R.id.edittext_project_name);
        ChipGroup projectTypeChipGroup = dialogView.findViewById(R.id.chip_group_project_type);
        ChipGroup templateStyleChipGroup = dialogView.findViewById(R.id.chip_group_template_style);
        View templateStyleLabel = dialogView.findViewById(R.id.text_template_style_label);
        View templateStyleScroll = dialogView.findViewById(R.id.scroll_template_style);

        // Set up project type selection
        projectTypeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_empty) {
                templateStyleLabel.setVisibility(View.GONE);
                templateStyleScroll.setVisibility(View.GONE);
            } else {
                templateStyleLabel.setVisibility(View.VISIBLE);
                templateStyleScroll.setVisibility(View.VISIBLE);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(mainActivity, R.style.AlertDialogCustom)
                .setTitle(context.getString(R.string.create_new_project))
                .setView(dialogView)
                .setPositiveButton(context.getString(R.string.create), null)
                .setNegativeButton(context.getString(R.string.cancel), null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String projectName = editTextProjectName.getText().toString().trim();

                if (projectName.isEmpty()) {
                    editTextProjectName.setError(context.getString(R.string.project_name_cannot_be_empty));
                    return;
                }

                // Get selected project type (only: empty, html_css_js, tailwind)
                int selectedProjectTypeId = projectTypeChipGroup.getCheckedChipId();
                String projectType = "empty";
                if (selectedProjectTypeId == R.id.chip_html_css_js) {
                    projectType = "html_css_js";
                } else if (selectedProjectTypeId == R.id.chip_tailwind) {
                    projectType = "tailwind";
                }

                // Get selected template style
                String templateStyle = "basic";
                if (templateStyleLabel.getVisibility() == View.VISIBLE) {
                    int selectedTemplateStyleId = templateStyleChipGroup.getCheckedChipId();
                    if (selectedTemplateStyleId == R.id.chip_responsive) {
                        templateStyle = "responsive";
                    }
                }

                File projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
                if (!projectsDir.exists()) {
                    projectsDir.mkdirs();
                }

                File newProjectDir = new File(projectsDir, projectName);
                if (newProjectDir.exists()) {
                    editTextProjectName.setError(context.getString(R.string.project_with_this_name_already_exists));
                    return;
                }

                try {
                    if (!newProjectDir.mkdirs()) {
                        throw new IOException(context.getString(R.string.failed_to_create_project_directory));
                    }

                    createTemplateFiles(newProjectDir, projectName, projectType, templateStyle);

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
                    Toast.makeText(context, context.getString(R.string.project_created, projectName), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    mainActivity.openProject(newProjectDir.getAbsolutePath(), projectName);
                } catch (IOException e) {
                    if (newProjectDir.exists()) {
                        deleteRecursive(newProjectDir);
                    }
                    Toast.makeText(context, "Error creating project: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }

    private void syncProjectsFromFilesystem() {
        if (!mainActivity.getPermissionManager().hasStoragePermission()) {
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

        projectsList.clear();
        projectsList.addAll(filesystemProjects);

        saveProjectsList();
    }

    private void createTemplateFiles(File projectDir, String projectName, String projectType, String templateStyle) throws IOException {
        FileManager fileManager = new FileManager(context, projectDir);
        switch (projectType) {
            case "empty":
                // no files
                break;
            case "html_css_js":
                createHtmlCssJsProject(projectDir, projectName, templateStyle, fileManager);
                break;
            case "tailwind":
                createTailwindProject(projectDir, projectName, templateStyle, fileManager);
                break;
            default:
                createHtmlCssJsProject(projectDir, projectName, "basic", fileManager);
                break;
        }
    }
    
    private void createHtmlCssJsProject(File projectDir, String projectName, String templateStyle, FileManager fileManager) throws IOException {
        if ("basic".equals(templateStyle)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getBasicHtmlTemplate(projectName));
            fileManager.writeFileContent(new File(projectDir, "style.css"),
                    templateManager.getBasicCssTemplate());
            fileManager.writeFileContent(new File(projectDir, "script.js"),
                    templateManager.getBasicJsTemplate());
        } else if ("responsive".equals(templateStyle)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getResponsiveHtmlTemplate(projectName));
            fileManager.writeFileContent(new File(projectDir, "style.css"),
                    templateManager.getResponsiveCssTemplate());
            fileManager.writeFileContent(new File(projectDir, "script.js"),
                    templateManager.getResponsiveJsTemplate());
        }
    }
    
    private void createTailwindProject(File projectDir, String projectName, String templateStyle, FileManager fileManager) throws IOException {
        // Tailwind via CDN, single index.html and optional script.js
        fileManager.writeFileContent(new File(projectDir, "index.html"),
                templateManager.getTailwindCdnHtmlTemplate(projectName));
        fileManager.writeFileContent(new File(projectDir, "script.js"),
                "// Tailwind project JavaScript\nconsole.log('Tailwind project loaded!');");
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
}
