package com.example.interfaz.util;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public static String formatDurationSeconds(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
