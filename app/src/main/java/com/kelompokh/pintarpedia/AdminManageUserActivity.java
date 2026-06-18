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

    private static final String TAG = "AdminManageUser";
    private RecyclerView rvUserList;
    private UserAdapter adapter;
    private List<UserModel> userList;
    private DatabaseReference mDatabase;
    private TextView tvTotalUser;
    private ValueEventListener usersValueListener;

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

        // Menggunakan nama node "Users" sesuai konfigurasi Firebase Rules Anda
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Memulai pemantauan data secara Real-time
        fetchUsersRealtime();
    }

    private void fetchUsersRealtime() {
        usersValueListener = new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();

                Log.d(TAG, "onDataChange dipicu. Apakah data ada? " + snapshot.exists());

                if (snapshot.exists()) {
                    Log.d(TAG, "Jumlah data anak ditemukan di Firebase: " + snapshot.getChildrenCount());

                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            UserModel user = data.getValue(UserModel.class);
                            if (user != null) {
                                // ==================== LOGIKA FILTER KELOMPOK H ====================
                                // Hanya masukkan pengguna ke daftar jika role-nya BUKAN admin.
                                // Jika property role null, default akan tetap dianggap sebagai user biasa.
                                if (user.getRole() == null || !"admin".equalsIgnoreCase(user.getRole())) {
                                    user.setUserId(data.getKey());
                                    userList.add(user);
                                }
                                // ==================================================================
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal mengurai data user: " + e.getMessage());
                        }
                    }
                }

                // Menampilkan total data pengguna setelah disaring (Murni hanya menghitung user biasa)
                if (tvTotalUser != null) {
                    tvTotalUser.setText("Total Pengguna: " + userList.size());
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Error: " + error.getMessage() + " | Code: " + error.getCode());
                if (error.getCode() == -1 || error.getCode() == 1) {
                    Toast.makeText(AdminManageUserActivity.this, "Akses Ditolak! Periksa kembali Rules Firebase Anda.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AdminManageUserActivity.this, "Masalah Koneksi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Pasang listener real-time ke referensi database
        mDatabase.addValueEventListener(usersValueListener);
    }

    private void showUserOptions(UserModel user) {
        String[] options = {"Jadikan Admin", "Jadikan User", "Hapus Pengguna"};

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
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Berhasil merubah peran ke " + newRole, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Gagal merubah peran: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmDeleteUser(UserModel user) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Pengguna")
                .setMessage("Apakah Anda yakin ingin menghapus " + user.getUsername() + "? Tindakan ini tidak bisa dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    mDatabase.child(user.getUserId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Pengguna berhasil dihapus", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Pembersihan listener wajib untuk menghindari kebocoran memori (memory leak)
        if (mDatabase != null && usersValueListener != null) {
            mDatabase.removeEventListener(usersValueListener);
        }
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