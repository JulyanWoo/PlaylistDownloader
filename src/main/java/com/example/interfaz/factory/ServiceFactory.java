package com.example.interfaz.factory;

import com.example.interfaz.controller.ProgressManager;
import com.example.interfaz.download.QueueManager;
import com.example.interfaz.event.EventBus;
import com.example.interfaz.event.EventPublisher;
import com.example.interfaz.service.DownloadService;
import com.example.interfaz.service.FilterService;
import com.example.interfaz.service.SongFilterService;
import com.example.interfaz.service.YouTubeDownloadService;
import com.example.interfaz.service.analyzer.DuplicateDetectionService;
import com.example.interfaz.service.analyzer.LibraryAnalyzerService;
import com.example.interfaz.service.analyzer.SongMetadataReader;
import com.example.interfaz.service.analyzer.SongNameNormalizer;
import com.example.interfaz.service.config.MusicFolderService;
import com.example.interfaz.service.download.DownloadCoordinator;
import com.example.interfaz.service.download.DownloadProgressParser;
import com.example.interfaz.service.ui.DialogService;
import com.example.interfaz.service.ui.FolderChooserService;
import com.example.interfaz.service.ui.NavigationService;
import com.example.interfaz.service.ui.ThemeService;
import com.example.interfaz.service.ui.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);
    private static ServiceFactory instance;

    private FilterService filterService;
    private DownloadService downloadService;
    private EventPublisher eventPublisher;

    private NavigationService navigationService;
    private ThemeService themeService;
    private DialogService dialogService;
    private WindowManager windowManager;
    private FolderChooserService folderChooserService;
    private MusicFolderService musicFolderService;
    private DownloadProgressParser downloadProgressParser;

    private SongNameNormalizer songNameNormalizer;
    private SongMetadataReader songMetadataReader;
    private DuplicateDetectionService duplicateDetectionService;
    private LibraryAnalyzerService libraryAnalyzerService;
    private com.example.interfaz.service.download.YtDlpUpdateService ytDlpUpdateService;

    public ServiceFactory() {
    }

    public ServiceFactory(FilterService filterService, DownloadService downloadService, EventPublisher eventPublisher) {
        this.filterService = filterService;
        this.downloadService = downloadService;
        this.eventPublisher = eventPublisher;
    }

    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }

    public FilterService getFilterService() {
        if (filterService == null) {
            filterService = new SongFilterService();
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
            eventPublisher = new EventBus();
        }
        return eventPublisher;
    }

    public NavigationService getNavigationService() {
        if (navigationService == null) {
            navigationService = new NavigationService();
        }
        return navigationService;
    }

    public ThemeService getThemeService() {
        if (themeService == null) {
            themeService = new ThemeService();
        }
        return themeService;
    }

    public DialogService getDialogService() {
        if (dialogService == null) {
            dialogService = new DialogService();
        }
        return dialogService;
    }

    public WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = new WindowManager(getDialogService());
        }
        return windowManager;
    }

    public MusicFolderService getMusicFolderService() {
        if (musicFolderService == null) {
            musicFolderService = new MusicFolderService();
        }
        return musicFolderService;
    }

    public FolderChooserService getFolderChooserService() {
        if (folderChooserService == null) {
            folderChooserService = new FolderChooserService(getMusicFolderService(), getDialogService());
        }
        return folderChooserService;
    }

    public DownloadProgressParser getDownloadProgressParser() {
        if (downloadProgressParser == null) {
            downloadProgressParser = new DownloadProgressParser();
        }
        return downloadProgressParser;
    }

    private QueueManager queueManager;

    public synchronized QueueManager getQueueManager() {
        if (queueManager == null) {
            queueManager = new QueueManager();
        }
        return queueManager;
    }

    public QueueManager createQueueManager() {
        return getQueueManager();
    }

    public ProgressManager createProgressManager() {
        return new ProgressManager();
    }

    private DownloadCoordinator downloadCoordinator;

    public void registerDownloadCoordinator(DownloadCoordinator coordinator) {
        this.downloadCoordinator = coordinator;
    }

    public SongNameNormalizer getSongNameNormalizer() {
        if (songNameNormalizer == null) {
            songNameNormalizer = new SongNameNormalizer();
        }
        return songNameNormalizer;
    }

    public SongMetadataReader getSongMetadataReader() {
        if (songMetadataReader == null) {
            songMetadataReader = new SongMetadataReader(getSongNameNormalizer());
        }
        return songMetadataReader;
    }

    public DuplicateDetectionService getDuplicateDetectionService() {
        if (duplicateDetectionService == null) {
            duplicateDetectionService = new DuplicateDetectionService();
        }
        return duplicateDetectionService;
    }

    public com.example.interfaz.service.download.YtDlpUpdateService getYtDlpUpdateService() {
        if (ytDlpUpdateService == null) {
            ytDlpUpdateService = new com.example.interfaz.service.download.YtDlpUpdateService();
        }
        return ytDlpUpdateService;
    }

    public LibraryAnalyzerService getLibraryAnalyzerService() {
        if (libraryAnalyzerService == null) {
            libraryAnalyzerService = new LibraryAnalyzerService(
                    getMusicFolderService(),
                    getSongMetadataReader(),
                    getDuplicateDetectionService(),
                    getEventPublisher()
            );
        }
        return libraryAnalyzerService;
    }

    public synchronized void shutdown() {
        if (libraryAnalyzerService != null) {
            try {
                libraryAnalyzerService.close();
                LOGGER.info("LibraryAnalyzerService liberado correctamente en ServiceFactory.shutdown()");
            } catch (Exception e) {
                LOGGER.error("Error al cerrar LibraryAnalyzerService en shutdown", e);
            } finally {
                libraryAnalyzerService = null;
            }
        }

        if (downloadCoordinator != null) {
            try {
                downloadCoordinator.close();
                LOGGER.info("DownloadCoordinator liberado correctamente en ServiceFactory.shutdown()");
            } catch (Exception e) {
                LOGGER.error("Error al cerrar DownloadCoordinator en shutdown", e);
            } finally {
                downloadCoordinator = null;
            }
        }

        if (downloadService != null) {
            try {
                downloadService.close();
                LOGGER.info("DownloadService liberado correctamente vía ServiceFactory.shutdown()");
            } catch (Exception e) {
                LOGGER.error("Error al cerrar DownloadService en shutdown", e);
            } finally {
                downloadService = null;
            }
        }
        downloadService = null;
        eventPublisher = null;
        filterService = null;
        navigationService = null;
        themeService = null;
        dialogService = null;
        windowManager = null;
        folderChooserService = null;
        musicFolderService = null;
        downloadProgressParser = null;
        songNameNormalizer = null;
        songMetadataReader = null;
        duplicateDetectionService = null;
        libraryAnalyzerService = null;
        ytDlpUpdateService = null;
    }

    public static synchronized void reset() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
