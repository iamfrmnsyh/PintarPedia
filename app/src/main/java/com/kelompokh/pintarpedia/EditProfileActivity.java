package com.kelompokh.pintarpedia;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kelompokh.pintarpedia.databinding.ActivityEditProfileBinding;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());
            loadCurrentData();
        } else {
            finish();
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadCurrentData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    binding.etUsername.setText(username);
                    binding.etPhone.setText(phone != null ? phone : "");
                    binding.etEmail.setText(email != null ? email : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : ""));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(EditProfileActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileChanges() {
        if (binding.etUsername.getText() == null) return;
        final String username = binding.etUsername.getText().toString().trim();

        final String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";

        if (username.isEmpty()) {
            binding.etUsername.setError("Nama tidak boleh kosong");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("phone", phone);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSave.setEnabled(true);
            if (task.isSuccessful()) {
                // Update SharedPreferences
                SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("nama_user", username);
                editor.putString("phone_user", phone);
                editor.apply();

                Toast.makeText(EditProfileActivity.this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Gagal memperbarui profil", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
