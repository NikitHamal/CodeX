package com.codex.apk;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class FileManager {
	private static final String TAG = "FileManager";
	private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
	private final Context context;
	private final File projectDir;

	// File change listener
	public interface FileChangeListener {
		void onFileCreated(File file);
		void onFileModified(File file);
		void onFileDeleted(File file);
		void onFileRenamed(File oldFile, File newFile);
	}

	private FileChangeListener fileChangeListener;

	public void setFileChangeListener(FileChangeListener listener) {
		this.fileChangeListener = listener;
	}

	public FileManager(Context context, File projectDir) {
		this.context = context;
		this.projectDir = projectDir;
	}

	public String readFileContent(File file) throws IOException {
		StringBuilder content = new StringBuilder();
		try (FileInputStream fis = new FileInputStream(file);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
		}
		return content.toString();
	}

	public void writeFileContent(File file, String content) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file);
			 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
			writer.write(content);
		}
		if (fileChangeListener != null) {
			fileChangeListener.onFileModified(file);
		}
	}

	public void createNewFile(File parentDirectory, String fileName) throws IOException {
		if (!parentDirectory.isDirectory() || !parentDirectory.canWrite()) {
			throw new IOException("Invalid or non-writable parent directory");
		}

		if (fileName.isEmpty()) {
			throw new IOException("File name cannot be empty");
		}

		if (autoInvalidFileNameChars.matcher(fileName).find()) {
			throw new IOException("File name contains invalid characters");
		}

		if (!isValidFileName(fileName)) {
			throw new IOException("Invalid file name");
		}

		File newFile = new File(parentDirectory, fileName);
		if (newFile.exists()) {
			throw new IOException("File already exists");
		}

		if (!newFile.createNewFile()) {
			throw new IOException("Failed to create file");
		}

		if (fileChangeListener != null) {
			fileChangeListener.onFileCreated(newFile);
		}
	}

	public void createNewDirectory(File parentDirectory, String folderName) throws IOException {
		if (!parentDirectory.isDirectory() || !parentDirectory.canWrite()) {
			throw new IOException("Invalid or non-writable parent directory");
		}

		if (folderName.isEmpty()) {
			throw new IOException("Folder name cannot be empty");
		}

		if (autoInvalidFileNameChars.matcher(folderName).find()) {
			throw new IOException("Folder name contains invalid characters");
		}

		if (!isValidFileName(folderName)) {
			throw new IOException("Invalid folder name");
		}

		File newFolder = new File(parentDirectory, folderName);
		if (newFolder.exists()) {
			throw new IOException("Folder already exists");
		}

		if (!newFolder.mkdirs()) {
			throw new IOException("Failed to create folder");
		}

		if (fileChangeListener != null) {
			fileChangeListener.onFileCreated(newFolder);
		}
	}

	public List<FileItem> loadFileTree() {
		List<FileItem> fileItems = new ArrayList<>();
		if (projectDir != null && projectDir.exists()) {
			scanDirectory(projectDir, 0, null, fileItems);
		} else {
			Toast.makeText(context, "Project directory not found.", Toast.LENGTH_SHORT).show();
		}

		Collections.sort(fileItems, (o1, o2) -> {
			if (o1.isDirectory() && !o2.isDirectory()) {
				return -1;
			}
			if (!o1.isDirectory() && o2.isDirectory()) {
				return 1;
			}
			return o1.getName().compareToIgnoreCase(o2.getName());
		});

		return fileItems;
	}

	private void scanDirectory(File dir, int level, FileItem parent, List<FileItem> fileItems) {
		File[] files = dir.listFiles();
		if (files != null) {
			List<File> sortedFiles = new ArrayList<>();
			Collections.addAll(sortedFiles, files);
			Collections.sort(sortedFiles, (f1, f2) -> {
				if (f1.isDirectory() && !f2.isDirectory()) return -1;
				if (!f1.isDirectory() && f2.isDirectory()) return 1;
				return f1.getName().compareToIgnoreCase(f2.getName());
			});

			for (File file : sortedFiles) {
				FileItem item = new FileItem(file, level, parent);
				fileItems.add(item);
				if (file.isDirectory() && item.isExpanded()) {
					scanDirectory(file, level + 1, item, fileItems);
				}
			}
		}
	}

	public void renameFileOrDir(File oldFile, File newFile) throws IOException {
		if (!oldFile.exists()) {
			throw new IOException("Source file/directory does not exist: " + oldFile.getAbsolutePath());
		}
		if (newFile.exists()) {
			throw new IOException("Target file/directory already exists: " + newFile.getAbsolutePath());
		}
		File parentDir = newFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				throw new IOException("Failed to create parent directory for new file: " + parentDir.getAbsolutePath());
			}
		}
		if (!oldFile.renameTo(newFile)) {
			throw new IOException("Failed to rename " + oldFile.getAbsolutePath() + " to " + newFile.getAbsolutePath());
		}

		if (fileChangeListener != null) {
			fileChangeListener.onFileRenamed(oldFile, newFile);
		}
	}

	public void deleteFileOrDirectory(File fileOrDirectory) throws IOException {
		if (!fileOrDirectory.exists()) {
			Log.w(TAG, "deleteFileByPath: File or directory does not exist: " + fileOrDirectory.getAbsolutePath());
			return;
		}

		if (fileOrDirectory.isDirectory()) {
			if (!deleteDirectoryRecursive(fileOrDirectory)) {
				throw new IOException("Failed to delete directory: " + fileOrDirectory.getAbsolutePath());
			}
		} else {
			if (!fileOrDirectory.delete()) {
				throw new IOException("Failed to delete file: " + fileOrDirectory.getAbsolutePath());
			}
		}

		if (fileChangeListener != null) {
			fileChangeListener.onFileDeleted(fileOrDirectory);
		}
	}

	private boolean deleteDirectoryRecursive(File dir) {
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			if (children != null) {
				for (File child : children) {
					if (!deleteDirectoryRecursive(child)) {
						return false;
					}
				}
			}
		}
		return dir.delete();
	}

	public File findFirstHtmlFile() {
		File[] files = projectDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".htm"));
		if (files != null && files.length > 0) {
			return files[0];
		}
		return null;
	}

	public String getRelativePath(File file, File baseDir) {
		String filePath = file.getAbsolutePath();
		String basePath = baseDir.getAbsolutePath();
		if (filePath.startsWith(basePath)) {
			String relative = filePath.substring(basePath.length());
			if (relative.startsWith(File.separator)) {
				relative = relative.substring(1);
			}
			return relative;
		}
		return file.getName();
	}

	public boolean isValidFileName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return false;
		}
		if (fileName.contains("/") || fileName.contains("\\") || fileName.contains(":")) {
			return false;
		}
		if (fileName.trim().isEmpty()) {
			return false;
		}
		return true;
	}
}