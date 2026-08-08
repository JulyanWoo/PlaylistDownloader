package com.example.interfaz.service.config;

import com.example.interfaz.config.ConfigurationManager;
import com.example.interfaz.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MusicFolderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicFolderService.class);

    public String getCurrentMusicFolder() {
        return FileUtils.getMusicDirectory();
    }

    public String getMusicFolderPath() {
        return getCurrentMusicFolder();
    }

    public File getMusicFolder() {
        return new File(getCurrentMusicFolder());
    }

    public String setMusicFolder(String newPath) {
        if (newPath == null || newPath.trim().isEmpty()) {
            return getCurrentMusicFolder();
        }
        String cleanPath = newPath.trim();
        FileUtils.setMusicDirectory(cleanPath);
        ConfigurationManager.getInstance().setMusicDirectory(cleanPath);
        ConfigurationManager.getInstance().saveConfiguration();
        LOGGER.info("Carpeta de música actualizada a: {}", cleanPath);
        return cleanPath;
    }

    public void setMusicFolderPath(String newPath) {
        setMusicFolder(newPath);
    }

    public String resetToDefaultMusicFolder() {
        String defaultPath = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "MUSICA";
        FileUtils.setMusicDirectory(defaultPath);
        ConfigurationManager.getInstance().setMusicDirectory(defaultPath);
        ConfigurationManager.getInstance().saveConfiguration();
        LOGGER.info("Carpeta de música restablecida por defecto a: {}", defaultPath);
        return defaultPath;
    }

    public String getDisplayPath() {
        String currentPath = getCurrentMusicFolder();
        File currentDir = new File(currentPath);
        return currentDir.getParent() != null ?
                new File(currentDir.getParent()).getName() + "/" + currentDir.getName() :
                currentDir.getName();
    }
}
