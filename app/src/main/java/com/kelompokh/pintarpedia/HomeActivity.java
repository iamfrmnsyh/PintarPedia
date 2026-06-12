package com.kelompokh.pintarpedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kelompokh.pintarpedia.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String TAG = "HomeActivity";

    // Variabel SDK Iklan AdMob
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    private NativeAd mNativeAd;
    private RewardedAd mRewardedAd;
    private RewardedInterstitialAd mRewardedInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inisialisasi View Binding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inisialisasi Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        // 2. IMPLEMENTASI FORMAT-FORMAT IKLAN (Preload di Background)
        initBannerAd();
        loadInterstitialAd();
        loadNativeAd();
        loadRewardedAd();
        loadRewardedInterstitialAd();

        // 3. Ambil Instance Firebase Auth Pertama Kali
        mAuth = FirebaseAuth.getInstance();

        setupClickListeners();
    }

    /**
     * PERBAIKAN STRUKTURAL UTAMA:
     * Memindahkan validasi akun dan referensi node data ke onResume agar token UID
     * dibaca secara segar setiap kali siklus perpindahan screen (Login -> Home) terjadi.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // A. Reset komponen teks ke mode tunggu untuk membersihkan sisa teks sesi akun lama
        binding.tvUsernameHome.setText("Memuat...");

        // B. Muat data profil dari SharedPreferences lokal terlebih dahulu jika tersedia
        loadLocalProfile();

        // C. Ambil verifikasi akun terautentikasi terkini dari Firebase Auth
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            Log.d(TAG, "User aktif terdeteksi. UID: " + uid);

            // Perbarui jalur node Database ke UID user baru
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            // Picu pengambilan data asinkron dari server Firebase Realtime
            syncUserData();
        } else {
            // Jika token user kosong/invalid, amankan rute dengan mengembalikan paksa ke halaman Login
            Log.w(TAG, "Akses ditolak: Sesi kosong.");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // ==================== 1. STRUKTUR IKLAN BANNER ====================
    private void initBannerAd() {
        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.banner_ad_unit_id));
        adView.setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(this, 360));

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() { Log.d(TAG, "Banner berhasil dimuat."); }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) { Log.e(TAG, "Banner gagal dimuat: " + adError.getMessage()); }
        });

        if (binding.adViewContainer != null) {
            binding.adViewContainer.removeAllViews();
            binding.adViewContainer.addView(adView);
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    // ==================== 2. STRUKTUR IKLAN INTERSTITIAL ====================
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial Ad BERHASIL dimuat.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial Ad GAGAL dimuat: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    private void checkAdBeforeMove(String kategori) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ditutup oleh user. Melanjutkan ke kuis.");
                    moveAsMapel(kategori);
                    loadInterstitialAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Interstitial gagal tayang: " + adError.getMessage());
                    moveAsMapel(kategori);
                }
            });
            mInterstitialAd.show(HomeActivity.this);
        } else {
            Log.d(TAG, "Interstitial belum siap, langsung pindah ke halaman.");
            moveAsMapel(kategori);
        }
    }

    // ==================== 3. STRUKTUR IKLAN NATIVE ====================
    private void loadNativeAd() {
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad_unit_id))
                .forNativeAd(nativeAd -> {
                    Log.d(TAG, "Iklan Native BERHASIL dimuat.");

                    if (isDestroyed()) {
                        nativeAd.destroy();
                        return;
                    }

                    if (mNativeAd != null) mNativeAd.destroy();
                    mNativeAd = nativeAd;

                    if (binding.nativeAdContainer != null) {
                        binding.nativeAdContainer.removeAllViews();

                        NativeAdView adViewComponent = new NativeAdView(HomeActivity.this);
                        setupSimpleNativeView(nativeAd, adViewComponent);

                        binding.nativeAdContainer.addView(adViewComponent);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        Log.e(TAG, "Iklan Native GAGAL dimuat: " + adError.getMessage());
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void setupSimpleNativeView(NativeAd nativeAd, NativeAdView adView) {
        android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(this);
        rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 24, 32, 24);
        rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(32f);
        shape.setColor(android.graphics.Color.parseColor("#FFFFFF"));
        shape.setStroke(2, android.graphics.Color.parseColor("#E2E8F0"));
        rootLayout.setBackground(shape);

        TextView tvHeadline = new TextView(this);
        tvHeadline.setText(nativeAd.getHeadline());
        tvHeadline.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvHeadline.setTextColor(android.graphics.Color.parseColor("#1E293B"));
        tvHeadline.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        adView.setHeadlineView(tvHeadline);
        rootLayout.addView(tvHeadline);

        if (nativeAd.getBody() != null) {
            TextView tvBody = new TextView(this);
            tvBody.setText(nativeAd.getBody());
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvBody.setTextColor(android.graphics.Color.parseColor("#64748B"));
            tvBody.setPadding(0, 8, 0, 16);
            adView.setBodyView(tvBody);
            rootLayout.addView(tvBody);
        }

        if (nativeAd.getCallToAction() != null) {
            Button btnCta = new Button(this);
            btnCta.setText(nativeAd.getCallToAction());
            btnCta.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
            btnCta.setTextColor(android.graphics.Color.WHITE);
            btnCta.setTransformationMethod(null);

            android.graphics.drawable.GradientDrawable btnShape = new android.graphics.drawable.GradientDrawable();
            btnShape.setCornerRadius(16f);
            btnShape.setColor(android.graphics.Color.parseColor("#2196F3"));
            btnCta.setBackground(btnShape);

            adView.setCallToActionView(btnCta);
            rootLayout.addView(btnCta);
        }

        adView.addView(rootLayout);
        adView.setNativeAd(nativeAd);
    }

    // ==================== 4. STRUKTUR IKLAN REWARDED VIDEO ====================
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, getString(R.string.rewarded_ad_unit_id), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        Log.i(TAG, "Rewarded Ad BERHASIL dimuat.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Rewarded Ad GAGAL dimuat: " + loadAdError.getMessage());
                        mRewardedAd = null;
                    }
                });
    }

    private void checkRewardBeforeMove(String kategori) {
        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Iklan reward ditutup oleh user.");
                    loadRewardedAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Iklan reward gagal tayang: " + adError.getMessage());
                    moveAsMapel(kategori);
                }
            });

            mRewardedAd.show(HomeActivity.this, rewardItem -> {
                Log.d(TAG, "User berhasil menyelesaikan video reward.");
                Toast.makeText(HomeActivity.this, "Akses premium terbuka! Selamat mengerjakan.", Toast.LENGTH_SHORT).show();
                moveAsMapel(kategori);
            });
        } else {
            Log.d(TAG, "Iklan reward belum siap, otomatis melewati kuis.");
            moveAsMapel(kategori);
        }
    }

    // ==================== 5. STRUKTUR IKLAN REWARDED INTERSTITIAL ====================
    private void loadRewardedInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedInterstitialAd.load(this, getString(R.string.rewarded_interstitial_ad_unit_id), adRequest,
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                        mRewardedInterstitialAd = rewardedInterstitialAd;
                        Log.i(TAG, "Rewarded Interstitial BERHASIL dimuat.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Rewarded Interstitial GAGAL dimuat: " + loadAdError.getMessage());
                        mRewardedInterstitialAd = null;
                    }
                });
    }

    private void checkRewardedInterstitialBeforeMove(String kategori) {
        if (mRewardedInterstitialAd != null) {
            mRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Interstitial ditutup.");
                    loadRewardedInterstitialAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    Log.e(TAG, "Rewarded Interstitial gagal tampil: " + adError.getMessage());
                    moveAsMapel(kategori);
                }
            });

            mRewardedInterstitialAd.show(HomeActivity.this, rewardItem -> {
                Log.d(TAG, "User sukses mengklaim reward dari Interstisial.");
                Toast.makeText(HomeActivity.this, "Bonus Akses Terbuka via Interstitial Reward!", Toast.LENGTH_SHORT).show();
                moveAsMapel(kategori);
            });
        } else {
            Log.d(TAG, "Stok Interstisial Reward kosong, otomatis bypass.");
            moveAsMapel(kategori);
        }
    }

    // ==================== 6. PROFIL & LOGIKA SISTEM UTAMA ====================
    private void loadLocalProfile() {
        SharedPreferences sharedPref = getSharedPreferences("USER_DATA", Context.MODE_PRIVATE);
        String namaLokal = sharedPref.getString("nama_user", "");

        if (!namaLokal.isEmpty()) {
            binding.tvUsernameHome.setText(namaLokal);
        }
    }

    private void syncUserData() {
        if (mDatabase == null) return;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nama = snapshot.child("username").getValue(String.class);
                    Log.d(TAG, "Firebase merespons. Username didapatkan: " + nama);

                    if (nama != null && !nama.isEmpty()) {
                        // 1. Perbarui teks antarmuka secara realtime
                        binding.tvUsernameHome.setText(nama);

                        // 2. Tulis data yang valid ke SharedPreferences lokal
                        getSharedPreferences("USER_DATA", Context.MODE_PRIVATE)
                                .edit()
                                .putString("nama_user", nama)
                                .apply();
                    } else {
                        binding.tvUsernameHome.setText("User PintarPedia");
                        Log.w(TAG, "Key 'username' kosong atau tidak ditemukan pada Firebase.");
                    }
                } else {
                    binding.tvUsernameHome.setText("Data Baru");
                    Log.w(TAG, "Node UID ini belum terbuat atau kosong di database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Gagal sinkronisasi data Firebase: " + error.getMessage());
            }
        });
    }

    private void setupClickListeners() {
        if (binding.cardUtbk2022 != null) binding.cardUtbk2022.setOnClickListener(v -> checkAdBeforeMove("UTBK 2022"));
        if (binding.cardUtbk2023 != null) binding.cardUtbk2023.setOnClickListener(v -> checkAdBeforeMove("UTBK 2023"));

        if (binding.cardUtbk2024 != null) binding.cardUtbk2024.setOnClickListener(v -> checkRewardedInterstitialBeforeMove("UTBK 2024"));
        if (binding.cardUtbk2025 != null) binding.cardUtbk2025.setOnClickListener(v -> checkRewardedInterstitialBeforeMove("UTBK 2025"));

        if (binding.cardPrediksi2026 != null) binding.cardPrediksi2026.setOnClickListener(v -> checkRewardBeforeMove("Prediksi 2026"));
        if (binding.cardBankSoal != null) binding.cardBankSoal.setOnClickListener(v -> checkRewardBeforeMove("Bank Soal"));

        if (binding.ivProfileHome != null) {
            binding.ivProfileHome.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void moveAsMapel(String kategori) {
        Intent intent = new Intent(HomeActivity.this, MapelActivity.class);
        intent.putExtra("KATEGORI_MENU", kategori);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        destroyBanner();

        if (mNativeAd != null) {
            mNativeAd.destroy();
            Log.d(TAG, "Resource iklan native berhasil dilepaskan.");
        }
        super.onDestroy();
    }

    public void destroyBanner() {
        if (adView != null) {
            View parentView = (View) adView.getParent();
            if (parentView instanceof ViewGroup) {
                ((ViewGroup) parentView).removeView(adView);
            }
            adView.destroy();
            Log.d(TAG, "Resource iklan banner berhasil dilepaskan.");
        }
        adView = null;
    }
}