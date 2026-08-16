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
    void testBuildRawAudioDownloadCommand() {
        YtDlpCommandBuilder builder = new YtDlpCommandBuilder();
        List<String> cmd = builder.buildRawAudioDownloadCommand("https://www.youtube.com/watch?v=test", "C:\\Temp");

        assertNotNull(cmd);
        assertTrue(cmd.contains("-f"));
        assertTrue(cmd.contains("bestaudio/best"));
        assertFalse(cmd.contains("-x"));
        assertFalse(cmd.contains("mp3"));
        assertTrue(cmd.contains("https://www.youtube.com/watch?v=test"));
        assertTrue(cmd.stream().anyMatch(arg -> arg.contains("raw_%(id)s___%(title)s.%(ext)s")));
    }

    @Test
    void testBuildRawAudioDownloadCommandWithCustomBaseName() {
        YtDlpCommandBuilder builder = new YtDlpCommandBuilder();
        List<String> cmd = builder.buildRawAudioDownloadCommand("https://www.youtube.com/watch?v=test", "C:\\Temp", "custom_base");

        assertNotNull(cmd);
        assertTrue(cmd.stream().anyMatch(arg -> arg.contains("custom_base.%(ext)s")));
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
