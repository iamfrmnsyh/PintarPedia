package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private FirebaseAuth mAuth;
    private AppOpenAd appOpenAd = null;
    private boolean isAdLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Inisialisasi Mobile Ads sejak dini
        MobileAds.initialize(this, initializationStatus -> {});

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 1. Animasi Fade In untuk Logo
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        if (logoContainer != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(1500); // Muncul dalam 1.5 detik
            logoContainer.startAnimation(fadeIn);
        }

        // 2. Mulai memuat dan menyiapkan App Open Ad
        fetchAppOpenAd();
    }

    /**
     * Meminta data iklan App Open dari server AdMob secara asinkron
     */
    private void fetchAppOpenAd() {
        if (isAdLoading || appOpenAd != null) {
            return;
        }

        isAdLoading = true;
        AdRequest request = new AdRequest.Builder().build();

        Log.d(TAG, "Mulai memuat App Open Ad...");
        AppOpenAd.load(
                this,
                getString(R.string.app_open_ad_unit_id),
                request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isAdLoading = false;
                        Log.d(TAG, "App Open Ad BERHASIL dimuat.");

                        // Iklan siap, tayangkan ke pengguna secara langsung
                        showAdAndMove();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        isAdLoading = false;
                        Log.e(TAG, "App Open Ad GAGAL dimuat: " + loadAdError.getMessage());

                        // FAILSAFE: Jika internet putus/iklan kosong, langsung bypass ke halaman utama
                        berpindahHalaman();
                    }
                });
    }

    /**
     * Menampilkan iklan ke layar penuh dan menangani callback aksi pengguna
     */
    private void showAdAndMove() {
        if (appOpenAd != null) {
            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Berjalan saat pengguna menekan tombol "Lewati/Close" atau iklan selesai tayang
                    Log.d(TAG, "Iklan ditutup oleh user.");
                    appOpenAd = null;
                    berpindahHalaman();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Iklan gagal ditayangkan di layar penuh: " + adError.getMessage());
                    berpindahHalaman();
                }
            });

            appOpenAd.show(SplashActivity.this);
        } else {
            berpindahHalaman();
        }
    }

    /**
     * LOGIKA NAVIGASI TERBARU:
     * Mengecek sesi login dan melakukan routing halaman berdasarkan ROLE di database.
     */
    private void berpindahHalaman() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "Sesi aktif ditemukan (UID: " + uid + "). Memeriksa role pengguna...");

            // Mengambil data role secara realtime/sekali baca dari Realtime Database
            // Catatan: Jika nama node Anda menggunakan huruf kapital, ganti "users" menjadi "Users"
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Intent intent;
                            String role = snapshot.getValue(String.class);

                            Log.d(TAG, "Role ditemukan dari Firebase: " + role);

                            if ("admin".equalsIgnoreCase(role)) {
                                // Skenario A1: Pengguna terdeteksi sebagai Admin
                                Log.d(TAG, "Merarahkan langsung ke AdminHomeActivity.");
                                intent = new Intent(SplashActivity.this, AdminHomeActivity.class);
                            } else {
                                // Skenario A2: Pengguna biasa (atau properti role belum diset)
                                Log.d(TAG, "Mengarahkan langsung ke HomeActivity.");
                                intent = new Intent(SplashActivity.this, HomeActivity.class);
                            }

                            startActivity(intent);
                            finish(); // Hancurkan SplashActivity
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Gagal memuat data role: " + error.getMessage());
                            // Failsafe penanganan error: Lempar ke Login jika koneksi gagal demi keamanan sesi
                            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
        } else {
            // Skenario B: Sesi kosong/belum login -> Masuk ke rute LoginActivity
            Log.d(TAG, "Tidak ada sesi aktif. Mengarahkan ke LoginActivity.");
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}