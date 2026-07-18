# Pintar Pedia - Solusi Belajar Efektif & Efisien

**Pintar Pedia** adalah aplikasi mobile Android yang dirancang khusus sebagai platform simulasi ujian UTBK dan latihan soal interaktif. Aplikasi ini menyediakan ekosistem lengkap bagi siswa (User) untuk mengasah kemampuan dan bagi pengelola (Admin) untuk manajemen bank soal secara real-time.

---

## 🚀 Fitur Utama

### 1. Sisi Pengguna (User)
- **Simulasi UTBK Realistis**: Paket soal terorganisir berdasarkan tahun (UTBK 2022 - 2025) dan Prediksi 2026.
- **Bank Soal Latihan**: Kumpulan soal umum untuk latihan harian.
- **Dukungan Soal Bergambar**: Menampilkan gambar pendukung soal secara fleksibel (Portrait/Landscape) untuk meningkatkan pemahaman konten.
- **Visualisasi Interaktif**: Penyesuaian tata letak (layout) otomatis antara gambar dan teks pertanyaan agar tetap rapi.
- **Hasil & Riwayat**: Skor instan setelah pengerjaan kuis dan pencatatan riwayat kuis di profil.
- **Manajemen Akun**: Fitur profil, ubah password, dan pembaruan informasi akun.
- **Integrasi Iklan**: Pengalaman premium dengan dukungan format iklan AdMob yang optimal.

### 2. Sisi Pengelola (Admin)
- **Dashboard Admin**: Ringkasan statistik dan akses cepat ke kontrol aplikasi.
- **Kelola Soal (Advanced)**:
    - **Upload Satuan**: Input soal satu per satu lengkap dengan unggahan gambar langsung dari galeri.
    - **Upload Masal (Bulk)**: Mendukung pengunggahan banyak soal sekaligus melalui paste teks atau file `.txt`.
    - **Deteksi Orientasi**: Gambar soal otomatis diperbaiki (rotasi) berdasarkan data EXIF agar tidak miring.
- **Manajemen Pengguna**: Admin dapat mempromosikan user menjadi admin, menonaktifkan akun, atau menghapus data user.
- **Kategori Dinamis**: Menambah kategori/tahun kuis baru yang langsung tersinkronisasi ke seluruh aplikasi user.

---

## 🛠️ Teknologi yang Digunakan

- **Platform**: Android Studio (Java)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 35 (Android 15)
- **Backend**: Firebase Realtime Database
- **Storage**: Firebase Storage (untuk gambar soal)
- **Iklan**: Google AdMob SDK
- **Library Pihak Ketiga**:
    - **Glide**: Untuk pemuatan gambar yang cepat dan efisien.
    - **Material Components**: Untuk UI/UX yang modern dan profesional.

---

## 📋 Format Input Soal Masal (.txt)

Agar soal dapat terbaca secara otomatis oleh sistem, gunakan format berikut dalam file `.txt`:

```text
SOAL
Apa ibu kota negara Indonesia?
A. Jakarta
B. Bandung
C. Surabaya
D. Medan
E. Yogyakarta
Jawaban: A

SOAL
[Pertanyaan Soal Berikutnya]
A. [Opsi A]
B. [Opsi B]
...
Jawaban: [Kunci]
```

---

## 🔧 Instalasi untuk Pengembang

1. **Clone Repositori**:
   ```bash
   git clone https://github.com/iamfrmnsyh/PintarPedia.git
   ```
2. **Konfigurasi Firebase**:
   - Masukkan file `google-services.json` ke folder `app/`.
   - Pastikan Firebase Realtime Database dan Storage sudah aktif di console Firebase.
3. **Build Project**:
   - Buka di Android Studio.
   - Sync Gradle.
   - Jalankan di Emulator atau Perangkat Fisik.

---

## 👤 Akun & Kontribusi
Dikembangkan oleh **Vertex Alpha** (Kelompok H).
Project Maintainer: [iamfrmnsyh](https://github.com/iamfrmnsyh)

---
© 2026 Pintar Pedia. All Rights Reserved.
