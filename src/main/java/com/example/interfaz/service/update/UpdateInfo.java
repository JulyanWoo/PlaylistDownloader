package com.example.interfaz.service.update;

import java.time.LocalDateTime;

/**
 * Encapsulates version status, update availability, and download URL for yt-dlp.
 */
public class UpdateInfo {

    private final String currentVersion;
    private final String latestVersion;
    private final boolean updateAvailable;
    private final String downloadUrl;
    private final LocalDateTime lastChecked;

    public UpdateInfo(String currentVersion, String latestVersion, boolean updateAvailable, String downloadUrl) {
        this.currentVersion = currentVersion != null ? currentVersion : "Desconocida";
        this.latestVersion = latestVersion != null ? latestVersion : "Desconocida";
        this.updateAvailable = updateAvailable;
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.lastChecked = LocalDateTime.now();
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
}
