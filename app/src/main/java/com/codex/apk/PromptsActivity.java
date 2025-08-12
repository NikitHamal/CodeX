package com.codex.apk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class PromptsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.activity_prompts);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        com.google.android.material.textfield.TextInputEditText customGeneralPrompt = findViewById(R.id.edit_text_custom_general_prompt);
        com.google.android.material.textfield.TextInputEditText customFileOpsPrompt = findViewById(R.id.edit_text_custom_fileops_prompt);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        String cg = SettingsActivity.getCustomGeneralPrompt(this);
        if (cg == null || cg.isEmpty()) {
            cg = PromptManager.getDefaultGeneralPrompt();
        }
        customGeneralPrompt.setText(cg);

        String cf = SettingsActivity.getCustomFileOpsPrompt(this);
        if (cf == null || cf.isEmpty()) {
            cf = PromptManager.getDefaultFileOpsPrompt();
        }
        customFileOpsPrompt.setText(cf);

        setupDebouncedSaver(customGeneralPrompt, s -> SettingsActivity.setCustomGeneralPrompt(this, s));
        setupDebouncedSaver(customFileOpsPrompt, s -> SettingsActivity.setCustomFileOpsPrompt(this, s));
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
