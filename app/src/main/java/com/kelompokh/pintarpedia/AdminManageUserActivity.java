package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminManageUserActivity extends AppCompatActivity {

    private RecyclerView rvUserList;
    private UserAdapter adapter;
    private List<UserModel> userList;
    private DatabaseReference mDatabase;
    private TextView tvTotalUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_user);

        // Inisialisasi UI
        tvTotalUser = findViewById(R.id.tvTotalUser); 
        rvUserList = findViewById(R.id.rvUserList);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList, this::showUserOptions);
        rvUserList.setAdapter(adapter);

        // Referensi Firebase ke node "Users"
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Memulai pemantauan data secara Real-time
        fetchUsersRealtime();
    }

    private void fetchUsersRealtime() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            UserModel user = data.getValue(UserModel.class);
                            if (user != null) {
                                user.setUserId(data.getKey());
                                userList.add(user);
                            }
                        } catch (Exception e) {
                            Log.e("AdminManageUser", "Error parsing user data: " + e.getMessage());
                        }
                    }
                }

                if (tvTotalUser != null) {
                    tvTotalUser.setText("Total Pengguna: " + userList.size());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminManageUser", "Firebase Error: " + error.getMessage() + " | Code: " + error.getCode());
                if (error.getCode() == -1 || error.getCode() == 1) {
                    Toast.makeText(AdminManageUserActivity.this, "Gagal: Akses Ditolak (Rules)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AdminManageUserActivity.this, "Masalah Koneksi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showUserOptions(UserModel user) {
        String[] options = {"Jadikan Admin", "Jadikan User", "Hapus Pengguna"};
        
        // Sesuaikan opsi berdasarkan role saat ini
        if ("admin".equalsIgnoreCase(user.getRole())) {
            options[0] = "Sudah Admin";
        } else {
            options[1] = "Sudah User";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kelola: " + user.getUsername());
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    updateUserRole(user, "admin");
                    break;
                case 1:
                    updateUserRole(user, "user");
                    break;
                case 2:
                    confirmDeleteUser(user);
                    break;
            }
        });
        builder.show();
    }

    private void updateUserRole(UserModel user, String newRole) {
        if (newRole.equalsIgnoreCase(user.getRole())) {
            Toast.makeText(this, "Peran sudah sesuai", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child(user.getUserId()).child("role").setValue(newRole)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Berhasil merubah peran", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal merubah peran", Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteUser(UserModel user) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Pengguna")
                .setMessage("Apakah Anda yakin ingin menghapus " + user.getUsername() + "? Tindakan ini tidak bisa dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    mDatabase.child(user.getUserId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Pengguna dihapus", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // --- INNER CLASS ADAPTER ---
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<UserModel> users;
        private final OnUserClickListener listener;

        interface OnUserClickListener {
            void onUserClick(UserModel user);
        }

        UserAdapter(List<UserModel> users, OnUserClickListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel user = users.get(position);

            holder.tvName.setText(user.getUsername());

            String roleStr = (user.getRole() != null) ? user.getRole().toUpperCase() : "USER";
            holder.tvDetail.setText(user.getEmail() + " • Role: " + roleStr);

            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetail;

            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(android.R.id.text1);
                tvDetail = view.findViewById(android.R.id.text2);

                tvName.setTextColor(0xFF1E293B);
                tvName.setTextSize(16);
                tvName.setPadding(0, 10, 0, 4);

                tvDetail.setTextColor(0xFF64748B);
                tvDetail.setTextSize(13);
            }
        }
    }
}
