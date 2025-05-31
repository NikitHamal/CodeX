package com.codex.apk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
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

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {
	
	private MaterialToolbar toolbar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		// Initialize toolbar
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle("Settings");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Load settings fragment
		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.settings_container, new SettingsFragment())
		.commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
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
					Toast.makeText(getContext(), "Theme will be applied when you restart the app", Toast.LENGTH_SHORT).show();
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
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	// Helper methods to get specific settings
	public static String getGeminiApiKey(android.content.Context context) {
		return getPreferences(context).getString("gemini_api_key", "Gemini API Key");
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
