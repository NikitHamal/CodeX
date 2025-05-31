package com.codex.apk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelSelectorBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SELECTED_MODEL = "selected_model";
    private static final String ARG_MODEL_NAMES = "model_names";

    private String selectedModel;
    private ArrayList<String> modelNames;
    private ModelSelectionListener listener;

    public interface ModelSelectionListener {
        void onModelSelected(String selectedModelDisplayName);
    }

    public static ModelSelectorBottomSheet newInstance(String selectedModel, List<String> modelNames) {
        ModelSelectorBottomSheet fragment = new ModelSelectorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_MODEL, selectedModel);
        args.putStringArrayList(ARG_MODEL_NAMES, new ArrayList<>(modelNames));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedModel = getArguments().getString(ARG_SELECTED_MODEL);
            modelNames = getArguments().getStringArrayList(ARG_MODEL_NAMES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_model_selector_bottom_sheet, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_models);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ModelAdapter adapter = new ModelAdapter(modelNames, selectedModel, model -> {
            if (listener != null) {
                listener.onModelSelected(model);
            }
            dismiss(); // Dismiss the bottom sheet after selection
        });
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void setModelSelectionListener(ModelSelectionListener listener) {
        this.listener = listener;
    }

    // Adapter for the RecyclerView
    private static class ModelAdapter extends RecyclerView.Adapter<ModelAdapter.ModelViewHolder> {

        private final List<String> models;
        private String currentSelectedModel;
        private final OnModelClickListener clickListener;

        public interface OnModelClickListener {
            void onModelClick(String model);
        }

        public ModelAdapter(List<String> models, String currentSelectedModel, OnModelClickListener clickListener) {
            this.models = models;
            this.currentSelectedModel = currentSelectedModel;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model_selection, parent, false);
            return new ModelViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
            String model = models.get(position);
            holder.modelName.setText(model);
            if (model.equals(currentSelectedModel)) {
                holder.checkmark.setVisibility(View.VISIBLE);
                holder.modelName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary)); // Highlight selected
            } else {
                holder.checkmark.setVisibility(View.GONE);
                holder.modelName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.on_surface));
            }
            holder.itemView.setOnClickListener(v -> {
                currentSelectedModel = model; // Update selection
                notifyDataSetChanged(); // Refresh UI to show new selection
                clickListener.onModelClick(model);
            });
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        static class ModelViewHolder extends RecyclerView.ViewHolder {
            TextView modelName;
            ImageView checkmark;

            ModelViewHolder(@NonNull View itemView) {
                super(itemView);
                modelName = itemView.findViewById(R.id.text_model_name);
                checkmark = itemView.findViewById(R.id.image_checkmark);
            }
        }
    }
}