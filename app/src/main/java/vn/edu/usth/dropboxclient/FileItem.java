package vn.edu.usth.dropboxclient;

public class FileItem {
    public String name;
    public String size;
    public String type;

    public FileItem(String name, String size, String type) {
        this.name = name;
        this.size = size;
        this.type = type;
    }
}
