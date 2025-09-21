package com.codex.apk;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;
import com.codex.apk.ai.ModelCapabilities;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class ModelsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.activity_models);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_models);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fab = findViewById(R.id.fab_add_model);
        fab.setOnClickListener(v -> {
            // Show add model dialog
            showAddModelDialog();
        });

        setupAdapter();
    }

    private void setupAdapter() {
        ModelAdapter adapter = new ModelAdapter(this, AIModel.getAllModels()) {
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                if (holder.getItemViewType() != 0) {
                    holder.itemView.setOnLongClickListener(view -> {
                        Object item = ((java.util.List<?>) getItems()).get(position);
                        if (item instanceof com.codex.apk.ai.AIModel) {
                            showModelActionsDialog((com.codex.apk.ai.AIModel) item);
                        }
                        return true;
                    });
                }
            }
            public java.util.List<?> getItems() { try { java.lang.reflect.Field f = ModelAdapter.class.getDeclaredField("items"); f.setAccessible(true); return (java.util.List<?>) f.get(this);} catch(Exception e){ return java.util.Collections.emptyList(); } }
        };
        recyclerView.setAdapter(adapter);
    }

    private void showAddModelDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Model");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_model, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText modelNameEditText = dialogView.findViewById(R.id.edit_text_model_name);
        com.google.android.material.textfield.TextInputEditText modelIdEditText = dialogView.findViewById(R.id.edit_text_model_id);
        android.widget.AutoCompleteTextView providerAutoComplete = dialogView.findViewById(R.id.auto_complete_provider);

        java.util.List<String> providerNames = new java.util.ArrayList<>();
        for (AIProvider provider : AIProvider.values()) {
            providerNames.add(provider.name());
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providerNames);
        providerAutoComplete.setAdapter(adapter);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = modelNameEditText.getText().toString().trim();
            String id = modelIdEditText.getText().toString().trim();
            String providerName = providerAutoComplete.getText().toString().trim();

            if (name.isEmpty() || id.isEmpty() || providerName.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            AIProvider provider = AIProvider.valueOf(providerName);
            AIModel.addCustomModel(new AIModel(id, name, provider, new ModelCapabilities(false, false, false, true, false, false, false, 0, 0)));

            setupAdapter(); // Refresh the list
            Toast.makeText(this, "Model " + name + " added!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showModelActionsDialog(AIModel model) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle(model.getDisplayName());
        String[] actions;
        String defaultModel = getSharedPreferences("model_settings", MODE_PRIVATE).getString("default_model", null);
        boolean isDefault = defaultModel != null && defaultModel.equals(model.getDisplayName());
        if (isDefault) {
            actions = new String[]{"Edit", "Delete", "Remove default"};
        } else {
            actions = new String[]{"Edit", "Delete", "Set as default"};
        }
        builder.setItems(actions, (dialog, which) -> {
            switch (actions[which]) {
                case "Edit":
                    showEditModelDialog(model);
                    break;
                case "Delete":
                    AIModel.removeModelByDisplayName(model.getDisplayName());
                    setupAdapter();
                    Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
                    break;
                case "Set as default":
                    setDefaultModel(model);
                    break;
                case "Remove default":
                    clearDefaultModel();
                    break;
            }
        }).show();
    }

    private void showEditModelDialog(AIModel model) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Model");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_model, null);
        builder.setView(dialogView);
        com.google.android.material.textfield.TextInputEditText modelNameEditText = dialogView.findViewById(R.id.edit_text_model_name);
        com.google.android.material.textfield.TextInputEditText modelIdEditText = dialogView.findViewById(R.id.edit_text_model_id);
        android.widget.AutoCompleteTextView providerAutoComplete = dialogView.findViewById(R.id.auto_complete_provider);
        modelNameEditText.setText(model.getDisplayName());
        modelIdEditText.setText(model.getModelId());
        providerAutoComplete.setText(model.getProvider().name(), false);
        java.util.List<String> providerNames = new java.util.ArrayList<>(); for (AIProvider p : AIProvider.values()) providerNames.add(p.name());
        providerAutoComplete.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, providerNames));
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = modelNameEditText.getText().toString().trim();
            String id = modelIdEditText.getText().toString().trim();
            String providerName = providerAutoComplete.getText().toString().trim();
            if (name.isEmpty() || id.isEmpty() || providerName.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            AIModel.updateModel(model.getDisplayName(), name, id, AIProvider.valueOf(providerName));
            setupAdapter();
            Toast.makeText(this, "Model updated", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setDefaultModel(AIModel model) {
        getSharedPreferences("model_settings", MODE_PRIVATE).edit().putString("default_model", model.getDisplayName()).apply();
        setupAdapter();
        Toast.makeText(this, "Set default model: " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    private void clearDefaultModel() {
        getSharedPreferences("model_settings", MODE_PRIVATE).edit().remove("default_model").apply();
        setupAdapter();
        Toast.makeText(this, "Default model cleared", Toast.LENGTH_SHORT).show();
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
