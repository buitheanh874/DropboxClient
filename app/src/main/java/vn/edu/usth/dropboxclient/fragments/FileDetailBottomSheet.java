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

// Kể từ Android Q (10), nên sử dụng phương thức download an toàn hơn hoặc lưu vào getExternalFilesDir
// Tạm thời sử dụng Context.getExternalFilesDir() để đảm bảo quyền truy cập file đơn giản hơn
// Tuy nhiên, để lưu vào thư mục Tải xuống công cộng, bạn CẦN cấp quyền WRITE_EXTERNAL_STORAGE trong Manifest
// hoặc sử dụng MediaStore API. Tôi sẽ sử dụng cách cũ nhưng lưu ý về quyền.

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

        // Khởi tạo Views
        // ImageView previewImage = view.findViewById(R.id.preview_image); // Hiện tại không dùng
        TextView fileName = view.findViewById(R.id.file_name);
        TextView fileType = view.findViewById(R.id.file_type);
        TextView fileSize = view.findViewById(R.id.file_size);
        TextView fileModified = view.findViewById(R.id.file_modified);

        MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        MaterialButton btnShare = view.findViewById(R.id.btn_share);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        // Hiển thị dữ liệu
        if (file != null) {
            fileName.setText(file.getName());
            fileType.setText("Type: " + file.getType().toUpperCase());
            fileSize.setText("Size: " + file.getFormattedSize());
            fileModified.setText("Modified: " + file.getFormattedDate());
        } else {
            // Xử lý trường hợp file null nếu cần
            dismiss();
            return;
        }

        // Tạm thời ẩn preview
        ImageView previewImage = view.findViewById(R.id.preview_image);
        previewImage.setVisibility(View.GONE);

        // Bắt sự kiện
        btnPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Preview feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnDownload.setOnClickListener(v -> downloadFile());

        btnShare.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Share feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Chỉ cho phép download/delete file, không phải folder
        if (file.isFolder()) {
            btnDownload.setVisibility(View.GONE);
        }
        btnDelete.setOnClickListener(v -> deleteFile());
    }

    /**
     * Tải file từ Dropbox về thư mục Downloads công cộng.
     * CẦN quyền WRITE_EXTERNAL_STORAGE (trên Android 9 trở xuống)
     * hoặc xử lý Scoped Storage (trên Android 10 trở lên)
     */
    private void downloadFile() {
        if (getContext() == null || dropboxClient == null) return;

        ProgressHelper progressHelper = new ProgressHelper(getContext(), "Downloading " + file.getName());
        progressHelper.show();

        executorService.execute(() -> {
            Context appContext = getContext();
            if (appContext == null) return;

            // LƯU Ý: Đây là phương pháp lưu file truyền thống vào thư mục Downloads công cộng.
            // Có thể cần xin quyền WRITE_EXTERNAL_STORAGE trong Manifest và Runtime.
            File downloadDir = appContext.getExternalFilesDir("DropboxDownloads");

            // Nếu bạn muốn lưu vào thư mục Downloads công cộng:
            // File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (downloadDir == null || !downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File localFile = new File(downloadDir, file.getName());
            String errorMessage = null;

            try {
                // Download file từ Dropbox
                try (InputStream inputStream = dropboxClient.files().download(file.getPath()).getInputStream();
                     OutputStream outputStream = new FileOutputStream(localFile)) {

                    byte[] buffer = new byte[4096];
                    long totalBytes = file.getSize();
                    long downloadedBytes = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;

                        // Cập nhật progress
                        if (totalBytes > 0 && getActivity() != null) {
                            int progress = (int) ((downloadedBytes * 100) / totalBytes);
                            // Giới hạn cập nhật UI để tránh quá tải
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

            // Cập nhật UI kết quả
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

    /**
     * Xóa file trên Dropbox.
     * Đã thêm kiểm tra đường dẫn hợp lệ để tránh lỗi 'path does not match pattern'.
     */
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
                        // Kiểm tra tính hợp lệ của đường dẫn theo quy tắc Dropbox
                        String path = file.getPath();
                        if (path == null || path.trim().isEmpty() || path.equals("/")) {
                            // Đây là nơi bắt lỗi "path does not match pattern" nếu path rỗng hoặc là root
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
        // Đảm bảo không để lại Executor đang chạy
        executorService.shutdownNow();
    }
}