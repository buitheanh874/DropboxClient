package vn.edu.usth.dropboxclient.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.usth.dropboxclient.R;
import vn.edu.usth.dropboxclient.activities.DetailActivity;
import vn.edu.usth.dropboxclient.models.FileItem;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    private final List<FileItem> data;
    private final Context ctx;

    public FileAdapter(List<FileItem> data, Context ctx) {
        this.data = data;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FileItem it = data.get(position);
        h.tvName.setText(it.getName());
        h.tvSize.setText(it.getSize());

        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(ctx, DetailActivity.class);
            // Truyền đủ dữ liệu (đặc biệt là id) để xoá chính xác
            i.putExtra("id", it.getId());
            i.putExtra("name", it.getName());
            i.putExtra("size", it.getSize());
            i.putExtra("type", it.getType());
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSize;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSize = itemView.findViewById(R.id.tvSize);
        }
    }
}
