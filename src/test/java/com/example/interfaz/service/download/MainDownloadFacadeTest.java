package com.example.interfaz.service.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.interfaz.event.EventBus;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.service.DownloadService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MainDownloadFacadeTest {

    private DownloadService downloadService;
    private DownloadProgressParser progressParser;
    private YtDlpUpdateService ytDlpUpdateService;
    private EventPublisher eventPublisher;
    private MainDownloadFacade downloadFacade;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        downloadService = new com.example.interfaz.service.YouTubeDownloadService();
        progressParser = new DownloadProgressParser();
        ytDlpUpdateService = new YtDlpUpdateService();
        eventPublisher = new EventBus();

        downloadFacade = new MainDownloadFacade(
                downloadService,
                progressParser,
                ytDlpUpdateService,
                eventPublisher
        );
    }

    @Test
    void testInitializationAndClose() {
        downloadFacade.initialize(null, null, null);
        assertFalse(downloadFacade.isDownloading());

        downloadFacade.close();
    }

    @Test
    void testAddToQueueWhenNotInitialized() {
        boolean added = downloadFacade.addToQueue("https://youtube.com/watch?v=12345678901");
        assertFalse(added);
    }

    @Test
    void testGetDownloadCoordinatorBeforeAndAfterInit() {
        assertNull(downloadFacade.getDownloadCoordinator());
        downloadFacade.initialize(null, null, null);
        assertNotNull(downloadFacade.getDownloadCoordinator());
    }
}
