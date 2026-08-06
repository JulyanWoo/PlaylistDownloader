package com.example.interfaz.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void testExtractFileName() {
        String fileName = FileUtils.extractFileName("C:/Downloads/Music/song.mp3");
        assertEquals("song.mp3", fileName);
    }

    @Test
    void testExtractSongTitle() {
        String title = FileUtils.extractSongTitle("Artist_-_Song_Title.mp3");
        assertEquals("Artist Song Title", title);
    }

    @Test
    void testGetFileExtension() {
        String ext = FileUtils.getFileExtension("song.MP3");
        assertEquals("mp3", ext);
    }

    @Test
    void testIsValidFilePath() {
        assertTrue(FileUtils.isValidFilePath("C:\\Users\\Test\\Desktop"));
        assertFalse(FileUtils.isValidFilePath(""));
    }
}
