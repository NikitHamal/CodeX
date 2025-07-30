package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class DialogHelper {
	private final Context context;
	private final FileManager fileManager;
	private final EditorActivity editorActivity; // Keep reference to EditorActivity

	public DialogHelper(Context context, FileManager fileManager, EditorActivity editorActivity) {
		this.context = context;
		this.fileManager = fileManager;
		this.editorActivity = editorActivity;
	}

	public interface FontFamilySelectionListener {
		void onFontFamilySelected(String fontFamily);
	}

	public void showNewFileDialog(File parentDirectory) {
		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_file, null);
		TextInputEditText editTextFileName = dialogView.findViewById(R.id.edittext_file_name);

		AlertDialog dialog = new MaterialAlertDialogBuilder(context)
		.setTitle("Create New File in " + parentDirectory.getName())
		.setView(dialogView)
		.setPositiveButton("Create", null)
		.setNegativeButton("Cancel", null)
		.create();

		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String fileName = editTextFileName.getText().toString().trim();
			try {
				fileManager.createNewFile(parentDirectory, fileName);
				editorActivity.loadFileTree(); // Call through EditorActivity
				editorActivity.openFile(new File(parentDirectory, fileName)); // Call through EditorActivity
				dialog.dismiss();
				editorActivity.closeDrawerIfOpen(); // Call through EditorActivity
			} catch (IOException e) {
				editTextFileName.setError(e.getMessage());
			}
		});
	}

	public void showFontFamilyDialog(String currentFontFamily, FontFamilySelectionListener listener) {
		// List of available font families with their display names
		String[] fontFamilies = new String[]{"Poppins", "Fira Code", "JetBrains Mono"};
		String[] fontFamilyValues = new String[]{"poppins", "firacode", "jetbrainsmono"};

		// Find current selection index
		int currentSelection = 0;
		for (int i = 0; i < fontFamilyValues.length; i++) {
			if (fontFamilyValues[i].equals(currentFontFamily)) {
				currentSelection = i;
				break;
			}
		}

		new MaterialAlertDialogBuilder(context)
		.setTitle("Select Font Family")
		.setSingleChoiceItems(fontFamilies, currentSelection, (dialog, which) -> {
			// Do nothing here, we'll handle selection in positive button
		})
		.setPositiveButton("Apply", (dialog, which) -> {
			int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
			if (selectedPosition >= 0 && selectedPosition < fontFamilyValues.length) {
				String selectedFont = fontFamilyValues[selectedPosition];
				listener.onFontFamilySelected(selectedFont);
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	public void showNewFolderDialog(File parentDirectory) {
		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_new_folder, null);
		EditText editTextFolderName = dialogView.findViewById(R.id.edittext_folder_name);

		AlertDialog dialog = new MaterialAlertDialogBuilder(context)
		.setTitle("Create New Folder in " + parentDirectory.getName())
		.setView(dialogView)
		.setPositiveButton("Create", null)
		.setNegativeButton("Cancel", null)
		.create();
		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String folderName = editTextFolderName.getText().toString().trim();
			try {
				fileManager.createNewDirectory(parentDirectory, folderName);
				editorActivity.loadFileTree(); // Call through EditorActivity
				dialog.dismiss();
				editorActivity.closeDrawerIfOpen(); // Call through EditorActivity
			} catch (IOException e) {
				editTextFolderName.setError(e.getMessage());
			}
		});
	}

	public void showUnsavedChangesDialog(Runnable onSave, Runnable onDiscard) {
		new MaterialAlertDialogBuilder(context)
		.setTitle("Unsaved Changes")
		.setMessage("You have unsaved changes. Do you want to save them before exiting?")
		.setPositiveButton("Save All", (dialog, which) -> onSave.run())
		.setNegativeButton("Discard", (dialog, which) -> onDiscard.run())
		.setNeutralButton("Cancel", null)
		.show();
	}

	public void showTabCloseConfirmationDialog(String fileName, Runnable onSave, Runnable onDiscard) {
		new MaterialAlertDialogBuilder(context)
		.setTitle("Unsaved Changes")
		.setMessage("Save changes to " + fileName + "?")
		.setPositiveButton("Save", (dialog, which) -> onSave.run())
		.setNegativeButton("Discard", (dialog, which) -> onDiscard.run())
		.setNeutralButton("Cancel", null)
		.show();
	}

	public void showCloseOtherTabsDialog(Runnable onSaveAll, Runnable onDiscardAll) {
		new MaterialAlertDialogBuilder(context)
		.setTitle("Unsaved Changes")
		.setMessage("Save changes in other tabs before closing?")
		.setPositiveButton("Save All Other", (dialog, which) -> onSaveAll.run())
		.setNegativeButton("Discard All Other", (dialog, which) -> onDiscardAll.run())
		.setNeutralButton("Cancel", null)
		.show();
	}

	public void showCloseAllTabsDialog(Runnable onSaveAll, Runnable onDiscardAll) {
		new MaterialAlertDialogBuilder(context)
		.setTitle("Unsaved Changes")
		.setMessage("Save all changes before closing tabs?")
		.setPositiveButton("Save All", (dialog, which) -> onSaveAll.run())
		.setNegativeButton("Discard All", (dialog, which) -> onDiscardAll.run())
		.setNeutralButton("Cancel", null)
		.show();
	}

	public void showIndexStatusDialog() {
		new MaterialAlertDialogBuilder(context)
		.setTitle("Codebase Index Status")
		.setMessage("The codebase index helps the AI understand your project structure. It updates automatically when files change.")
		.setPositiveButton("Force Refresh", (dialog, which) -> {
			if (editorActivity != null && editorActivity.aiAssistantManager != null) {
				editorActivity.aiAssistantManager.refreshCodebaseIndex(); // Call through AiAssistantManager
			}
		})
		.setNegativeButton("OK", null)
		.show();
	}

	public void showApiKeyDialog(String preferenceKey, String dialogTitle, Runnable onSave) {
		View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_api_key, null);
		TextInputEditText editTextApiKey = dialogView.findViewById(R.id.edittext_api_key);

		// Pre-fill existing value (if any) so users can update it easily
		SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
		String existing = prefs.getString(preferenceKey, "");
		editTextApiKey.setText(existing);

		AlertDialog dialog = new MaterialAlertDialogBuilder(context)
				.setTitle(dialogTitle)
				.setView(dialogView)
				.setPositiveButton("Save", null)
				.setNegativeButton("Cancel", null)
				.create();

		dialog.show();

		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String apiKey = editTextApiKey.getText().toString().trim();
			if (apiKey.isEmpty()) {
				editTextApiKey.setError("API key cannot be empty");
			} else {
				prefs.edit().putString(preferenceKey, apiKey).apply();
				onSave.run();
				dialog.dismiss();
			}
		});
	}

	// Legacy helper – defaults to Gemini key for backward compatibility
	public void showApiKeyDialog(Runnable onSave) {
		showApiKeyDialog("gemini_api_key", "Set API Key", onSave);
	}

	public void showAiActionSummary(List<String> actionSummaries, String explanation) {
		StringBuilder message = new StringBuilder();
		if (explanation != null && !explanation.trim().isEmpty()) {
			message.append(explanation).append("\n\n");
		} else {
			message.append("AI processing complete.\n\n");
		}

		if (actionSummaries.isEmpty()) {
			message.append("No specific file actions were performed by the AI.");
		} else {
			message.append("Actions performed:\n");
			for (String action : actionSummaries) {
				message.append("• ").append(action).append("\n");
			}
		}

		new MaterialAlertDialogBuilder(context)
		.setTitle("AI Assistant Results")
		.setMessage(message.toString())
		.setPositiveButton("OK", null)
		.show();
	}
}
