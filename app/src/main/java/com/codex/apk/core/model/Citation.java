package com.codex.apk.core.model;

public class Citation {
    private final int startIndex;
    private final int endIndex;
    private final String url;
    private final String license;

    public Citation(int startIndex, int endIndex, String url, String license) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.url = url;
        this.license = license;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getUrl() {
        return url;
    }

    public String getLicense() {
        return license;
    }
}
