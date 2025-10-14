package vn.edu.usth.dropboxclient;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.oauth.DbxCredential;

public class DropboxClientFactory {

    private static DbxClientV2 client;

    public static void init(DbxCredential credential) {
        if (credential == null) {
            throw new IllegalArgumentException("DbxCredential must not be null.");
        }
        DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();
        client = new DbxClientV2(config, credential);
    }
    public static DbxClientV2 getClient() {
        if (client == null) {
            throw new IllegalStateException("Dropbox client not initialized. Call init() after login first.");
        }
        return client;
    }
    public static void clear() {
        client = null;
    }
}
