package com.codex.apk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class ApiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.activity_api);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        com.google.android.material.textfield.TextInputEditText apiKeyEditText = findViewById(R.id.edit_text_api_key);
        com.google.android.material.textfield.TextInputEditText openRouterApiKeyEditText = findViewById(R.id.edit_text_openrouter_api_key);
        com.google.android.material.textfield.TextInputEditText cookie1psidEditText = findViewById(R.id.edit_text_secure_1psid);
        com.google.android.material.textfield.TextInputEditText cookie1psidtsEditText = findViewById(R.id.edit_text_secure_1psidts);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedApiKey = prefs.getString("gemini_api_key", "");
        String savedOpenRouterApiKey = prefs.getString("openrouter_api_key", "");
        String saved1psid = prefs.getString("secure_1psid", "");
        String saved1psidts = prefs.getString("secure_1psidts", "");

        if (apiKeyEditText != null) apiKeyEditText.setText(savedApiKey);
        if (openRouterApiKeyEditText != null) openRouterApiKeyEditText.setText(savedOpenRouterApiKey);
        if (cookie1psidEditText != null) cookie1psidEditText.setText(saved1psid);
        if (cookie1psidtsEditText != null) cookie1psidtsEditText.setText(saved1psidts);

        setupDebouncedSaver(apiKeyEditText, s -> prefs.edit().putString("gemini_api_key", s).apply());
        setupDebouncedSaver(openRouterApiKeyEditText, s -> prefs.edit().putString("openrouter_api_key", s).apply());
        setupDebouncedSaver(cookie1psidEditText, s -> prefs.edit().putString("secure_1psid", s).apply());
        setupDebouncedSaver(cookie1psidtsEditText, s -> prefs.edit().putString("secure_1psidts", s).apply());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
