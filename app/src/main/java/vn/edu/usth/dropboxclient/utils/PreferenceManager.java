package vn.edu.usth.dropboxclient.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.dropbox.core.oauth.DbxCredential;

public class PreferenceManager {

    private static final String PREF_NAME = "DropboxClonePrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SORT_BY = "sort_by";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String SORT_NAME = "name";
    public static final String SORT_DATE = "date";
    public static final String SORT_SIZE = "size";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_APP_KEY = "app_key";

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setTheme(String theme) {
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, THEME_LIGHT);
    }

    public void setSortBy(String sortBy) {
        prefs.edit().putString(KEY_SORT_BY, sortBy).apply();
    }

    public String getSortBy() {
        return prefs.getString(KEY_SORT_BY, SORT_NAME);
    }

    public void saveDropboxCredential(DbxCredential credential) {
        if (credential == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, credential.getAccessToken());
        editor.putString(KEY_REFRESH_TOKEN, credential.getRefreshToken());
        editor.putLong(KEY_EXPIRES_AT, credential.getExpiresAt());
        editor.putString(KEY_APP_KEY, credential.getAppKey());
        editor.apply();
    }

    public DbxCredential getDropboxCredential() {
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1);
        String appKey = prefs.getString(KEY_APP_KEY, null);

        if (accessToken == null || refreshToken == null || appKey == null) {
            return null;
        }
        return new DbxCredential(accessToken, expiresAt, refreshToken, appKey);
    }

    public void clearDropboxCredential() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_EXPIRES_AT)
                .remove(KEY_APP_KEY)
                .apply();
    }
}
