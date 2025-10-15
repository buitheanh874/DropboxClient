package vn.edu.usth.dropboxclient;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PreferenceManager prefManager = new PreferenceManager(this);
        String currentTheme = prefManager.getTheme();
        if (PreferenceManager.THEME_DARK.equals(currentTheme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}