package com.kelompokh.pintarpedia;

public class RiwayatKuisModel {
    private String subject;
    private long timestamp;
    private int score;
    private int correct;
    private int total;

    public RiwayatKuisModel() {
    }

    public RiwayatKuisModel(String subject, long timestamp, int score, int correct, int total) {
        this.subject = subject;
        this.timestamp = timestamp;
        this.score = score;
        this.correct = correct;
        this.total = total;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
