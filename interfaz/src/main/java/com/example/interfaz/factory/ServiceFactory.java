package com.example.interfaz.factory;

import com.example.interfaz.service.*;
import com.example.interfaz.download.QueueManager;
import com.example.interfaz.controller.ProgressManager;
import com.example.interfaz.controller.UIStateManager;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.EventBus;
import javafx.scene.control.*;

/**
 * Factory para la creación e inyección de dependencias de servicios
 * Implementa el patrón Singleton y Factory para centralizar la gestión de dependencias
 */
public class ServiceFactory {
    
    private static ServiceFactory instance;
    
    private FilterService filterService;
    private DownloadService downloadService;
    private EventPublisher eventPublisher;
    
    private ServiceFactory() {
    }
    
    /**
     * Obtiene la instancia singleton del factory
     * @return instancia del ServiceFactory
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    /**
     * Obtiene o crea el servicio de filtrado
     * @return instancia del FilterService
     */
    public FilterService getFilterService() {
        if (filterService == null) {
            filterService = SongFilterService.getInstance();
        }
        return filterService;
    }
    
    /**
     * Obtiene o crea el servicio de descarga
     * @return instancia del DownloadService
     */
    public DownloadService getDownloadService() {
        if (downloadService == null) {
            downloadService = new YouTubeDownloadService();
        }
        return downloadService;
    }
    
    /**
     * Obtiene o crea el publicador de eventos
     * @return instancia del EventPublisher
     */
    public EventPublisher getEventPublisher() {
        if (eventPublisher == null) {
            eventPublisher = EventBus.getInstance();
        }
        return eventPublisher;
    }
    
    /**
     * Crea un nuevo QueueManager con las dependencias necesarias
     * @param queueListView ListView para mostrar la cola
     * @return nueva instancia de QueueManager
     */
    public QueueManager createQueueManager(ListView<String> queueListView) {
        return new QueueManager(queueListView);
    }
    
    /**
     * Crea una instancia de ProgressManager con las dependencias necesarias
     */
    public ProgressManager createProgressManager() {
        return new ProgressManager();
    }
    
    /**
     * Crea un nuevo UIStateManager con las dependencias necesarias
     * @param inputField Campo de entrada de URL
     * @param addButton Botón de agregar
     * @param startButton Botón de inicio
     * @param pauseButton Botón de pausa
     * @param cancelButton Botón de cancelar
     * @param clearQueueButton Botón de limpiar cola
     * @param removeSelectedButton Botón de remover seleccionados
     * @param queueListView Lista de cola
     * @return nueva instancia de UIStateManager
     */
    public UIStateManager createUIStateManager(
            TextField inputField,
            Button addButton,
            Button startButton,
            Button pauseButton,
            Button cancelButton,
            Button clearQueueButton,
            Button removeSelectedButton,
            ListView<String> queueListView) {
        return new UIStateManager(
            inputField, addButton, startButton, pauseButton,
            cancelButton, clearQueueButton, removeSelectedButton, queueListView
        );
    }
    
    /**
     * Reinicia el factory (útil para testing)
     */
    public static void reset() {
        instance = null;
    }
    
    /**
     * Configura dependencias personalizadas (útil para testing)
     * @param filterService Servicio de filtrado personalizado
     * @param downloadService Servicio de descarga personalizado
     */
    public void setServices(FilterService filterService, DownloadService downloadService) {
        this.filterService = filterService;
        this.downloadService = downloadService;
    }
    
    /**
     * Configura todas las dependencias personalizadas (útil para testing)
     * @param filterService Servicio de filtrado personalizado
     * @param downloadService Servicio de descarga personalizado
     * @param eventPublisher Publicador de eventos personalizado
     */
    public void setServices(FilterService filterService, DownloadService downloadService, EventPublisher eventPublisher) {
        this.filterService = filterService;
        this.downloadService = downloadService;
        this.eventPublisher = eventPublisher;
    }
}