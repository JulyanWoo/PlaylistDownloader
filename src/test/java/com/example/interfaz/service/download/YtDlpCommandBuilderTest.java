package com.example.interfaz.service.download;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class YtDlpCommandBuilderTest {

    @Test
    void testBuildSingleSongCommand() {
        YtDlpCommandBuilder builder = new YtDlpCommandBuilder();
        List<String> cmd = builder.buildSingleSongCommand("https://www.youtube.com/watch?v=test", "C:\\Music");

        assertNotNull(cmd);
        assertTrue(cmd.size() >= 5);
        assertTrue(cmd.contains("-x"));
        assertTrue(cmd.contains("mp3"));
        assertTrue(cmd.contains("https://www.youtube.com/watch?v=test"));
    }

    @Test
    void testBuildPlaylistCommand() {
        YtDlpCommandBuilder builder = new YtDlpCommandBuilder();
        List<String> cmd = builder.buildPlaylistCommand("https://www.youtube.com/playlist?list=test", "C:\\Music", 3);

        assertNotNull(cmd);
        assertTrue(cmd.contains("--playlist-start"));
        assertTrue(cmd.contains("3"));
        assertTrue(cmd.contains("https://www.youtube.com/playlist?list=test"));
    }
}
