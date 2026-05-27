package com.example.interfaz.factory;

import com.example.interfaz.service.*;
import com.example.interfaz.download.QueueManager;
import com.example.interfaz.controller.ProgressManager;
import com.example.interfaz.controller.UIStateManager;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.event.EventBus;
import javafx.scene.control.*;

public class ServiceFactory {

    private static ServiceFactory instance;

    private FilterService filterService;
    private DownloadService downloadService;
    private EventPublisher eventPublisher;

    private ServiceFactory() {
    }

    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }

    public FilterService getFilterService() {
        if (filterService == null) {
            filterService = SongFilterService.getInstance();
        }
        return filterService;
    }

    public DownloadService getDownloadService() {
        if (downloadService == null) {
            downloadService = new YouTubeDownloadService();
        }
        return downloadService;
    }

    public EventPublisher getEventPublisher() {
        if (eventPublisher == null) {
            eventPublisher = EventBus.getInstance();
        }
        return eventPublisher;
    }

    public QueueManager createQueueManager(ListView<String> queueListView) {
        return new QueueManager(queueListView);
    }

    public ProgressManager createProgressManager() {
        return new ProgressManager();
    }

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

    public static void reset() {
        instance = null;
    }

    public void setServices(FilterService filterService, DownloadService downloadService) {
        this.filterService = filterService;
        this.downloadService = downloadService;
    }

    public void setServices(FilterService filterService, DownloadService downloadService, EventPublisher eventPublisher) {
        this.filterService = filterService;
        this.downloadService = downloadService;
        this.eventPublisher = eventPublisher;
    }
}
