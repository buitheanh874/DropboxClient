package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.network.FakeApi;
import vn.edu.usth.dropboxclient.utils.Callback;

public class DetailActivity extends AppCompatActivity {

    private int fileId = -1;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        TextView tvName = findViewById(R.id.tvName);
        TextView tvSize = findViewById(R.id.tvSize);
        progress = findViewById(R.id.progressDelete);
        Button btnDelete = findViewById(R.id.btnDelete);

        fileId = getIntent().getIntExtra("id", -1);
        String name = getIntent().getStringExtra("name");
        String size = getIntent().getStringExtra("size");

        tvName.setText(name != null ? name : "");
        tvSize.setText(size != null ? size : "");

        if (fileId < 0) {
            Toast.makeText(this, "Invalid file id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnDelete.setOnClickListener(v -> doDelete());
    }

    private void doDelete() {
        setLoading(true);
        FakeApi.delete(getApplicationContext(), fileId, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                setLoading(false);
                if (ok) {
                    Toast.makeText(DetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(DetailActivity.this, "Not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(DetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
