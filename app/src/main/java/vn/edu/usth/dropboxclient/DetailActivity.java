package vn.edu.usth.dropboxclient;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ImageView img = findViewById(R.id.imgDetail);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvSize = findViewById(R.id.tvDetailSize);
        TextView tvType = findViewById(R.id.tvDetailType);

        // Nhận dữ liệu từ Intent
        FileItem item = (FileItem) getIntent().getSerializableExtra("file_item");

        if (item != null) {
            tvName.setText(item.name);
            tvSize.setText(item.size);
            tvType.setText(item.type);

            int icon = android.R.drawable.ic_menu_save;
            if ("image".equalsIgnoreCase(item.type)) icon = android.R.drawable.ic_menu_gallery;
            if ("video".equalsIgnoreCase(item.type)) icon = android.R.drawable.ic_media_play;
            if ("pdf".equalsIgnoreCase(item.type)) icon = android.R.drawable.ic_menu_view;

            img.setImageResource(icon);
        }
    }
}
