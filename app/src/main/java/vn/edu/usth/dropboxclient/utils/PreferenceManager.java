package vn.edu.usth.dropboxclient.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "DropboxClonePrefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SORT_BY = "sort_by";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String SORT_NAME = "name";
    public static final String SORT_DATE = "date";
    public static final String SORT_SIZE = "size";

    private SharedPreferences prefs;

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
}