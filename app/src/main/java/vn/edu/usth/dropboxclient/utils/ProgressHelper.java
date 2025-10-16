package vn.edu.usth.dropboxclient.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import vn.edu.usth.dropboxclient.R;

public class ProgressHelper {

    private Dialog progressDialog;
    private CircularProgressIndicator progressBar;
    private TextView progressText;
    private TextView progressTitle;
    private TextView progressFileName;

    public ProgressHelper(Context context, String title) {
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);

        progressBar = view.findViewById(R.id.progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        progressTitle = view.findViewById(R.id.progress_title);
        progressFileName = view.findViewById(R.id.progress_file_name);

        progressTitle.setText(title);

        progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
    }

    public void setFileName(String fileName) {
        if (progressFileName != null && fileName != null) {
            progressFileName.setText(fileName);
            progressFileName.setVisibility(View.VISIBLE);
        }
    }

    public void show() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            try {
                progressDialog.show();
            } catch (Exception e) {
            }
        }
    }

    public void updateProgress(int progress) {
        if (progressBar != null && progressText != null) {
            // Ensure progress is between 0-100
            progress = Math.max(0, Math.min(100, progress));
            progressBar.setProgress(progress);
            progressText.setText(progress + "%");
        }
    }

    public void dismiss() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    public boolean isShowing() {
        return progressDialog != null && progressDialog.isShowing();
    }
}