package vn.edu.usth.dropboxclient.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    private RadioGroup themeGroup;
    private RadioGroup sortGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefManager = new PreferenceManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        themeGroup = findViewById(R.id.theme_group);
        sortGroup = findViewById(R.id.sort_group);

        // Load current settings
        String currentTheme = prefManager.getTheme();
        if (PreferenceManager.THEME_LIGHT.equals(currentTheme)) {
            themeGroup.check(R.id.radio_light);
        } else {
            themeGroup.check(R.id.radio_dark);
        }

        String currentSort = prefManager.getSortBy();
        if (PreferenceManager.SORT_NAME.equals(currentSort)) {
            sortGroup.check(R.id.radio_sort_name);
        } else if (PreferenceManager.SORT_DATE.equals(currentSort)) {
            sortGroup.check(R.id.radio_sort_date);
        } else {
            sortGroup.check(R.id.radio_sort_size);
        }

        MaterialButton aboutButton = findViewById(R.id.btn_about);
        aboutButton.setOnClickListener(v ->
                Toast.makeText(this, "Dropbox Clone v1.0\nMock app for demonstration", Toast.LENGTH_LONG).show()
        );

        MaterialButton saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        // Save theme
        int themeId = themeGroup.getCheckedRadioButtonId();
        if (themeId == R.id.radio_light) {
            prefManager.setTheme(PreferenceManager.THEME_LIGHT);
        } else {
            prefManager.setTheme(PreferenceManager.THEME_DARK);
        }

        // Save sort preference
        int sortId = sortGroup.getCheckedRadioButtonId();
        if (sortId == R.id.radio_sort_name) {
            prefManager.setSortBy(PreferenceManager.SORT_NAME);
        } else if (sortId == R.id.radio_sort_date) {
            prefManager.setSortBy(PreferenceManager.SORT_DATE);
        } else {
            prefManager.setSortBy(PreferenceManager.SORT_SIZE);
        }

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }   // <-- closes saveSettings()

}   // <-- closes SettingsActivity
