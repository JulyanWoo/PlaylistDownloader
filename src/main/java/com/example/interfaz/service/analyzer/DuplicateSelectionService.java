package com.example.interfaz.service.analyzer;

import java.util.ArrayList;
import java.util.List;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;
import com.example.interfaz.model.analyzer.GroupClassification;
import com.example.interfaz.util.FormatUtils;

public class DuplicateSelectionService {

    public record SelectionSummary(int selectedCount, long bytesToFree) {

        public String getFormattedSummary() {
            return String.format("%d duplicados seleccionados (%s a liberar)", selectedCount, FormatUtils.formatSize(bytesToFree));
        }
    }

    public void keepOriginals(List<DuplicateGroup> groups) {
        selectConfirmedDuplicatesOnly(groups);
    }

    public void selectConfirmedDuplicatesOnly(List<DuplicateGroup> groups) {
        if (groups == null) {
            return;
        }
        for (DuplicateGroup group : groups) {
            boolean isConfirmed = (group.getClassification() == GroupClassification.CONFIRMED_DUPLICATE);
            for (DuplicateCandidate c : group.getCandidates()) {
                if (isConfirmed) {
                    c.setSelectedForDeletion(!c.isOriginal());
                } else {
                    c.setSelectedForDeletion(false);
                }
            }
        }
    }

    public void selectDuplicates(List<DuplicateGroup> groups) {
        if (groups == null) {
            return;
        }
        for (DuplicateGroup group : groups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                c.setSelectedForDeletion(!c.isOriginal());
            }
        }
    }

    public void unselectAll(List<DuplicateGroup> groups) {
        if (groups == null) {
            return;
        }
        for (DuplicateGroup group : groups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                c.setSelectedForDeletion(false);
            }
        }
    }

    public List<DuplicateCandidate> getSelectedCandidates(List<DuplicateGroup> groups) {
        List<DuplicateCandidate> selected = new ArrayList<>();
        if (groups == null) {
            return selected;
        }

        for (DuplicateGroup group : groups) {
            for (DuplicateCandidate c : group.getCandidates()) {
                if (c.isSelectedForDeletion()) {
                    selected.add(c);
                }
            }
        }
        return selected;
    }

    public SelectionSummary calculateSelectionSummary(List<DuplicateGroup> groups) {
        int count = 0;
        long bytes = 0;
        if (groups != null) {
            for (DuplicateGroup group : groups) {
                for (DuplicateCandidate candidate : group.getCandidates()) {
                    if (candidate.isSelectedForDeletion()) {
                        count++;
                        bytes += candidate.getSongFile().getSize();
                    }
                }
            }
        }
        return new SelectionSummary(count, bytes);
    }
}
