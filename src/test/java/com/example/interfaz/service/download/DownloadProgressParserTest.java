package com.example.interfaz.service.download;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class DownloadProgressParserTest {

    @Test
    void testParseProgressPercentage() {
        DownloadProgressParser parser = new DownloadProgressParser();
        AtomicReference<Double> capturedProgress = new AtomicReference<>(0.0);

        parser.parseAndDispatch("PROGRESS:75.5", new DownloadProgressParser.ProgressListener() {
            @Override public void onOverallProgress(int current, int total) {}
            @Override public void onSongStart(String songTitle) {}
            @Override public void onCurrentProgress(double progress, String statusText) {
                capturedProgress.set(progress);
            }
            @Override public void onSpeedUpdate(String speed) {}
            @Override public void onEtaUpdate(String eta) {}
            @Override public void onStatusUpdate(String statusMessage) {}
            @Override public void onGenericMessage(String message) {}
        });

        assertEquals(0.755, capturedProgress.get(), 0.001);
    }

    @Test
    void testParseSpeedUpdateWithWhitespace() {
        DownloadProgressParser parser = new DownloadProgressParser();
        AtomicReference<String> capturedSpeed = new AtomicReference<>("");

        parser.parseAndDispatch("   SPEED:3.5MiB/s  ", new DownloadProgressParser.ProgressListener() {
            @Override public void onOverallProgress(int current, int total) {}
            @Override public void onSongStart(String songTitle) {}
            @Override public void onCurrentProgress(double progress, String statusText) {}
            @Override public void onSpeedUpdate(String speed) {
                capturedSpeed.set(speed);
            }
            @Override public void onEtaUpdate(String eta) {}
            @Override public void onStatusUpdate(String statusMessage) {}
            @Override public void onGenericMessage(String message) {}
        });

        assertEquals("3.5MiB/s", capturedSpeed.get());
    }

    @Test
    void testParsePlaylistProgress() {
        DownloadProgressParser parser = new DownloadProgressParser();
        AtomicBoolean called = new AtomicBoolean(false);

        parser.parseAndDispatch("PLAYLIST_PROGRESS:3/20", new DownloadProgressParser.ProgressListener() {
            @Override public void onOverallProgress(int current, int total) {
                assertEquals(3, current);
                assertEquals(20, total);
                called.set(true);
            }
            @Override public void onSongStart(String songTitle) {}
            @Override public void onCurrentProgress(double progress, String statusText) {}
            @Override public void onSpeedUpdate(String speed) {}
            @Override public void onEtaUpdate(String eta) {}
            @Override public void onStatusUpdate(String statusMessage) {}
            @Override public void onGenericMessage(String message) {}
        });

        assertTrue(called.get());
    }

    @Test
    void testInvalidNumberDoesNotThrowException() {
        DownloadProgressParser parser = new DownloadProgressParser();

        assertDoesNotThrow(() -> {
            parser.parseAndDispatch("PROGRESS:abc", new DownloadProgressParser.ProgressListener() {
                @Override public void onOverallProgress(int current, int total) {}
                @Override public void onSongStart(String songTitle) {}
                @Override public void onCurrentProgress(double progress, String statusText) {}
                @Override public void onSpeedUpdate(String speed) {}
                @Override public void onEtaUpdate(String eta) {}
                @Override public void onStatusUpdate(String statusMessage) {}
                @Override public void onGenericMessage(String message) {}
            });
        });
    }
}
