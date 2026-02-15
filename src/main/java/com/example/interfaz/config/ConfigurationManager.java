package com.example.interfaz.config;

import com.example.interfaz.util.FileUtils;
import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Gestor centralizado de configuraciones de la aplicación
 * Maneja rutas personalizadas y preferencias del usuario
 */
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
    
    /**
     * Carga la configuración desde el archivo de propiedades
     */
    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                LOGGER.info("Configuración cargada desde " + CONFIG_FILE);
                
                // Aplicar configuración de directorio de música si existe
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
    
    /**
     * Guarda la configuración actual en el archivo de propiedades
     */
    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Configuración de usuario - Interfaz de descarga de música");
            LOGGER.info("Configuración guardada en " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar configuración: " + e.getMessage());
        }
    }
    
    /**
     * Establece el directorio de música personalizado
     * @param directory ruta del directorio de música
     */
    public void setMusicDirectory(String directory) {
        if (directory != null && !directory.isEmpty()) {
            properties.setProperty("music.directory", directory);
            FileUtils.setMusicDirectory(directory);
            saveConfiguration();
            LOGGER.info("Directorio de música actualizado: " + directory);
        }
    }
    
    /**
     * Obtiene el directorio de música configurado
     * @return ruta del directorio de música
     */
    public String getMusicDirectory() {
        return properties.getProperty("music.directory", FileUtils.getMusicDirectory());
    }
    
    /**
     * Restablece la configuración a valores por defecto
     */
    public void resetToDefaults() {
        properties.clear();
        FileUtils.setMusicDirectory(null); // Esto hará que use el valor por defecto
        saveConfiguration();
        LOGGER.info("Configuración restablecida a valores por defecto");
    }
    
    /**
     * Obtiene una propiedad de configuración
     * @param key clave de la propiedad
     * @param defaultValue valor por defecto si no existe
     * @return valor de la propiedad
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Establece una propiedad de configuración
     * @param key clave de la propiedad
     * @param value valor de la propiedad
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfiguration();
    }
}