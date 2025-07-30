package com.codex.apk;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
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

import com.google.android.material.appbar.MaterialToolbar;

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
		LinearLayout modelSelectorLayout = findViewById(R.id.layout_model_selector);
		TextView selectedModelText = findViewById(R.id.text_selected_model);
		LinearLayout themeSelectorLayout = findViewById(R.id.layout_theme_selector);
		TextView selectedThemeText = findViewById(R.id.text_selected_theme);
		
		// Load saved settings
		SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
		SharedPreferences defaultPrefs = getPreferences(this);
		String savedApiKey = prefs.getString("gemini_api_key", "");
		String savedHuggingFaceToken = prefs.getString("huggingface_token", "");
		String savedModel = prefs.getString("selected_model", "Gemini 2.5 Flash");
		String savedTheme = defaultPrefs.getString("app_theme", "light");
		
		if (apiKeyEditText != null) {
			apiKeyEditText.setText(savedApiKey);
		}

		if (huggingFaceTokenEditText != null) {
			huggingFaceTokenEditText.setText(savedHuggingFaceToken);
		}
		
		if (selectedModelText != null) {
			selectedModelText.setText(savedModel);
		}
		
		if (selectedThemeText != null) {
			selectedThemeText.setText(getThemeDisplayName(savedTheme));
		}

		// Set up model selector click
		if (modelSelectorLayout != null) {
			modelSelectorLayout.setOnClickListener(v -> showModelSelector());
		}
		
		// Set up theme selector click
		if (themeSelectorLayout != null) {
			themeSelectorLayout.setOnClickListener(v -> showThemeSelector());
		}

		// Set up about card click
		com.google.android.material.card.MaterialCardView aboutCard = findViewById(R.id.about_card);
		if (aboutCard != null) {
			aboutCard.setOnClickListener(v -> {
				Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
				startActivity(intent);
			});
		}

		// Set up save functionality with multiple triggers
		if (apiKeyEditText != null) {
			// Save on focus change
			apiKeyEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (!hasFocus) {
					String apiKey = apiKeyEditText.getText().toString().trim();
					prefs.edit().putString("gemini_api_key", apiKey).apply();
					Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show();
				}
			});
			
			// Also save on text change with debouncing
			apiKeyEditText.addTextChangedListener(new android.text.TextWatcher() {
				private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
				private Runnable saveRunnable;
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}
				
				@Override
				public void afterTextChanged(android.text.Editable s) {
					if (saveRunnable != null) {
						handler.removeCallbacks(saveRunnable);
					}
					saveRunnable = () -> {
						String apiKey = s.toString().trim();
						prefs.edit().putString("gemini_api_key", apiKey).apply();
					};
					handler.postDelayed(saveRunnable, 1000); // Save after 1 second of no typing
				}
			});
		}

		if (huggingFaceTokenEditText != null) {
			// Save on focus change
			huggingFaceTokenEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (!hasFocus) {
					String token = huggingFaceTokenEditText.getText().toString().trim();
					prefs.edit().putString("huggingface_token", token).apply();
					Toast.makeText(this, "Hugging Face Token saved", Toast.LENGTH_SHORT).show();
				}
			});

			// Also save on text change with debouncing
			huggingFaceTokenEditText.addTextChangedListener(new android.text.TextWatcher() {
				private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
				private Runnable saveRunnable;

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {}

				@Override
				public void afterTextChanged(android.text.Editable s) {
					if (saveRunnable != null) {
						handler.removeCallbacks(saveRunnable);
					}
					saveRunnable = () -> {
						String token = s.toString().trim();
						prefs.edit().putString("huggingface_token", token).apply();
					};
					handler.postDelayed(saveRunnable, 1000); // Save after 1 second of no typing
				}
			});
		}
	}
	
	private void showModelSelector() {
		List<String> modelNamesList = AIAssistant.AIModel.getAllDisplayNames();
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
			
			
			// Auto save preference
			SwitchPreferenceCompat autoSavePreference = findPreference("auto_save");
			if (autoSavePreference != null) {
				autoSavePreference.setOnPreferenceChangeListener((preference, newValue) -> {
					boolean enabled = (Boolean) newValue;
					Toast.makeText(getContext(), enabled ? "Auto save enabled" : "Auto save disabled", Toast.LENGTH_SHORT).show();
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
	
	public static boolean isAutoSaveEnabled(android.content.Context context) {
		return getPreferences(context).getBoolean("auto_save", true);
	}
	
	public static boolean isLineNumbersEnabled(android.content.Context context) {
		return getPreferences(context).getBoolean("line_numbers", true);
	}
	
	public static boolean isAiHistoryEnabled(android.content.Context context) {
		return getPreferences(context).getBoolean("ai_history", true);
	}
}