package com.example.interfaz.model.analyzer;

import java.util.ArrayList;
import java.util.List;

public class DuplicateCandidate {

    private final SongFile songFile;
    private boolean selectedForDeletion;
    private boolean original;
    private double similarityScore;
    private int originalScore;
    private List<String> scoreReasons;
    private String detectedLanguage;
    private double languageConfidence;
    private SimilarityBreakdown similarityBreakdown;

    public DuplicateCandidate(SongFile songFile) {
        this(songFile, false, false, 1.0);
    }

    public DuplicateCandidate(SongFile songFile, boolean selectedForDeletion, boolean original) {
        this(songFile, selectedForDeletion, original, 1.0);
    }

    public DuplicateCandidate(SongFile songFile, boolean selectedForDeletion, boolean original, double similarityScore) {
        this.songFile = songFile;
        this.selectedForDeletion = selectedForDeletion;
        this.original = original;
        this.similarityScore = similarityScore;
        this.originalScore = 0;
        this.scoreReasons = new ArrayList<>();
        this.detectedLanguage = "Desconocido";
        this.languageConfidence = 0.0;
    }

    public SongFile getSongFile() {
        return songFile;
    }

    public boolean isSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(boolean selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
        if (original) {
            this.selectedForDeletion = false;
        }
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public int getSimilarityPercentage() {
        return (int) Math.round(similarityScore * 100);
    }

    public int getOriginalScore() {
        return originalScore;
    }

    public void setOriginalScore(int originalScore) {
        this.originalScore = originalScore;
    }

    public List<String> getScoreReasons() {
        return scoreReasons;
    }

    public void setScoreReasons(List<String> scoreReasons) {
        this.scoreReasons = scoreReasons != null ? scoreReasons : new ArrayList<>();
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        this.detectedLanguage = detectedLanguage != null ? detectedLanguage : "Desconocido";
    }

    public double getLanguageConfidence() {
        return languageConfidence;
    }

    public void setLanguageConfidence(double languageConfidence) {
        this.languageConfidence = languageConfidence;
    }

    public SimilarityBreakdown getSimilarityBreakdown() {
        return similarityBreakdown;
    }

    public void setSimilarityBreakdown(SimilarityBreakdown similarityBreakdown) {
        this.similarityBreakdown = similarityBreakdown;
    }
}
