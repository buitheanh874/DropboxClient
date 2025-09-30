package vn.edu.usth.dropboxclient.activities;

import android.content.Intent;
import android.os.Bundle;
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
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.adapters.FileAdapter;
import vn.edu.usth.dropboxclient.fragments.FileDetailBottomSheet;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.utils.MockDataProvider;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FileAdapter.OnFileClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private List<FileItem> currentFiles;
    private List<FileItem> allFiles;
    private PreferenceManager prefManager;

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
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_shared) {
                Toast.makeText(this, "Shared", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_recents) {
                Toast.makeText(this, "Recents", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_offline) {
                Toast.makeText(this, "Offline", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        recyclerView = findViewById(R.id.recycler_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(this, this);
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadFiles();
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showFabMenu());

        loadFiles();
    }

    private void loadFiles() {
        allFiles = MockDataProvider.getInstance().getFilesForParent(null);
        currentFiles = new ArrayList<>(allFiles);
        sortFiles();
        adapter.submitList(currentFiles);
    }

    private void sortFiles() {
        String sortBy = prefManager.getSortBy();
        if (PreferenceManager.SORT_NAME.equals(sortBy)) {
            Collections.sort(currentFiles, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        } else if (PreferenceManager.SORT_DATE.equals(sortBy)) {
            Collections.sort(currentFiles, (f1, f2) -> f2.getModifiedDate().compareTo(f1.getModifiedDate()));
        } else if (PreferenceManager.SORT_SIZE.equals(sortBy)) {
            Collections.sort(currentFiles, (f1, f2) -> Long.compare(f2.getSize(), f1.getSize()));
        }
    }

    private void showFabMenu() {
        String[] options = {"Create Folder", "Upload File", "Scan Photo"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("New")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCreateFolderDialog();
                            break;
                        case 1:
                            uploadFile();
                            break;
                        case 2:
                            scanPhoto();
                            break;
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
                        FileItem newFolder = new FileItem(
                                String.valueOf(System.currentTimeMillis()),
                                folderName,
                                "folder",
                                "2025-09-29T12:00:00",
                                0,
                                null
                        );
                        MockDataProvider.getInstance().addFile(newFolder);
                        loadFiles();
                        Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadFile() {
        FileItem newFile = new FileItem(
                String.valueOf(System.currentTimeMillis()),
                "new_file_" + System.currentTimeMillis() + ".pdf",
                "pdf",
                "2025-09-29T12:00:00",
                123456,
                null
        );
        MockDataProvider.getInstance().addFile(newFile);
        loadFiles();
        Toast.makeText(this, "File uploaded (mock)", Toast.LENGTH_SHORT).show();
    }

    private void scanPhoto() {
        Toast.makeText(this, "Scan photo (mock)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFileClick(FileItem file) {
        if (file.isFolder()) {
            Intent intent = new Intent(this, FolderActivity.class);
            intent.putExtra("folder", file);
            startActivity(intent);
        } else {
            FileDetailBottomSheet bottomSheet = FileDetailBottomSheet.newInstance(file);
            bottomSheet.show(getSupportFragmentManager(), "FileDetailBottomSheet");
        }
    }

    @Override
    public void onFileMenuClick(FileItem file) {
        String[] options = {"Share", "Rename", "Delete"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(file.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Share (mock)", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(this, "Rename (mock)", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            MockDataProvider.getInstance().deleteFile(file.getId());
                            loadFiles();
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

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
    protected void onResume() {
        super.onResume();
        loadFiles();
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
