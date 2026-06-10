package com.kelompokh.pintarpedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kelompokh.pintarpedia.databinding.ActivityLoginBinding;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    // Kode Rahasia untuk memvalidasi akses Admin
    private static final String SECRET_ADMIN_CODE = "PINTARPEDIA_ADMIN";

    @Override
    protected void onStart() {
        super.onStart();
        // --- 1. LOGIKA SESSION MANAGEMENT (AUTO-LOGIN) ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
            String role = sharedPref.getString("role_user", "user");
            redirectByRole(role);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        setupGoogleSignIn();
        setupUIListeners();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupUIListeners() {
        // Logika Toggle Role (Admin vs User)
        binding.toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnRoleAdmin) {
                    binding.tvLoginTitle.setText("Login Admin");
                    binding.tlAdminCode.setVisibility(View.VISIBLE);
                    binding.btnGoogleSignin.setVisibility(View.GONE);
                    binding.layoutOr.setVisibility(View.GONE);
                    binding.tvResetPassword.setVisibility(View.GONE); // Admin biasanya tidak reset via app
                } else {
                    binding.tvLoginTitle.setText("Login User");
                    binding.tlAdminCode.setVisibility(View.GONE);
                    binding.btnGoogleSignin.setVisibility(View.VISIBLE);
                    binding.layoutOr.setVisibility(View.VISIBLE);
                    binding.tvResetPassword.setVisibility(View.VISIBLE);
                }
            }
        });

        // Tombol Login Utama
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            if (binding.toggleRole.getCheckedButtonId() == R.id.btnRoleAdmin) {
                loginSebagaiAdmin(email, password);
            } else {
                loginSebagaiUser(email, password);
            }
        });

        // --- 2. LOGIKA NAVIGASI RESET PASSWORD ---
        binding.tvResetPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });

        // Navigasi Lainnya
        binding.btnGoogleSignin.setOnClickListener(v -> signInGoogle());
        binding.tvSignup.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginSebagaiAdmin(String email, String password) {
        String inputCode = binding.etAdminCode.getText().toString().trim();
        if (!inputCode.equals(SECRET_ADMIN_CODE)) {
            Toast.makeText(this, "Kode Rahasia Admin Salah!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handleLoginSuccess("admin", false);
                    } else {
                        // Opsi: Jika akun admin belum ada, buat otomatis
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, createRes -> {
                                    if (createRes.isSuccessful()) handleLoginSuccess("admin", false);
                                    else Toast.makeText(this, "Gagal Akses Admin", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void loginSebagaiUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Cek verifikasi email jika diperlukan
                        if (user != null && user.isEmailVerified()) {
                            handleLoginSuccess("user", false);
                        } else if (user != null) {
                            Toast.makeText(this, "Silakan verifikasi email Anda.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(this, "Login Gagal. Cek kembali akun Anda.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleLoginSuccess(String role, boolean isGoogleLogin) {
        // --- 3. SIMPAN ROLE KE SHARED PREFERENCES ---
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("role_user", role);
        editor.apply();

        if (mAuth.getUid() != null) {
            // Ambil data profil dari Firebase agar SharedPreferences terupdate
            mDatabase.child(mAuth.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("username").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);

                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString("nama_user", name);
                        edit.putString("phone_user", phone);
                        edit.apply();
                        
                        // Opsional: Pastikan role di DB sinkron jika login sebagai admin
                        if (role.equals("admin") && !snapshot.hasChild("role")) {
                            mDatabase.child(mAuth.getUid()).child("role").setValue("admin");
                        }
                        
                        redirectByRole(role);
                    } else if (isGoogleLogin || role.equals("admin")) {
                        // Akun baru (Hanya untuk Google Login atau Admin baru)
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String name = user.getDisplayName() != null ? user.getDisplayName() : (role.equals("admin") ? "Admin" : "User");
                            String email = user.getEmail();

                            Map<String, Object> newUser = new HashMap<>();
                            newUser.put("userId", user.getUid());
                            newUser.put("username", name);
                            newUser.put("email", email);
                            newUser.put("phone", "-");
                            newUser.put("role", role);
                            mDatabase.child(user.getUid()).setValue(newUser);

                            if (role.equals("user")) {
                                Map<String, Object> stats = new HashMap<>();
                                stats.put("totalKuis", 0);
                                stats.put("totalPoin", 0);
                                stats.put("peringkat", "-");
                                mDatabase.child(user.getUid()).child("stats").setValue(stats);
                            }

                            SharedPreferences.Editor edit = sharedPref.edit();
                            edit.putString("nama_user", name);
                            edit.putString("phone_user", "-");
                            edit.apply();
                        }
                        redirectByRole(role);
                    } else {
                        // User login manual tapi data di Database "Users" tidak ada
                        mAuth.signOut();
                        Toast.makeText(LoginActivity.this, "Akun belum terdaftar di aplikasi. Silakan registrasi terlebih dahulu.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    redirectByRole(role);
                }
            });
        } else {
            redirectByRole(role);
        }
    }

    private void redirectByRole(String role) {
        if (role.equalsIgnoreCase("admin")) {
            startActivity(new Intent(this, AdminHomeActivity.class));
        } else {
            startActivity(new Intent(this, HomeActivity.class));
        }
        finish(); // Tutup LoginActivity agar tidak bisa di-Back
    }

    // --- GOOGLE SIGN IN LOGIC ---
    private void signInGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) handleLoginSuccess("user", true);
                });
    }
}