package com.example.interfaz.download;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueueManagerTest {

    @Test
    void testQueueManagerStatesAndFailedHandling() {
        QueueManager manager = new QueueManager();

        assertTrue(manager.isEmpty());
        assertEquals(0, manager.getTotalDownloads());

        assertTrue(manager.addToQueue("https://youtube.com/watch?v=1"));
        assertTrue(manager.addToQueue("https://youtube.com/watch?v=2"));
        assertEquals(2, manager.getTotalDownloads());

        String first = manager.pollNext();
        assertEquals("https://youtube.com/watch?v=1", first);
        assertTrue(manager.isProcessing("https://youtube.com/watch?v=1"));

        manager.markAsCompleted(first);
        assertFalse(manager.isProcessing("https://youtube.com/watch?v=1"));
        assertEquals(1, manager.getProcessedCount());

        String second = manager.pollNext();
        assertEquals("https://youtube.com/watch?v=2", second);
        assertTrue(manager.isProcessing("https://youtube.com/watch?v=2"));

        manager.markAsFailed(second);
        assertFalse(manager.isProcessing("https://youtube.com/watch?v=2"));
        assertEquals(1, manager.getFailedCount());

        assertEquals(1.0, manager.getOverallProgress(), 0.001);
    }
}
