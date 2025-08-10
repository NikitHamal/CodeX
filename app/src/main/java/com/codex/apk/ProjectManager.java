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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

                // Get selected project type
                int selectedProjectTypeId = projectTypeChipGroup.getCheckedChipId();
                String projectType = "empty";
                if (selectedProjectTypeId == R.id.chip_html_css_js) {
                    projectType = "html_css_js";
                } else if (selectedProjectTypeId == R.id.chip_react) {
                    projectType = "react";
                } else if (selectedProjectTypeId == R.id.chip_nextjs) {
                    projectType = "nextjs";
                } else if (selectedProjectTypeId == R.id.chip_vue) {
                    projectType = "vue";
                } else if (selectedProjectTypeId == R.id.chip_angular) {
                    projectType = "angular";
                } else if (selectedProjectTypeId == R.id.chip_node) {
                    projectType = "node";
                } else if (selectedProjectTypeId == R.id.chip_python) {
                    projectType = "python";
                } else if (selectedProjectTypeId == R.id.chip_php) {
                    projectType = "php";
                } else if (selectedProjectTypeId == R.id.chip_tailwind) {
                    projectType = "tailwind";
                } else if (selectedProjectTypeId == R.id.chip_bootstrap) {
                    projectType = "bootstrap";
                } else if (selectedProjectTypeId == R.id.chip_material_ui) {
                    projectType = "material_ui";
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
                // Create empty project with no files
                break;
                
            case "html_css_js":
                createHtmlCssJsProject(projectDir, projectName, templateStyle, fileManager);
                break;
                
            case "react":
                createReactProject(projectDir, projectName, fileManager);
                break;
                
            case "nextjs":
                createNextJsProject(projectDir, projectName, fileManager);
                break;
                
            case "vue":
                createVueProject(projectDir, projectName, fileManager);
                break;
                
            case "angular":
                createAngularProject(projectDir, projectName, fileManager);
                break;
                
            case "node":
                createNodeProject(projectDir, projectName, fileManager);
                break;
                
            case "python":
                createPythonProject(projectDir, projectName, fileManager);
                break;
                
            case "php":
                createPhpProject(projectDir, projectName, fileManager);
                break;
                
            case "tailwind":
                createTailwindProject(projectDir, projectName, templateStyle, fileManager);
                break;
                
            case "bootstrap":
                createBootstrapProject(projectDir, projectName, templateStyle, fileManager);
                break;
                
            case "material_ui":
                createMaterialUiProject(projectDir, projectName, fileManager);
                break;
                
            default:
                // Fallback to basic HTML/CSS/JS
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
    
    private void createReactProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create src directory
        File srcDir = new File(projectDir, "src");
        srcDir.mkdirs();
        
        // Create public directory
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                templateManager.getPackageJsonTemplate(projectName, "react"));
        
        // Create React app files
        fileManager.writeFileContent(new File(srcDir, "App.js"),
                templateManager.getReactAppTemplate(projectName));
        fileManager.writeFileContent(new File(srcDir, "index.js"),
                "import React from 'react';\nimport ReactDOM from 'react-dom';\nimport './index.css';\nimport App from './App';\n\nReactDOM.render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>,\n  document.getElementById('root')\n);");
        fileManager.writeFileContent(new File(srcDir, "index.css"),
                "body {\n  margin: 0;\n  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',\n    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',\n    sans-serif;\n  -webkit-font-smoothing: antialiased;\n  -moz-osx-font-smoothing: grayscale;\n}\n\ncode {\n  font-family: source-code-pro, Menlo, Monaco, Consolas, 'Courier New',\n    monospace;\n}");
        fileManager.writeFileContent(new File(srcDir, "App.css"),
                ".App {\n  text-align: center;\n}\n\n.App-header {\n  background-color: #282c34;\n  padding: 20px;\n  color: white;\n}\n\n.App-header h1 {\n  margin: 0;\n  font-size: 2rem;\n}\n\n.App-header p {\n  margin: 10px 0 0 0;\n  font-size: 1.2rem;\n}");
        
        // Create public/index.html
        fileManager.writeFileContent(new File(publicDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\" />\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n    <title>" + projectName + "</title>\n  </head>\n  <body>\n    <noscript>You need to enable JavaScript to run this app.</noscript>\n    <div id=\"root\"></div>\n  </body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis project was created with CodeX.\n\n## Available Scripts\n\nIn the project directory, you can run:\n\n### `npm start`\nRuns the app in the development mode.\n\n### `npm run build`\nBuilds the app for production.\n\n### `npm test`\nLaunches the test runner.\n\n### `npm run eject`\n**Note: this is a one-way operation. Once you `eject`, you can't go back!**");
    }
    
    private void createNextJsProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create pages directory
        File pagesDir = new File(projectDir, "pages");
        pagesDir.mkdirs();
        
        // Create styles directory
        File stylesDir = new File(projectDir, "styles");
        stylesDir.mkdirs();
        
        // Create public directory
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                templateManager.getPackageJsonTemplate(projectName, "nextjs"));
        
        // Create Next.js config
        fileManager.writeFileContent(new File(projectDir, "next.config.js"),
                templateManager.getNextConfigTemplate());
        
        // Create pages/index.js
        fileManager.writeFileContent(new File(pagesDir, "index.js"),
                templateManager.getNextJsAppTemplate(projectName));
        
        // Create styles/Home.module.css
        fileManager.writeFileContent(new File(stylesDir, "Home.module.css"),
                ".container {\n  min-height: 100vh;\n  padding: 0 0.5rem;\n  display: flex;\n  flex-direction: column;\n  justify-content: center;\n  align-items: center;\n}\n\n.main {\n  padding: 5rem 0;\n  flex: 1;\n  display: flex;\n  flex-direction: column;\n  justify-content: center;\n  align-items: center;\n}\n\n.title {\n  margin: 0;\n  line-height: 1.15;\n  font-size: 4rem;\n  text-align: center;\n}\n\n.description {\n  text-align: center;\n  line-height: 1.5;\n  font-size: 1.5rem;\n}");
        
        // Create public/favicon.ico placeholder
        fileManager.writeFileContent(new File(publicDir, "favicon.ico"),
                "");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a [Next.js](https://nextjs.org/) project created with CodeX.\n\n## Getting Started\n\nFirst, run the development server:\n\n```bash\nnpm run dev\n# or\nyarn dev\n```\n\nOpen [http://localhost:3000](http://localhost:3000) with your browser to see the result.");
    }
    
    private void createVueProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create src directory
        File srcDir = new File(projectDir, "src");
        srcDir.mkdirs();
        
        // Create public directory
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                templateManager.getPackageJsonTemplate(projectName, "vue"));
        
        // Create Vue config
        fileManager.writeFileContent(new File(projectDir, "vue.config.js"),
                templateManager.getVueConfigTemplate());
        
        // Create src/App.vue
        fileManager.writeFileContent(new File(srcDir, "App.vue"),
                templateManager.getVueAppTemplate(projectName));
        
        // Create src/main.js
        fileManager.writeFileContent(new File(srcDir, "main.js"),
                "import { createApp } from 'vue'\nimport App from './App.vue'\n\ncreateApp(App).mount('#app')");
        
        // Create public/index.html
        fileManager.writeFileContent(new File(publicDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\">\n    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n    <meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">\n    <title>" + projectName + "</title>\n  </head>\n  <body>\n    <noscript>\n      <strong>We're sorry but " + projectName + " doesn't work properly without JavaScript enabled. Please enable it to continue.</strong>\n    </noscript>\n    <div id=\"app\"></div>\n  </body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Vue.js project created with CodeX.\n\n## Project setup\n```\nnpm install\n```\n\n### Compiles and hot-reloads for development\n```\nnpm run serve\n```\n\n### Compiles and minifies for production\n```\nnpm run build\n```");
    }
    
    private void createAngularProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create src directory
        File srcDir = new File(projectDir, "src");
        srcDir.mkdirs();
        
        // Create src/app directory
        File appDir = new File(srcDir, "app");
        appDir.mkdirs();
        
        // Create angular.json
        fileManager.writeFileContent(new File(projectDir, "angular.json"),
                templateManager.getAngularConfigTemplate(projectName));
        
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                templateManager.getPackageJsonTemplate(projectName, "angular"));
        
        // Create src/app/app.component.ts
        fileManager.writeFileContent(new File(appDir, "app.component.ts"),
                templateManager.getAngularAppTemplate(projectName));
        
        // Create src/main.ts
        fileManager.writeFileContent(new File(srcDir, "main.ts"),
                "import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';\nimport { AppModule } from './app/app.module';\n\nplatformBrowserDynamic().bootstrapModule(AppModule)\n  .catch(err => console.log(err));");
        
        // Create src/app/app.module.ts
        fileManager.writeFileContent(new File(srcDir, "app/app.module.ts"),
                "import { NgModule } from '@angular/core';\nimport { BrowserModule } from '@angular/platform-browser';\nimport { AppComponent } from './app.component';\n\n@NgModule({\n  declarations: [\n    AppComponent\n  ],\n  imports: [\n    BrowserModule\n  ],\n  providers: [],\n  bootstrap: [AppComponent]\n})\nexport class AppModule { }");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is an Angular project created with CodeX.\n\n## Development server\n\nRun `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.\n\n## Build\n\nRun `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.");
    }
    
    private void createNodeProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                templateManager.getPackageJsonTemplate(projectName, "node"));
        
        // Create app.js
        fileManager.writeFileContent(new File(projectDir, "app.js"),
                templateManager.getNodeBackendTemplate(projectName));
        
        // Create public directory for static files
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create public/index.html
        fileManager.writeFileContent(new File(publicDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>" + projectName + " API</title>\n</head>\n<body>\n    <h1>" + projectName + " API</h1>\n    <p>Backend server is running. Check the console for server details.</p>\n</body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Node.js backend project created with CodeX.\n\n## Getting Started\n\n1. Install dependencies:\n```bash\nnpm install\n```\n\n2. Start the server:\n```bash\nnpm start\n```\n\n3. For development with auto-restart:\n```bash\nnpm run dev\n```\n\nThe server will run on port 3000 by default.");
    }
    
    private void createPythonProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create requirements.txt
        fileManager.writeFileContent(new File(projectDir, "requirements.txt"),
                templateManager.getRequirementsTxtTemplate());
        
        // Create app.py
        fileManager.writeFileContent(new File(projectDir, "app.py"),
                templateManager.getPythonBackendTemplate(projectName));
        
        // Create static directory
        File staticDir = new File(projectDir, "static");
        staticDir.mkdirs();
        
        // Create templates directory
        File templatesDir = new File(projectDir, "templates");
        templatesDir.mkdirs();
        
        // Create static/index.html
        fileManager.writeFileContent(new File(staticDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>" + projectName + " API</title>\n</head>\n<body>\n    <h1>" + projectName + " API</h1>\n    <p>Python Flask backend is running. Check the console for server details.</p>\n</body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Python Flask backend project created with CodeX.\n\n## Getting Started\n\n1. Install dependencies:\n```bash\npip install -r requirements.txt\n```\n\n2. Run the application:\n```bash\npython app.py\n```\n\nThe server will run on port 5000 by default.");
    }
    
    private void createPhpProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create composer.json
        fileManager.writeFileContent(new File(projectDir, "composer.json"),
                templateManager.getComposerJsonTemplate(projectName));
        
        // Create index.php
        fileManager.writeFileContent(new File(projectDir, "index.php"),
                templateManager.getPhpBackendTemplate(projectName));
        
        // Create public directory
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create public/index.html
        fileManager.writeFileContent(new File(publicDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>" + projectName + " API</title>\n</head>\n<body>\n    <h1>" + projectName + " API</title>\n    <p>PHP backend is running. Check the console for server details.</p>\n</body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a PHP backend project created with CodeX.\n\n## Getting Started\n\n1. Install dependencies (if using Composer):\n```bash\ncomposer install\n```\n\n2. Run with a PHP server:\n```bash\nphp -S localhost:8000\n```\n\nThe server will run on port 8000 by default.");
    }
    
    private void createTailwindProject(File projectDir, String projectName, String templateStyle, FileManager fileManager) throws IOException {
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                "{\n  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Tailwind CSS project created with CodeX\",\n  \"scripts\": {\n    \"build\": \"tailwindcss -i ./src/input.css -o ./dist/output.css --watch\"\n  },\n  \"devDependencies\": {\n    \"tailwindcss\": \"^3.3.0\"\n  }\n}");
        
        // Create tailwind.config.js
        fileManager.writeFileContent(new File(projectDir, "tailwind.config.js"),
                templateManager.getTailwindConfigTemplate());
        
        // Create src directory
        File srcDir = new File(projectDir, "src");
        srcDir.mkdirs();
        
        // Create dist directory
        File distDir = new File(projectDir, "dist");
        distDir.mkdirs();
        
        // Create src/input.css
        fileManager.writeFileContent(new File(srcDir, "input.css"),
                templateManager.getTailwindCssTemplate());
        
        // Create dist/output.css (initial build)
        fileManager.writeFileContent(new File(distDir, "output.css"),
                templateManager.getTailwindCssTemplate());
        
        // Create index.html
        if ("basic".equals(templateStyle)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getBasicHtmlTemplate(projectName));
        } else {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    templateManager.getResponsiveHtmlTemplate(projectName));
        }
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Tailwind CSS project created with CodeX.\n\n## Getting Started\n\n1. Install dependencies:\n```bash\nnpm install\n```\n\n2. Build CSS:\n```bash\nnpm run build\n```\n\nThis will watch for changes and rebuild the CSS automatically.");
    }
    
    private void createBootstrapProject(File projectDir, String projectName, String templateStyle, FileManager fileManager) throws IOException {
        // Create index.html
        if ("basic".equals(templateStyle)) {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>" + projectName + "</title>\n    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n</head>\n<body>\n    <div class=\"container mt-5\">\n        <h1 class=\"text-primary\">" + projectName + "</h1>\n        <p class=\"lead\">Welcome to your Bootstrap project!</p>\n        <button class=\"btn btn-primary\" id=\"clickMe\">Click Me!</button>\n    </div>\n    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n    <script src=\"script.js\"></script>\n</body>\n</html>");
        } else {
            fileManager.writeFileContent(new File(projectDir, "index.html"),
                    "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>" + projectName + "</title>\n    <link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">\n</head>\n<body>\n    <nav class=\"navbar navbar-expand-lg navbar-dark bg-primary\">\n        <div class=\"container\">\n            <a class=\"navbar-brand\" href=\"#\">" + projectName + "</a>\n            <button class=\"navbar-toggler\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#navbarNav\">\n                <span class=\"navbar-toggler-icon\"></span>\n            </button>\n            <div class=\"collapse navbar-collapse\" id=\"navbarNav\">\n                <ul class=\"navbar-nav ms-auto\">\n                    <li class=\"nav-item\">\n                        <a class=\"nav-link active\" href=\"#\">Home</a>\n                    </li>\n                    <li class=\"nav-item\">\n                        <a class=\"nav-link\" href=\"#\">About</a>\n                    </li>\n                    <li class=\"nav-item\">\n                        <a class=\"nav-link\" href=\"#\">Contact</a>\n                    </li>\n                </ul>\n            </div>\n        </div>\n    </nav>\n    <div class=\"container mt-5\">\n        <div class=\"row\">\n            <div class=\"col-md-8 mx-auto text-center\">\n                <h1 class=\"display-4\">Welcome to " + projectName + "</h1>\n                <p class=\"lead\">A responsive Bootstrap website template.</p>\n                <button class=\"btn btn-primary btn-lg\">Get Started</button>\n            </div>\n        </div>\n    </div>\n    <script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js\"></script>\n    <script src=\"script.js\"></script>\n</body>\n</html>");
        }
        
        // Create script.js
        fileManager.writeFileContent(new File(projectDir, "script.js"),
                "// Bootstrap project JavaScript\nconsole.log('Bootstrap project loaded!');");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Bootstrap project created with CodeX.\n\n## Getting Started\n\nOpen `index.html` in your browser to view the project.\n\n## Features\n\n- Responsive design using Bootstrap 5\n- Mobile-first approach\n- Modern UI components");
    }
    
    private void createMaterialUiProject(File projectDir, String projectName, FileManager fileManager) throws IOException {
        // Create package.json
        fileManager.writeFileContent(new File(projectDir, "package.json"),
                "{\n  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n  \"version\": \"1.0.0\",\n  \"description\": \"Material-UI project created with CodeX\",\n  \"scripts\": {\n    \"start\": \"react-scripts start\",\n    \"build\": \"react-scripts build\"\n  },\n  \"dependencies\": {\n    \"react\": \"^18.2.0\",\n    \"react-dom\": \"^18.2.0\",\n    \"@mui/material\": \"^5.14.0\",\n    \"@emotion/react\": \"^11.11.0\",\n    \"@emotion/styled\": \"^11.11.0\",\n    \"react-scripts\": \"5.0.1\"\n  }\n}");
        
        // Create src directory
        File srcDir = new File(projectDir, "src");
        srcDir.mkdirs();
        
        // Create public directory
        File publicDir = new File(projectDir, "public");
        publicDir.mkdirs();
        
        // Create src/App.js
        fileManager.writeFileContent(new File(srcDir, "App.js"),
                "import React from 'react';\nimport { ThemeProvider, createTheme } from '@mui/material/styles';\nimport { CssBaseline, Container, Typography, Button, Box } from '@mui/material';\nimport theme from './theme';\n\nfunction App() {\n  return (\n    <ThemeProvider theme={theme}>\n      <CssBaseline />\n      <Container maxWidth=\"md\">\n        <Box sx={{ mt: 8, textAlign: 'center' }}>\n          <Typography variant=\"h2\" component=\"h1\" gutterBottom>\n            " + projectName + "\n          </Typography>\n          <Typography variant=\"h5\" component=\"h2\" gutterBottom color=\"text.secondary\">\n            Welcome to your Material-UI project!\n          </Typography>\n          <Button variant=\"contained\" size=\"large\" sx={{ mt: 3 }}>\n            Get Started\n          </Button>\n        </Box>\n      </Container>\n    </ThemeProvider>\n  );\n}\n\nexport default App;");
        
        // Create src/theme.js
        fileManager.writeFileContent(new File(srcDir, "theme.js"),
                templateManager.getMaterialUiTemplate());
        
        // Create src/index.js
        fileManager.writeFileContent(new File(srcDir, "index.js"),
                "import React from 'react';\nimport ReactDOM from 'react-dom';\nimport App from './App';\n\nReactDOM.render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>,\n  document.getElementById('root')\n);");
        
        // Create public/index.html
        fileManager.writeFileContent(new File(publicDir, "index.html"),
                "<!DOCTYPE html>\n<html lang=\"en\">\n  <head>\n    <meta charset=\"utf-8\" />\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n    <title>" + projectName + "</title>\n  </head>\n  <body>\n    <noscript>You need to enable JavaScript to run this app.</noscript>\n    <div id=\"root\"></div>\n  </body>\n</html>");
        
        // Create README
        fileManager.writeFileContent(new File(projectDir, "README.md"),
                "# " + projectName + "\n\nThis is a Material-UI React project created with CodeX.\n\n## Getting Started\n\n1. Install dependencies:\n```bash\nnpm install\n```\n\n2. Start the development server:\n```bash\nnpm start\n```\n\nThe app will open in your browser at `http://localhost:3000`.");
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
