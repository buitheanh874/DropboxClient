package vn.edu.usth.dropboxclient;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileVH> {

    private final List<FileItem> data;

    public FileAdapter(List<FileItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public FileVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileVH h, int position) {
        FileItem it = data.get(position);
        h.tvName.setText(it.name);
        h.tvSize.setText(it.size);

        // Icon đơn giản dựa vào loại file
        int icon = android.R.drawable.ic_menu_save;
        if ("image".equalsIgnoreCase(it.type)) icon = android.R.drawable.ic_menu_gallery;
        if ("video".equalsIgnoreCase(it.type)) icon = android.R.drawable.ic_media_play;
        if ("pdf".equalsIgnoreCase(it.type))   icon = android.R.drawable.ic_menu_view;

        h.img.setImageResource(icon);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class FileVH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvSize;

        FileVH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgIcon);
            tvName = itemView.findViewById(R.id.tvFileName);
            tvSize = itemView.findViewById(R.id.tvFileSize);
        }
    }
}
