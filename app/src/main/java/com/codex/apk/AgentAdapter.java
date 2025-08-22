package com.codex.apk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.ai.AIModel;
import com.codex.apk.ai.ModelRegistry;

import java.util.List;

public class AgentAdapter extends RecyclerView.Adapter<AgentAdapter.AgentViewHolder> {

    private final Context context;
    private final List<CustomAgent> agents;
    private final OnAgentEditClickListener listener;

    public interface OnAgentEditClickListener {
        void onEditClick(CustomAgent agent);
    }

    public AgentAdapter(Context context, List<CustomAgent> agents, OnAgentEditClickListener listener) {
        this.context = context;
        this.agents = agents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AgentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_agent, parent, false);
        return new AgentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgentViewHolder holder, int position) {
        CustomAgent agent = agents.get(position);
        holder.bind(agent, listener);
    }

    @Override
    public int getItemCount() {
        return agents.size();
    }

    static class AgentViewHolder extends RecyclerView.ViewHolder {
        private final TextView agentName;
        private final TextView agentPrompt;
        private final TextView agentModel;
        private final View editButton;

        public AgentViewHolder(@NonNull View itemView) {
            super(itemView);
            agentName = itemView.findViewById(R.id.text_agent_name);
            agentPrompt = itemView.findViewById(R.id.text_agent_prompt);
            agentModel = itemView.findViewById(R.id.text_agent_model);
            editButton = itemView.findViewById(R.id.button_edit_agent);
        }

        public void bind(CustomAgent agent, OnAgentEditClickListener listener) {
            agentName.setText(agent.name);
            agentPrompt.setText(agent.prompt);

            AIModel model = ModelRegistry.byId(agent.modelId);
            if (model != null) {
                agentModel.setText("Model: " + model.getDisplayName());
            } else {
                agentModel.setText("Model: " + agent.modelId);
            }

            editButton.setOnClickListener(v -> listener.onEditClick(agent));
        }
    }
}
