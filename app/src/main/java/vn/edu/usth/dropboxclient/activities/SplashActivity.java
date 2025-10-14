package vn.edu.usth.dropboxclient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.dropbox.core.oauth.DbxCredential;
import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            PreferenceManager prefManager = new PreferenceManager(this);
            DbxCredential credential = prefManager.getDropboxCredential();
            if (credential != null) {
                try {
                    DropboxClientFactory.init(credential);
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } catch (Exception e) {
                    startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                }
            } else {
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            finish();
        }, 2000);
    }
}