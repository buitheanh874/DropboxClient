package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.utils.ProgressHelper;
import vn.edu.usth.dropboxclient.utils.ErrorHandler;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {
    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private DbxClientV2 dropboxClient;
    private final List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();
    private ExecutorService executorService;
    private ActivityResultLauncher<Intent> pickFileLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_main);
        executorService = Executors.newFixedThreadPool(2);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else {
                Toast.makeText(this, "Feature not implemented yet", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, this);
        recyclerView.setAdapter(adapter);
        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
        swipeRefresh.setOnRefreshListener(this::loadDropboxFiles);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showFabMenu());
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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
        loadDropboxFiles();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (dropboxClient != null && adapter != null && recyclerView != null) {
            loadDropboxFiles();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }
    private void loadDropboxFiles() {
        if (swipeRefresh == null || adapter == null) {
            Log.e(TAG, "Views not initialized, skipping load");
            return;
        }
        swipeRefresh.setRefreshing(true);
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(2);
        }
        executorService.execute(() -> {
            List<FileItem> files = new ArrayList<>();
            try {
                ListFolderResult result = dropboxClient.files().listFolder("");
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
                Log.e(TAG, "Error loading files from Dropbox", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ErrorHandler.showErrorDialog(this, "Load Failed", "Could not load files from Dropbox: " + e.getMessage());
                    }
                });
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                allFiles.clear();
                allFiles.addAll(files);
                currentFiles = new ArrayList<>(files);
                if (adapter != null) {
                    adapter.submitList(currentFiles);
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
        progressHelper.show();
        executorService.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        progressHelper.dismiss();
                        ErrorHandler.showErrorDialog(this, "Upload Failed", "Cannot open selected file");
                    });
                    return;
                }
                String fileName = getFileNameFromUri(fileUri);
                if (fileName == null) {
                    fileName = "file_" + System.currentTimeMillis();
                }
                final String finalFileName = fileName;
                String dropboxPath = "/" + finalFileName;
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
                    loadDropboxFiles();
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
                    } else {
                        Log.w(TAG, "DISPLAY_NAME column not found for uri: " + uri);
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
            } else {
                Log.w(TAG, "URI path is null, cannot determine file name: " + uri);
            }
        }
        return result;
    }
    private void showCreateFolderDialog() {
        EditText input = new EditText(this);
        input.setHint("Folder name");

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
                dropboxClient.files().createFolderV2("/" + folderName);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadDropboxFiles();
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
            Intent intent = new Intent(MainActivity.this, FolderActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        } else {
            FileDetailBottomSheet bottomSheet = FileDetailBottomSheet.newInstance(file);
            bottomSheet.setOnFileDeletedListener(this::loadDropboxFiles);
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
                        deleteDropboxFile(file.getPath());
                    }
                })
                .show();
    }
    private void deleteDropboxFile(String path) {
        if (path == null || path.trim().isEmpty() || path.equals("/")) {
            ErrorHandler.showErrorDialog(this, "Delete Failed", "Cannot delete root folder or invalid path.");
            Log.w(TAG, "Attempted to delete invalid path: " + path);
            return;
        }
        ErrorHandler.showConfirmDialog(
                this,
                "Delete",
                "Are you sure you want to delete '" + path + "'?",
                "Delete",
                "Cancel",
                () -> {
                    executorService.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(path);
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                loadDropboxFiles();
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
                    loadDropboxFiles();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    filterFiles(newText);
                    return true;
                }
            });
        }
        return true;
    }
    private void filterFiles(String query) {
        if (query == null || query.isEmpty()) {
            currentFiles = new ArrayList<>(allFiles);
        } else {
            currentFiles = new ArrayList<>();
            String lower = query.toLowerCase();
            for (FileItem file : allFiles) {
                if (file.getName() != null && file.getName().toLowerCase().contains(lower)) {
                    currentFiles.add(file);
                }
            }
        }
        if (adapter != null) {
            adapter.submitList(currentFiles);
        }
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate in time.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}