package vn.edu.usth.dropboxclient.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.utils.Callback;

public class FakeApi {

    private static final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());

    private static final String PREF = "fake_server";
    private static final String KEY  = "files";
    private static final String KEY_VER = "files_version";
    private static final int DATA_VERSION = 1;

    private static final List<FileItem> SERVER = new ArrayList<>();
    private static int AUTO_ID = 100;

    private static void seedDefaults() {
        SERVER.clear();
        SERVER.add(new FileItem(1, "Documents", String.valueOf(0), "folder"));
        SERVER.add(new FileItem(2, "photo.jpg", String.valueOf(540 * 1024), "file"));
        SERVER.add(new FileItem(3, "music.mp3", String.valueOf(3 * 1024 * 1024), "file"));
        AUTO_ID = 100;
    }

    private static void ensureLoaded(Context ctx) {
        if (!SERVER.isEmpty()) return;
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            int ver = sp.getInt(KEY_VER, 0);
            String json = sp.getString(KEY, null);

            if (ver != DATA_VERSION || json == null || json.trim().isEmpty()) {
                seedDefaults();
                save(ctx);
                sp.edit().putInt(KEY_VER, DATA_VERSION).apply();
                return;
            }

            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                seedDefaults();
                save(ctx);
                return;
            }

            int maxId = 0;
            for (int i = 0; i < arr.length(); i++) {
                FileItem it = FileItem.fromJson(arr.getJSONObject(i));
                SERVER.add(it);
                if (it.getId() > maxId) maxId = it.getId();
            }
            AUTO_ID = Math.max(100, maxId + 1);

        } catch (Exception e) {
            seedDefaults();
            save(ctx);
        }
    }

    private static void save(Context ctx) {
        try {
            JSONArray arr = new JSONArray();
            for (FileItem it : SERVER) arr.put(it.toJson());
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY, arr.toString())
                    .putInt(KEY_VER, DATA_VERSION)
                    .apply();
        } catch (Exception ignored) {}
    }

    public static void reset(Context ctx, Callback<Boolean> cb) {
        io.execute(() -> {
            seedDefaults();
            save(ctx);
            main.post(() -> cb.onSuccess(true));
        });
    }

    public static void getFileList(Context ctx, Callback<List<FileItem>> cb) {
        io.execute(() -> {
            try {
                ensureLoaded(ctx);
                Thread.sleep(300);
                main.post(() -> cb.onSuccess(new ArrayList<>(SERVER)));
            } catch (Exception e) {
                main.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public static void upload(Context ctx, FileItem file, Callback<FileItem> cb) {
        io.execute(() -> {
            try {
                ensureLoaded(ctx);
                Thread.sleep(600);
                FileItem uploaded = new FileItem(
                        AUTO_ID++,
                        file.getName(),
                        file.getSize(),
                        "file"
                );
                SERVER.add(uploaded);
                save(ctx);
                main.post(() -> cb.onSuccess(uploaded));
            } catch (Exception e) {
                main.post(() -> cb.onError("Upload failed: " + e.getMessage()));
            }
        });
    }

    public static void delete(Context ctx, int id, Callback<Boolean> cb) {
        io.execute(() -> {
            try {
                ensureLoaded(ctx);
                Thread.sleep(400);
                boolean removed = SERVER.removeIf(it -> it.getId() == id);
                if (removed) save(ctx);
                main.post(() -> cb.onSuccess(removed));
            } catch (Exception e) {
                main.post(() -> cb.onError("Delete failed: " + e.getMessage()));
            }
        });
    }
}
