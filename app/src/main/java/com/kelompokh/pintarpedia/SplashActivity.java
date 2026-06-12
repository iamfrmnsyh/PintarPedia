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
     * LOGIKA KUNCI NAVIGASI SISI:
     * Mengecek token sesi Firebase secara aman setelah layar iklan selesai.
     */
    private void berpindahHalaman() {
        Intent intent;

        if (mAuth.getCurrentUser() != null) {
            // Skenario A: User sudah login sebelumnya -> Langsung ke HomeActivity
            Log.d(TAG, "Sesi aktif ditemukan. Mengarahkan langsung ke HomeActivity.");
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        } else {
            // Skenario B: Sesi kosong/baru install -> Masuk ke rute LoginActivity
            Log.d(TAG, "Tidak ada sesi aktif. Mengarahkan ke LoginActivity.");
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Menghancurkan SplashActivity agar tidak bisa di-back oleh user
    }
}