package com.codex.apk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.codex.apk.core.Prefs;

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
        com.google.android.material.textfield.TextInputEditText cookie1psidEditText = findViewById(R.id.edit_text_secure_1psid);
        com.google.android.material.textfield.TextInputEditText cookie1psidtsEditText = findViewById(R.id.edit_text_secure_1psidts);

        SharedPreferences prefs = Prefs.of(this);
        String savedApiKey = Prefs.getString(this, Prefs.KEY_GEMINI_API, "");
        String saved1psid = Prefs.getString(this, Prefs.KEY_SECURE_1PSID, "");
        String saved1psidts = Prefs.getString(this, Prefs.KEY_SECURE_1PSIDTS, "");

        if (apiKeyEditText != null) apiKeyEditText.setText(savedApiKey);
        if (cookie1psidEditText != null) cookie1psidEditText.setText(saved1psid);
        if (cookie1psidtsEditText != null) cookie1psidtsEditText.setText(saved1psidts);

        setupDebouncedSaver(apiKeyEditText, s -> Prefs.putString(this, Prefs.KEY_GEMINI_API, s));
        setupDebouncedSaver(cookie1psidEditText, s -> Prefs.putString(this, Prefs.KEY_SECURE_1PSID, s));
        setupDebouncedSaver(cookie1psidtsEditText, s -> Prefs.putString(this, Prefs.KEY_SECURE_1PSIDTS, s));
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
