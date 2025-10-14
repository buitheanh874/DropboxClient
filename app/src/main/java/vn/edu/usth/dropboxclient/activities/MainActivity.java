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
import java.util.concurrent.TimeUnit; // Thêm import này

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

    // Sử dụng Thread pool có kích thước cố định (FixedThreadPool) là tốt
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private ActivityResultLauncher<Intent> pickFileLauncher;

    // Đường dẫn thư mục hiện tại (mặc định là root) - Cần thiết cho FolderActivity sau này
    // Tuy nhiên trong MainActivity này chỉ làm việc với root nên ta giữ nguyên loadDropboxFiles() cho root ("").

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            // Chuyển sang màn hình Auth nếu chưa đăng nhập
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        swipeRefresh.setOnRefreshListener(this::loadDropboxFiles); // Sử dụng method reference

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showFabMenu());

        // register ActivityResult launcher
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

        loadDropboxFiles();
    }

    /**
     * Lấy danh sách file thật từ Dropbox sử dụng Executor
     * (Không đổi, hoạt động tốt)
     */
    private void loadDropboxFiles() {
        swipeRefresh.setRefreshing(true);
        executorService.execute(() -> {
            List<FileItem> files = new ArrayList<>();
            try {
                // Liệt kê thư mục gốc ("")
                ListFolderResult result = dropboxClient.files().listFolder("");
                for (Metadata meta : result.getEntries()) {
                    if (meta instanceof FileMetadata) {
                        FileMetadata f = (FileMetadata) meta;
                        // ✅ SỬA: Sử dụng constructor đúng thứ tự
                        files.add(new FileItem(
                                f.getId(),
                                f.getName(),
                                f.getPathLower(),  // ✅ path (đúng vị trí)
                                getFileType(f.getName()),  // ✅ type
                                f.getSize(),
                                f.getServerModified() != null ? f.getServerModified().toString() : ""
                        ));
                    } else if (meta instanceof FolderMetadata) {
                        FolderMetadata folder = (FolderMetadata) meta;
                        // ✅ SỬA: Sử dụng constructor đúng thứ tự
                        files.add(new FileItem(
                                folder.getId(),
                                folder.getName(),
                                folder.getPathLower(),  // ✅ path
                                "folder",  // ✅ type
                                0,
                                ""
                        ));
                    }
                }
            } catch (DbxException e) {
                Log.e(TAG, "Error loading files from Dropbox", e);
                runOnUiThread(() -> ErrorHandler.showErrorDialog(this, "Load Failed", "Could not load files from Dropbox: " + e.getMessage()));
            }

            runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                allFiles.clear();
                allFiles.addAll(files);
                currentFiles = new ArrayList<>(files);
                adapter.submitList(currentFiles);
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

    /**
     * Mở file picker để chọn file từ device
     * (Không đổi, hoạt động tốt)
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Intent chooser = Intent.createChooser(intent, "Select file to upload");
        pickFileLauncher.launch(chooser);
    }

    /**
     * Upload file lên Dropbox. Đã sửa lỗi "effectively final"
     * (Không đổi, hoạt động tốt)
     */
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

                // Upload không progress tracking
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

    /**
     * Lấy tên file từ URI
     * (Không đổi, hoạt động tốt)
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

    /**
     * Tạo folder trên Dropbox sử dụng Executor
     * (Không đổi, hoạt động tốt)
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

            // *** SỬA: Gán listener để tải lại danh sách file sau khi xóa từ BottomSheet
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
                        // Gọi deleteDropboxFile với kiểm tra đường dẫn an toàn
                        deleteDropboxFile(file.getPath());
                    }
                })
                .show();
    }

    /**
     * Xóa file trên Dropbox.
     * ĐÃ SỬA: Thêm kiểm tra path để ngăn lỗi IllegalArgumentException từ Dropbox SDK (khi path là "" hoặc "/").
     */
    private void deleteDropboxFile(String path) {
        // *** SỬA LỖI: Ngăn chặn lỗi IllegalArgumentException khi path không hợp lệ
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
                                Toast.makeText(MainActivity.this, "Deleted successfully: " + path, Toast.LENGTH_SHORT).show();
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

    /**
     * Rename file trên Dropbox sử dụng Executor
     * (Không đổi, hoạt động tốt)
     */
    private void renameFile(FileItem file, String newName) {
        executorService.execute(() -> {
            try {
                String oldPath = file.getPath();
                // Lấy chỉ số cuối cùng của dấu '/' (trừ trường hợp root)
                int lastSlashIndex = oldPath.lastIndexOf('/');
                String parentPath = (lastSlashIndex > 0) ? oldPath.substring(0, lastSlashIndex) : "";
                String newPath = parentPath + "/" + newName;

                // Move file (rename)
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
                public boolean onQueryTextSubmit(String query) { return false; }

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
        // *** SỬA: Dùng shutdownNow() để cố gắng dừng các tác vụ đang chạy ngay lập tức khi Activity bị hủy
        executorService.shutdownNow();
        try {
            // Chờ một chút để đảm bảo các tác vụ đã dừng
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                Log.w(TAG, "Executor did not terminate in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}