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

public class QueueManager {

    private final ConcurrentLinkedQueue<String> downloadQueue;
    private final ObservableList<String> queueItems;
    private final ListView<String> queueListView;
    private final AtomicInteger totalItems;
    private final AtomicInteger processedItems;

    public QueueManager(ListView<String> queueListView) {
        this.downloadQueue = new ConcurrentLinkedQueue<>();
        this.queueItems = FXCollections.observableArrayList();
        this.queueListView = queueListView;
        this.totalItems = new AtomicInteger(0);
        this.processedItems = new AtomicInteger(0);

        initializeQueue();
    }

    private void initializeQueue() {
        queueListView.setItems(queueItems);
        queueListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

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

    public String getNextUrl() {
        return downloadQueue.peek();
    }

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

    public void markAsCompleted(String url) {
        if (url != null) {
            removeFromQueue(url);
            processedItems.incrementAndGet();
        }
    }

    public boolean isEmpty() {
        return downloadQueue.isEmpty();
    }

    public int size() {
        return downloadQueue.size();
    }

    public int getQueueSize() {
        return queueItems.size();
    }

    public void clearQueue() {
        downloadQueue.clear();
        totalItems.set(0);
        processedItems.set(0);

        Platform.runLater(() -> {
            queueItems.clear();
        });
    }

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

    public double getOverallProgress() {
        int total = totalItems.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) processedItems.get() / total;
    }

    public int getProcessedCount() {
        return processedItems.get();
    }

    public int getTotalCount() {
        return totalItems.get();
    }

    public void resetProgress() {
        processedItems.set(0);
        totalItems.set(downloadQueue.size());
    }

    public List<String> getAllItems() {
        return new ArrayList<>(downloadQueue);
    }

    public String getQueueStatus() {
        return String.format("Cola: %d elementos, %d procesados", 
                           size(), getProcessedCount());
    }

    public boolean contains(String url) {
        return downloadQueue.contains(url);
    }
}
