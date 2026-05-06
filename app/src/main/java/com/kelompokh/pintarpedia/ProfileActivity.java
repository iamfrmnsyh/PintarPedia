package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kelompokh.pintarpedia.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    binding.profileImage.setImageURI(imageUri);
                    uploadImageToFirebase();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance().getReference("ProfilePictures");

        // 1. Load data lokal (agar saat buka tidak kosong)
        loadDataLocal();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            // 2. Tarik data terbaru dari Firebase & simpan ke Lokal
            fetchUserProfile();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        initClickListeners();
    }

    private void initClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // 3. Navigasi ke Ganti Password (Activity Baru)
        binding.btnUbahPassword.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, UpdatePasswordActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> prosesLogout());
    }

    private void fetchUserProfile() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();

                    // --- INFO PRIBADI ---
                    String nama = snapshot.child("username").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String bPlace = snapshot.child("birthPlace").getValue(String.class);
                    String bDate = snapshot.child("birthDate").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (nama != null) {
                        binding.tvNamaHeader.setText(nama);
                        binding.tvUsernameDetail.setText(nama);
                        editor.putString("nama_user", nama);
                    }
                    if (phone != null) {
                        binding.tvPhone.setText(phone);
                        editor.putString("phone_user", phone);
                    }
                    if (bPlace != null && bDate != null) {
                        binding.tvTTL.setText(bPlace + ", " + bDate);
                        editor.putString("birthPlace_user", bPlace);
                        editor.putString("birthDate_user", bDate);
                    }

                    // --- FOTO PROFIL ---
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ProfileActivity.this).load(imageUrl).placeholder(R.drawable.profile_placeholder).into(binding.profileImage);
                        editor.putString("image_user", imageUrl);
                    }

                    // --- STATISTIK (XP, KUIS, PERINGKAT) ---
                    if (snapshot.hasChild("stats")) {
                        Long totalKuis = snapshot.child("stats").child("totalKuis").getValue(Long.class);
                        Long totalPoin = snapshot.child("stats").child("totalPoin").getValue(Long.class);
                        String rank = snapshot.child("stats").child("peringkat").getValue(String.class);

                        if (totalKuis != null) {
                            binding.tvStatKuis.setText(String.valueOf(totalKuis));
                            editor.putLong("kuis_user", totalKuis);
                        }
                        if (totalPoin != null) {
                            binding.tvStatPoin.setText(String.valueOf(totalPoin));
                            editor.putLong("poin_user", totalPoin);
                        }
                        if (rank != null) {
                            binding.tvStatRank.setText(rank);
                            editor.putString("rank_user", rank);
                        }
                    }
                    editor.apply(); // Simpan semua perubahan ke lokal
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseProfile", "Error: " + error.getMessage());
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadDataLocal() {
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        binding.tvNamaHeader.setText(sharedPref.getString("nama_user", "User"));
        binding.tvUsernameDetail.setText(sharedPref.getString("nama_user", "User"));
        binding.tvPhone.setText(sharedPref.getString("phone_user", "-"));
        binding.tvTTL.setText(sharedPref.getString("birthPlace_user", "-") + ", " + sharedPref.getString("birthDate_user", ""));

        // Load stats dari lokal
        binding.tvStatKuis.setText(String.valueOf(sharedPref.getLong("kuis_user", 0)));
        binding.tvStatPoin.setText(String.valueOf(sharedPref.getLong("poin_user", 0)));
        binding.tvStatRank.setText(sharedPref.getString("rank_user", "-"));

        if (mAuth.getCurrentUser() != null) {
            binding.tvIdUser.setText("ID: #" + mAuth.getCurrentUser().getUid().substring(0, 8).toUpperCase());
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri != null && mAuth.getCurrentUser() != null) {
            StorageReference fileRef = mStorage.child(mAuth.getCurrentUser().getUid() + ".jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                mDatabase.child("profileImageUrl").setValue(uri.toString());
                Toast.makeText(ProfileActivity.this, "Foto Diperbarui", Toast.LENGTH_SHORT).show();
            }));
        }
    }

    private void prosesLogout() {
        getSharedPreferences("USER_DATA", Context.MODE_PRIVATE).edit().clear().apply();
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}