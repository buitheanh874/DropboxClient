package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import vn.edu.usth.dropboxclient.utils.ErrorHandler;
import vn.edu.usth.dropboxclient.utils.ProgressHelper;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "FolderActivity";
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FileItem currentFolder;
    private DbxClientV2 dropboxClient;
    private List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private ActivityResultLauncher<Intent> pickFileLauncher;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);
        emptyView = findViewById(R.id.empty_view);

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

        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> showFabMenu());
        }
        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            uploadFileToDropbox(fileUri);
                        }
                    }
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
                                f.getPathLower(),
                                getFileType(f.getName()),
                                f.getSize(),
                                f.getServerModified() != null ? f.getServerModified().toString() : ""
                        ));
                    } else if (meta instanceof FolderMetadata) {
                        FolderMetadata folder = (FolderMetadata) meta;
                        files.add(new FileItem(
                                folder.getId(),
                                folder.getName(),
                                folder.getPathLower(),
                                "folder",
                                0,
                                ""
                        ));
                    }
                }
            } catch (DbxException e) {
                Log.e(TAG, "Error loading subfolder contents: ", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ErrorHandler.showErrorDialog(this, "Load Failed", "Could not load folder contents: " + e.getMessage());
                    }
                });
            }

            runOnUiThread(() -> {
                allFiles = files;
                currentFiles = new ArrayList<>(files);
                adapter.submitList(currentFiles);

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

    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "file";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private void showFabMenu() {
        String[] options = {"Upload File", "Create Folder"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("New")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openFilePicker();
                    } else {
                        showCreateFolderDialog();
                    }
                })
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(intent, "Select file to upload");
        pickFileLauncher.launch(chooser);
    }

    private void uploadFileToDropbox(Uri fileUri) {
        if (getApplicationContext() == null) return;
        ProgressHelper progressHelper = new ProgressHelper(this, "Uploading file...");

        String fileName = getFileNameFromUri(fileUri);
        if (fileName == null) {
            fileName = "file_" + System.currentTimeMillis();
        }
        progressHelper.setFileName(fileName);
        progressHelper.show();

        final String finalFileName = fileName;

        executorService.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressHelper.dismiss();
                        ErrorHandler.showErrorDialog(this, "Upload Failed", "Cannot open selected file");
                    });
                    return;
                }

                String dropboxPath = currentFolder.getPath() + "/" + finalFileName;

                dropboxClient.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .uploadAndFinish(inputStream);

                runOnUiThread(() -> {
                    progressHelper.updateProgress(100);
                    progressHelper.dismiss();
                    ErrorHandler.showSuccessDialog(
                            this,
                            "Upload Complete",
                            "File uploaded successfully: " + finalFileName
                    );
                    loadFolderContents();
                });
            } catch (DbxException e) {
                Log.e(TAG, "Dropbox Upload error", e);
                runOnUiThread(() -> {
                    progressHelper.dismiss();
                    ErrorHandler.showErrorDialog(this, "Upload Failed", e.getMessage());
                });
            } catch (IOException e) {
                Log.e(TAG, "IO error during upload", e);
                runOnUiThread(() -> {
                    progressHelper.dismiss();
                    ErrorHandler.showErrorDialog(this, "Upload Failed", "Cannot read file");
                });
            }
        });
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            String[] projection = {android.provider.OpenableColumns.DISPLAY_NAME};
            try (android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying file name", e);
            }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1 && cut + 1 < path.length()) {
                    result = path.substring(cut + 1);
                } else {
                    result = path;
                }
            }
        }
        return result;
    }
    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create Folder")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String folderName = input.getText().toString().trim();
                    if (!folderName.isEmpty()) {
                        createDropboxFolder(folderName);
                    } else {
                        Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createDropboxFolder(String folderName) {
        executorService.execute(() -> {
            try {
                String newFolderPath = currentFolder.getPath() + "/" + folderName;
                dropboxClient.files().createFolderV2(newFolderPath);
                runOnUiThread(() -> {
                    Toast.makeText(FolderActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadFolderContents();
                });
            } catch (DbxException e) {
                Log.e(TAG, "Failed to create folder", e);
                runOnUiThread(() ->
                        ErrorHandler.showErrorDialog(this, "Create Folder Failed", e.getMessage())
                );
            }
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
            bottomSheet.setOnFileDeletedListener(this::loadFolderContents);
            bottomSheet.show(getSupportFragmentManager(), "FileDetailBottomSheet");
        }
    }
    @Override
    public void onFileMenuClick(FileItem file) {
        String[] options = {"Rename", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(file);
                    } else {
                        deleteDropboxFile(file);
                    }
                })
                .show();
    }
    private void deleteDropboxFile(FileItem file) {
        String path = file.getPath();
        if (path == null || path.trim().isEmpty() || path.equals("/")) {
            ErrorHandler.showErrorDialog(this, "Delete Failed", "Cannot delete root folder or invalid path.");
            Log.w(TAG, "Attempted to delete invalid path: " + path);
            return;
        }
        ErrorHandler.showConfirmDialog(
                this,
                "Delete",
                "Are you sure you want to delete '" + file.getName() + "'?",
                "Delete",
                "Cancel",
                () -> {
                    executorService.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(path);
                            runOnUiThread(() -> {
                                Toast.makeText(FolderActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                loadFolderContents();
                            });
                        } catch (DbxException e) {
                            Log.e(TAG, "Delete failed", e);
                            runOnUiThread(() ->
                                    ErrorHandler.showErrorDialog(this, "Delete Failed", e.getMessage())
                            );
                        }
                    });
                },
                null
        );
    }

    private void showRenameDialog(FileItem file) {
        EditText input = new EditText(this);
        input.setText(file.getName());
        input.setHint("New name");
        input.setPadding(50, 20, 50, 20);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(file.getName())) {
                        renameFile(file, newName);
                    } else if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renameFile(FileItem file, String newName) {
        executorService.execute(() -> {
            try {
                String oldPath = file.getPath();
                int lastSlashIndex = oldPath.lastIndexOf('/');
                String parentPath = (lastSlashIndex > 0) ? oldPath.substring(0, lastSlashIndex) : "";
                String newPath = parentPath + "/" + newName;

                dropboxClient.files().moveV2(oldPath, newPath);

                runOnUiThread(() -> {
                    ErrorHandler.showSuccessDialog(
                            this,
                            "Renamed",
                            "File renamed to: " + newName
                    );
                    loadFolderContents();
                });
            } catch (DbxException e) {
                Log.e(TAG, "Rename failed", e);
                runOnUiThread(() ->
                        ErrorHandler.showErrorDialog(this, "Rename Failed", e.getMessage())
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