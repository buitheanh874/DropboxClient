package vn.edu.usth.dropboxclient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton; // Thêm import này
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dropbox.core.android.Auth;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.R; // Thêm import này
import vn.edu.usth.dropboxclient.utils.PreferenceManager;

public class AuthActivity extends AppCompatActivity {

    private static final String APP_KEY = "xxnncso2v6rq3ml";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_auth);


        MaterialButton loginButton = findViewById(R.id.btn_login_dropbox);


        loginButton.setOnClickListener(v -> {

            DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();
            Auth.startOAuth2PKCE(
                    AuthActivity.this,
                    APP_KEY,
                    config,
                    null,
                    null,
                    null
            );
        });
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
        }

    }
}