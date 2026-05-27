package com.example.interfaz.service;

import com.example.interfaz.model.Song;
import javafx.concurrent.Task;

public interface DownloadService {

    Task<Void> downloadSong(String url, String outputPath);

    boolean canHandle(String url);

    Song getSongInfo(String url);

    void cancelAllDownloads();

    boolean hasActiveDownloads();

    void pauseDownload();

    void resumeDownload();

    void stopDownload();
}
