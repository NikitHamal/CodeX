package com.codex.apk;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class AgentsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.setupTheme(this);
        setContentView(R.layout.activity_agents);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_agents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fab = findViewById(R.id.fab_add_agent);
        fab.setOnClickListener(v -> {
            showAddAgentDialog(null);
        });

        setupAdapter();
    }

    private void setupAdapter() {
        AgentAdapter adapter = new AgentAdapter(this, SettingsActivity.getCustomAgents(this), agent -> {
            showAddAgentDialog(agent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void showAddAgentDialog(CustomAgent agentToEdit) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle(agentToEdit == null ? "Add New Agent" : "Edit Agent");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_agent, null);
        builder.setView(dialogView);

        com.google.android.material.textfield.TextInputEditText agentNameEditText = dialogView.findViewById(R.id.edit_text_agent_name);
        com.google.android.material.textfield.TextInputEditText agentPromptEditText = dialogView.findViewById(R.id.edit_text_agent_prompt);
        android.widget.AutoCompleteTextView modelAutoComplete = dialogView.findViewById(R.id.auto_complete_model);

        java.util.List<String> modelNames = AIModel.getAllDisplayNames();
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, modelNames);
        modelAutoComplete.setAdapter(adapter);

        if (agentToEdit != null) {
            agentNameEditText.setText(agentToEdit.name);
            agentPromptEditText.setText(agentToEdit.prompt);
            AIModel model = AIModel.fromModelId(agentToEdit.modelId);
            if (model != null) {
                modelAutoComplete.setText(model.getDisplayName(), false);
            }
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = agentNameEditText.getText().toString().trim();
            String prompt = agentPromptEditText.getText().toString().trim();
            String modelDisplayName = modelAutoComplete.getText().toString().trim();

            if (name.isEmpty() || prompt.isEmpty() || modelDisplayName.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            AIModel selectedModel = AIModel.fromDisplayName(modelDisplayName);
            if (selectedModel == null) {
                Toast.makeText(this, "Invalid model selected", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.List<CustomAgent> agents = SettingsActivity.getCustomAgents(this);
            if (agentToEdit == null) {
                // Add new agent
                agents.add(new CustomAgent(java.util.UUID.randomUUID().toString(), name, prompt, selectedModel.getModelId()));
            } else {
                // Update existing agent
                agentToEdit.name = name;
                agentToEdit.prompt = prompt;
                agentToEdit.modelId = selectedModel.getModelId();
            }
            SettingsActivity.setCustomAgents(this, agents);
            setupAdapter(); // Refresh the list
        });
        builder.setNegativeButton("Cancel", null);
        if (agentToEdit != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                java.util.List<CustomAgent> agents = SettingsActivity.getCustomAgents(this);
                agents.removeIf(a -> a.id.equals(agentToEdit.id));
                SettingsActivity.setCustomAgents(this, agents);
                setupAdapter(); // Refresh the list
            });
        }
        builder.show();
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
