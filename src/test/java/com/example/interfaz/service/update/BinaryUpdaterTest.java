package com.example.interfaz.service.update;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class BinaryUpdaterTest {

    private final BinaryUpdater updater = new BinaryUpdater();

    @Test
    void testValidateBinaryReturnsFalseForInvalidPath() {
        assertFalse(updater.validateBinary(Path.of("non_existent_binary.exe")));
    }

    @Test
    void testUpdateBinaryReturnsFalseForNonExistentDirectory(@TempDir Path tempDir) {
        Path invalidTarget = tempDir.resolve("subfolder_does_not_exist").resolve("yt-dlp.exe");
        boolean result = updater.updateBinary(invalidTarget.toString(), "http://invalid-url", line -> {});
        assertFalse(result);
    }

    @Test
    void testUpdateBinaryReturnsFalseForInvalidDownloadUrl(@TempDir Path tempDir) throws Exception {
        Path dummyTarget = tempDir.resolve("yt-dlp.exe");
        Files.writeString(dummyTarget, "dummy");

        boolean result = updater.updateBinary(dummyTarget.toString(), "http://localhost:59999/non_existent", line -> {});
        assertFalse(result);
        assertTrue(Files.exists(dummyTarget), "Target file should remain intact on download failure");
    }
}
