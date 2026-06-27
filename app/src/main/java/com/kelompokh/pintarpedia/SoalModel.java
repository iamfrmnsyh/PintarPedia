package com.kelompokh.pintarpedia;

public class SoalModel {
    // Atribut data soal
    private String idSoal;
    private String pertanyaan;
    private String opsiA;
    private String opsiB;
    private String opsiC;
    private String opsiD;
    private String opsiE;
    private String jawabanBenar;
    private String kategori; 
    private String urlGambar;

    // 1. Constructor Kosong
    public SoalModel() {
    }

    // 2. Constructor Lengkap
    public SoalModel(String idSoal, String pertanyaan, String opsiA, String opsiB,
                     String opsiC, String opsiD, String opsiE,
                     String jawabanBenar, String kategori, String urlGambar) {
        this.idSoal = idSoal;
        this.pertanyaan = pertanyaan;
        this.opsiA = opsiA;
        this.opsiB = opsiB;
        this.opsiC = opsiC;
        this.opsiD = opsiD;
        this.opsiE = opsiE;
        this.jawabanBenar = jawabanBenar;
        this.kategori = kategori;
        this.urlGambar = urlGambar;
    }

    // 3. Getter dan Setter (Standar profesional Java)
    public String getIdSoal() { return idSoal; }
    public void setIdSoal(String idSoal) { this.idSoal = idSoal; }

    public String getPertanyaan() { return pertanyaan; }
    public void setPertanyaan(String pertanyaan) { this.pertanyaan = pertanyaan; }

    public String getOpsiA() { return opsiA; }
    public void setOpsiA(String opsiA) { this.opsiA = opsiA; }

    public String getOpsiB() { return opsiB; }
    public void setOpsiB(String opsiB) { this.opsiB = opsiB; }

    public String getOpsiC() { return opsiC; }
    public void setOpsiC(String opsiC) { this.opsiC = opsiC; }

    public String getOpsiD() { return opsiD; }
    public void setOpsiD(String opsiD) { this.opsiD = opsiD; }

    public String getOpsiE() { return opsiE; }
    public void setOpsiE(String opsiE) { this.opsiE = opsiE; }

    public String getJawabanBenar() { return jawabanBenar; }
    public void setJawabanBenar(String jawabanBenar) { this.jawabanBenar = jawabanBenar; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getUrlGambar() { return urlGambar; }
    public void setUrlGambar(String urlGambar) { this.urlGambar = urlGambar; }
}