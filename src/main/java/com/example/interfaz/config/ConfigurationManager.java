package com.example.interfaz.config;

import com.example.interfaz.util.FileUtils;
import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ConfigurationManager {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());
    private static final String CONFIG_FILE = "user-config.properties";
    private static ConfigurationManager instance;
    private Properties properties;

    private ConfigurationManager() {
        properties = new Properties();
        loadConfiguration();
    }

    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                LOGGER.info("Configuración cargada desde " + CONFIG_FILE);

                String musicDir = properties.getProperty("music.directory");
                if (musicDir != null && !musicDir.isEmpty()) {
                    FileUtils.setMusicDirectory(musicDir);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error al cargar configuración: " + e.getMessage());
            }
        } else {
            LOGGER.info("Archivo de configuración no encontrado, usando valores por defecto");
        }
    }

    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Configuración de usuario - Interfaz de descarga de música");
            LOGGER.info("Configuración guardada en " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar configuración: " + e.getMessage());
        }
    }

    public void setMusicDirectory(String directory) {
        if (directory != null && !directory.isEmpty()) {
            properties.setProperty("music.directory", directory);
            FileUtils.setMusicDirectory(directory);
            saveConfiguration();
            LOGGER.info("Directorio de música actualizado: " + directory);
        }
    }

    public String getMusicDirectory() {
        return properties.getProperty("music.directory", FileUtils.getMusicDirectory());
    }

    public void resetToDefaults() {
        properties.clear();
        FileUtils.setMusicDirectory(null); 
        saveConfiguration();
        LOGGER.info("Configuración restablecida a valores por defecto");
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfiguration();
    }
}
