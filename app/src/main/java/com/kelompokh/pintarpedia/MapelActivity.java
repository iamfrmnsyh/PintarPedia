package com.kelompokh.pintarpedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MapelActivity extends AppCompatActivity {
    private TextView tvQuestion, tvTitle, tvProgressText;
    private MaterialButton btn1, btn2, btn3, btn4, btn5;
    private ProgressBar progressBar;

    private List<SoalModel> questionList;
    private int questionCounter = 0;
    private int totalQuestions;
    private SoalModel currentQuestion;

    private int scoreBenar = 0;
    private int scoreSalah = 0;
    private String subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_detail);

        // 1. Inisialisasi View
        tvQuestion = findViewById(R.id.tvQuestion);
        tvTitle = findViewById(R.id.tvSubjectTitle);
        tvProgressText = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.quizProgressBar);

        btn1 = findViewById(R.id.btnOpt1);
        btn2 = findViewById(R.id.btnOpt2);
        btn3 = findViewById(R.id.btnOpt3);
        btn4 = findViewById(R.id.btnOpt4);
        btn5 = findViewById(R.id.btnOpt5);

        // 2. Ambil kategori dari Intent
        subject = getIntent().getStringExtra("KATEGORI_MENU");
        if (subject == null) subject = "Bank Soal";
        tvTitle.setText(subject);

        questionList = new ArrayList<>();

        // 3. Load Data Soal
        loadQuestionsFromFirebase();

        // 4. Listener Klik (Sistem Rahasia)
        btn1.setOnClickListener(v -> handleAnswer("A", btn1));
        btn2.setOnClickListener(v -> handleAnswer("B", btn2));
        btn3.setOnClickListener(v -> handleAnswer("C", btn3));
        btn4.setOnClickListener(v -> handleAnswer("D", btn4));
        btn5.setOnClickListener(v -> handleAnswer("E", btn5));
    }

    private void loadQuestionsFromFirebase() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("soal_utbk");
        Query query;

        if (subject.equals("Bank Soal")) {
            query = mDatabase;
        } else {
            query = mDatabase.orderByChild("kategori").equalTo(subject);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                questionList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    SoalModel soal = data.getValue(SoalModel.class);
                    if (soal != null) questionList.add(soal);
                }

                if (!questionList.isEmpty()) {
                    Collections.shuffle(questionList);
                    totalQuestions = questionList.size();
                    progressBar.setMax(totalQuestions);
                    showNextQuestion();
                } else {
                    Toast.makeText(MapelActivity.this, "Belum ada soal tersedia", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapelActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleAnswer(String selectedOption, MaterialButton tappedButton) {
        // Kunci tombol agar tidak klik ganda
        setButtonsEnabled(false);

        // --- LOGIKA RAHASIA: Hitung skor di background tanpa warna Merah/Hijau ---
        if (currentQuestion != null && Objects.equals(selectedOption, currentQuestion.getJawabanBenar())) {
            scoreBenar++;
        } else {
            scoreSalah++;
        }

        // Beri tanda visual NETRAL (Biru Muda) bahwa tombol sudah dipilih
        tappedButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BBDEFB"))); // Light Blue
        tappedButton.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1976D2"))); // Darker Blue Stroke

        // Pindah soal setelah delay singkat (500ms)
        new Handler().postDelayed(() -> {
            resetButtonStyles();
            showNextQuestion();
            setButtonsEnabled(true);
        }, 500);
    }

    @SuppressLint("SetTextI18n")
    private void showNextQuestion() {
        if (questionCounter < totalQuestions) {
            currentQuestion = questionList.get(questionCounter);

            tvQuestion.setText(currentQuestion.getPertanyaan());
            btn1.setText("A. " + currentQuestion.getOpsiA());
            btn2.setText("B. " + currentQuestion.getOpsiB());
            btn3.setText("C. " + currentQuestion.getOpsiC());
            btn4.setText("D. " + currentQuestion.getOpsiD());
            btn5.setText("E. " + currentQuestion.getOpsiE());

            questionCounter++;
            tvProgressText.setText(questionCounter + "/" + totalQuestions);
            progressBar.setProgress(questionCounter);
        } else {
            // Selesai kuis, kirim data ke ResultActivity
            Intent intent = new Intent(MapelActivity.this, ResultActivity.class);
            intent.putExtra("SCORE_BENAR", scoreBenar);
            intent.putExtra("SCORE_SALAH", scoreSalah);
            intent.putExtra("TOTAL_SOAL", totalQuestions);
            intent.putExtra("SUBJECT", subject);
            startActivity(intent);
            finish();
        }
    }

    private void resetButtonStyles() {
        // Kembalikan ke warna default (Putih dengan stroke abu-abu)
        ColorStateList white = ColorStateList.valueOf(Color.WHITE);
        ColorStateList strokeGray = ColorStateList.valueOf(Color.parseColor("#D1D1D1"));

        MaterialButton[] buttons = {btn1, btn2, btn3, btn4, btn5};
        for (MaterialButton btn : buttons) {
            btn.setBackgroundTintList(white);
            btn.setStrokeColor(strokeGray);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btn1.setEnabled(enabled);
        btn2.setEnabled(enabled);
        btn3.setEnabled(enabled);
        btn4.setEnabled(enabled);
        btn5.setEnabled(enabled);
    }
}