package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;

public class FolderActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;
    private FileItem currentFolder;
    private DbxClientV2 dropboxClient;
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private ActivityResultLauncher<Intent> pickFileLauncher;

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
        emptyView = findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, this);
        recyclerView.setAdapter(adapter);

        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        swipeRefresh.setOnRefreshListener(this::loadFiles);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showMenu());

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadFile(uri);
                    }
                });

        loadFiles();
    }

    private void loadFiles() {
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            List<FileItem> files = new ArrayList<>();
            try {
                for (Metadata meta : dropboxClient.files().listFolder(currentFolder.getPath()).getEntries()) {
                    if (meta instanceof FileMetadata) {
                        FileMetadata f = (FileMetadata) meta;
                        files.add(new FileItem(f.getId(), f.getName(), f.getPathLower(),
                                getType(f.getName()), f.getSize(),
                                f.getServerModified() != null ? f.getServerModified().toString() : ""));
                    } else if (meta instanceof FolderMetadata) {
                        FolderMetadata folder = (FolderMetadata) meta;
                        files.add(new FileItem(folder.getId(), folder.getName(),
                                folder.getPathLower(), "folder", 0, ""));
                    }
                }
            } catch (DbxException e) {
                runOnUiThread(() -> Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show());
            }

            runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                adapter.submitList(files);
                if (files.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            });
        });
    }

    private String getType(String name) {
        if (name == null || !name.contains(".")) return "file";
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    private void showMenu() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("New")
                .setItems(new String[]{"Upload File", "Create Folder"}, (d, w) -> {
                    if (w == 0) openFilePicker();
                    else showCreateFolderDialog();
                })
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickFileLauncher.launch(Intent.createChooser(intent, "Select file"));
    }

    private void uploadFile(Uri uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("Cannot open file");

                String name = getFileName(uri);
                if (name == null) name = "file_" + System.currentTimeMillis();

                dropboxClient.files().uploadBuilder(currentFolder.getPath() + "/" + name)
                        .withMode(WriteMode.ADD).uploadAndFinish(is);

                String finalName = name;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Uploaded: " + finalName, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String getFileName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) return cursor.getString(idx);
                }
            } catch (Exception e) {
                Log.e("FolderActivity", "Error getting file name", e);
            }
        }
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            return cut != -1 ? path.substring(cut + 1) : path;
        }
        return null;
    }

    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Folder")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) createFolder(name);
                    else Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createFolder(String name) {
        executor.execute(() -> {
            try {
                dropboxClient.files().createFolderV2(currentFolder.getPath() + "/" + name);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            } catch (DbxException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onFileClick(FileItem file) {
        if (file.isFolder()) {
            Intent intent = new Intent(this, FolderActivity.class);
            intent.putExtra("folder", file);
            startActivity(intent);
        } else {
            FileDetailBottomSheet sheet = FileDetailBottomSheet.newInstance(file);
            sheet.setOnFileDeletedListener(this::loadFiles);
            sheet.show(getSupportFragmentManager(), "FileDetail");
        }
    }

    @Override
    public void onFileMenuClick(FileItem file) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(new String[]{"Rename", "Delete"}, (d, w) -> {
                    if (w == 0) showRenameDialog(file);
                    else deleteFile(file);
                })
                .show();
    }

    private void showRenameDialog(FileItem file) {
        EditText input = new EditText(this);
        input.setText(file.getName());
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) renameFile(file, name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameFile(FileItem file, String newName) {
        executor.execute(() -> {
            try {
                String oldPath = file.getPath();
                int idx = oldPath.lastIndexOf('/');
                String parent = idx > 0 ? oldPath.substring(0, idx) : "";
                dropboxClient.files().moveV2(oldPath, parent + "/" + newName);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Renamed to: " + newName, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            } catch (DbxException e) {
                runOnUiThread(() -> Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteFile(FileItem file) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete")
                .setMessage("Delete '" + file.getName() + "'?")
                .setPositiveButton("Delete", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(file.getPath());
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadFiles();
                            });
                        } catch (DbxException e) {
                            runOnUiThread(() -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}