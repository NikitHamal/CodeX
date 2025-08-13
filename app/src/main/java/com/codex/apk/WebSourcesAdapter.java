package com.codex.apk;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codex.apk.ai.WebSource;
import java.util.List;

public class WebSourcesAdapter extends RecyclerView.Adapter<WebSourcesAdapter.WebSourceViewHolder> {
    
    private List<WebSource> webSources;
    
    public WebSourcesAdapter(List<WebSource> webSources) {
        this.webSources = webSources;
    }
    
    @NonNull
    @Override
    public WebSourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_web_source, parent, false);
        return new WebSourceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull WebSourceViewHolder holder, int position) {
        WebSource source = webSources.get(position);
        holder.bind(source);
    }
    
    @Override
    public int getItemCount() {
        return webSources != null ? webSources.size() : 0;
    }
    
    class WebSourceViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageFavicon;
        private TextView textTitle;
        private TextView textSnippet;
        private TextView textUrl;
        
        public WebSourceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFavicon = itemView.findViewById(R.id.image_favicon);
            textTitle = itemView.findViewById(R.id.text_title);
            textSnippet = itemView.findViewById(R.id.text_snippet);
            textUrl = itemView.findViewById(R.id.text_url);
            
            itemView.setOnClickListener(v -> {
                WebSource source = webSources.get(getAdapterPosition());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(source.url));
                itemView.getContext().startActivity(intent);
            });
        }
        
        public void bind(WebSource source) {
            textTitle.setText(source.title);
            textSnippet.setText(source.snippet);
            textUrl.setText(source.url);
            
            // TODO: Load favicon from source.favicon if available
            // For now, using default icon
        }
    }
}