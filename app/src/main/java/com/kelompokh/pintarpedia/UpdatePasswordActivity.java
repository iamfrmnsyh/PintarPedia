package com.kelompokh.pintarpedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UpdatePasswordActivity extends AppCompatActivity {

    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_password);

        mAuth = FirebaseAuth.getInstance();

        // Inisialisasi View (Pastikan ID ini ada di XML Anda)
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnUpdatePassword); // Sesuaikan ID tombol
        progressBar = findViewById(R.id.loadingUpdate);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> handleUpdatePassword());
    }

    private void handleUpdatePassword() {
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // 1. Validasi Input Kosong
        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Semua kolom harus diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Validasi Panjang Password
        if (newPass.length() < 6) {
            etNewPassword.setError("Password minimal 6 karakter");
            return;
        }

        // 3. Validasi Kecocokan Password Baru
        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Konfirmasi password tidak cocok!");
            return;
        }

        // 4. Validasi agar Password Baru tidak sama dengan Password Lama (Opsional tapi bagus)
        if (newPass.equals(oldPass)) {
            etNewPassword.setError("Password baru tidak boleh sama dengan password lama");
            return;
        }

        processUpdate(oldPass, newPass);
    }

    private void processUpdate(String oldPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            progressBar.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);

            // LOGIKA RE-AUTHENTICATION (Memastikan sandi lama benar)
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

            user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
                if (reAuthTask.isSuccessful()) {
                    // JIKA SANDI LAMA BENAR -> BARU UPDATE KE SANDI BARU
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        progressBar.setVisibility(View.GONE);

                        if (updateTask.isSuccessful()) {
                            // Hapus sesi lama & paksa login ulang
                            mAuth.signOut();
                            Toast.makeText(UpdatePasswordActivity.this,
                                    "Berhasil! Password lama sudah tidak berlaku. Silakan login ulang.",
                                    Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(UpdatePasswordActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            btnSubmit.setEnabled(true);
                            Toast.makeText(this, "Gagal update password baru.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // JIKA SANDI LAMA SALAH
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    etOldPassword.setError("Password lama salah!");
                    etOldPassword.requestFocus();
                }
            });
        }
    }
}