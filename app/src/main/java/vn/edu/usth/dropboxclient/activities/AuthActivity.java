package vn.edu.usth.dropboxclient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.android.Auth;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;

public class AuthActivity extends AppCompatActivity {

    private static final String APP_KEY = "xxnncso2v6rq3ml";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();

        Auth.startOAuth2PKCE(
                this,
                APP_KEY,
                config,
                null,
                null,
                null
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        DbxCredential credential = Auth.getDbxCredential();

        if (credential != null) {
            PreferenceManager pref = new PreferenceManager(this);
            pref.saveDropboxCredential(credential);
            DropboxClientFactory.init(credential);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Login failed or cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}
