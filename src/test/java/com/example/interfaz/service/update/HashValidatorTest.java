package com.example.interfaz.service.update;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class HashValidatorTest {

    private final HashValidator validator = new HashValidator();

    @Test
    void testComputeSha256(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "hello world");

        String hash = validator.computeSha256(testFile);
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash debe ser una cadena hex de 64 caracteres");
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash);
    }

    @Test
    void testParseHashFromSumsFile() {
        String sumsContent = """
            b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9 *test.txt
            e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855 *yt-dlp.exe
            """;

        String hash = validator.parseHashFromSumsFile(sumsContent, "yt-dlp.exe");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);

        String nonExistent = validator.parseHashFromSumsFile(sumsContent, "unknown.exe");
        assertEquals("", nonExistent);
    }

    @Test
    void testVerifyFileHashWithEmptySumsUrlReturnsTrue(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("sample.exe");
        Files.writeString(testFile, "content");

        assertTrue(validator.verifyFileHash(testFile, ""));
        assertTrue(validator.verifyFileHash(testFile, null));
    }
}
