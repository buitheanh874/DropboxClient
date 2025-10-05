package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.network.FakeApi;
import vn.edu.usth.dropboxclient.utils.Callback;

public class UploadActivity extends AppCompatActivity {

    private EditText etName, etSize;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        etName = findViewById(R.id.etName);
        etSize = findViewById(R.id.etSize); // bytes, ví dụ 2048
        progress = findViewById(R.id.progressUpload);
        Button btnUpload = findViewById(R.id.btnUpload);

        btnUpload.setOnClickListener(v -> doUpload());
    }

    private void doUpload() {
        String name = etName.getText().toString().trim();
        String sizeStr = etSize.getText().toString().trim();
        if (name.isEmpty()) { etName.setError("Enter file name"); return; }
        if (sizeStr.isEmpty()) sizeStr = "0";

        setLoading(true);

        // Model hiện tại: (id, name, size(String), type)
        FileItem temp = new FileItem(0, name, sizeStr, "file");

        FakeApi.upload(getApplicationContext(), temp, new Callback<FileItem>() {
            @Override
            public void onSuccess(FileItem data) {
                setLoading(false);
                Toast.makeText(UploadActivity.this, "Upload success: " + data.getName(), Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(UploadActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
