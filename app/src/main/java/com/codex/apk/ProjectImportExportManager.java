package com.codex.apk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import com.google.gson.Gson;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProjectImportExportManager {

    private static final String TAG = "ProjectImportExport";
    private static final String CHAT_HISTORY_FILE_NAME = "chat_history.json";
    public static final int REQUEST_CODE_PICK_ZIP_FILE = 103;

    private final MainActivity mainActivity;
    private final Context context;

    public ProjectImportExportManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.context = mainActivity.getApplicationContext();
    }

    public void exportProject(File projectDir, String projectName) {
        new Thread(() -> {
            if (!mainActivity.getPermissionManager().hasStoragePermission()) {
                mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.storage_permission_required_to_export_projects), Toast.LENGTH_LONG).show());
                return;
            }

            File exportDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File zipFile = new File(exportDir, projectName + ".codex");
            File chatHistoryFile = new File(projectDir, CHAT_HISTORY_FILE_NAME);

            AIChatHistoryManager chatHistoryManager = new AIChatHistoryManager(context, projectDir.getAbsolutePath());
            List<ChatMessage> chatHistory = new ArrayList<>();
            QwenConversationState qwenState = new QwenConversationState();
            chatHistoryManager.loadChatState(chatHistory, qwenState);

            boolean chatExported = false;
            if (!chatHistory.isEmpty()) {
                try (FileOutputStream fos = new FileOutputStream(chatHistoryFile);
                     java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, "UTF-8")) {
                    Gson gson = new Gson();
                    gson.toJson(chatHistory, osw);
                    chatExported = true;
                } catch (IOException e) {
                    Log.e(TAG, "Error exporting chat history", e);
                }
            }

            try {
                zipDirectory(projectDir, zipFile);
                if (zipFile.exists() && zipFile.length() > 0) {
                    mainActivity.runOnUiThread(() -> {
                        Toast.makeText(context, context.getString(R.string.project_exported_to, projectName, zipFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
                        shareFile(zipFile);
                    });
                } else {
                    mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.failed_to_create_exported_project_file), Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.failed_to_export_project, e.getMessage()), Toast.LENGTH_LONG).show());
            } finally {
                if (chatHistoryFile.exists() && chatExported) {
                    chatHistoryFile.delete();
                }
            }
        }).start();
    }

    public void importProject() {
        if (!mainActivity.getPermissionManager().hasStoragePermission()) {
            Toast.makeText(context, context.getString(R.string.storage_permission_required_to_import_projects), Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            mainActivity.startActivityForResult(Intent.createChooser(intent, context.getString(R.string.select_project_to_import)), REQUEST_CODE_PICK_ZIP_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, context.getString(R.string.please_install_a_file_manager), Toast.LENGTH_SHORT).show();
        }
    }

    public void handleImportZipFile(Uri uri) {
        new Thread(() -> {
            try {
                File projectsDir = new File(Environment.getExternalStorageDirectory(), "CodeX/Projects");
                String fileName = getFileNameFromUri(uri);
                String projectName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                File newProjectDir = new File(projectsDir, projectName);

                if (newProjectDir.exists()) {
                    mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.project_with_name_already_exists, projectName), Toast.LENGTH_LONG).show());
                    return;
                }

                unzipFile(uri, newProjectDir);

                File importedChatHistoryFile = new File(newProjectDir, CHAT_HISTORY_FILE_NAME);
                if (importedChatHistoryFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(importedChatHistoryFile);
                         java.io.InputStreamReader isr = new java.io.InputStreamReader(fis, "UTF-8")) {
                        Gson gson = new Gson();
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<ArrayList<ChatMessage>>() {}.getType();
                        List<ChatMessage> chatHistory = gson.fromJson(isr, listType);

                        if (chatHistory != null && !chatHistory.isEmpty()) {
                            AIChatHistoryManager chatHistoryManager = new AIChatHistoryManager(context, newProjectDir.getAbsolutePath());
                            QwenConversationState qwenState = new QwenConversationState(); // Create a new state for the imported project
                            chatHistoryManager.saveChatState(chatHistory, qwenState);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error importing chat history", e);
                    } finally {
                        importedChatHistoryFile.delete();
                    }
                }

                mainActivity.runOnUiThread(() -> {
                    Toast.makeText(context, context.getString(R.string.project_imported_successfully, projectName), Toast.LENGTH_SHORT).show();
                    mainActivity.getProjectManager().loadProjectsList();
                    mainActivity.openProject(newProjectDir.getAbsolutePath(), projectName);
                });

            } catch (IOException e) {
                mainActivity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.failed_to_import_project, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void zipDirectory(File directory, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
            zipFile(directory, directory, zos);
        }
    }

    private void zipFile(File rootDir, File sourceFile, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        if (sourceFile.isDirectory()) {
            for (File file : sourceFile.listFiles()) {
                zipFile(rootDir, file, zos);
            }
        } else {
            String relativePath = rootDir.toURI().relativize(sourceFile.toURI()).getPath();
            ZipEntry entry = new ZipEntry(relativePath);
            zos.putNextEntry(entry);
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mainActivity.startActivity(Intent.createChooser(shareIntent, "Share Project"));
    }

    private void unzipFile(Uri zipFileUri, File targetDirectory) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(zipFileUri);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                if (!file.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath())) {
                    throw new IOException("Zip entry is trying to escape target directory.");
                }
                if (ze.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
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
}
