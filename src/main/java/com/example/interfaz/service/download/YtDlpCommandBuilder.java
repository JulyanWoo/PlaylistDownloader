package com.example.interfaz.service.download;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class YtDlpCommandBuilder {

    private final BinaryResolver binaryResolver;

    public YtDlpCommandBuilder(BinaryResolver binaryResolver) {
        this.binaryResolver = binaryResolver;
    }

    public YtDlpCommandBuilder() {
        this(new BinaryResolver());
    }

    /**
     * Builds a command to download a single song (audio only, mp3).
     * <ul>
     * <li>--no-playlist : ignores &amp;list= params in URLs (radio/mix)</li>
     * <li>--newline : forces yt-dlp to flush each progress line immediately</li>
     * <li>--socket-timeout 10 : drops hung network connections within 10 s</li>
     * </ul>
     */
    public List<String> buildSingleSongCommand(String url, String outputDir) {
        List<String> cmd = new ArrayList<>();
        cmd.add(binaryResolver.resolveYtDlpPath());
        cmd.add("-x");
        cmd.add("--audio-format");
        cmd.add("mp3");
        cmd.add("--newline");
        cmd.add("--socket-timeout");
        cmd.add("10");

        String ffmpegPath = binaryResolver.resolveFfmpegPath();
        if (binaryResolver.isExistingPath(ffmpegPath)) {
            cmd.add("--ffmpeg-location");
            cmd.add(ffmpegPath);
        }

        cmd.add("-o");
        cmd.add(outputDir + File.separator + "%(title)s.%(ext)s");
        cmd.add("--no-overwrites");
        cmd.add(url);
        return cmd;
    }

    /**
     * Builds a command to download an entire playlist (audio only, mp3).
     * Includes --newline and --socket-timeout 10 for reliable progress output.
     */
    public List<String> buildPlaylistCommand(String playlistUrl, String outputDir, int startFromVideo) {
        List<String> cmd = new ArrayList<>();
        cmd.add(binaryResolver.resolveYtDlpPath());
        cmd.add("-x");
        cmd.add("--audio-format");
        cmd.add("mp3");
        cmd.add("--newline");
        cmd.add("--socket-timeout");
        cmd.add("10");

        String ffmpegPath = binaryResolver.resolveFfmpegPath();
        if (binaryResolver.isExistingPath(ffmpegPath)) {
            cmd.add("--ffmpeg-location");
            cmd.add(ffmpegPath);
        }

        cmd.add("-o");
        cmd.add(Paths.get(outputDir, "%(title)s.%(ext)s").toString());
        cmd.add("--playlist-start");
        cmd.add(String.valueOf(startFromVideo));
        cmd.add("--no-overwrites");
        cmd.add(playlistUrl);
        return cmd;
    }
}
