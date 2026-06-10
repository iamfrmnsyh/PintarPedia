package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.MobileAds;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Inisialisasi Mobile Ads sejak dini
        MobileAds.initialize(this, initializationStatus -> {});

        // 1. Animasi Fade In untuk Logo
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1500); // Muncul dalam 1.5 detik
        logoContainer.startAnimation(fadeIn);

        // 2. Delay 3 detik sebelum pindah ke Login
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Tutup splash screen agar tidak bisa kembali dengan tombol back
        }, 5000);
    }
}