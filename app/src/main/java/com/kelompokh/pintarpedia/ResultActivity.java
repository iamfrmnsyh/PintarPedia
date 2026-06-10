package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // 1. Inisialisasi View
        TextView tvSkor = findViewById(R.id.tvSkor);
        TextView tvDetail = findViewById(R.id.tvDetail);
        Button btnHome = findViewById(R.id.btnHome);

        // 2. Ambil data dari Intent (MapelActivity)
        int benar = getIntent().getIntExtra("SCORE_BENAR", 0);
        int salah = getIntent().getIntExtra("SCORE_SALAH", 0);
        int total = getIntent().getIntExtra("TOTAL_SOAL", 0);
        String subject = getIntent().getStringExtra("SUBJECT");

        // 3. Hitung Nilai Akhir
        int nilaiAkhir = 0;
        if (total > 0) {
            nilaiAkhir = (benar * 100) / total;
        }

        // 4. Tampilkan ke Layar
        tvSkor.setText(String.valueOf(nilaiAkhir));
        tvDetail.setText("Benar: " + benar + " | Salah: " + salah);

        // 5. Simpan ke Firebase Riwayat & Update Stats
        if (mAuth.getCurrentUser() != null) {
            saveResultToFirebase(mAuth.getCurrentUser().getUid(), subject, benar, total, nilaiAkhir);
        }

        // 6. Tombol Kembali ke Menu Utama
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveResultToFirebase(String uid, String subject, int benar, int total, int nilai) {
        DatabaseReference userRef = mDatabase.child(uid);

        // A. Simpan Riwayat
        String riwayatId = userRef.child("riwayatKuis").push().getKey();
        Map<String, Object> riwayat = new HashMap<>();
        riwayat.put("subject", subject != null ? subject : "Umum");
        riwayat.put("score", nilai);
        riwayat.put("correct", benar);
        riwayat.put("total", total);
        riwayat.put("timestamp", System.currentTimeMillis());

        if (riwayatId != null) {
            userRef.child("riwayatKuis").child(riwayatId).setValue(riwayat);
        }

        // B. Update Statistik (XP/Poin & Jumlah Kuis)
        userRef.child("stats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long currentKuis = 0;
                long currentPoin = 0;

                if (snapshot.exists()) {
                    Long k = snapshot.child("totalKuis").getValue(Long.class);
                    Long p = snapshot.child("totalPoin").getValue(Long.class);
                    if (k != null) currentKuis = k;
                    if (p != null) currentPoin = p;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("totalKuis", currentKuis + 1);
                updates.put("totalPoin", currentPoin + ((long) benar * 10)); // 1 benar = 10 poin
                userRef.child("stats").updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}