package vn.edu.usth.dropboxclient.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.utils.ErrorHandler;
import vn.edu.usth.dropboxclient.utils.ProgressHelper;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException; // Thêm import cho IOException
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "FileDetailBottomSheet";
    private FileItem file;
    private DbxClientV2 dropboxClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface OnFileDeletedListener {
        void onFileDeleted();
    }

    private OnFileDeletedListener deleteListener;

    public void setOnFileDeletedListener(OnFileDeletedListener listener) {
        this.deleteListener = listener;
    }

    public static FileDetailBottomSheet newInstance(FileItem file) {
        FileDetailBottomSheet fragment = new FileDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            file = (FileItem) getArguments().getSerializable("file");
        }
        try {
            dropboxClient = DropboxClientFactory.getClient();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Dropbox client not initialized", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_file_detail, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView fileName = view.findViewById(R.id.file_name);
        TextView fileType = view.findViewById(R.id.file_type);
        TextView fileSize = view.findViewById(R.id.file_size);
        TextView fileModified = view.findViewById(R.id.file_modified);

        MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        MaterialButton btnShare = view.findViewById(R.id.btn_share);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        if (file != null) {
            fileName.setText(file.getName());
            fileType.setText("Type: " + file.getType().toUpperCase());
            fileSize.setText("Size: " + file.getFormattedSize());
            fileModified.setText("Modified: " + file.getFormattedDate());
        } else {
            dismiss();
            return;
        }

        ImageView previewImage = view.findViewById(R.id.preview_image);
        previewImage.setVisibility(View.GONE);
        btnPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Preview feature coming soon", Toast.LENGTH_SHORT).show();
        });
        btnDownload.setOnClickListener(v -> downloadFile());
        btnShare.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Share feature coming soon", Toast.LENGTH_SHORT).show();
        });
        if (file.isFolder()) {
            btnDownload.setVisibility(View.GONE);
        }
        btnDelete.setOnClickListener(v -> deleteFile());
    }

    private void downloadFile() {
        if (getContext() == null || dropboxClient == null) return;

        ProgressHelper progressHelper = new ProgressHelper(getContext(), "Downloading " + file.getName());
        progressHelper.show();
        executorService.execute(() -> {
            Context appContext = getContext();
            if (appContext == null) return;
            File downloadDir = appContext.getExternalFilesDir("DropboxDownloads");
            if (downloadDir == null || !downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            File localFile = new File(downloadDir, file.getName());
            String errorMessage = null;
            try {
                try (InputStream inputStream = dropboxClient.files().download(file.getPath()).getInputStream();
                     OutputStream outputStream = new FileOutputStream(localFile)) {
                    byte[] buffer = new byte[4096];
                    long totalBytes = file.getSize();
                    long downloadedBytes = 0;
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        if (totalBytes > 0 && getActivity() != null) {
                            int progress = (int) ((downloadedBytes * 100) / totalBytes);
                            if (progress % 5 == 0 || progress == 100) {
                                getActivity().runOnUiThread(() -> progressHelper.updateProgress(progress));
                            }
                        }
                    }
                }

            } catch (DbxException e) {
                Log.e(TAG, "Dropbox download error", e);
                errorMessage = "Dropbox Error: " + e.getMessage();
            } catch (IOException e) {
                Log.e(TAG, "IO error during download", e);
                errorMessage = "IO Error: Cannot save file. Check storage permissions.";
            } catch (Exception e) {
                Log.e(TAG, "General download error", e);
                errorMessage = "An unknown error occurred: " + e.getMessage();
            }
            if (getActivity() != null) {
                final String finalErrorMessage = errorMessage;
                getActivity().runOnUiThread(() -> {
                    progressHelper.dismiss();
                    if (finalErrorMessage == null) {
                        ErrorHandler.showSuccessDialog(
                                appContext,
                                "Download Complete",
                                "File saved to:\n" + localFile.getAbsolutePath()
                        );
                        dismiss();
                    } else {
                        ErrorHandler.showErrorDialog(appContext, "Download Failed", finalErrorMessage);
                    }
                });
            }
        });
    }
    private void deleteFile() {
        if (getContext() == null || dropboxClient == null) return;

        ErrorHandler.showConfirmDialog(
                getContext(),
                "Delete " + (file.isFolder() ? "Folder" : "File"),
                "Are you sure you want to delete '" + file.getName() + "'?",
                "Delete",
                "Cancel",
                () -> {
                    executorService.execute(() -> {
                        String path = file.getPath();
                        if (path == null || path.trim().isEmpty() || path.equals("/")) {
                            Log.e(TAG, "Delete failed: Invalid path provided: " + path);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        ErrorHandler.showErrorDialog(getContext(), "Delete Failed", "Invalid path or attempting to delete root folder.")
                                );
                            }
                            return;
                        }
                        try {
                            dropboxClient.files().deleteV2(path);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), file.getName() + " deleted", Toast.LENGTH_SHORT).show();
                                    if (deleteListener != null) {
                                        deleteListener.onFileDeleted();
                                    }
                                    dismiss();
                                });
                            }
                        } catch (DbxException e) {
                            Log.e(TAG, "Delete failed", e);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        ErrorHandler.showErrorDialog(getContext(), "Delete Failed", e.getMessage())
                                );
                            }
                        }
                    });
                },
                null
        );
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdownNow();
    }
}