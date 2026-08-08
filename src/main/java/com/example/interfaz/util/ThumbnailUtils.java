package com.example.interfaz.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbnailUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailUtils.class);
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("(?:v=|/v/|/embed/|/shorts/|youtu\\.be/)([a-zA-Z0-9_-]{11})");

    public static String extractVideoId(String url) {
        if (url == null || url.isBlank()) return null;
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String getThumbnailUrl(String videoId) {
        if (videoId == null || videoId.isBlank()) return null;
        return "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
    }

    public static boolean isPlaylistUrl(String url) {
        return url != null && (url.contains("list=") || url.contains("/playlist"));
    }

    public static List<String> fetchPlaylistVideoIds(String playlistUrl, String ytDlpPath) {
        List<String> videoIds = new ArrayList<>();
        if (playlistUrl == null || playlistUrl.isBlank() || ytDlpPath == null) {
            return videoIds;
        }

        String mainVideoId = extractVideoId(playlistUrl);
        if (mainVideoId != null) {
            videoIds.add(mainVideoId);
        }

        try {
            List<String> command = List.of(
                ytDlpPath,
                "--flat-playlist",
                "--get-id",
                "--playlist-end", "4",
                "--no-warnings",
                playlistUrl
            );
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && videoIds.size() < 4) {
                    line = line.trim();
                    if (line.length() == 11 && !videoIds.contains(line)) {
                        videoIds.add(line);
                    }
                }
                boolean finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Could not fetch playlist video IDs via yt-dlp: {}", e.getMessage());
        }

        return videoIds;
    }
}
