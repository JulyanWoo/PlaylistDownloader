package com.example.interfaz.service.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class AudioConversionServiceTest {

    @Test
    void testBuildFfmpegCommand(@TempDir File tempDir) throws IOException {
        BinaryResolver resolver = new BinaryResolver();
        try (AudioConversionService service = new AudioConversionService(resolver)) {
            File rawFile = new File(tempDir, "song.webm");
            Files.writeString(rawFile.toPath(), "mock-raw-audio");

            File targetMp3 = new File(tempDir, "song.mp3");

            List<String> cmd = service.buildFfmpegCommand(rawFile, targetMp3);

            assertNotNull(cmd);
            assertTrue(cmd.contains("-i"));
            assertTrue(cmd.contains(rawFile.getAbsolutePath()));
            assertTrue(cmd.contains("-acodec"));
            assertTrue(cmd.contains("libmp3lame"));
            assertTrue(cmd.contains("-q:a"));
            assertTrue(cmd.contains("2"));
            assertTrue(cmd.contains(targetMp3.getAbsolutePath()));
        }
    }

    @Test
    void testRejectNonExistentRawFile(@TempDir File tempDir) {
        try (AudioConversionService service = new AudioConversionService()) {
            File rawFile = new File(tempDir, "non_existent.webm");
            File targetMp3 = new File(tempDir, "out.mp3");

            CompletableFuture<File> future = service.convertToMp3(rawFile, targetMp3, null);
            assertTrue(future.isCompletedExceptionally());
            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void testRejectOperationsAfterClose(@TempDir File tempDir) throws IOException {
        File rawFile = new File(tempDir, "song.webm");
        Files.writeString(rawFile.toPath(), "dummy");
        File targetMp3 = new File(tempDir, "song.mp3");

        AudioConversionService service = new AudioConversionService();
        service.close();

        CompletableFuture<File> future = service.convertToMp3(rawFile, targetMp3, null);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void testCancelAllDoesNotThrow() {
        try (AudioConversionService service = new AudioConversionService()) {
            assertDoesNotThrow(service::cancelAll);
        }
    }
}
