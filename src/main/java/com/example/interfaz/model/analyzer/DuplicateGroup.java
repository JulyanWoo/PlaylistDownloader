package com.example.interfaz.model.analyzer;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGroup {

    private final String groupName;
    private final List<DuplicateCandidate> candidates;
    private GroupClassification classification;
    private double confidence;

    public DuplicateGroup(String groupName, List<DuplicateCandidate> candidates, GroupClassification classification) {
        this(groupName, candidates, classification, 1.0);
    }

    public DuplicateGroup(String groupName, List<DuplicateCandidate> candidates, GroupClassification classification, double confidence) {
        this.groupName = groupName;
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        this.classification = classification;
        this.confidence = confidence;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<DuplicateCandidate> getCandidates() {
        return candidates;
    }

    public GroupClassification getClassification() {
        return classification;
    }

    public void setClassification(GroupClassification classification) {
        this.classification = classification;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getConfidencePercentage() {
        return (int) Math.round(confidence * 100);
    }
}
