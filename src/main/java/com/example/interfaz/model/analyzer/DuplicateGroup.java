package com.example.interfaz.model.analyzer;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGroup {

    private final String groupName;
    private final List<DuplicateCandidate> candidates;
    private GroupClassification classification;

    public DuplicateGroup(String groupName, List<DuplicateCandidate> candidates, GroupClassification classification) {
        this.groupName = groupName;
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        this.classification = classification;
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
}
