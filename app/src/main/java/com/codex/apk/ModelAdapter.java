package com.codex.apk;

import android.content.Context;
import android.content.SharedPreferences;
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
    private final Map<String, Boolean> checkedStates = new LinkedHashMap<>();
    private final SharedPreferences prefs;
    private OnProviderHeaderLongClickListener headerLongClickListener;

    public ModelAdapter(Context context, List<AIModel> models) {
        this.context = context;
        this.prefs = context.getSharedPreferences("model_settings", Context.MODE_PRIVATE);
        this.modelsByProvider = new LinkedHashMap<>();

        // Initialize checked states from SharedPreferences
        for (AIModel model : models) {
            String key = "model_" + model.getDisplayName() + "_enabled";
            checkedStates.put(model.getDisplayName(), prefs.getBoolean(key, true));
        }


        // Group models by provider
        for (AIModel model : models) {
            if (!modelsByProvider.containsKey(model.getProvider())) {
                modelsByProvider.put(model.getProvider(), new ArrayList<>());
            }
            modelsByProvider.get(model.getProvider()).add(model);
        }

        // Create a flat list for the adapter
        for (Map.Entry<AIProvider, List<AIModel>> entry : modelsByProvider.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }
    }

    public interface OnProviderHeaderLongClickListener {
        void onProviderHeaderLongClick(AIProvider provider);
    }

    public void setOnProviderHeaderLongClickListener(OnProviderHeaderLongClickListener l) {
        this.headerLongClickListener = l;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof AIProvider ? TYPE_HEADER : TYPE_ITEM;
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
            AIProvider provider = (AIProvider) items.get(position);
            headerViewHolder.bind(provider);
        } else {
            ModelViewHolder modelViewHolder = (ModelViewHolder) holder;
            AIModel model = (AIModel) items.get(position);
            modelViewHolder.bind(model);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView providerName;
        private final CheckBox selectAllCheckbox;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            providerName = itemView.findViewById(R.id.text_provider_name);
            selectAllCheckbox = itemView.findViewById(R.id.checkbox_select_all);
        }

        public void bind(AIProvider provider) {
            providerName.setText(provider.name());

            List<AIModel> providerModels = modelsByProvider.get(provider);
            boolean allChecked = true;
            if (providerModels != null && !providerModels.isEmpty()) {
                for (AIModel model : providerModels) {
                    if (!checkedStates.getOrDefault(model.getDisplayName(), true)) {
                        allChecked = false;
                        break;
                    }
                }
            } else {
                allChecked = false;
            }

            selectAllCheckbox.setOnCheckedChangeListener(null);
            selectAllCheckbox.setChecked(allChecked);

            selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (providerModels != null) {
                    SharedPreferences.Editor editor = prefs.edit();
                    for (AIModel model : providerModels) {
                        checkedStates.put(model.getDisplayName(), isChecked);
                        String key = "model_" + model.getDisplayName() + "_enabled";
                        editor.putBoolean(key, isChecked);
                    }
                    editor.apply();
                    notifyDataSetChanged();
                }
            });

            // Long press on header invokes callback if set
            itemView.setOnLongClickListener(v -> {
                if (headerLongClickListener != null) {
                    headerLongClickListener.onProviderHeaderLongClick(provider);
                    return true;
                }
                return false;
            });
        }
    }

    class ModelViewHolder extends RecyclerView.ViewHolder {
        private final TextView modelName;
        private final TextView modelId;
        private final TextView defaultBadge;
        private final CheckBox modelEnabledCheckbox;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.text_model_name);
            modelId = itemView.findViewById(R.id.text_model_id);
            defaultBadge = itemView.findViewById(R.id.text_default_badge);
            modelEnabledCheckbox = itemView.findViewById(R.id.checkbox_model_enabled);
        }

        public void bind(AIModel model) {
            String defaultModel = prefs.getString("default_model", null);
            boolean isDefault = defaultModel != null && defaultModel.equals(model.getDisplayName());
            modelName.setText(model.getDisplayName());
            defaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);
            modelId.setText(model.getModelId());

            modelEnabledCheckbox.setOnCheckedChangeListener(null);
            modelEnabledCheckbox.setChecked(checkedStates.getOrDefault(model.getDisplayName(), true));

            modelEnabledCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkedStates.put(model.getDisplayName(), isChecked);
                String key = "model_" + model.getDisplayName() + "_enabled";
                prefs.edit().putBoolean(key, isChecked).apply();
                notifyDataSetChanged(); // To update the header checkbox
            });
        }
    }
}
