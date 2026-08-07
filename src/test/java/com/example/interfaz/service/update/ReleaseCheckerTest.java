package com.example.interfaz.service.update;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseCheckerTest {

    private final ReleaseChecker checker = new ReleaseChecker();

    @Test
    void testIsNewerVersionAvailable() {
        assertTrue(checker.isNewerVersionAvailable("2026.03.17", "2026.07.04"));
        assertFalse(checker.isNewerVersionAvailable("2026.07.04", "2026.03.17"));
        assertFalse(checker.isNewerVersionAvailable("2026.07.04", "2026.07.04"));
    }

    @Test
    void testIsNewerVersionAvailableWithUnknownOrNull() {
        assertTrue(checker.isNewerVersionAvailable("Desconocida", "2026.07.04"));
        assertTrue(checker.isNewerVersionAvailable(null, "2026.07.04"));
        assertFalse(checker.isNewerVersionAvailable("2026.07.04", "Desconocida"));
        assertFalse(checker.isNewerVersionAvailable("2026.07.04", null));
    }

    @Test
    void testCheckForUpdatesExecutesWithoutException() {
        UpdateInfo info = checker.checkForUpdates("2026.03.17");
        assertNotNull(info);
        assertNotNull(info.getCurrentVersion());
        assertNotNull(info.getLatestVersion());
        assertNotNull(info.getDownloadUrl());
    }
}
