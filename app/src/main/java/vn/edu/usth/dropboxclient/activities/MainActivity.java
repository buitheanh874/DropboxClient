package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.network.FakeApi;
import vn.edu.usth.dropboxclient.utils.Callback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private final List<FileItem> fileList = new ArrayList<>();

    // Chỉ reload khi activity con trả RESULT_OK (upload/delete xong)
    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadFiles();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: MainActivity started");

        recyclerView = findViewById(R.id.rvFiles);
        tvEmpty = findViewById(R.id.tvEmpty);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        FloatingActionButton fab = findViewById(R.id.fab);

        adapter = new FileAdapter(fileList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load 1 lần khi mở app
        loadFiles();

        // Pull to refresh
        swipeRefresh.setOnRefreshListener(this::loadFiles);

        // Mở UploadActivity
        fab.setOnClickListener(v ->
                launcher.launch(new Intent(MainActivity.this, UploadActivity.class))
        );
    }

    private void loadFiles() {
        swipeRefresh.setRefreshing(true);
        FakeApi.getFileList(getApplicationContext(), new Callback<List<FileItem>>() {
            @Override
            public void onSuccess(List<FileItem> files) {
                Log.i(TAG, "loadFiles: Success, count=" + files.size());
                fileList.clear();
                fileList.addAll(files);
                adapter.notifyDataSetChanged();

                if (files.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "loadFiles: Error - " + error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }
}
