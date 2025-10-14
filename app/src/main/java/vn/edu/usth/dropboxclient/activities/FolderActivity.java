package vn.edu.usth.dropboxclient.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FileItem currentFolder;
    private DbxClientV2 dropboxClient;
    private List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

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

        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            Toast.makeText(this, "Dropbox client not initialized", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        swipeRefresh.setOnRefreshListener(() -> {
            loadFolderContents();
            swipeRefresh.setRefreshing(false);
        });

        loadFolderContents();
    }

    private void loadFolderContents() {
        executorService.execute(() -> {
            List<FileItem> files = new ArrayList<>();
            try {
                ListFolderResult result = dropboxClient.files().listFolder(currentFolder.getPath());
                for (Metadata meta : result.getEntries()) {
                    if (meta instanceof FileMetadata) {
                        FileMetadata f = (FileMetadata) meta;
                        files.add(new FileItem(
                                f.getId(),
                                f.getName(),
                                "file",
                                f.getServerModified().toString(),
                                f.getSize(),
                                f.getPathLower()
                        ));
                    } else if (meta instanceof FolderMetadata) {
                        FolderMetadata folder = (FolderMetadata) meta;
                        files.add(new FileItem(
                                folder.getId(),
                                folder.getName(),
                                "folder",
                                "",
                                0,
                                folder.getPathLower()
                        ));
                    }
                }
            } catch (DbxException e) {
                Log.e("Dropbox", "Error loading subfolder contents: ", e);
            }

            runOnUiThread(() -> {
                allFiles = files;
                currentFiles = new ArrayList<>(files);
                adapter.submitList(currentFiles);
                if (files.isEmpty()) {
                    Toast.makeText(FolderActivity.this, "Folder is empty", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onFileClick(FileItem file) {
        if (file.isFolder()) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("folder", file);
            android.content.Intent intent = new android.content.Intent(this, FolderActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            FileDetailBottomSheet bottomSheet = FileDetailBottomSheet.newInstance(file);
            bottomSheet.show(getSupportFragmentManager(), "FileDetailBottomSheet");
        }
    }

    @Override
    public void onFileMenuClick(FileItem file) {
        String[] options = {"Delete", "Rename"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        deleteDropboxFile(file.getPath());
                    } else {
                        Toast.makeText(this, "Rename not yet implemented", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void deleteDropboxFile(String path) {
        executorService.execute(() -> {
            try {
                dropboxClient.files().deleteV2(path);
                runOnUiThread(() -> {
                    Toast.makeText(FolderActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadFolderContents();
                });
            } catch (DbxException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(FolderActivity.this, "Delete failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}