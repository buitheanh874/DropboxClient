package vn.edu.usth.dropboxclient.models;

import org.json.JSONObject;

public class FileItem {
    // Model hiện tại: id(int), name(String), size(String), type(String: "file"/"folder")
    private int id;
    private String name;
    private String size;
    private String type;

    public FileItem(int id, String name, String size, String type) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSize() { return size; }
    public String getType() { return type; }

    // --- JSON helpers dùng để persist qua SharedPreferences ---
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("size", size);
            o.put("type", type);
        } catch (Exception ignored) {}
        return o;
    }

    public static FileItem fromJson(JSONObject o) {
        return new FileItem(
                o.optInt("id"),
                o.optString("name"),
                o.optString("size"),
                o.optString("type")
        );
    }
}
