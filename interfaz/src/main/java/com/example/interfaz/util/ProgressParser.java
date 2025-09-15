package com.example.interfaz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para parsear mensajes de progreso de descargas
 * Extrae información como porcentajes, velocidad y ETA de los mensajes de yt-dlp
 */
public class ProgressParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressParser.class);
    
    // Patrones regex para extraer información
    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern SPEED_PATTERN = Pattern.compile("at\\s+([\\d.]+(?:[KMGT]?i?B/s|B/s))");
    private static final Pattern ETA_PATTERN = Pattern.compile("ETA\\s+([\\d:]+|\\d+s?)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("of\\s+([\\d.]+(?:[KMGT]?i?B))");
    
    /**
     * Información parseada de un mensaje de progreso
     */
    public static class ProgressInfo {
        private final double percentage;
        private final String speed;
        private final String eta;
        private final String size;
        private final String originalMessage;
        
        public ProgressInfo(double percentage, String speed, String eta, String size, String originalMessage) {
            this.percentage = percentage;
            this.speed = speed;
            this.eta = eta;
            this.size = size;
            this.originalMessage = originalMessage;
        }
        
        public double getPercentage() { return percentage; }
        public String getSpeed() { return speed; }
        public String getEta() { return eta; }
        public String getSize() { return size; }
        public String getOriginalMessage() { return originalMessage; }
        
        public boolean hasPercentage() { return percentage >= 0; }
        public boolean hasSpeed() { return speed != null && !speed.isEmpty(); }
        public boolean hasEta() { return eta != null && !eta.isEmpty(); }
        public boolean hasSize() { return size != null && !size.isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("ProgressInfo{percentage=%.1f%%, speed='%s', eta='%s', size='%s'}", 
                               percentage, speed, eta, size);
        }
    }
    
    /**
     * Parsea un mensaje de progreso y extrae toda la información disponible
     */
    public static ProgressInfo parseProgressMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new ProgressInfo(-1, null, null, null, message);
        }
        
        double percentage = extractPercentage(message);
        String speed = extractSpeed(message);
        String eta = extractEta(message);
        String size = extractSize(message);
        
        LOGGER.debug("Parseado: {} -> {:.1f}%, {}, {}, {}", message, percentage, speed, eta, size);
        
        return new ProgressInfo(percentage, speed, eta, size, message);
    }
    
    /**
     * Extrae el porcentaje de un mensaje de progreso
     */
    public static double extractPercentage(String message) {
        if (message == null) return -1;
        
        Matcher matcher = PERCENTAGE_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Error parseando porcentaje: {}", matcher.group(1), e);
            }
        }
        return -1;
    }
    
    /**
     * Extrae la velocidad de descarga de un mensaje
     */
    public static String extractSpeed(String message) {
        if (message == null) return null;
        
        Matcher matcher = SPEED_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extrae el ETA (tiempo estimado) de un mensaje
     */
    public static String extractEta(String message) {
        if (message == null) return null;
        
        Matcher matcher = ETA_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extrae el tamaño del archivo de un mensaje
     */
    public static String extractSize(String message) {
        if (message == null) return null;
        
        Matcher matcher = SIZE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Verifica si un mensaje contiene información de progreso
     */
    public static boolean isProgressMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        return message.contains("%") || 
               message.contains("ETA") || 
               message.contains("at ") && message.contains("/s");
    }
    
    /**
     * Verifica si un mensaje indica que la descarga ha terminado
     */
    public static boolean isCompletionMessage(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("100%") ||
               lowerMessage.contains("download completed") ||
               lowerMessage.contains("finished downloading") ||
               lowerMessage.contains("already downloaded");
    }
    
    /**
     * Verifica si un mensaje indica un error
     */
    public static boolean isErrorMessage(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("error") ||
               lowerMessage.contains("failed") ||
               lowerMessage.contains("unable to") ||
               lowerMessage.contains("not found") ||
               lowerMessage.contains("unavailable");
    }
    
    /**
     * Verifica si un mensaje indica el inicio de una nueva descarga
     */
    public static boolean isStartMessage(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("downloading") ||
               lowerMessage.contains("[download]") ||
               lowerMessage.contains("destination:");
    }
    
    /**
     * Limpia un mensaje de progreso removiendo caracteres especiales
     */
    public static String cleanProgressMessage(String message) {
        if (message == null) return "";
        
        // Remover caracteres de control ANSI
        String cleaned = message.replaceAll("\\u001B\\[[;\\d]*m", "");
        
        // Remover caracteres de retorno de carro
        cleaned = cleaned.replaceAll("\\r", "");
        
        // Remover espacios extra
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Convierte un progreso de 0-100 a 0.0-1.0
     */
    public static double percentageToProgress(double percentage) {
        if (percentage < 0) return 0.0;
        if (percentage > 100) return 1.0;
        return percentage / 100.0;
    }
    
    /**
     * Convierte un progreso de 0.0-1.0 a 0-100
     */
    public static double progressToPercentage(double progress) {
        if (progress < 0) return 0.0;
        if (progress > 1) return 100.0;
        return progress * 100.0;
    }
    
    /**
     * Formatea un porcentaje para mostrar
     */
    public static String formatPercentage(double percentage) {
        return String.format("%.1f%%", percentage);
    }
    
    /**
     * Formatea un progreso (0.0-1.0) como porcentaje
     */
    public static String formatProgress(double progress) {
        return formatPercentage(progressToPercentage(progress));
    }
}