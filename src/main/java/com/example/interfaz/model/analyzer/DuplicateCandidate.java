package com.example.interfaz.model.analyzer;

public class DuplicateCandidate {

    private final SongFile songFile;
    private boolean selectedForDeletion;
    private boolean original;

    public DuplicateCandidate(SongFile songFile) {
        this.songFile = songFile;
        this.selectedForDeletion = false;
        this.original = false;
    }

    public DuplicateCandidate(SongFile songFile, boolean selectedForDeletion, boolean original) {
        this.songFile = songFile;
        this.selectedForDeletion = selectedForDeletion;
        this.original = original;
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
}
