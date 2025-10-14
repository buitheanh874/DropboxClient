package vn.edu.usth.dropboxclient;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.oauth.DbxCredential;

/**
 * Factory để tạo và quản lý một đối tượng duy nhất DbxClientV2
 * Giúp gọi Dropbox API ở mọi nơi sau khi đăng nhập thành công.
 */
public class DropboxClientFactory {

    private static DbxClientV2 client;

    /**
     * Khởi tạo client sau khi có credential (chỉ gọi 1 lần sau login).
     */
    public static void init(DbxCredential credential) {
        if (credential == null) {
            throw new IllegalArgumentException("DbxCredential must not be null.");
        }

        // Tạo cấu hình cho Dropbox SDK
        DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();

        // Tạo client chính thức
        client = new DbxClientV2(config, credential);
    }

    /**
     * Trả về client hiện tại để dùng trong toàn app.
     */
    public static DbxClientV2 getClient() {
        if (client == null) {
            throw new IllegalStateException("Dropbox client not initialized. Call init() after login first.");
        }
        return client;
    }

    /**
     * Xoá client (khi logout)
     */
    public static void clear() {
        client = null;
    }
}
