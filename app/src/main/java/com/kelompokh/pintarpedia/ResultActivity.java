package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // 1. Inisialisasi View
        TextView tvSkor = findViewById(R.id.tvSkor);
        TextView tvDetail = findViewById(R.id.tvDetail);
        Button btnHome = findViewById(R.id.btnHome);

        // 2. Ambil data dari Intent (MapelActivity)
        int benar = getIntent().getIntExtra("SCORE_BENAR", 0);
        int salah = getIntent().getIntExtra("SCORE_SALAH", 0);
        int total = getIntent().getIntExtra("TOTAL_SOAL", 0);

        // 3. Hitung Nilai Akhir
        int nilaiAkhir = 0;
        if (total > 0) {
            nilaiAkhir = (benar * 100) / total;
        }

        // 4. Tampilkan ke Layar
        tvSkor.setText(String.valueOf(nilaiAkhir));
        tvDetail.setText("Benar: " + benar + " | Salah: " + salah);

        // 5. Tombol Kembali ke Menu Utama
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });
    }
}