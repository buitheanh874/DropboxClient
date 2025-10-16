package vn.edu.usth.dropboxclient.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

import com.dropbox.core.v2.DbxClientV2;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileDetailBottomSheet extends BottomSheetDialogFragment {

    private FileItem file;
    private DbxClientV2 dropboxClient;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private OnFileDeletedListener deleteListener;

    public interface OnFileDeletedListener {
        void onFileDeleted();
    }

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
        } catch (Exception ignored) {}
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
        ImageView previewImage = view.findViewById(R.id.preview_image);

        MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        MaterialButton btnShare = view.findViewById(R.id.btn_share);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        fileName.setText(file.getName());
        fileType.setText("Type: " + file.getType().toUpperCase());
        fileSize.setText("Size: " + file.getFormattedSize());
        fileModified.setText("Modified: " + file.getFormattedDate());

        String type = file.getType().toLowerCase();
        if (type.matches("jpg|jpeg|png|gif|bmp|webp")) {
            loadImage(previewImage);
            btnPreview.setVisibility(View.GONE);
        } else {
            btnPreview.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Preview not supported", Toast.LENGTH_SHORT).show()
            );
        }

        btnDownload.setOnClickListener(v -> download());
        btnShare.setOnClickListener(v -> share());
        btnDelete.setOnClickListener(v -> delete());

        if (file.isFolder()) {
            btnDownload.setVisibility(View.GONE);
            btnShare.setVisibility(View.GONE);
        }
    }

    private void loadImage(ImageView imageView) {
        Toast.makeText(getContext(), "Loading image...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                InputStream in = dropboxClient.files().download(file.getPath()).getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                in.close();

                runOnUI(() -> {
                    if (bitmap != null) {
                        imageView.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(bitmap);
                    }
                    Toast.makeText(getContext(), "Image loaded", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUI(() -> Toast.makeText(getContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void download() {
        Toast.makeText(getContext(), "Downloading...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                File dir = getContext().getExternalFilesDir("Downloads");
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, file.getName());
                InputStream in = dropboxClient.files().download(file.getPath()).getInputStream();
                OutputStream out = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();

                runOnUI(() -> {
                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle("Download Complete")
                            .setMessage("Saved to:\n" + outFile.getAbsolutePath())
                            .setPositiveButton("OK", null)
                            .show();
                    dismiss();
                });
            } catch (Exception e) {
                runOnUI(() -> Toast.makeText(getContext(), "Download failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void share() {
        Toast.makeText(getContext(), "Creating share link...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                String shareUrl = dropboxClient.sharing()
                        .createSharedLinkWithSettings(file.getPath())
                        .getUrl();

                runOnUI(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, file.getName() + "\n" + shareUrl);
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                });
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("shared_link_already_exists")) {
                    getExistingLink();
                } else {
                    runOnUI(() -> Toast.makeText(getContext(), "Share failed", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void getExistingLink() {
        try {
            String shareUrl = dropboxClient.sharing()
                    .listSharedLinksBuilder()
                    .withPath(file.getPath())
                    .start()
                    .getLinks()
                    .get(0)
                    .getUrl();

            runOnUI(() -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, file.getName() + "\n" + shareUrl);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            });
        } catch (Exception e) {
            runOnUI(() -> Toast.makeText(getContext(), "Cannot get link", Toast.LENGTH_SHORT).show());
        }
    }

    private void delete() {
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Delete")
                .setMessage("Delete '" + file.getName() + "'?")
                .setPositiveButton("Delete", (d, w) -> {
                    executor.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(file.getPath());
                            runOnUI(() -> {
                                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                if (deleteListener != null) deleteListener.onFileDeleted();
                                dismiss();
                            });
                        } catch (Exception e) {
                            runOnUI(() -> Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runOnUI(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}