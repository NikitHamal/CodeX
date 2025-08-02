package com.codex.apk.ai;

public class WebSource {
    public final String url;
    public final String title;
    public final String snippet;
    public final String favicon;

    public WebSource(String url, String title, String snippet, String favicon) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.favicon = favicon;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getFavicon() {
        return favicon;
    }
}
