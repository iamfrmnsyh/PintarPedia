package com.kelompokh.pintarpedia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminHomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private TextView tvTotalUser, tvTotalQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvTotalUser = findViewById(R.id.tvTotalUserCount);
        tvTotalQuiz = findViewById(R.id.tvTotalQuizCount);
        MaterialButton btnLogout = findViewById(R.id.btnLogoutAdmin);
        MaterialCardView cardUser = findViewById(R.id.cardManageUser);
        MaterialCardView cardQuiz = findViewById(R.id.cardManageQuiz);

        fetchDashboardData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AdminHomeActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        // Hubungkan menu Kelola User
        cardUser.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminManageUserActivity.class);
            startActivity(intent);
        });

        // Hubungkan menu Kelola Quiz
        cardQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminAddSoalActivity.class);
            startActivity(intent);
        });
    }

    private void fetchDashboardData() {
        // Path "Users" sesuai dengan database kita
        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalUser.setText(String.valueOf(snapshot.exists() ? snapshot.getChildrenCount() : 0));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ADMIN", error.getMessage());
            }
        });

        mDatabase.child("soal_utbk").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalQuiz.setText(String.valueOf(snapshot.exists() ? snapshot.getChildrenCount() : 0));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ADMIN", error.getMessage());
            }
        });
    }
}