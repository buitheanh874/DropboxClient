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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;
// import vn.edu.usth.dropboxclient.utils.PreferenceManager; // đã bỏ vì chưa dùng

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    // private PreferenceManager prefManager; // xóa nếu không dùng

    private DbxClientV2 dropboxClient;

    private List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();

    private static final int REQUEST_CODE_PICK_FILE = 1001; // vẫn giữ nếu cần cho logic khác
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // Sử dụng Activity Result API thay cho startActivityForResult
    private ActivityResultLauncher<Intent> pickFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // nếu sau này cần PreferenceManager thì mở dòng dưới
        // prefManager = new PreferenceManager(this);

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

        swipeRefresh.setOnRefreshListener(() -> {
            loadDropboxFiles();
            swipeRefresh.setRefreshing(false);
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showFabMenu());

        // register ActivityResult launcher
        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri fileUri = result.getData().getData();
                            if (fileUri != null) {
                                uploadFileToDropbox(fileUri);
                            }
                        }
                    }
                });

        loadDropboxFiles();
    }

    /**
     * Lấy danh sách file thật từ Dropbox sử dụng Executor
     */
    private void loadDropboxFiles() {
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
                                "file",
                                f.getServerModified() != null ? f.getServerModified().toString() : "",
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
                Log.e(TAG, "Error loading files from Dropbox", e);
            }

            runOnUiThread(() -> {
                allFiles = files;
                currentFiles = new ArrayList<>(files);
                adapter.submitList(currentFiles);
                Toast.makeText(MainActivity.this, "Loaded from Dropbox", Toast.LENGTH_SHORT).show();
            });
        });
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

    /**
     * Mở file picker để chọn file từ device
     * Sử dụng ActivityResultLauncher thay cho startActivityForResult
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // dùng launcher
        Intent chooser = Intent.createChooser(intent, "Select file to upload");
        pickFileLauncher.launch(chooser);
    }

    /**
     * Upload file lên Dropbox sử dụng Executor
     */
    private void uploadFileToDropbox(Uri fileUri) {
        executorService.execute(() -> {
            // dùng try-with-resources để tự đóng InputStream
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {

                if (inputStream == null) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Cannot open selected file", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Lấy tên file từ URI
                String fileName = getFileNameFromUri(fileUri);
                if (fileName == null) {
                    fileName = "file_" + System.currentTimeMillis();
                }

                // Upload file với WriteMode.ADD (không ghi đè)
                String dropboxPath = "/" + fileName;
                dropboxClient.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.ADD)
                        .uploadAndFinish(inputStream);

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                    loadDropboxFiles();
                });
            } catch (DbxException e) {
                Log.e(TAG, "Dropbox Upload error", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Upload failed (Dropbox)", Toast.LENGTH_SHORT).show()
                );
            } catch (IOException e) {
                Log.e(TAG, "IO error during upload", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Upload failed (IO)", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Lấy tên file từ URI
     * - dùng try-with-resources cho Cursor
     * - kiểm tra getColumnIndex >= 0 trước khi dùng
     * - tránh lastIndexOf trên null
     */
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
                    result = path; // fallback
                }
            } else {
                // không có path -> trả về null
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
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Tạo folder trên Dropbox sử dụng Executor
     */
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
                        Toast.makeText(MainActivity.this, "Failed to create folder", Toast.LENGTH_SHORT).show()
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

    /**
     * Xóa file từ Dropbox sử dụng Executor
     */
    private void deleteDropboxFile(String path) {
        executorService.execute(() -> {
            try {
                dropboxClient.files().deleteV2(path);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadDropboxFiles();
                });
            } catch (DbxException e) {
                Log.e(TAG, "Delete failed", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Delete failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFiles(newText);
                return true;
            }
        });
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
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
