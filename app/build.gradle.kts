plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kelompokh.pintarpedia"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kelompokh.pintarpedia"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Firebase (Menggunakan BoM versi 33.0.0 yang sudah Anda pasang)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // --- TAMBAHKAN LIBRARY STORAGE DI SINI ---
    implementation("com.google.firebase:firebase-storage")

    // --- TAMBAHKAN LIBRARY GLIDE (UNTUK LOADING GAMBAR) ---
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    // ------------------------------------------------------

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    // UI Dasar
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}