package vn.edu.usth.dropboxclient.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import vn.edu.usth.dropboxclient.R;

public class ProgressHelper {

    private Dialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView progressTitle;

    public ProgressHelper(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);

        progressBar = view.findViewById(R.id.progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        progressTitle = view.findViewById(R.id.progress_title);

        progressTitle.setText(title);

        progressDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(false)
                .create();
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