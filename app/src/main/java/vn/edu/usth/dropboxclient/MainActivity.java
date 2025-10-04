package vn.edu.usth.dropboxclient;

import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Lấy RecyclerView từ layout
        RecyclerView rv = findViewById(R.id.rvFiles);

        // 2. Đặt LayoutManager (dọc)
        rv.setLayoutManager(new LinearLayoutManager(this));

        // 3. Tạo dữ liệu giả lập
        List<FileItem> files = new ArrayList<>();
        files.add(new FileItem("bai_tap_1.docx", "215 KB", "document"));
        files.add(new FileItem("anh_minh_hoa.png", "524 KB", "image"));
        files.add(new FileItem("video_demo.mp4", "12 MB", "video"));
        files.add(new FileItem("tai_lieu.pdf", "1.3 MB", "pdf"));

        // 4. Gắn adapter
        FileAdapter adapter = new FileAdapter(files);
        rv.setAdapter(adapter);

        // 5. Hiển thị tvEmpty nếu danh sách trống
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        if (files.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }
}
