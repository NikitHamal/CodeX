package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.AIProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Context context;
    private final List<Object> items = new ArrayList<>();
    private final Map<AIProvider, List<AIModel>> modelsByProvider;

    public ModelAdapter(Context context, List<AIModel> models) {
        this.context = context;
        this.modelsByProvider = new LinkedHashMap<>();

        for (AIModel model : models) {
            if (!modelsByProvider.containsKey(model.getProvider())) {
                modelsByProvider.put(model.getProvider(), new ArrayList<>());
            }
            modelsByProvider.get(model.getProvider()).add(model);
        }

        for (Map.Entry<AIProvider, List<AIModel>> entry : modelsByProvider.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof AIProvider) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_provider_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_model, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.bind((AIProvider) items.get(position));
        } else {
            ModelViewHolder modelViewHolder = (ModelViewHolder) holder;
            modelViewHolder.bind((AIModel) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView providerName;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            providerName = itemView.findViewById(R.id.text_provider_name);
        }

        public void bind(AIProvider provider) {
            providerName.setText(provider.name());
        }
    }

    class ModelViewHolder extends RecyclerView.ViewHolder {
        private final TextView modelName;
        private final TextView modelId;
        private final CheckBox modelEnabledCheckbox;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.text_model_name);
            modelId = itemView.findViewById(R.id.text_model_id);
            modelEnabledCheckbox = itemView.findViewById(R.id.checkbox_model_enabled);
        }

        public void bind(AIModel model) {
            modelName.setText(model.getDisplayName());
            modelId.setText(model.getModelId());

            android.content.SharedPreferences prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
            String key = "model_" + model.getModelId() + "_enabled";
            boolean isEnabled = prefs.getBoolean(key, true); // Default to enabled
            modelEnabledCheckbox.setChecked(isEnabled);

            modelEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(key, isChecked).apply();
            });
        }
    }
}
