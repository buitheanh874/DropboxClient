package vn.edu.usth.dropboxclient.fragments;

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
import vn.edu.usth.dropboxclient.utils.MockDataProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class FileDetailBottomSheet extends BottomSheetDialogFragment {

    private FileItem file;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_file_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView previewImage = view.findViewById(R.id.preview_image);
        TextView fileName = view.findViewById(R.id.file_name);
        TextView fileType = view.findViewById(R.id.file_type);
        TextView fileSize = view.findViewById(R.id.file_size);
        TextView fileModified = view.findViewById(R.id.file_modified);

        MaterialButton btnPreview = view.findViewById(R.id.btn_preview);
        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        MaterialButton btnShare = view.findViewById(R.id.btn_share);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        fileName.setText(file.getName());
        fileType.setText("Type: " + file.getType().toUpperCase());
        fileSize.setText("Size: " + file.getFormattedSize());
        fileModified.setText("Modified: " + file.getFormattedDate());

        // Show preview for images
        if ("image".equalsIgnoreCase(file.getType())) {
            previewImage.setImageResource(R.drawable.sample_image);
            previewImage.setVisibility(View.VISIBLE);
        } else {
            previewImage.setVisibility(View.GONE);
        }

        btnPreview.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Preview (mock)", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        btnDownload.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Download (mock)", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        btnShare.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Share (mock)", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            MockDataProvider.getInstance().deleteFile(file.getId());
            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
            dismiss();
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }
}