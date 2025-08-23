package com.codex.apk.core.model;

/**
 * Attachment for messages (images, documents, etc.).
 */
public class Attachment {
    private final String type;
    private final String url;
    private final byte[] data;
    private final String mimeType;
    private final String filename;

    public Attachment(String type, String url, byte[] data, String mimeType, String filename) {
        this.type = type;
        this.url = url;
        this.data = data;
        this.mimeType = mimeType;
        this.filename = filename;
    }

    public String getType() { return type; }
    public String getUrl() { return url; }
    public byte[] getData() { return data; }
    public String getMimeType() { return mimeType; }
    public String getFilename() { return filename; }
}
