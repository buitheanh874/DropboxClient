package vn.edu.usth.dropboxclient.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.utils.MockDataProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;

public class FolderActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FileItem currentFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        currentFolder = (FileItem) getIntent().getSerializableExtra("folder");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentFolder.getName());

        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, this);
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadFolderContents();
            swipeRefresh.setRefreshing(false);
        });

        loadFolderContents();
    }

    private void loadFolderContents() {
        List<FileItem> files = MockDataProvider.getInstance().getFilesForParent(currentFolder.getId());
        adapter.submitList(files);
    }

    @Override
    public void onFileClick(FileItem file) {
        if (file.isFolder()) {
            // Navigate to subfolder (not implemented - would need recursive activity)
            Toast.makeText(this, "Opening " + file.getName(), Toast.LENGTH_SHORT).show();
        } else {
            FileDetailBottomSheet bottomSheet = FileDetailBottomSheet.newInstance(file);
            bottomSheet.show(getSupportFragmentManager(), "FileDetailBottomSheet");
        }
    }

    @Override
    public void onFileMenuClick(FileItem file) {
        String[] options = {"Share", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "Share (mock)", Toast.LENGTH_SHORT).show();
                    } else {
                        MockDataProvider.getInstance().deleteFile(file.getId());
                        loadFolderContents();
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}