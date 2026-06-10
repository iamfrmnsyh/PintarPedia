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
    private TextView tvTotalUser, tvTotalQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        mAuth = FirebaseAuth.getInstance();

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
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long count = snapshot.getChildrenCount();
                    tvTotalUser.setText(String.valueOf(count));
                    Log.d("FIREBASE_ADMIN", "Total Users updated: " + count);
                } else {
                    tvTotalUser.setText("0");
                    Log.d("FIREBASE_ADMIN", "No users found in node 'Users'");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ADMIN", "Error Fetch Users: " + error.getMessage() + " (Code: " + error.getCode() + ")");
                // Jika error code 1, biasanya adalah Permission Denied (Masalah di Firebase Rules)
                if (error.getCode() == -1 || error.getCode() == 1) {
                    Toast.makeText(AdminHomeActivity.this, "Akses Database Ditolak. Periksa Firebase Rules.", Toast.LENGTH_LONG).show();
                }
            }
        });

        DatabaseReference quizRef = FirebaseDatabase.getInstance().getReference("soal_utbk");
        quizRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long count = snapshot.getChildrenCount();
                    tvTotalQuiz.setText(String.valueOf(count));
                } else {
                    tvTotalQuiz.setText("0");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE_ADMIN", "Error Fetch Quiz: " + error.getMessage());
            }
        });
    }
}