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

    // 🔑 App key của bạn (thay bằng key thật trong Dropbox App Console)
    private static final String APP_KEY = "xxnncso2v6rq3ml";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cấu hình client Dropbox SDK
        DbxRequestConfig config = DbxRequestConfig.newBuilder("USTH-DropboxClient/1.0").build();

        // Bắt đầu quy trình xác thực OAuth2 (PKCE)
        Auth.startOAuth2PKCE(
                this,           // Context hiện tại
                APP_KEY,        // App key
                config,         // Config của Dropbox
                null,           // Scope (null = quyền mặc định)
                null,           // Redirect URI (SDK tự xử lý)
                null            // Session ID (bỏ qua)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Khi quay lại sau khi login thành công
        DbxCredential credential = Auth.getDbxCredential();

        if (credential != null) {
            // ✅ Lưu credential để dùng sau này
            PreferenceManager pref = new PreferenceManager(this);
            pref.saveDropboxCredential(credential);

            // ✅ Khởi tạo Dropbox client
            DropboxClientFactory.init(credential);

            // ✅ Chuyển sang màn hình chính
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // ❌ Nếu login thất bại hoặc bị hủy
            Toast.makeText(this, "Login failed or cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}
