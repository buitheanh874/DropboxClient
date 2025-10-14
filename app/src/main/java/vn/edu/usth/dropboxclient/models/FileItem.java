package vn.edu.usth.dropboxclient.models;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.io.Serializable;

public class FileItem implements Serializable {


    private String id;
    private String name;
    private String path;
    private boolean isFolder;
    private Date modified;
    private long size;
    private String type;

    public FileItem(FileMetadata metadata) {
        this.id = metadata.getId();
        this.name = metadata.getName();
        this.path = metadata.getPathLower();
        this.isFolder = false;
        this.modified = metadata.getServerModified();
        this.size = metadata.getSize();
        this.type = extractType(name);
    }

    public FileItem(FolderMetadata metadata) {
        this.id = metadata.getId();
        this.name = metadata.getName();
        this.path = metadata.getPathLower();
        this.isFolder = true;
        this.modified = null;
        this.size = 0;
        this.type = "folder";
    }
    public FileItem(Metadata metadata) {
        this.name = metadata.getName();
        this.path = metadata.getPathLower();
        this.id = metadata.getPathLower();
        if (metadata instanceof FileMetadata) {
            FileMetadata f = (FileMetadata) metadata;
            this.isFolder = false;
            this.modified = f.getServerModified();
            this.size = f.getSize();
            this.type = extractType(f.getName());
        } else {
            this.isFolder = true;
            this.modified = null;
            this.size = 0;
            this.type = "folder";
        }
    }

    private String extractType(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase(Locale.getDefault());
    }
    public FileItem(String id, String name, String path, String type, long size, String modifiedStr) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.size = size;
        this.isFolder = "folder".equalsIgnoreCase(type);

        if (modifiedStr != null && !modifiedStr.isEmpty()) {
            try {
                this.modified = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(modifiedStr);
            } catch (Exception e) {
                this.modified = null;
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public Date getModified() {
        return modified;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String getFormattedDate() {
        if (modified == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(modified);
    }
    public String getFormattedSize() {
        if (isFolder) return "";
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", size / Math.pow(1024, exp), pre);
    }
}
