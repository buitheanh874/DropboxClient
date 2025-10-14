
    package vn.edu.usth.dropboxclient;

import android.content.Context;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

    public class DropboxConfig {
        private static DbxClientV2 client;

        public static DbxClientV2 getClient(Context context) {
            if (client == null) {
                String accessToken = context
                        .getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE)
                        .getString("access_token", null);
                if (accessToken == null) {
                    throw new IllegalStateException("Access token is null. User not logged in.");
                }
                DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();
                client = new DbxClientV2(config, accessToken);
            }
            return client;
        }
    }


