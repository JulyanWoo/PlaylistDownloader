package com.example.interfaz.service.download;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryResolver.class);

    private static final String YT_DLP_ENV = "YT_DLP_PATH";
    private static final String FFMPEG_ENV = "FFMPEG_PATH";
    private static final String DEFAULT_YT_DLP_RELATIVE = "Libs/yt-dlp.exe";
    private static final String DEFAULT_FFMPEG_RELATIVE = "Libs/ffmpeg-2024-09-26-git-f43916e217-full_build/ffmpeg-2024-09-26-git-f43916e217-full_build/bin/ffmpeg.exe";

    public String resolveYtDlpPath() {
        String env = System.getenv(YT_DLP_ENV);
        if (isExistingFile(env)) return env;

        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_YT_DLP_RELATIVE);
        if (Files.exists(relative)) return relative.toString();

        File jarFolder = getJarFolder();
        if (jarFolder != null) {
            File jarRelative = new File(jarFolder, DEFAULT_YT_DLP_RELATIVE);
            if (jarRelative.exists()) return jarRelative.getAbsolutePath();

            File jarParentRelative = new File(jarFolder.getParentFile(), DEFAULT_YT_DLP_RELATIVE);
            if (jarParentRelative.exists()) return jarParentRelative.getAbsolutePath();
        }

        String exe = getExecutableName("yt-dlp");
        Path srcMainRelative = Paths.get(System.getProperty("user.dir"), "src", "main", "Libs", exe);
        if (Files.exists(srcMainRelative)) return srcMainRelative.toString();

        String underSrc = findInDir(Paths.get(System.getProperty("user.dir"), "src", "main", "Libs"), exe);
        if (underSrc != null) return underSrc;

        String foundInPath = findInSystemPath(exe);
        if (foundInPath != null) return foundInPath;

        return exe;
    }

    public String resolveFfmpegPath() {
        String env = System.getenv(FFMPEG_ENV);
        if (isExistingFile(env)) return env;

        Path relative = Paths.get(System.getProperty("user.dir"), DEFAULT_FFMPEG_RELATIVE);
        if (Files.exists(relative)) return relative.toString();

        File jarFolder = getJarFolder();
        if (jarFolder != null) {
            File jarRelative = new File(jarFolder, DEFAULT_FFMPEG_RELATIVE);
            if (jarRelative.exists()) return jarRelative.getAbsolutePath();

            File jarParentRelative = new File(jarFolder.getParentFile(), DEFAULT_FFMPEG_RELATIVE);
            if (jarParentRelative.exists()) return jarParentRelative.getAbsolutePath();
        }

        String exe = getExecutableName("ffmpeg");
        Path srcMainLibs = Paths.get(System.getProperty("user.dir"), "src", "main", "Libs");
        String foundLocal = findInDir(srcMainLibs, exe);
        if (foundLocal != null) return foundLocal;

        String foundInPath = findInSystemPath(exe);
        if (foundInPath != null) return foundInPath;

        return exe;
    }

    public boolean isExistingPath(String path) {
        return isExistingFile(path);
    }

    private boolean isExistingFile(String path) {
        if (path == null || path.trim().isEmpty()) return false;
        return new File(path).exists();
    }

    private File getJarFolder() {
        try {
            return new File(BinaryResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            LOGGER.debug("Could not resolve JAR folder", e);
            return null;
        }
    }

    private String getExecutableName(String baseName) {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        return isWin ? baseName + ".exe" : baseName;
    }

    private String findInSystemPath(String exe) {
        String path = System.getenv("PATH");
        if (path == null || path.isEmpty()) return null;
        String[] dirs = path.split(File.pathSeparator);
        for (String dir : dirs) {
            File f = new File(dir, exe);
            if (f.exists() && f.isFile()) return f.getAbsolutePath();
        }
        return null;
    }

    private String findInDir(Path dir, String exe) {
        if (dir == null || !Files.exists(dir)) return null;
        try (var stream = Files.walk(dir, 4)) {
            var opt = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(exe))
                    .findFirst();
            return opt.map(Path::toString).orElse(null);
        } catch (IOException e) {
            LOGGER.debug("Error traversing directory {}", dir, e);
            return null;
        }
    }
}
