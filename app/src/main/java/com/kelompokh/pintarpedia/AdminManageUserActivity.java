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
        adapter = new UserAdapter(userList);
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
                for (DataSnapshot data : snapshot.getChildren()) {
                    UserModel user = data.getValue(UserModel.class);
                    if (user != null) {
                        userList.add(user);
                    }
                }

                // Update jumlah total user di layar
                if (tvTotalUser != null) {
                    tvTotalUser.setText("Total Pengguna: " + userList.size());
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminManageUser", error.getMessage());
                Toast.makeText(AdminManageUserActivity.this, "Koneksi Firebase Bermasalah", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- INNER CLASS ADAPTER ---
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private final List<UserModel> users;

        UserAdapter(List<UserModel> users) {
            this.users = users;
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

                tvName.setTextColor(0xFF0F172A); 
                tvName.setTextSize(16);
                tvName.setPadding(0, 10, 0, 4);

                tvDetail.setTextColor(0xFF64748B);
                tvDetail.setTextSize(13);
            }
        }
    }
}
