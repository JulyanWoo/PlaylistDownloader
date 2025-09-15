package com.example.interfaz.download;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestor de la cola de descargas
 * Responsable de manejar la cola de URLs y la interfaz de lista
 */
public class QueueManager {
    
    private final ConcurrentLinkedQueue<String> downloadQueue;
    private final ObservableList<String> queueItems;
    private final ListView<String> queueListView;
    private final AtomicInteger totalItems;
    private final AtomicInteger processedItems;
    
    /**
     * Constructor que inicializa el gestor de cola
     */
    public QueueManager(ListView<String> queueListView) {
        this.downloadQueue = new ConcurrentLinkedQueue<>();
        this.queueItems = FXCollections.observableArrayList();
        this.queueListView = queueListView;
        this.totalItems = new AtomicInteger(0);
        this.processedItems = new AtomicInteger(0);
        
        initializeQueue();
    }
    
    /**
     * Inicializa la configuración de la cola
     */
    private void initializeQueue() {
        queueListView.setItems(queueItems);
        queueListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    
    /**
     * Agrega una URL a la cola
     * @param url URL a agregar
     * @return true si se agregó exitosamente
     */
    public boolean addToQueue(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String trimmedUrl = url.trim();
        
        if (downloadQueue.contains(trimmedUrl)) {
            return false;
        }
        
        downloadQueue.offer(trimmedUrl);
        totalItems.incrementAndGet();
        
        Platform.runLater(() -> {
            queueItems.add(trimmedUrl);
        });
        
        return true;
    }
    
    /**
     * Obtiene y remueve la siguiente URL de la cola
     * @return siguiente URL o null si la cola está vacía
     */
    public String pollNext() {
        String url = downloadQueue.poll();
        if (url != null) {
            processedItems.incrementAndGet();
            Platform.runLater(() -> {
                queueItems.remove(url);
            });
        }
        return url;
    }
    
    /**
     * Obtiene la siguiente URL para descargar sin removerla
     * @return siguiente URL o null si la cola está vacía
     */
    public String getNextUrl() {
        return downloadQueue.peek();
    }
    
    /**
     * Remueve una URL específica de la cola
     * @param url URL a remover
     * @return true si se removió exitosamente
     */
    public boolean removeFromQueue(String url) {
        if (url == null) {
            return false;
        }
        
        boolean removed = downloadQueue.remove(url);
        if (removed) {
            Platform.runLater(() -> {
                queueItems.remove(url);
            });
            totalItems.decrementAndGet();
        }
        return removed;
    }
    
    /**
     * Marca una URL como completada
     * @param url URL completada
     */
    public void markAsCompleted(String url) {
        if (url != null) {
            removeFromQueue(url);
            processedItems.incrementAndGet();
        }
    }
     
     /**
      * Verifica si la cola está vacía
      * @return true si la cola está vacía
      */
    public boolean isEmpty() {
        return downloadQueue.isEmpty();
    }
    
    /**
     * Obtiene el tamaño actual de la cola
     * @return número de elementos en la cola
     */
    public int size() {
        return downloadQueue.size();
    }
    
    /**
     * Obtiene el tamaño de la cola (alias para compatibilidad)
     */
    public int getQueueSize() {
        return queueItems.size();
    }
    
    /**
     * Limpia toda la cola
     */
    public void clearQueue() {
        downloadQueue.clear();
        totalItems.set(0);
        processedItems.set(0);
        
        Platform.runLater(() -> {
            queueItems.clear();
        });
    }
    
    /**
     * Remueve los elementos seleccionados de la cola
     * @return número de elementos removidos
     */
    public int removeSelectedItems() {
        List<String> selectedItems = new ArrayList<>(queueListView.getSelectionModel().getSelectedItems());
        
        int removedCount = selectedItems.size();
        for (String item : selectedItems) {
            downloadQueue.remove(item);
            totalItems.decrementAndGet();
        }
        
        Platform.runLater(() -> {
            queueItems.removeAll(selectedItems);
        });
        
        return removedCount;
    }
    
    /**
     * Obtiene el progreso total de la cola
     * @return valor entre 0.0 y 1.0 representando el progreso
     */
    public double getOverallProgress() {
        int total = totalItems.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) processedItems.get() / total;
    }
    
    /**
     * Obtiene el número total de elementos procesados
     * @return número de elementos procesados
     */
    public int getProcessedCount() {
        return processedItems.get();
    }
    
    /**
     * Obtiene el número total de elementos
     * @return número total de elementos
     */
    public int getTotalCount() {
        return totalItems.get();
    }
    
    /**
     * Resetea los contadores de progreso
     */
    public void resetProgress() {
        processedItems.set(0);
        totalItems.set(downloadQueue.size());
    }
    
    /**
     * Obtiene una copia de todos los elementos en la cola
     * @return lista con todos los elementos
     */
    public List<String> getAllItems() {
        return new ArrayList<>(downloadQueue);
    }
    
    /**
     * Obtiene información de estado de la cola
     * @return string con información de estado
     */
    public String getQueueStatus() {
        return String.format("Cola: %d elementos, %d procesados", 
                           size(), getProcessedCount());
    }
    
    /**
     * Verifica si la cola contiene una URL específica
     * @param url URL a verificar
     * @return true si la URL está en la cola
     */
    public boolean contains(String url) {
        return downloadQueue.contains(url);
    }
}