package vn.edu.usth.dropboxclient.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator; // Import mới
import vn.edu.usth.dropboxclient.R;

public class ProgressHelper {

    private Dialog progressDialog;
    private CircularProgressIndicator progressBar;
    private TextView progressText;
    private TextView progressTitle;
    private TextView progressFileName;

    public ProgressHelper(Context context, String title) {
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
        if (progressFileName != null) {
            progressFileName.setText(fileName);
        }
    }

    public void show() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void updateProgress(int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
            progressText.setText(progress + "%");
        }
    }

    public void dismiss() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}