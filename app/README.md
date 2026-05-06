git clone https://github.com/iamfrmnsyh/PintarPedia.git
```
2.  **Buka di Android Studio**:
    Pilih `Open an Existing Project` dan arahkan ke folder hasil clone.
3.  **Konfigurasi Firebase**:
    *   Tambahkan file `google-services.json` ke dalam direktori `app/`.
    *   Pastikan SHA-1 fingerprint sudah terdaftar di Firebase Console.
4.  **Build Project**:
    Tunggu proses Gradle sync selesai, lalu jalankan aplikasi pada emulator atau perangkat fisik.

---

## 📄 Lisensi

Proyek ini dikembangkan untuk tujuan akademik dan pengembangan portofolio. Seluruh kode sumber di bawah repositori ini dapat dipelajari danBerikut adalah draf file `README.md` yang terstruktur dan profesional untuk proyek **PinterPedia**. File ini disusun untuk memberikan gambaran jelas mengenai arsitektur aplikasi, teknologi yang digunakan, serta panduan navigasi bagi pengembang lain atau penguji.

---

# PinterPedia - Mobile Education Quiz Application

**PinterPedia** adalah aplikasi edukasi berbasis Android yang dirancang untuk memfasilitasi kuis interaktif bagi siswa. Aplikasi ini mengintegrasikan manajemen pengguna, navigasi yang kompleks, dan sinkronisasi data secara real-time.

## 📁 Struktur Proyek

Proyek ini mengikuti pola arsitektur Android standar dengan pemisahan peran yang jelas antara UI, logika bisnis, dan data.
```text
app/src/main/java/com/example/pinterpedia/
├── activity/            # Mengelola siklus hidup layar (Login, Register, Dashboard)
├── adapter/             # Bridge antara kumpulan data dan UI (RecyclerView adapters)
├── fragment/            # Komponen UI modular untuk navigasi (Home, Profile, Quiz)
├── model/               # Data Classes (User, Quiz, Score, Category)
├── helper/              # Kelas utilitas (FirebaseHelper, InputValidator)
└── config/              # Konfigurasi aplikasi dan konstanta