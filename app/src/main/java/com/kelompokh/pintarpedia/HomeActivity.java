package com.kelompokh.pintarpedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kelompokh.pintarpedia.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // 1. Ambil Nama dari SharedPreferences (Data Lokal)
        loadLocalProfile();

        // 2. Sinkronisasi Nama dari Firebase
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            syncUserData();
        }

        setupClickListeners();
    }

    private void loadLocalProfile() {
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        // Default value diganti menjadi kosong agar kita bisa deteksi jika data belum ada
        String namaLokal = sharedPref.getString("nama_user", "");

        if (!namaLokal.isEmpty()) {
            binding.tvUsernameHome.setText(namaLokal);
        } else {
            binding.tvUsernameHome.setText("Memuat..."); // Tampilan sementara saat sync
        }
    }

    private void syncUserData() {
        // Menggunakan addValueEventListener agar jika Admin mengubah nama di console,
        // nama di HP user ikut berubah saat itu juga
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nama = snapshot.child("username").getValue(String.class);
                    if (nama != null && !nama.isEmpty()) {
                        binding.tvUsernameHome.setText(nama);

                        // Simpan ke lokal agar saat aplikasi dibuka lagi sudah ada namanya
                        getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)
                                .edit().putString("nama_user", nama).apply();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupClickListeners() {
        binding.cardUtbk2022.setOnClickListener(v -> moveAsMapel("UTBK 2022"));
        binding.cardUtbk2023.setOnClickListener(v -> moveAsMapel("UTBK 2023"));
        binding.cardUtbk2024.setOnClickListener(v -> moveAsMapel("UTBK 2024"));
        binding.cardUtbk2025.setOnClickListener(v -> moveAsMapel("UTBK 2025"));
        binding.cardPrediksi2026.setOnClickListener(v -> moveAsMapel("Prediksi 2026"));
        binding.cardBankSoal.setOnClickListener(v -> moveAsMapel("Bank Soal"));

        binding.ivProfileHome.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void moveAsMapel(String kategori) {
        Intent intent = new Intent(HomeActivity.this, MapelActivity.class);
        intent.putExtra("KATEGORI_MENU", kategori);
        startActivity(intent);
    }
}