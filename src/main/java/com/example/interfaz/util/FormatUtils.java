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

    public static String abbreviatePath(String pathStr, int maxLength) {
        if (pathStr == null || pathStr.length() <= maxLength) {
            return pathStr != null ? pathStr : "";
        }
        String normalized = pathStr.replace('/', '\\');
        int lastSep = normalized.lastIndexOf('\\');
        if (lastSep == -1) {
            return normalized.substring(0, maxLength - 3) + "...";
        }
        String fileName = normalized.substring(lastSep);
        int prevSep = normalized.lastIndexOf('\\', lastSep - 1);
        String parentDir = prevSep != -1 ? normalized.substring(prevSep, lastSep) : "";

        int prefixLen = Math.min(12, lastSep);
        String prefix = normalized.substring(0, prefixLen);

        String result = prefix + "\\...\\" + parentDir + fileName;
        if (result.length() > maxLength) {
            return prefix + "\\...\\" + fileName;
        }
        return result;
    }
}
