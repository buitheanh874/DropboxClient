package vn.edu.usth.dropboxclient;

import java.io.Serializable;

public class FileItem implements Serializable {
    public String name;
    public String size;
    public String type;

    public FileItem(String name, String size, String type) {
        this.name = name;
        this.size = size;
        this.type = type;
    }
}
