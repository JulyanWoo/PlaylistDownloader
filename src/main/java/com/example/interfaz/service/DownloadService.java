package com.example.interfaz.service;

import java.util.concurrent.CompletableFuture;

import com.example.interfaz.model.Song;

public interface DownloadService extends AutoCloseable {

    /**
     * Starts an asynchronous download of a track or playlist URL.
     *
     * @param url target media URL
     * @param outputPath target directory or empty for default
     * @return CompletableFuture completing with true on success or completing
     * exceptionally on error
     */
    CompletableFuture<Boolean> downloadSong(String url, String outputPath);

    boolean canHandle(String url);

    Song getSongInfo(String url);

    void pauseDownload();

    void resumeDownload();

    void stopDownload();

    @Override
    void close();
}
