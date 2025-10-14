package vn.edu.usth.dropboxclient.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.models.FileItem;

public class FileAdapter extends ListAdapter<FileItem, FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileItem file);
        void onFileMenuClick(FileItem file);
    }

    private Context context;
    private OnFileClickListener listener;

    public FileAdapter(Context context, OnFileClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<FileItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<FileItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FileItem oldItem, @NonNull FileItem newItem) {
            // ✅ SỬA: Dùng Objects.equals() để xử lý null an toàn cho Date
            return oldItem.getName().equals(newItem.getName()) &&
                    java.util.Objects.equals(oldItem.getModified(), newItem.getModified()) &&
                    oldItem.getSize() == newItem.getSize() &&
                    oldItem.getType().equals(newItem.getType());
        }
    };

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem file = getItem(position);
        holder.bind(file, listener);
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView nameView;
        private TextView detailsView;
        private ImageView menuView;
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.file_icon);
            nameView = itemView.findViewById(R.id.file_name);
            detailsView = itemView.findViewById(R.id.file_details);
            menuView = itemView.findViewById(R.id.file_menu);
        }
        public void bind(FileItem file, OnFileClickListener listener) {
            nameView.setText(file.getName());
            String details = file.getFormattedDate();
            if (!file.isFolder() && file.getSize() > 0) {
                details += " • " + file.getFormattedSize();
            }
            detailsView.setText(details);
            int iconRes = R.drawable.ic_file;
            if (file.isFolder()) {
                iconRes = R.drawable.ic_folder;
            } else if ("pdf".equalsIgnoreCase(file.getType())) {
                iconRes = R.drawable.ic_pdf;
            } else if ("image".equalsIgnoreCase(file.getType())) {
                iconRes = R.drawable.ic_image;
            } else if ("doc".equalsIgnoreCase(file.getType()) || "docx".equalsIgnoreCase(file.getType())) {
                iconRes = R.drawable.ic_doc;
            } else if ("excel".equalsIgnoreCase(file.getType()) || "xlsx".equalsIgnoreCase(file.getType())) {
                iconRes = R.drawable.ic_excel;
            } else if ("ppt".equalsIgnoreCase(file.getType()) || "pptx".equalsIgnoreCase(file.getType())) {
                iconRes = R.drawable.ic_ppt;
            }
            iconView.setImageResource(iconRes);

            itemView.setOnClickListener(v -> listener.onFileClick(file));
            menuView.setOnClickListener(v -> listener.onFileMenuClick(file));
        }
    }
}