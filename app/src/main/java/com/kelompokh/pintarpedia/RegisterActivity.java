package com.kelompokh.pintarpedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText etUsername, etEmail, etPassword, etConfirmPassword, etNomor;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Inisialisasi View
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etNomor = findViewById(R.id.etPhone);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void performRegistration() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String nomor = etNomor.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || nomor.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Konfirmasi password tidak cocok");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Panggil fungsi simpan data (Struktur terpadu tunggal)
                            saveUserToFirebaseDatabase(user.getUid(), username, email, nomor);

                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                                        if (emailTask.isSuccessful()) {
                                            showInAppNotification("Verifikasi Terkirim",
                                                    "Link verifikasi dikirim ke " + email, true);
                                        } else {
                                            // Failsafe jika gagal mengirim email verifikasi karena server sibuk
                                            btnRegister.setEnabled(true);
                                            showInAppNotification("Verifikasi Gagal",
                                                    "Gagal mengirim email: " + emailTask.getException().getMessage(), false);
                                        }
                                    });
                        }
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirebaseDatabase(String userId, String username, String email, String phone) {
        // 1. Map untuk Data Profil Utama
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("username", username);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("birthPlace", "-");
        userMap.put("birthDate", "-");
        userMap.put("role", "user");

        // 2. Map Tambahan untuk Folder 'stats' (Data Awal)
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("totalKuis", 0);
        statsMap.put("totalPoin", 0);
        statsMap.put("peringkat", "-");

        // ==================== PERBAIKAN STRUKTUR PAKET DATA ====================
        // Memasukkan statsMap langsung ke dalam struktur userMap agar dikirim bersamaan.
        // Hal ini mencegah terjadinya tabrakan asinkron yang bisa menghapus data profil.
        userMap.put("stats", statsMap);

        // Kirimkan data ke database murni dalam 1 kali tembak (Lebih aman & hemat kuota)
        mDatabase.child(userId).setValue(userMap);
        // =======================================================================

        // Simpan lokal juga
        saveUserDataLocal(username, phone);
    }

    private void showInAppNotification(String title, String message, boolean isSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(message).setCancelable(false);
        if (isSuccess) {
            builder.setPositiveButton("Ke Halaman Login", (dialog, which) -> {
                // 1. Sign out dari session pendaftaran Firebase
                mAuth.signOut();

                // 2. Clear SharedPreferences lokal agar tidak terjadi data ghosting
                SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
                sharedPref.edit().clear().apply();

                // 3. Pindah ke halaman login
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        } else {
            builder.setPositiveButton("Coba Lagi", (dialog, which) -> dialog.dismiss());
        }
        builder.show();
    }

    private void saveUserDataLocal(String name, String phone) {
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("nama_user", name);
        editor.putString("phone_user", phone);
        editor.putString("birthPlace_user", "-");
        editor.putString("birthDate_user", "-");
        editor.putString("role_user", "user");
        editor.apply();
    }
}