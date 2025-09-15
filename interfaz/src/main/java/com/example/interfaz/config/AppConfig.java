package com.example.interfaz.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuración centralizada de la aplicación
 * Maneja propiedades y configuraciones globales
 */
public class AppConfig {
    
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static AppConfig instance;
    private Properties properties;
    
    private static final String DEFAULT_DOWNLOAD_PATH = "./downloads";
    private static final int DEFAULT_MAX_CONCURRENT_DOWNLOADS = 3;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.70;
    
    private AppConfig() {
        loadProperties();
    }
    
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    private void loadProperties() {
        properties = new Properties();
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) {
                properties.load(input);
                LOGGER.info("Propiedades cargadas desde app.properties");
            } else {
                LOGGER.info("Archivo app.properties no encontrado, usando valores por defecto");
            }
        } catch (IOException e) {
            LOGGER.warning("Error cargando propiedades: " + e.getMessage());
        }
    }
    
    // Getters para configuraciones
    
    public String getDownloadPath() {
        return properties.getProperty("download.path", DEFAULT_DOWNLOAD_PATH);
    }
    
    public int getMaxConcurrentDownloads() {
        return Integer.parseInt(properties.getProperty("download.max.concurrent", String.valueOf(DEFAULT_MAX_CONCURRENT_DOWNLOADS)));
    }
    
    public int getTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("download.timeout.seconds", String.valueOf(DEFAULT_TIMEOUT_SECONDS)));
    }
    
    public double getSimilarityThreshold() {
        return Double.parseDouble(properties.getProperty("filter.similarity.threshold", String.valueOf(DEFAULT_SIMILARITY_THRESHOLD)));
    }
    
    public String getYtDlpPath() {
        return properties.getProperty("ytdlp.path", "yt-dlp");
    }
    
    public String getAppTitle() {
        return properties.getProperty("app.title", "YouTube Downloader - Descargador de Música");
    }
    
    public String getAppVersion() {
        return properties.getProperty("app.version", "1.0.0");
    }
    
    public boolean isDebugMode() {
        return Boolean.parseBoolean(properties.getProperty("app.debug", "false"));
    }
    
    // Setters para configuraciones dinámicas
    
    public void setDownloadPath(String path) {
        properties.setProperty("download.path", path);
    }
    
    public void setMaxConcurrentDownloads(int max) {
        properties.setProperty("download.max.concurrent", String.valueOf(max));
    }
    
    public void setSimilarityThreshold(double threshold) {
        properties.setProperty("filter.similarity.threshold", String.valueOf(threshold));
    }
    
    /**
     * Obtiene una propiedad personalizada
     * @param key clave de la propiedad
     * @param defaultValue valor por defecto
     * @return valor de la propiedad
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Establece una propiedad personalizada
     * @param key clave de la propiedad
     * @param value valor de la propiedad
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}