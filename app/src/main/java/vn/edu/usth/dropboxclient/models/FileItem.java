package vn.edu.usth.dropboxclient.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem implements Serializable {
    private String id;
    private String name;
    private String type;
    private String modified;
    private long size;
    private String parentId;

    public FileItem(String id, String name, String type, String modified, long size, String parentId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.modified = modified;
        this.size = size;
        this.parentId = parentId;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getModified() { return modified; }
    public void setModified(String modified) { this.modified = modified; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public boolean isFolder() {
        return "folder".equalsIgnoreCase(type);
    }

    public String getFormattedSize() {
        if (isFolder()) return "";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(modified);
            return date != null ? outputFormat.format(date) : modified;
        } catch (ParseException e) {
            return modified;
        }
    }

    public Date getModifiedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            return inputFormat.parse(modified);
        } catch (ParseException e) {
            return new Date();
        }
    }
}