package com.kelompokh.pintarpedia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class AdminAddSoalActivity extends AppCompatActivity {

    private TextInputEditText etBulkSoal;
    private android.widget.Spinner spinnerKategori;

    // SINGLE QUESTION VIEWS
    private ImageView ivPreviewSoal;
    private TextInputEditText etSinglePertanyaan, etOpsiA, etOpsiB, etOpsiC, etOpsiD, etOpsiE, etKunciJawaban;
    private Uri imageUri = null;

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

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ivPreviewSoal.setImageURI(imageUri);
                        ivPreviewSoal.setVisibility(View.VISIBLE);
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

        ivPreviewSoal = findViewById(R.id.ivPreviewSoal);
        etSinglePertanyaan = findViewById(R.id.etSinglePertanyaan);
        etOpsiA = findViewById(R.id.etOpsiA);
        etOpsiB = findViewById(R.id.etOpsiB);
        etOpsiC = findViewById(R.id.etOpsiC);
        etOpsiD = findViewById(R.id.etOpsiD);
        etOpsiE = findViewById(R.id.etOpsiE);
        etKunciJawaban = findViewById(R.id.etKunciJawaban);

        MaterialButton btnPilihGambar = findViewById(R.id.btnPilihGambar);
        MaterialButton btnSimpanSingleSoal = findViewById(R.id.btnSimpanSingleSoal);
        MaterialButton btnUploadBulk = findViewById(R.id.btnUploadBulk);
        MaterialButton btnChooseFile = findViewById(R.id.btnChooseFile);

        // 3. Setup Spinner Kategori
        String[] daftarKategori = {"UTBK 2022", "UTBK 2023", "UTBK 2024", "UTBK 2025", "Prediksi 2026", "Bank Soal"};
        ArrayAdapter<String> adapterKat = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, daftarKategori);
        spinnerKategori.setAdapter(adapterKat);

        // 4. Listeners
        if (btnPilihGambar != null) btnPilihGambar.setOnClickListener(v -> openImagePicker());
        if (btnSimpanSingleSoal != null) btnSimpanSingleSoal.setOnClickListener(v -> validasiDanSimpanSoalSatuan());

        if (btnUploadBulk != null) {
            btnUploadBulk.setOnClickListener(v -> {
                String bulkText = etBulkSoal.getText() != null ? etBulkSoal.getText().toString() : "";
                prosesUploadSoalBulk(bulkText);
            });
        }

        if (btnChooseFile != null) {
            btnChooseFile.setOnClickListener(v -> openFilePicker());
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void validasiDanSimpanSoalSatuan() {
        String q = etSinglePertanyaan.getText().toString().trim();
        String a = etOpsiA.getText().toString().trim();
        String b = etOpsiB.getText().toString().trim();
        String c = etOpsiC.getText().toString().trim();
        String d = etOpsiD.getText().toString().trim();
        String e = etOpsiE.getText().toString().trim();
        String kunci = etKunciJawaban.getText().toString().trim().toUpperCase();
        String kategori = spinnerKategori.getSelectedItem().toString();

        if (q.isEmpty() || a.isEmpty() || b.isEmpty() || kunci.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi pertanyaan, minimal 2 opsi, dan kunci!", Toast.LENGTH_SHORT).show();
            return;
        }

        String strGambar = "-";
        if (imageUri != null) {
            strGambar = konversiUriKeBase64(imageUri);
            if (strGambar == null) return;
        }

        simpanKeFirebase(q, a, b, c, d, e, kunci, kategori, strGambar, true);
        resetFormSatuan();
    }

    private String konversiUriKeBase64(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) return null;

            // PERBAIKAN: Tangani rotasi otomatis berdasarkan data EXIF gambar
            bitmap = rotateBitmapIfRequired(bitmap, uri);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal proses gambar", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Bitmap rotateBitmapIfRequired(Bitmap img, Uri selectedImage) {
        try {
            InputStream input = getContentResolver().openInputStream(selectedImage);
            ExifInterface ei;
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                ei = new ExifInterface(input);
            } else {
                ei = new ExifInterface(selectedImage.getPath());
            }

            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (Exception e) {
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private void resetFormSatuan() {
        etSinglePertanyaan.setText("");
        etOpsiA.setText(""); etOpsiB.setText(""); etOpsiC.setText("");
        etOpsiD.setText(""); etOpsiE.setText("");
        etKunciJawaban.setText("");
        imageUri = null;
        ivPreviewSoal.setVisibility(View.GONE);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih File Soal (.txt)"));
    }

    private void ekstrakTeksDokumen(Uri uri) {
        // Fokus murni pada berkas teks (.txt)
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
            Toast.makeText(this, "Gagal membaca berkas teks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void prosesUploadSoalBulk(String dataMentah) {
        String kategori = spinnerKategori.getSelectedItem().toString();

        if (dataMentah == null || dataMentah.trim().isEmpty()) {
            Toast.makeText(this, "Data soal kosong!", Toast.LENGTH_SHORT).show();
            return;
        }

        String teksNormal = dataMentah.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        String[] semuaBaris = teksNormal.split("\\n");

        int suksesCount = 0;
        String q = "", a = "", b = "", c = "", d = "", e = "", kunci = "";
        StringBuilder sbPertanyaan = new StringBuilder();

        for (String s : semuaBaris) {
            String line = s.trim();
            if (line.isEmpty()) continue;

            // Mendeteksi awal soal baru (opsional, jika format file menggunakan pemisah kata "SOAL")
            if (line.toUpperCase().startsWith("SOAL")) {
                if (!q.isEmpty() && !a.isEmpty() && !b.isEmpty() && !kunci.isEmpty()) {
                    simpanKeFirebase(q, a, b, c, d, e, kunci, kategori, "-", false);
                    suksesCount++;
                }
                q = ""; a = ""; b = ""; c = ""; d = ""; e = ""; kunci = "";
                sbPertanyaan.setLength(0);
                continue;
            }

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
            } else {
                if (sbPertanyaan.length() > 0) sbPertanyaan.append("\n");
                sbPertanyaan.append(line);
                q = sbPertanyaan.toString();
            }
        }

        // Simpan soal terakhir di dalam file
        if (!q.isEmpty() && !a.isEmpty() && !b.isEmpty() && !kunci.isEmpty()) {
            simpanKeFirebase(q, a, b, c, d, e, kunci, kategori, "-", false);
            suksesCount++;
        }

        if (suksesCount > 0) {
            Toast.makeText(this, suksesCount + " Soal berhasil diunggah!", Toast.LENGTH_LONG).show();
            etBulkSoal.setText("");
        } else {
            Toast.makeText(this, "Format berkas tidak sesuai!", Toast.LENGTH_LONG).show();
        }
    }

    private void simpanKeFirebase(String q, String a, String b, String c, String d, String e, String kunci, String kat, String urlGambar, boolean showToast) {
        String idSoal = mDatabase.push().getKey();
        SoalModel dataSoal = new SoalModel(idSoal, q, a, b, c, d, e, kunci, kat, urlGambar);

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