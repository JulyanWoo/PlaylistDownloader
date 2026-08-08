package com.example.interfaz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatUtilsTest {

    @Test
    void testFormatDuration() {
        assertEquals("01:30", FormatUtils.formatDuration(90000));
        assertEquals("3:20", FormatUtils.formatDurationSeconds(200));
    }

    @Test
    void testFormatSize() {
        assertEquals("500 B", FormatUtils.formatSize(500));
        assertTrue(FormatUtils.formatSize(1024).endsWith("KB"));
    }

    @Test
    void testAbbreviatePathShortPath() {
        String path = "C:\\Music\\song.mp3";
        assertEquals(path, FormatUtils.abbreviatePath(path, 40));
    }

    @Test
    void testAbbreviatePathLongPath() {
        String path = "C:\\Users\\JULI-HERMOSO\\Desktop\\Playlists\\HeavyMetal\\IronMaiden.mp3";
        String abbreviated = FormatUtils.abbreviatePath(path, 40);
        assertTrue(abbreviated.contains("..."));
        assertTrue(abbreviated.endsWith("IronMaiden.mp3"));
    }
}
