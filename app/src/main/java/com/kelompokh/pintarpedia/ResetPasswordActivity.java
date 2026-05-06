package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnCekAkun;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // 1. Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Inisialisasi View dari XML
        etEmail = findViewById(R.id.et_email_reset);
        btnCekAkun = findViewById(R.id.btn_kirim_email);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);
        progressBar = findViewById(R.id.loadingReset); // Pastikan ID ini ada di XML

        // 3. Logika Klik Tombol Cek Akun
        btnCekAkun.setOnClickListener(v -> validasiDanPindahKeUpdate());

        // 4. Logika Kembali ke Login
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> finish());
        }
    }

    @SuppressLint("SetTextI18n")
    private void validasiDanPindahKeUpdate() {
        // Null safety check untuk input email
        if (etEmail.getText() == null) return;

        String email = etEmail.getText().toString().trim();

        // Validasi Input Kosong
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email wajib diisi!");
            etEmail.requestFocus();
            return;
        }

        // Validasi Format Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid!");
            etEmail.requestFocus();
            return;
        }

        // Tampilkan loading dan matikan tombol agar tidak double click
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnCekAkun.setEnabled(false);
        btnCekAkun.setText("Mengecek...");

        // LOGIKA FIREBASE: Cek apakah email terdaftar
        //noinspection deprecation
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            // Sembunyikan loading dan aktifkan tombol kembali
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            btnCekAkun.setEnabled(true);
            btnCekAkun.setText("Validasi Akun");

            if (task.isSuccessful()) {
                // Mengecek apakah ada metode login yang terdaftar untuk email ini
                boolean terdaftar = task.getResult() != null &&
                        task.getResult().getSignInMethods() != null &&
                        !task.getResult().getSignInMethods().isEmpty();

                if (terdaftar) {
                    // AKUN DITEMUKAN
                    Toast.makeText(this, "Akun ditemukan! Silakan buat password baru.", Toast.LENGTH_SHORT).show();

                    // Berpindah ke UpdatePasswordActivity
                    Intent intent = new Intent(ResetPasswordActivity.this, UpdatePasswordActivity.class);
                    intent.putExtra("EMAIL_TARGET", email); // Mengirim email ke halaman berikutnya
                    startActivity(intent);
                    finish(); // Menutup halaman reset agar tidak bisa kembali ke sini
                } else {
                    // AKUN TIDAK DITEMUKAN
                    etEmail.setError("Email ini belum terdaftar di sistem kami");
                    Toast.makeText(this, "Maaf, akun tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Gagal koneksi atau error Firebase lainnya
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Terjadi kesalahan";
                Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}