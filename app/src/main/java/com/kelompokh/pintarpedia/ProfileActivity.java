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

        // Pemeriksaan Autentikasi Utama
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            // Tarik data terbaru dari Firebase secara asinkron di background
            fetchUserProfile();
        } else {
            forceNavToLogin();
        }

        initClickListeners();
    }

    /**
     * TAMBAHAN UTAMA: Lifecycle onResume dipicu kembali saat user menutup
     * halaman EditProfileActivity, menjamin data lokal selalu sinkron dan segar.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadDataLocal();
    }

    private void initClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // Menuju halaman Edit Profil
        binding.btnEditProfile.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

        // Menuju halaman Ganti Password
        binding.btnUbahPassword.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, UpdatePasswordActivity.class)));

        // Memicu aksi logout bersih
        binding.btnLogout.setOnClickListener(v -> prosesLogout());
    }

    private void fetchUserProfile() {
        if (mDatabase == null) return;

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
                    String email = snapshot.child("email").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (nama != null) {
                        editor.putString("nama_user", nama);
                    }
                    if (phone != null) {
                        editor.putString("phone_user", phone);
                    }
                    if (email != null) {
                        editor.putString("email_user", email);
                    }

                    // --- FOTO PROFIL ---
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        editor.putString("image_user", imageUrl);
                    }

                    editor.apply(); // Simpan semua perubahan baru ke lokal cache

                    // Muat ulang tampilan UI agar langsung merefleksikan data Firebase terbaru
                    loadDataLocal();
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

        String currentName = sharedPref.getString("nama_user", "User");
        binding.tvNamaHeader.setText(currentName);
        binding.tvUsernameDetail.setText(currentName);
        binding.tvPhone.setText(sharedPref.getString("phone_user", "-"));

        String email = sharedPref.getString("email_user", "");
        if (!email.isEmpty()) {
            binding.tvEmailDetail.setText(email);
        } else if (mAuth.getCurrentUser() != null) {
            binding.tvEmailDetail.setText(mAuth.getCurrentUser().getEmail());
        } else {
            binding.tvEmailDetail.setText("-");
        }

        // Tampilkan UID Singkat
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String displayId = uid.length() >= 8 ? uid.substring(0, 8) : uid;
            binding.tvIdUser.setText("ID: #" + displayId.toUpperCase());
        }

        // Tampilkan gambar profil lokal / dari URL cache Glide
        String cachedImg = sharedPref.getString("image_user", "");
        if (!cachedImg.isEmpty() && !isDestroyed()) {
            Glide.with(ProfileActivity.this)
                    .load(cachedImg)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(binding.profileImage);
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

    /**
     * PERBAIKAN LOGOUT: Menghapus data SharedPreferences secara menyeluruh
     * sebelum melakukan sign out agar data tidak tertinggal untuk user berikutnya.
     */
    private void prosesLogout() {
        // 1. Bersihkan seluruh data sesi lokal (nama, email, foto, dll)
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        // 2. Keluar dari Firebase Authentication
        mAuth.signOut();

        // 3. Alihkan ke LoginActivity dan hancurkan tumpukan activity lama
        Toast.makeText(this, "Berhasil keluar akun", Toast.LENGTH_SHORT).show();
        forceNavToLogin();
    }

    private void forceNavToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}