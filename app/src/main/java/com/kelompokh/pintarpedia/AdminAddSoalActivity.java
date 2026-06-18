package com.kelompokh.pintarpedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class AdminAddSoalActivity extends AppCompatActivity {

    private TextInputEditText etBulkSoal;
    private android.widget.Spinner spinnerKategori;
    private DatabaseReference mDatabase;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        ekstrakTeksDokumen(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_soal);

        // 1. Inisialisasi Firebase Reference ke node "soal_utbk"
        mDatabase = FirebaseDatabase.getInstance().getReference("soal_utbk");

        // 2. Inisialisasi Views
        etBulkSoal = findViewById(R.id.etBulkSoal);
        spinnerKategori = findViewById(R.id.spinnerKategori);

        MaterialButton btnUploadBulk = findViewById(R.id.btnUploadBulk);
        MaterialButton btnChooseFile = findViewById(R.id.btnChooseFile);

        // 3. Setup Spinner Kategori
        String[] daftarKategori = {"UTBK 2022", "UTBK 2023", "UTBK 2024", "UTBK 2025", "Prediksi 2026", "Bank Soal"};
        ArrayAdapter<String> adapterKat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, daftarKategori);
        spinnerKategori.setAdapter(adapterKat);

        // 4. Listener Tombol Upload Bulk
        if (btnUploadBulk != null) {
            btnUploadBulk.setOnClickListener(v -> {
                String bulkText = etBulkSoal.getText() != null ? etBulkSoal.getText().toString() : "";
                prosesUploadSoalBulk(bulkText);
            });
        }

        // 5. Listener Tombol Pilih File
        if (btnChooseFile != null) {
            btnChooseFile.setOnClickListener(v -> openFilePicker());
        }
    }

    /**
     * PERBAIKAN FILTER FILE PICKER KELOMPOK H:
     * Mengubah tipe dokumen sasaran agar berfokus pada PDF, DOC, dan DOCX
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        // Menentukan MIME Types spesifik untuk PDF dan Microsoft Word (Doc/Docx)
        String[] mimeTypes = {
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih File Soal (.pdf, .doc, .docx)"));
    }

    /**
     * Manajemen Router Ekstraksi Berkas Berdasarkan Ekstensi Dokumen
     */
    private void ekstrakTeksDokumen(Uri uri) {
        String type = getContentResolver().getType(uri);

        if (type != null) {
            if (type.equals("application/pdf")) {
                bacaFilePDF(uri);
            } else if (type.contains("msword") || type.contains("wordprocessingml")) {
                bacaFileWord(uri);
            } else {
                // Failsafe cadangan jika ada berkas teks murni yang lolos filter
                bacaFileTeksSederhana(uri);
            }
        }
    }

    private void bacaFilePDF(Uri uri) {
        // TIPS: Untuk membaca PDF secara murni tanpa error enkripsi biner,
        // Kelompok H direkomendasikan menambahkan dependency 'com.tom-roush:pdfbox-android:2.0.27.0' di build.gradle
        Toast.makeText(this, "Membaca berkas PDF...", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            // Kerangka baca stream (Gunakan PDFBox / library OCR untuk ekstraksi teks)
            if (inputStream != null) {
                // Contoh pembacaan standar (Ubah bagian ini sesuai library PDF extractor pilihan Anda)
                bacaFileTeksSederhana(uri);
                inputStream.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bacaFileWord(Uri uri) {
        // TIPS: Untuk dokumen Word (.docx), diperlukan pustaka extractor berbasis Apache POI Scratchpad
        Toast.makeText(this, "Membaca dokumen Word...", Toast.LENGTH_SHORT).show();
        bacaFileTeksSederhana(uri);
    }

    private void bacaFileTeksSederhana(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();
                inputStream.close();
                prosesUploadSoalBulk(stringBuilder.toString());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membaca struktur teks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void prosesUploadSoalBulk(String dataMentah) {
        String kategori = spinnerKategori.getSelectedItem().toString();

        if (dataMentah == null || dataMentah.trim().isEmpty()) {
            Toast.makeText(this, "Data soal kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Membagi teks dokumen per blok soal (dipisahkan baris kosong ganda)
        String[] kumpulanBlok = dataMentah.trim().split("\\n\\s*\\n");
        int suksesCount = 0;

        for (String blok : kumpulanBlok) {
            String[] baris = blok.trim().split("\\n");

            String q = "", a = "", b = "", c = "", d = "", e = "", kunci = "";
            StringBuilder sbPertanyaan = new StringBuilder();

            for (String s : baris) {
                String line = s.trim();
                if (line.isEmpty()) continue;

                if (line.toUpperCase().startsWith("A.") || line.toUpperCase().startsWith("A ")) {
                    a = line.substring(2).trim();
                } else if (line.toUpperCase().startsWith("B.") || line.toUpperCase().startsWith("B ")) {
                    b = line.substring(2).trim();
                } else if (line.toUpperCase().startsWith("C.") || line.toUpperCase().startsWith("C ")) {
                    c = line.substring(2).trim();
                } else if (line.toUpperCase().startsWith("D.") || line.toUpperCase().startsWith("D ")) {
                    d = line.substring(2).trim();
                } else if (line.toUpperCase().startsWith("E.") || line.toUpperCase().startsWith("E ")) {
                    e = line.substring(2).trim();
                } else if (line.toUpperCase().contains("JAWABAN:") || line.toUpperCase().contains("KUNCI:")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        kunci = parts[1].trim().toUpperCase();
                        if (kunci.length() > 1) kunci = kunci.substring(0, 1);
                    }
                } else if (line.length() == 1 && line.toUpperCase().matches("[ABCDE]")) {
                    kunci = line.toUpperCase();
                } else {
                    if (sbPertanyaan.length() > 0) sbPertanyaan.append("\n");
                    sbPertanyaan.append(line.replaceAll("^\\d+[\\.\\)]\\s*", ""));
                }
            }

            q = sbPertanyaan.toString();

            if (!q.isEmpty() && !a.isEmpty() && !b.isEmpty() && !c.isEmpty() && !d.isEmpty() && !e.isEmpty() && !kunci.isEmpty()) {
                simpanKeFirebase(q, a, b, c, d, e, kunci, kategori, false);
                suksesCount++;
            }
        }

        if (suksesCount > 0) {
            Toast.makeText(this, suksesCount + " Soal berhasil diunggah ke " + kategori, Toast.LENGTH_LONG).show();
            etBulkSoal.setText("");
        } else {
            Toast.makeText(this, "Format berkas tidak sesuai atau data kosong!", Toast.LENGTH_LONG).show();
        }
    }

    private void simpanKeFirebase(String q, String a, String b, String c, String d, String e, String kunci, String kat, boolean showToast) {
        String idSoal = mDatabase.push().getKey();
        HashMap<String, Object> dataSoal = new HashMap<>();
        dataSoal.put("idSoal", idSoal);
        dataSoal.put("pertanyaan", q);
        dataSoal.put("opsiA", a);
        dataSoal.put("opsiB", b);
        dataSoal.put("opsiC", c);
        dataSoal.put("opsiD", d);
        dataSoal.put("opsiE", e);
        dataSoal.put("jawabanBenar", kunci);
        dataSoal.put("kategori", kat);
        dataSoal.put("timestamp", System.currentTimeMillis());

        if (idSoal != null) {
            mDatabase.child(idSoal).setValue(dataSoal)
                    .addOnSuccessListener(unused -> {
                        if (showToast) {
                            Toast.makeText(AdminAddSoalActivity.this, "Berhasil Simpan Soal", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(err -> Toast.makeText(AdminAddSoalActivity.this, "Gagal: " + err.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}