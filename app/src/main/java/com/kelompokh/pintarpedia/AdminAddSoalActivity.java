package com.kelompokh.pintarpedia;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Objects;

public class AdminAddSoalActivity extends AppCompatActivity {

    private TextInputEditText etPertanyaan, etOpsiA, etOpsiB, etOpsiC, etOpsiD, etOpsiE, etBulkSoal;
    private android.widget.Spinner spinnerJawaban, spinnerKategori;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_soal);

        // 1. Inisialisasi Firebase Reference ke node "soal_utbk"
        mDatabase = FirebaseDatabase.getInstance().getReference("soal_utbk");

        // 2. Inisialisasi Views
        etPertanyaan = findViewById(R.id.etPertanyaan);
        etOpsiA = findViewById(R.id.etOpsiA);
        etOpsiB = findViewById(R.id.etOpsiB);
        etOpsiC = findViewById(R.id.etOpsiC);
        etOpsiD = findViewById(R.id.etOpsiD);
        etOpsiE = findViewById(R.id.etOpsiE);
        etBulkSoal = findViewById(R.id.etBulkSoal);

        spinnerJawaban = findViewById(R.id.spinnerJawaban);
        spinnerKategori = findViewById(R.id.spinnerKategori);

        MaterialButton btnUpload = findViewById(R.id.btnUploadSoal);
        MaterialButton btnUploadBulk = findViewById(R.id.btnUploadBulk);

        // 3. Setup Spinner Kategori (Sesuaikan dengan menu di Home User)
        String[] daftarKategori = {"UTBK 2022", "UTBK 2023", "UTBK 2024", "UTBK 2025", "Prediksi 2026", "Bank Soal"};
        ArrayAdapter<String> adapterKat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, daftarKategori);
        spinnerKategori.setAdapter(adapterKat);

        // 4. Setup Spinner Jawaban
        String[] pilihanJawaban = {"A", "B", "C", "D", "E"};
        ArrayAdapter<String> adapterJaw = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pilihanJawaban);
        spinnerJawaban.setAdapter(adapterJaw);

        // 5. Listener Tombol Upload Satuan
        btnUpload.setOnClickListener(v -> prosesUploadSoalSatuan());

        // 6. Listener Tombol Upload Bulk
        btnUploadBulk.setOnClickListener(v -> prosesUploadSoalBulk());
    }

    private void prosesUploadSoalSatuan() {
        String pertanyaan = Objects.requireNonNull(etPertanyaan.getText()).toString().trim();
        String a = Objects.requireNonNull(etOpsiA.getText()).toString().trim();
        String b = Objects.requireNonNull(etOpsiB.getText()).toString().trim();
        String c = Objects.requireNonNull(etOpsiC.getText()).toString().trim();
        String d = Objects.requireNonNull(etOpsiD.getText()).toString().trim();
        String e = Objects.requireNonNull(etOpsiE.getText()).toString().trim();
        String kunci = spinnerJawaban.getSelectedItem().toString();
        String kategori = spinnerKategori.getSelectedItem().toString();

        if (pertanyaan.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || e.isEmpty()) {
            Toast.makeText(this, "Harap isi semua field pertanyaan dan opsi!", Toast.LENGTH_SHORT).show();
            return;
        }

        simpanKeFirebase(pertanyaan, a, b, c, d, e, kunci, kategori, true);
    }

    private void prosesUploadSoalBulk() {
        String dataMentah = Objects.requireNonNull(etBulkSoal.getText()).toString().trim();
        String kategori = spinnerKategori.getSelectedItem().toString();

        if (dataMentah.isEmpty()) {
            Toast.makeText(this, "Teks bulk kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] kumpulanSoal = dataMentah.split("#");
        int suksesCount = 0;

        for (String soalBaris : kumpulanSoal) {
            String[] part = soalBaris.split("\\|");
            if (part.length == 7) {
                simpanKeFirebase(
                        part[0].trim(),
                        part[1].trim(),
                        part[2].trim(),
                        part[3].trim(),
                        part[4].trim(),
                        part[5].trim(),
                        part[6].trim().toUpperCase(),
                        kategori,
                        false // Jangan tampilkan toast per soal agar tidak mengganggu
                );
                suksesCount++;
            }
        }

        if (suksesCount > 0) {
            Toast.makeText(this, suksesCount + " Soal berhasil diunggah ke " + kategori, Toast.LENGTH_LONG).show();
            etBulkSoal.setText("");
        } else {
            Toast.makeText(this, "Format salah! Gunakan: Pertanyaan|A|B|C|D|E|Kunci", Toast.LENGTH_LONG).show();
        }
    }

    private void simpanKeFirebase(String q, String a, String b, String c, String d, String e, String kunci, String kat, boolean showToast) {
        // Ambil ID Unik dari Firebase
        String idSoal = mDatabase.push().getKey();

        // Menggunakan HashMap agar struktur JSON konsisten
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
                            resetFormSatuan();
                        }
                    })
                    .addOnFailureListener(err -> Toast.makeText(AdminAddSoalActivity.this, "Gagal: " + err.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void resetFormSatuan() {
        etPertanyaan.setText("");
        etOpsiA.setText("");
        etOpsiB.setText("");
        etOpsiC.setText("");
        etOpsiD.setText("");
        etOpsiE.setText("");
        etPertanyaan.requestFocus();
    }
}