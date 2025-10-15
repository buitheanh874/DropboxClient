package vn.edu.usth.dropboxclient.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.MediaController;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.models.FileItem;
import vn.edu.usth.dropboxclient.DropboxClientFactory;
import vn.edu.usth.dropboxclient.utils.ErrorHandler;
import vn.edu.usth.dropboxclient.utils.ProgressHelper;

import com.dropbox.core.v2.DbxClientV2;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "FileDetailBottomSheet";
    private FileItem file;
    private DbxClientV2 dropboxClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView previewImage;
    private VideoView previewVideo;
    private View audioControls;
    private MaterialButton btnPlayPause;
    private TextView audioFileName;

    private MediaPlayer mediaPlayer;
    private File tempFile;

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
        } catch (Exception e) {
            Log.e(TAG, "Dropbox client error", e);
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

        previewImage = view.findViewById(R.id.preview_image);
        previewVideo = view.findViewById(R.id.preview_video);
        audioControls = view.findViewById(R.id.audio_controls);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        audioFileName = view.findViewById(R.id.audio_file_name);

        MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        MaterialButton btnShare = view.findViewById(R.id.btn_share);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        fileName.setText(file.getName());
        fileType.setText("Type: " + file.getType().toUpperCase());
        fileSize.setText("Size: " + file.getFormattedSize());
        fileModified.setText("Modified: " + file.getFormattedDate());

        String type = file.getType().toLowerCase();
        if (isImage(type)) {
            loadImage();
            btnPreview.setVisibility(View.GONE);
        } else if (isAudio(type)) {
            setupAudio();
            btnPreview.setVisibility(View.GONE);
        } else if (isVideo(type)) {
            btnPreview.setOnClickListener(v -> loadVideo());
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

    private boolean isImage(String type) {
        return type.matches("jpg|jpeg|png|gif|bmp|webp");
    }

    private boolean isAudio(String type) {
        return type.matches("mp3|wav|m4a|aac|ogg|flac");
    }

    private boolean isVideo(String type) {
        return type.matches("mp4|avi|mkv|mov|wmv|flv");
    }

    private void loadImage() {
        ProgressHelper progress = new ProgressHelper(getContext(), "Loading image...");
        progress.show();

        executor.execute(() -> {
            try {
                InputStream in = dropboxClient.files().download(file.getPath()).getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                in.close();

                runOnUI(() -> {
                    progress.dismiss();
                    if (bitmap != null) {
                        previewImage.setVisibility(View.VISIBLE);
                        previewImage.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                runOnUI(() -> {
                    progress.dismiss();
                    showError("Preview Failed", e.getMessage());
                });
            }
        });
    }

    private void setupAudio() {
        audioControls.setVisibility(View.VISIBLE);
        audioFileName.setText(file.getName());
        btnPlayPause.setText("Load Audio");

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) loadAudio();
            else toggleAudio();
        });
    }

    private void loadAudio() {
        ProgressHelper progress = new ProgressHelper(getContext(), "Loading audio...");
        progress.show();

        executor.execute(() -> {
            try {
                tempFile = new File(getContext().getCacheDir(), file.getName());
                downloadToFile(tempFile);

                runOnUI(() -> {
                    progress.dismiss();
                    try {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        btnPlayPause.setText("Pause");
                        mediaPlayer.setOnCompletionListener(mp -> btnPlayPause.setText("Play"));
                    } catch (Exception e) {
                        showError("Audio Error", e.getMessage());
                    }
                });
            } catch (Exception e) {
                runOnUI(() -> {
                    progress.dismiss();
                    showError("Load Failed", e.getMessage());
                });
            }
        });
    }

    private void toggleAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setText("Play");
        } else {
            mediaPlayer.start();
            btnPlayPause.setText("Pause");
        }
    }

    private void loadVideo() {
        ProgressHelper progress = new ProgressHelper(getContext(), "Loading video...");
        progress.show();

        executor.execute(() -> {
            try {
                tempFile = new File(getContext().getCacheDir(), file.getName());

                InputStream in = dropboxClient.files().download(file.getPath()).getInputStream();
                OutputStream out = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];
                int read;
                long total = 0;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                    int prog = (int) ((total * 100) / file.getSize());
                    runOnUI(() -> progress.updateProgress(prog));
                }

                in.close();
                out.close();

                runOnUI(() -> {
                    progress.dismiss();
                    previewVideo.setVisibility(View.VISIBLE);
                    previewVideo.setVideoURI(Uri.fromFile(tempFile));
                    previewVideo.setMediaController(new MediaController(getContext()));
                    previewVideo.start();
                });
            } catch (Exception e) {
                runOnUI(() -> {
                    progress.dismiss();
                    showError("Video Load Failed", e.getMessage());
                });
            }
        });
    }

    private void download() {
        ProgressHelper progress = new ProgressHelper(getContext(), "Downloading...");
        progress.show();

        executor.execute(() -> {
            try {
                File dir = getContext().getExternalFilesDir("Downloads");
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, file.getName());

                InputStream in = dropboxClient.files().download(file.getPath()).getInputStream();
                OutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[4096];
                int read;
                long total = 0;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    total += read;
                    int prog = (int) ((total * 100) / file.getSize());
                    runOnUI(() -> progress.updateProgress(prog));
                }

                in.close();
                out.close();

                runOnUI(() -> {
                    progress.dismiss();
                    ErrorHandler.showSuccessDialog(
                            getContext(),
                            "Download Complete",
                            "Saved to:\n" + outFile.getAbsolutePath()
                    );
                    dismiss();
                });
            } catch (Exception e) {
                runOnUI(() -> {
                    progress.dismiss();
                    showError("Download Failed", e.getMessage());
                });
            }
        });
    }

    private void share() {
        ProgressHelper progress = new ProgressHelper(getContext(), "Creating share link...");
        progress.show();

        executor.execute(() -> {
            try {
                // Tạo share link từ Dropbox
                String shareUrl = dropboxClient.sharing()
                        .createSharedLinkWithSettings(file.getPath())
                        .getUrl();

                runOnUI(() -> {
                    progress.dismiss();

                    // Mở Android Share Sheet
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            file.getName() + "\n" + shareUrl);

                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                });

            } catch (Exception e) {
                // Nếu link đã tồn tại, lấy lại
                if (e.getMessage() != null && e.getMessage().contains("shared_link_already_exists")) {
                    getExistingLink(progress);
                } else {
                    runOnUI(() -> {
                        progress.dismiss();
                        showError("Share Failed", e.getMessage());
                    });
                }
            }
        });
    }

    private void getExistingLink(ProgressHelper progress) {
        try {
            String shareUrl = dropboxClient.sharing()
                    .listSharedLinksBuilder()
                    .withPath(file.getPath())
                    .start()
                    .getLinks()
                    .get(0)
                    .getUrl();

            runOnUI(() -> {
                progress.dismiss();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, file.getName() + "\n" + shareUrl);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            });
        } catch (Exception e) {
            runOnUI(() -> {
                progress.dismiss();
                showError("Share Failed", "Cannot get link");
            });
        }
    }

    private void delete() {
        ErrorHandler.showConfirmDialog(
                getContext(),
                "Delete",
                "Delete '" + file.getName() + "'?",
                "Delete",
                "Cancel",
                () -> {
                    executor.execute(() -> {
                        try {
                            dropboxClient.files().deleteV2(file.getPath());
                            runOnUI(() -> {
                                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                if (deleteListener != null) deleteListener.onFileDeleted();
                                dismiss();
                            });
                        } catch (Exception e) {
                            runOnUI(() -> showError("Delete Failed", e.getMessage()));
                        }
                    });
                },
                null
        );
    }

    private void downloadToFile(File file) throws Exception {
        InputStream in = dropboxClient.files().download(this.file.getPath()).getInputStream();
        OutputStream out = new FileOutputStream(file);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    private void runOnUI(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    private void showError(String title, String message) {
        ErrorHandler.showErrorDialog(getContext(), title, message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cleanup
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }

        executor.shutdownNow();
    }
}