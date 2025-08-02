package com.codex.apk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import com.codex.apk.ai.AIModel;

public class ModelPickerAdapter extends RecyclerView.Adapter<ModelPickerAdapter.ModelViewHolder> {
    
    private List<AIModel> models;
    private AIModel selectedModel;
    private OnModelSelectedListener listener;
    
    public interface OnModelSelectedListener {
        void onModelSelected(AIModel model);
    }
    
    public ModelPickerAdapter(List<AIModel> models, AIModel selectedModel, OnModelSelectedListener listener) {
        this.models = models;
        this.selectedModel = selectedModel;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_picker, parent, false);
        return new ModelViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        AIModel model = models.get(position);
        holder.bind(model, model == selectedModel);
    }
    
    @Override
    public int getItemCount() {
        return models.size();
    }
    
    public void updateSelectedModel(AIModel model) {
        this.selectedModel = model;
        notifyDataSetChanged();
    }
    
    class ModelViewHolder extends RecyclerView.ViewHolder {
        private RadioButton radioModel;
        private TextView textModelName;
        
        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            radioModel = itemView.findViewById(R.id.radio_model);
            textModelName = itemView.findViewById(R.id.text_model_name);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModelSelected(models.get(getAdapterPosition()));
                }
            });
        }
        
        public void bind(AIModel model, boolean isSelected) {
            radioModel.setChecked(isSelected);
            textModelName.setText(model.getDisplayName());
        }
    }
}