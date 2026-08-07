package com.example.interfaz.service.update;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class UpdateHistoryTest {

    @Test
    void testRecordAndLoadHistory(@TempDir Path tempDir) {
        Path historyFile = tempDir.resolve("config").resolve("update-history.json");
        UpdateHistory history = new UpdateHistory(historyFile);

        assertTrue(history.loadHistory().isEmpty(), "Historial inicial debe estar vacío");

        history.recordUpdate("2026.03.17", "2026.07.04", true);
        history.recordUpdate("2026.07.04", "2026.08.01", false);

        List<UpdateHistory.Entry> entries = history.loadHistory();
        assertEquals(2, entries.size());

        assertEquals("2026.03.17", entries.get(0).fromVersion());
        assertEquals("2026.07.04", entries.get(0).toVersion());
        assertTrue(entries.get(0).success());

        assertEquals("2026.07.04", entries.get(1).fromVersion());
        assertEquals("2026.08.01", entries.get(1).toVersion());
        assertFalse(entries.get(1).success());
    }
}
