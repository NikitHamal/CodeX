package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;

import java.util.List;

public class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelViewHolder> {

    private final Context context;
    private final List<AIModel> models;

    public ModelAdapter(Context context, List<AIModel> models) {
        this.context = context;
        this.models = models;
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_model, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        AIModel model = models.get(position);
        holder.bind(model);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    class ModelViewHolder extends RecyclerView.ViewHolder {
        private final TextView modelName;
        private final TextView modelId;
        private final LinearLayout providerCheckboxesContainer;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.text_model_name);
            modelId = itemView.findViewById(R.id.text_model_id);
            providerCheckboxesContainer = itemView.findViewById(R.id.container_provider_checkboxes);
        }

        public void bind(AIModel model) {
            modelName.setText(model.getDisplayName());
            modelId.setText(model.getModelId());

            providerCheckboxesContainer.removeAllViews();
            android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE);

            for (AIProvider provider : AIProvider.values()) {
                CheckBox checkBox = new CheckBox(context);
                checkBox.setText(provider.name());

                String key = "model_" + model.getModelId() + "_" + provider.name() + "_enabled";
                boolean isEnabled = prefs.getBoolean(key, true); // Default to enabled
                checkBox.setChecked(isEnabled);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean(key, isChecked).apply();
                });

                providerCheckboxesContainer.addView(checkBox);
            }
        }
    }
}
