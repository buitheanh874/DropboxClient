package vn.edu.usth.dropboxclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

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
import vn.edu.usth.dropboxclient.utils.ErrorHandler;
import vn.edu.usth.dropboxclient.utils.ProgressHelper;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private View emptyView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private DbxClientV2 dropboxClient;
    private final List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();
    private ExecutorService executor;
    private ActivityResultLauncher<Intent> pickFileLauncher;

    // FIX: Flag to prevent operations after destroy
    private volatile boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isDestroyed = false;
        executor = Executors.newFixedThreadPool(2);

        initViews();
        setupToolbar();
        setupDrawer();
        setupRecyclerView();
        setupFab();
        setupFilePicker();
        setupBackPress();

        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        loadFiles();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupDrawer() {
        NavigationView navView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, (Toolbar) findViewById(R.id.toolbar),
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else {
                Toast.makeText(this, "Feature not implemented", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, this);
        recyclerView.setAdapter(adapter);
        swipeRefresh.setOnRefreshListener(this::loadFiles);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("New")
                    .setItems(new String[]{"Upload File", "Create Folder"}, (d, w) -> {
                        if (w == 0) openFilePicker();
                        else showCreateFolderDialog();
                    })
                    .show();
        });
    }

    private void setupFilePicker() {
        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) uploadFile(uri);
                    }
                }
        );
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    private void loadFiles() {
        // FIX: Check if destroyed before starting new task
        if (isDestroyed) return;

        swipeRefresh.setRefreshing(true);

        executor.execute(() -> {
            // FIX: Double check inside thread
            if (isDestroyed) return;

            List<FileItem> files = new ArrayList<>();
            try {
                for (Metadata meta : dropboxClient.files().listFolder("").getEntries()) {
                    if (meta instanceof FileMetadata) {
                        FileMetadata f = (FileMetadata) meta;
                        files.add(new FileItem(
                                f.getId(), f.getName(), f.getPathLower(),
                                getFileType(f.getName()), f.getSize(),
                                f.getServerModified() != null ? f.getServerModified().toString() : ""
                        ));
                    } else if (meta instanceof FolderMetadata) {
                        FolderMetadata folder = (FolderMetadata) meta;
                        files.add(new FileItem(
                                folder.getId(), folder.getName(),
                                folder.getPathLower(), "folder", 0, ""
                        ));
                    }
                }
            } catch (DbxException e) {
                Log.e(TAG, "Load error", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ErrorHandler.showErrorDialog(this, "Load Failed", e.getMessage());
                    }
                });
            }

            runOnUiThread(() -> {
                if (isDestroyed) return;

                swipeRefresh.setRefreshing(false);
                allFiles.clear();
                allFiles.addAll(files);
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

    private String getFileType(String name) {
        if (name == null || !name.contains(".")) return "file";
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickFileLauncher.launch(Intent.createChooser(intent, "Select file"));
    }

    private void uploadFile(Uri uri) {
        ProgressHelper progress = new ProgressHelper(this, "Uploading...");

        String name = getFileName(uri);
        if (name == null) name = "file_" + System.currentTimeMillis();

        progress.setFileName(name);
        progress.show();

        final String finalName = name;

        executor.execute(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("Cannot open file");

                dropboxClient.files().uploadBuilder("/" + finalName)
                        .withMode(WriteMode.ADD)
                        .uploadAndFinish(is);

                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    progress.dismiss();
                    ErrorHandler.showSuccessDialog(this, "Success", "Uploaded: " + finalName);
                    loadFiles();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    progress.dismiss();
                    ErrorHandler.showErrorDialog(this, "Failed", e.getMessage());
                });
            }
        });
    }

    private String getFileName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) return cursor.getString(idx);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
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
                dropboxClient.files().createFolderV2("/" + name);
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            } catch (DbxException e) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    ErrorHandler.showErrorDialog(this, "Failed", e.getMessage());
                });
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
                    else deleteDropboxFile(file.getPath());
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
                    if (isDestroyed) return;
                    ErrorHandler.showSuccessDialog(this, "Renamed", "File renamed");
                    loadFiles();
                });
            } catch (DbxException e) {
                runOnUiThread(() -> {
                    if (isDestroyed) return;
                    ErrorHandler.showErrorDialog(this, "Failed", e.getMessage());
                });
            }
        });
    }

    private void deleteDropboxFile(String path) {
        if (path == null || path.trim().isEmpty() || path.equals("/")) {
            ErrorHandler.showErrorDialog(this, "Failed", "Invalid path");
            return;
        }
        ErrorHandler.showConfirmDialog(this, "Delete", "Delete '" + path + "'?",
                "Delete", "Cancel", () -> {
                    executor.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(path);
                            runOnUiThread(() -> {
                                if (isDestroyed) return;
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                loadFiles();
                            });
                        } catch (DbxException e) {
                            runOnUiThread(() -> {
                                if (isDestroyed) return;
                                ErrorHandler.showErrorDialog(this, "Failed", e.getMessage());
                            });
                        }
                    });
                }, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    filterFiles(text);
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
        adapter.submitList(currentFiles);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isDestroyed = true;
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Interrupted while waiting for executor", e);
            }
        }
    }
}