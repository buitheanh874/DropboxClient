package vn.edu.usth.dropboxclient.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private PreferenceManager prefManager;

    private DbxClientV2 dropboxClient;

    private List<FileItem> allFiles = new ArrayList<>();
    private List<FileItem> currentFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefManager = new PreferenceManager(this);

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
        dropboxClient = null;
        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            // Chưa init → quay về màn hình login
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

        // Lần đầu load dữ liệu
        loadDropboxFiles();
    }

    /**
     * Lấy danh sách file thật từ Dropbox
     */
    private void loadDropboxFiles() {
        new AsyncTask<Void, Void, List<FileItem>>() {

            @Override
            protected List<FileItem> doInBackground(Void... voids) {
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
                    Log.e("Dropbox", "Error loading files: ", e);
                }
                return files;
            }

            @Override
            protected void onPostExecute(List<FileItem> files) {
                allFiles = files;
                currentFiles = new ArrayList<>(files);
                adapter.submitList(currentFiles);
                Toast.makeText(MainActivity.this, "Loaded from Dropbox", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void showFabMenu() {
        String[] options = {"Upload File", "Create Folder"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("New")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "Upload coming soon", Toast.LENGTH_SHORT).show();
                    } else {
                        showCreateFolderDialog();
                    }
                })
                .show();
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

    private void createDropboxFolder(String folderName) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    dropboxClient.files().createFolderV2("/" + folderName);
                    return true;
                } catch (DbxException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(MainActivity.this, "Folder created", Toast.LENGTH_SHORT).show();
                    loadDropboxFiles();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    @Override
    public void onFileClick(FileItem file) {
        if (file.isFolder()) {
            Toast.makeText(this, "Opening folder: " + file.getName(), Toast.LENGTH_SHORT).show();
            // bạn có thể tạo FolderActivity để load các file bên trong folder
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
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    dropboxClient.files().deleteV2(path);
                    return true;
                } catch (DbxException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadDropboxFiles();
                } else {
                    Toast.makeText(MainActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
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
        if (query.isEmpty()) {
            currentFiles = new ArrayList<>(allFiles);
        } else {
            currentFiles = new ArrayList<>();
            for (FileItem file : allFiles) {
                if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    currentFiles.add(file);
                }
            }
        }
        adapter.submitList(currentFiles);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
