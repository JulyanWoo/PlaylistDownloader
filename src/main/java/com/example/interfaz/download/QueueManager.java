package com.example.interfaz.download;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class QueueManager {

    private final Queue<String> downloadQueue;
    private final Set<String> queuedUrls;
    private final Set<String> processingUrls;
    private int totalDownloads;
    private int processedItems;
    private int failedItems;

    public QueueManager() {
        this.downloadQueue = new LinkedList<>();
        this.queuedUrls = new HashSet<>();
        this.processingUrls = new HashSet<>();
        this.totalDownloads = 0;
        this.processedItems = 0;
        this.failedItems = 0;
    }

    public synchronized boolean addToQueue(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String trimmedUrl = url.trim();

        if (!processingUrls.contains(trimmedUrl) && queuedUrls.add(trimmedUrl)) {
            downloadQueue.offer(trimmedUrl);
            totalDownloads++;
            return true;
        }

        return false;
    }

    public synchronized String pollNext() {
        String url = downloadQueue.poll();
        if (url != null) {
            queuedUrls.remove(url);
            processingUrls.add(url);
        }
        return url;
    }

    public synchronized boolean removeFromQueue(String url) {
        if (url == null) {
            return false;
        }

        String trimmedUrl = url.trim();
        boolean removed = downloadQueue.remove(trimmedUrl);
        if (removed) {
            queuedUrls.remove(trimmedUrl);
            if (totalDownloads > 0) {
                totalDownloads--;
            }
        }
        return removed;
    }

    public synchronized void markAsCompleted(String url) {
        if (url != null) {
            if (processingUrls.remove(url.trim())) {
                processedItems++;
            }
        }
    }

    public synchronized void markAsFailed(String url) {
        if (url != null) {
            if (processingUrls.remove(url.trim())) {
                failedItems++;
            }
        }
    }

    public synchronized boolean isEmpty() {
        return downloadQueue.isEmpty();
    }

    public synchronized int size() {
        return downloadQueue.size();
    }

    public synchronized void clearQueue() {
        downloadQueue.clear();
        queuedUrls.clear();
        totalDownloads = processingUrls.size() + processedItems + failedItems;
    }

    public synchronized void clearAll() {
        downloadQueue.clear();
        queuedUrls.clear();
        processingUrls.clear();
        totalDownloads = 0;
        processedItems = 0;
        failedItems = 0;
    }

    public synchronized double getOverallProgress() {
        if (totalDownloads == 0) {
            return 0.0;
        }
        return (double) (processedItems + failedItems) / totalDownloads;
    }

    public synchronized int getProcessedCount() {
        return processedItems;
    }

    public synchronized int getFailedCount() {
        return failedItems;
    }

    public synchronized int getTotalCount() {
        return totalDownloads;
    }

    public synchronized int getTotalDownloads() {
        return totalDownloads;
    }

    public synchronized void resetProgress() {
        processedItems = 0;
        failedItems = 0;
        totalDownloads = downloadQueue.size() + processingUrls.size();
    }

    public synchronized List<String> getAllItems() {
        return new ArrayList<>(downloadQueue);
    }

    public synchronized String getQueueStatus() {
        return String.format("Pendientes: %d, Descargando: %d, Completados: %d, Fallidos: %d", 
                           downloadQueue.size(), processingUrls.size(), processedItems, failedItems);
    }

    public synchronized boolean contains(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        return queuedUrls.contains(trimmed) || processingUrls.contains(trimmed);
    }

    public synchronized boolean isQueued(String url) {
        return url != null && queuedUrls.contains(url.trim());
    }

    public synchronized boolean isProcessing(String url) {
        return url != null && processingUrls.contains(url.trim());
    }
}
