package com.kelompokh.pintarpedia;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private static final String TAG = "MyApplication";
    private AppOpenAd appOpenAd = null;
    private boolean isShowingAd = false;
    private long loadTime = 0;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);

        // Mendaftarkan observer untuk mendeteksi kapan aplikasi masuk ke foreground
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    /** Metode untuk memuat iklan di background */
    public void fetchAd() {
        if (isAdAvailable()) {
            return;
        }

        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                appOpenAd = ad;
                loadTime = (new Date()).getTime();
                Log.d(TAG, "App Open Ad BERHASIL dimuat di background.");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "App Open Ad GAGAL dimuat di background: " + loadAdError.getMessage());
            }
        };

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(
                this,
                getString(R.string.app_open_ad_unit_id),
                request,
                loadCallback
        );
    }

    /** Memeriksa apakah iklan tersedia dan belum kedaluwarsa (4 jam) */
    private boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /** Menampilkan iklan jika tersedia */
    public void showAdIfAvailable(@NonNull Activity activity) {
        // Jangan tampilkan iklan jika sedang tayang atau tidak ada stok iklan
        if (!isShowingAd && isAdAvailable()) {
            Log.d(TAG, "Menampilkan App Open Ad...");

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Bersihkan objek iklan lama setelah ditutup dan muat yang baru
                    appOpenAd = null;
                    isShowingAd = false;
                    fetchAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;
                    fetchAd();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                }
            });

            appOpenAd.show(activity);
        } else {
            Log.d(TAG, "Iklan tidak tersedia atau sedang tayang, melakukan fetch ulang.");
            fetchAd();
        }
    }

    // ==================== LIFECYCLE OBSERVER (DETEKSI FOREGROUND) ====================
    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);

        // Dipicu otomatis saat aplikasi dibuka kembali dari background (minimize -> resume)
        if (currentActivity != null) {

            // MODIFIKASI PENTING: Jangan tampilkan iklan otomatis jika user sedang berada di gerbang autentikasi awal.
            // Biarkan SplashActivity, LoginActivity, atau RegisterActivity mengontrol iklannya sendiri.
            if (currentActivity instanceof SplashActivity ||
                    currentActivity instanceof LoginActivity ||
                    currentActivity instanceof RegisterActivity) {

                Log.d(TAG, "onStart: Melompati App Open Ad karena berada di layar autentikasi/splash.");
                return;
            }

            // Tampilkan iklan jika aplikasi di-minimize lalu dibuka kembali saat di HomeActivity, MapelActivity, dll.
            showAdIfAvailable(currentActivity);
        }
    }

    // ==================== ACTIVITY LIFECYCLE CALLBACKS ====================
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // Update activity yang sedang aktif di layar pengguna
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (!isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}