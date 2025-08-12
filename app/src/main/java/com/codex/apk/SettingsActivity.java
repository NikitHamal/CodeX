package com.codex.apk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.util.AttributeSet;
import java.util.List;
import com.codex.apk.ai.AIModel;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {
	
	private MaterialToolbar toolbar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set up theme based on user preferences
		ThemeManager.setupTheme(this);
		
		try {
			setContentView(R.layout.settings);
			
			// Initialize toolbar
			toolbar = findViewById(R.id.toolbar);
			if (toolbar != null) {
				setSupportActionBar(toolbar);
				if (getSupportActionBar() != null) {
					getSupportActionBar().setTitle("Settings");
					getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				}
			}
			
			// Settings UI is directly in the layout, no fragment needed
			// Initialize settings controls
			initializeSettings();
		} catch (Exception e) {
			Toast.makeText(this, "Error loading settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void initializeSettings() {
		// Initialize settings controls from the layout
		com.google.android.material.textfield.TextInputEditText apiKeyEditText = findViewById(R.id.edit_text_api_key);
		com.google.android.material.textfield.TextInputEditText huggingFaceTokenEditText = findViewById(R.id.edit_text_hugging_face_token);
		com.google.android.material.textfield.TextInputEditText cookie1psidEditText = findViewById(R.id.edit_text_secure_1psid);
		com.google.android.material.textfield.TextInputEditText cookie1psidtsEditText = findViewById(R.id.edit_text_secure_1psidts);
		com.google.android.material.textfield.TextInputEditText customGeneralPrompt = findViewById(R.id.edit_text_custom_general_prompt);
		com.google.android.material.textfield.TextInputEditText customFileOpsPrompt = findViewById(R.id.edit_text_custom_fileops_prompt);
		LinearLayout modelSelectorLayout = findViewById(R.id.layout_model_selector);
		TextView selectedModelText = findViewById(R.id.text_selected_model);
		LinearLayout themeSelectorLayout = findViewById(R.id.layout_theme_selector);
		TextView selectedThemeText = findViewById(R.id.text_selected_theme);
		MaterialButton buttonAddAgent = findViewById(R.id.button_add_agent);
		LinearLayout containerAgents = findViewById(R.id.container_agents_list);
		
		// Load saved settings
		SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
		SharedPreferences defaultPrefs = getPreferences(this);
		String savedApiKey = prefs.getString("gemini_api_key", "");
		String savedHuggingFaceToken = prefs.getString("huggingface_token", "");
		String savedModel = prefs.getString("selected_model", "Gemini 2.5 Flash");
		String savedTheme = defaultPrefs.getString("app_theme", "light");
		String saved1psid = prefs.getString("secure_1psid", "");
		String saved1psidts = prefs.getString("secure_1psidts", "");
		
		if (apiKeyEditText != null) apiKeyEditText.setText(savedApiKey);
		if (huggingFaceTokenEditText != null) huggingFaceTokenEditText.setText(savedHuggingFaceToken);
		if (cookie1psidEditText != null) cookie1psidEditText.setText(saved1psid);
		if (cookie1psidtsEditText != null) cookie1psidtsEditText.setText(saved1psidts);
		if (selectedModelText != null) selectedModelText.setText(savedModel);
		if (selectedThemeText != null) selectedThemeText.setText(getThemeDisplayName(savedTheme));

		// Load custom prompts
		if (customGeneralPrompt != null) customGeneralPrompt.setText(getCustomGeneralPrompt(this));
		if (customFileOpsPrompt != null) customFileOpsPrompt.setText(getCustomFileOpsPrompt(this));

		// Load custom agents list
		if (containerAgents != null) {
			renderAgentsList(containerAgents);
		}

		// Clicks
		if (modelSelectorLayout != null) modelSelectorLayout.setOnClickListener(v -> showModelSelector());
		if (themeSelectorLayout != null) themeSelectorLayout.setOnClickListener(v -> showThemeSelector());
		if (buttonAddAgent != null) buttonAddAgent.setOnClickListener(v -> showAddAgentDialog(containerAgents));

		// Save handlers (keys and tokens already set up before)
		setupDebouncedSaver(customGeneralPrompt, s -> setCustomGeneralPrompt(this, s));
		setupDebouncedSaver(customFileOpsPrompt, s -> setCustomFileOpsPrompt(this, s));
	}

	private void setupDebouncedSaver(com.google.android.material.textfield.TextInputEditText editText, java.util.function.Consumer<String> onSave) {
		if (editText == null) return;
		editText.addTextChangedListener(new android.text.TextWatcher() {
			private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
			private Runnable saveRunnable;
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			public void afterTextChanged(android.text.Editable s) {
				if (saveRunnable != null) handler.removeCallbacks(saveRunnable);
				saveRunnable = () -> onSave.accept(s.toString().trim());
				handler.postDelayed(saveRunnable, 700);
			}
		});
	}

	private void showAddAgentDialog(LinearLayout containerAgents) {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_file, null);
		TextView title = dialogView.findViewById(R.id.text_title);
		title.setText("Create New Agent");
		com.google.android.material.textfield.TextInputLayout tilName = dialogView.findViewById(R.id.input_layout_name);
		com.google.android.material.textfield.TextInputLayout tilPath = dialogView.findViewById(R.id.input_layout_path);
		com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.edit_text_name);
		com.google.android.material.textfield.TextInputEditText etPrompt = dialogView.findViewById(R.id.edit_text_path);
		tilName.setHint("Agent Name");
		tilPath.setHint("Agent Prompt");
		etPrompt.setMinLines(3);
		etPrompt.setMaxLines(8);

		new MaterialAlertDialogBuilder(this)
			.setView(dialogView)
			.setPositiveButton("Save", (d, w) -> {
				String name = etName.getText() != null ? etName.getText().toString().trim() : "";
				String prompt = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : "";
				if (name.isEmpty()) { Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show(); return; }
				java.util.List<CustomAgent> agents = getCustomAgents(this);
				String id = java.util.UUID.randomUUID().toString();
				// default to currently selected model if available
				String modelId = AIModel.fromDisplayName(getSharedPreferences("settings", MODE_PRIVATE).getString("selected_model", "Gemini 2.5 Flash")).getModelId();
				agents.add(new CustomAgent(id, name, prompt, modelId));
				setCustomAgents(this, agents);
				renderAgentsList(containerAgents);
			})
			.setNegativeButton("Cancel", null)
			.show();
	}

	private void renderAgentsList(LinearLayout container) {
		container.removeAllViews();
		java.util.List<CustomAgent> agents = getCustomAgents(this);
		for (int i = 0; i < agents.size(); i++) {
			CustomAgent a = agents.get(i);
			View row = LayoutInflater.from(this).inflate(R.layout.item_project, container, false);
			TextView name = row.findViewById(R.id.text_project_name);
			TextView path = row.findViewById(R.id.text_project_path);
			name.setText(a.name);
			path.setText(a.modelId);
			row.setOnClickListener(v -> showEditAgentDialog(a, container));
			container.addView(row);
		}
	}

	private void showEditAgentDialog(CustomAgent agent, LinearLayout containerAgents) {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_file, null);
		TextView title = dialogView.findViewById(R.id.text_title);
		title.setText("Edit Agent");
		com.google.android.material.textfield.TextInputLayout tilName = dialogView.findViewById(R.id.input_layout_name);
		com.google.android.material.textfield.TextInputLayout tilPath = dialogView.findViewById(R.id.input_layout_path);
		com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.edit_text_name);
		com.google.android.material.textfield.TextInputEditText etPrompt = dialogView.findViewById(R.id.edit_text_path);
		tilName.setHint("Agent Name");
		tilPath.setHint("Agent Prompt");
		etName.setText(agent.name);
		etPrompt.setText(agent.prompt);
		etPrompt.setMinLines(3);
		etPrompt.setMaxLines(8);

		new MaterialAlertDialogBuilder(this)
			.setView(dialogView)
			.setPositiveButton("Save", (d, w) -> {
				agent.name = etName.getText() != null ? etName.getText().toString().trim() : agent.name;
				agent.prompt = etPrompt.getText() != null ? etPrompt.getText().toString().trim() : agent.prompt;
				setCustomAgents(this, getCustomAgents(this)); // persists current list
				renderAgentsList(containerAgents);
			})
			.setNeutralButton("Delete", (d, w) -> {
				java.util.List<CustomAgent> agents = getCustomAgents(this);
				agents.removeIf(a -> a.id.equals(agent.id));
				setCustomAgents(this, agents);
				renderAgentsList(containerAgents);
			})
			.setNegativeButton("Cancel", null)
			.show();
	}
	
	private void showModelSelector() {
		List<String> modelNamesList = AIModel.getAllDisplayNames();
		String[] modelNames = modelNamesList.toArray(new String[0]);
		String currentModel = getSharedPreferences("settings", MODE_PRIVATE).getString("selected_model", "Gemini 2.5 Flash");
		int selectedIndex = -1;
		
		// Find current model index
		for (int i = 0; i < modelNames.length; i++) {
			if (modelNames[i].equals(currentModel)) {
				selectedIndex = i;
				break;
			}
		}
		
		new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
				.setTitle("Select AI Model")
				.setSingleChoiceItems(modelNames, selectedIndex, (dialog, which) -> {
					String selectedModelName = modelNames[which];
					TextView selectedModelText = findViewById(R.id.text_selected_model);
					if (selectedModelText != null) {
						selectedModelText.setText(selectedModelName);
					}
					getSharedPreferences("settings", MODE_PRIVATE)
						.edit()
						.putString("selected_model", selectedModelName)
						.apply();
					dialog.dismiss();
				})
				.setNegativeButton("Cancel", null)
				.show();
	}

	private void showThemeSelector() {
		String currentTheme = getPreferences(this).getString("app_theme", "light");
		String[] themeEntries = getResources().getStringArray(R.array.theme_entries);
		String[] themeValues = getResources().getStringArray(R.array.theme_values);

		int currentIndex = 0;
		for (int i = 0; i < themeValues.length; i++) {
			if (themeValues[i].equals(currentTheme)) {
				currentIndex = i;
				break;
			}
		}

		new MaterialAlertDialogBuilder(this, R.style.AlertDialogCustom)
			.setTitle("Select Theme")
			.setSingleChoiceItems(themeEntries, currentIndex, (dialog, which) -> {
				String selectedTheme = themeValues[which];
				String selectedThemeDisplay = themeEntries[which];

				TextView selectedThemeText = findViewById(R.id.text_selected_theme);
				if (selectedThemeText != null) {
					selectedThemeText.setText(selectedThemeDisplay);
				}

				// Save the theme preference using default preferences
				getPreferences(this)
					.edit()
					.putString("app_theme", selectedTheme)
					.apply();

				// Apply theme immediately
				ThemeManager.switchTheme(this, selectedTheme);

				dialog.dismiss();
			})
			.setNegativeButton("Cancel", null)
			.show();
	}

	private String getThemeDisplayName(String themeValue) {
		String[] themeEntries = getResources().getStringArray(R.array.theme_entries);
		String[] themeValues = getResources().getStringArray(R.array.theme_values);

		for (int i = 0; i < themeValues.length; i++) {
			if (themeValues[i].equals(themeValue)) {
				return themeEntries[i];
			}
		}
		return "Light"; // Default
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);
			
			// API Key preference
			EditTextPreference apiKeyPreference = findPreference("gemini_api_key");
			if (apiKeyPreference != null) {
				apiKeyPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
					String value = preference.getText();
					if (value == null || value.isEmpty()) {
						return "Not set (using default key)";
					} else {
						return "API key is set";
					}
				});
				
				apiKeyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
					String apiKey = (String) newValue;
					if (apiKey.isEmpty()) {
						Toast.makeText(getContext(), "Using default API key", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getContext(), "API key updated", Toast.LENGTH_SHORT).show();
					}
					return true;
				});
			}
			
			// Theme preference
			ListPreference themePreference = findPreference("app_theme");
			if (themePreference != null) {
				themePreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
						themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
				String theme = (String) newValue;
				if (getActivity() != null) {
					ThemeManager.switchTheme(getActivity(), theme);
				}
				return true;
			});
			}
			
			// Font size preference
			ListPreference fontSizePreference = findPreference("font_size");
			if (fontSizePreference != null) {
				fontSizePreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
			}
			
			// Font family preference
			Preference fontFamilyPreference = findPreference("font_family");
			if (fontFamilyPreference != null) {
				fontFamilyPreference.setSummaryProvider(preference -> {
					String currentFont = SettingsActivity.getFontFamily(requireContext());
					return "Current: " + currentFont.substring(0, 1).toUpperCase() + currentFont.substring(1);
				});
				
				fontFamilyPreference.setOnPreferenceClickListener(preference -> {
					String currentFont = SettingsActivity.getFontFamily(requireContext());
					new DialogHelper(requireContext(), null, null).showFontFamilyDialog(currentFont, selectedFont -> {
						// Save the new selection
						SharedPreferences.Editor editor = SettingsActivity.getPreferences(requireContext()).edit();
						editor.putString("font_family", selectedFont);
						editor.apply();
						
						// Update the preference summary
						fontFamilyPreference.setSummary("Current: " + 
						selectedFont.substring(0, 1).toUpperCase() + 
						selectedFont.substring(1));
						
						Toast.makeText(requireContext(), 
						"Font family will be applied when you restart the app", 
						Toast.LENGTH_SHORT).show();

						// Recreate the activity to apply changes
						requireActivity().recreate();
					});
					return true;
				});
			}
			
			// Hugging Face Token preference
			EditTextPreference hfTokenPreference = findPreference("huggingface_token");
			if (hfTokenPreference != null) {
				hfTokenPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
					String value = preference.getText();
					if (value == null || value.isEmpty()) {
						return "Not set (required for Deepseek R1)";
					} else {
						return "Token is set";
					}
				});
				
				hfTokenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
					String token = (String) newValue;
					if (token.isEmpty()) {
						Toast.makeText(getContext(), "Deepseek R1 will not work without token", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getContext(), "Hugging Face token updated", Toast.LENGTH_SHORT).show();
					}
					return true;
				});
			}
			
			// Qwen API Token preference
			EditTextPreference qwenTokenPreference = findPreference("qwen_api_token");
			if (qwenTokenPreference != null) {
				qwenTokenPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
					String value = preference.getText();
					if (value == null || value.isEmpty()) {
						return "Using default token";
					} else {
						return "Custom token is set";
					}
				});

				qwenTokenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
					String token = (String) newValue;
					if (token.isEmpty()) {
						Toast.makeText(getContext(), "Using default Qwen token", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getContext(), "Qwen API token updated", Toast.LENGTH_SHORT).show();
					}
					return true;
				});
			}

			
			// Default read-only preference
			SwitchPreferenceCompat readOnlyPref = findPreference("default_read_only");
			if (readOnlyPref != null) {
				readOnlyPref.setOnPreferenceChangeListener((preference, newValue) -> {
					boolean enabled = (Boolean) newValue;
					Toast.makeText(getContext(), enabled ? "Read-only by default" : "Editable by default", Toast.LENGTH_SHORT).show();
					return true;
				});
			}

			// Default wrap preference
			SwitchPreferenceCompat wrapPref = findPreference("default_word_wrap");
			if (wrapPref != null) {
				wrapPref.setOnPreferenceChangeListener((preference, newValue) -> {
					boolean enabled = (Boolean) newValue;
					Toast.makeText(getContext(), enabled ? "Word wrap enabled by default" : "Word wrap disabled by default", Toast.LENGTH_SHORT).show();
					return true;
				});
			}
			
			// Line numbers preference
			SwitchPreferenceCompat lineNumbersPreference = findPreference("line_numbers");
			if (lineNumbersPreference != null) {
				lineNumbersPreference.setOnPreferenceChangeListener((preference, newValue) -> {
					boolean enabled = (Boolean) newValue;
					Toast.makeText(getContext(), enabled ? "Line numbers enabled" : "Line numbers disabled", Toast.LENGTH_SHORT).show();
					return true;
				});
			}
			
			// AI history preference
			SwitchPreferenceCompat aiHistoryPreference = findPreference("ai_history");
			if (aiHistoryPreference != null) {
				aiHistoryPreference.setOnPreferenceChangeListener((preference, newValue) -> {
					boolean enabled = (Boolean) newValue;
					Toast.makeText(getContext(), enabled ? "AI history enabled" : "AI history disabled", Toast.LENGTH_SHORT).show();
					return true;
				});
			}
		}
	}
	
	// Helper method to get preferences
	public static SharedPreferences getPreferences(android.content.Context context) {
		return context.getSharedPreferences("settings", MODE_PRIVATE);
	}
	
	// Helper methods to get specific settings
	public static String getGeminiApiKey(android.content.Context context) {
		return getPreferences(context).getString("gemini_api_key", "");
	}
	
	public static String getAppTheme(android.content.Context context) {
		return getPreferences(context).getString("app_theme", "light");
	}
	
	public static int getFontSize(android.content.Context context) {
		String fontSize = getPreferences(context).getString("font_size", "medium");
		switch (fontSize) {
			case "small":
			return 12;
			case "large":
			return 18;
			case "xlarge":
			return 22;
			case "medium":
			default:
			return 14;
		}
	}
	
	
	public static String getFontFamily(Context context) {
		return getPreferences(context).getString("font_family", "poppins");
	}
	
	public static String getFontWeight(Context context) {
		return getPreferences(context).getString("font_weight", "reg");
	}
	
	public static String getFontFileName(Context context) {
		String family = getFontFamily(context);
		String weight = getFontWeight(context);
		
		switch (family) {
			case "poppins":
			return "poppins_" + weight + ".ttf";
			case "firacode":
			return "firacode_" + (weight.equals("reg") ? "regular" : 
			weight.equals("med") ? "medium" : "semibold") + ".ttf";
			case "jetbrainsmono":
			return "jetbrainsmono_" + (weight.equals("reg") ? "regular" : 
			weight.equals("med") ? "medium" : "semibold") + ".ttf";
			default:
			return "poppins_reg.ttf";
		}
	}
	
	public static String getHuggingFaceToken(Context context) {
		return getPreferences(context).getString("huggingface_token", "");
	}
	
	public static String getQwenApiToken(Context context) {
		return getPreferences(context).getString("qwen_api_token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjhiYjQ1NjVmLTk3NjUtNDQwNi04OWQ5LTI3NmExMTIxMjBkNiIsImxhc3RfcGFzc3dvcmRfY2hhbmdlIjoxNzUwNjYwODczLCJleHAiOjE3NTU4NDg1NDh9.pb0IybY9tQkriqMUOos72FKtZM3G4p1_aDzwqqh5zX4");
	}

	public static String getSecure1PSID(Context context) {
		return getPreferences(context).getString("secure_1psid", "");
	}

	public static String getSecure1PSIDTS(Context context) {
		return getPreferences(context).getString("secure_1psidts", "");
	}

	// Custom prompt settings
	public static String getCustomFileOpsPrompt(Context context) {
		return getPreferences(context).getString("custom_fileops_prompt", "");
	}
	public static void setCustomFileOpsPrompt(Context context, String prompt) {
		getPreferences(context).edit().putString("custom_fileops_prompt", prompt != null ? prompt : "").apply();
	}
	public static String getCustomGeneralPrompt(Context context) {
		return getPreferences(context).getString("custom_general_prompt", "");
	}
	public static void setCustomGeneralPrompt(Context context, String prompt) {
		getPreferences(context).edit().putString("custom_general_prompt", prompt != null ? prompt : "").apply();
	}

	public static java.util.List<CustomAgent> getCustomAgents(Context context) {
		String json = getPreferences(context).getString("custom_agents", "[]");
		try {
			java.util.List<CustomAgent> list = new java.util.ArrayList<>();
			com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
			for (int i = 0; i < arr.size(); i++) {
				com.google.gson.JsonObject o = arr.get(i).getAsJsonObject();
				String id = o.has("id") ? o.get("id").getAsString() : java.util.UUID.randomUUID().toString();
				String name = o.has("name") ? o.get("name").getAsString() : "Unnamed";
				String prompt = o.has("prompt") ? o.get("prompt").getAsString() : "";
				String modelId = o.has("modelId") ? o.get("modelId").getAsString() : "";
				list.add(new CustomAgent(id, name, prompt, modelId));
			}
			return list;
		} catch (Exception e) {
			return new java.util.ArrayList<>();
		}
	}
	public static void setCustomAgents(Context context, java.util.List<CustomAgent> agents) {
		com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
		for (CustomAgent a : agents) {
			com.google.gson.JsonObject o = new com.google.gson.JsonObject();
			o.addProperty("id", a.id);
			o.addProperty("name", a.name);
			o.addProperty("prompt", a.prompt);
			o.addProperty("modelId", a.modelId);
			arr.add(o);
		}
		getPreferences(context).edit().putString("custom_agents", arr.toString()).apply();
	}

	// Cache helpers for __Secure-1PSIDTS keyed by the 1PSID value
	public static String getCached1psidts(Context context, String psid) {
		if (psid == null || psid.isEmpty()) return "";
		return getPreferences(context).getString("cached_1psidts_" + psid, "");
	}
	public static void setCached1psidts(Context context, String psid, String psidts) {
		if (psid == null || psid.isEmpty() || psidts == null || psidts.isEmpty()) return;
		getPreferences(context).edit().putString("cached_1psidts_" + psid, psidts).apply();
	}

	// Free provider chat metadata cache (per model id). Value is JSON array string like [cid, rid, rcid]
	public static String getFreeConversationMetadata(Context context, String modelId) {
		if (modelId == null) return "";
		return getPreferences(context).getString("free_conv_meta_" + modelId, "");
	}
	public static void setFreeConversationMetadata(Context context, String modelId, String metadataJsonArray) {
		if (modelId == null || metadataJsonArray == null || metadataJsonArray.isEmpty()) return;
		getPreferences(context).edit().putString("free_conv_meta_" + modelId, metadataJsonArray).apply();
	}

    public static boolean isDefaultReadOnly(android.content.Context context) {
        return getPreferences(context).getBoolean("default_read_only", false);
    }

    public static boolean isDefaultWordWrap(android.content.Context context) {
        return getPreferences(context).getBoolean("default_word_wrap", true);
    }
	
	public static boolean isLineNumbersEnabled(android.content.Context context) {
		return getPreferences(context).getBoolean("line_numbers", true);
	}
	
	public static boolean isAiHistoryEnabled(android.content.Context context) {
		return getPreferences(context).getBoolean("ai_history", true);
	}
}