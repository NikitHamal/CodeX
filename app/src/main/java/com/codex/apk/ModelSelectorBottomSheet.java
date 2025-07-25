package com.codex.apk;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
// import com.google.android.material.chip.Chip; // Removed for Sketchware Pro compatibility
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectorBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SELECTED_MODEL = "selected_model";
    private static final String ARG_MODEL_NAMES = "model_names";

    private String selectedModel;
    private ArrayList<String> originalModelNames;
    private ArrayList<String> filteredModelNames;
    private ModelSelectionListener listener;
    private ModelAdapter adapter;
    private TextInputEditText searchEditText;

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
            originalModelNames = getArguments().getStringArrayList(ARG_MODEL_NAMES);
            filteredModelNames = new ArrayList<>(originalModelNames);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_model_selector_bottom_sheet, container, false);

        // Initialize search functionality
        searchEditText = view.findViewById(R.id.edit_text_search);
        setupSearchFunctionality();

        // Initialize RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_models);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ModelAdapter(filteredModelNames, selectedModel, model -> {
            if (listener != null) {
                listener.onModelSelected(model);
            }
            dismiss();
        });
        recyclerView.setAdapter(adapter);

        // Model info button
        MaterialButton modelInfoButton = view.findViewById(R.id.button_model_info);
        modelInfoButton.setOnClickListener(v -> showModelInfoDialog());

        return view;
    }

    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterModels(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterModels(String query) {
        filteredModelNames.clear();
        if (query.isEmpty()) {
            filteredModelNames.addAll(originalModelNames);
        } else {
            for (String model : originalModelNames) {
                if (model.toLowerCase().contains(query.toLowerCase())) {
                    filteredModelNames.add(model);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showModelInfoDialog() {
        // TODO: Implement model information dialog
        // This would show detailed information about each model's capabilities
    }

    public void setModelSelectionListener(ModelSelectionListener listener) {
        this.listener = listener;
    }

    // Enhanced Adapter for the RecyclerView
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
            holder.bind(model, model.equals(currentSelectedModel), clickListener);
        }

        @Override
        public int getItemCount() {
            return models.size();
        }

        static class ModelViewHolder extends RecyclerView.ViewHolder {
            TextView modelName;
            TextView modelCapabilities;
            ImageView checkmark;
            ImageView modelIcon;
            LinearLayout featureBadges;
            TextView visionChip;
            TextView functionCallingChip;
            TextView latestChip;

            ModelViewHolder(@NonNull View itemView) {
                super(itemView);
                modelName = itemView.findViewById(R.id.text_model_name);
                modelCapabilities = itemView.findViewById(R.id.text_model_capabilities);
                checkmark = itemView.findViewById(R.id.image_checkmark);
                modelIcon = itemView.findViewById(R.id.image_model_icon);
                featureBadges = itemView.findViewById(R.id.layout_feature_badges);
                visionChip = itemView.findViewById(R.id.chip_vision);
                functionCallingChip = itemView.findViewById(R.id.chip_function_calling);
                latestChip = itemView.findViewById(R.id.chip_latest);
            }

            void bind(String model, boolean isSelected, OnModelClickListener clickListener) {
                modelName.setText(model);
                
                // Get AIModel enum from display name to access capabilities
                AIAssistant.AIModel aiModel = AIAssistant.AIModel.fromDisplayName(model);
                if (aiModel != null) {
                    modelCapabilities.setText(aiModel.getCapabilities());
                    
                    // Show/hide feature badges
                    boolean hasFeatures = false;
                    
                    // Vision capability
                    if (aiModel.supportsVision()) {
                        visionChip.setVisibility(View.VISIBLE);
                        hasFeatures = true;
                    } else {
                        visionChip.setVisibility(View.GONE);
                    }
                    
                    // Function calling capability
                    if (aiModel.supportsFunctionCalling()) {
                        functionCallingChip.setVisibility(View.VISIBLE);
                        hasFeatures = true;
                    } else {
                        functionCallingChip.setVisibility(View.GONE);
                    }
                    
                    // Latest model indicator
                    if (aiModel == AIAssistant.AIModel.GEMINI_2_5_FLASH || 
                        aiModel == AIAssistant.AIModel.GEMINI_2_5_PRO ||
                        aiModel == AIAssistant.AIModel.GEMINI_2_0_FLASH_EXP) {
                        latestChip.setVisibility(View.VISIBLE);
                        hasFeatures = true;
                    } else {
                        latestChip.setVisibility(View.GONE);
                    }
                    
                    featureBadges.setVisibility(hasFeatures ? View.VISIBLE : View.GONE);
                } else {
                    modelCapabilities.setText("Advanced AI capabilities");
                    featureBadges.setVisibility(View.GONE);
                }

                // Selection state
                if (isSelected) {
                    checkmark.setVisibility(View.VISIBLE);
                    itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.primary_container, null));
                } else {
                    checkmark.setVisibility(View.GONE);
                    itemView.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.surface_container_low, null));
                }

                itemView.setOnClickListener(v -> {
                    clickListener.onModelClick(model);
                });
            }
        }
    }
}