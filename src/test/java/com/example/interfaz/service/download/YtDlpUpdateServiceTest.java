package com.example.interfaz.service.download;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YtDlpUpdateServiceTest {

    @Test
    void testGetCurrentVersionReturnsNonNull() {
        YtDlpUpdateService updateService = new YtDlpUpdateService();
        String version = updateService.getCurrentVersion();
        assertNotNull(version, "La versión no debe ser nula");
        assertFalse(version.isBlank(), "La versión no debe estar vacía");
    }

    @Test
    void testUpdateYtDlpAsyncBlocksWhenDownloadActive() throws Exception {
        YtDlpUpdateService updateService = new YtDlpUpdateService();
        updateService.setActiveDownloadChecker(() -> true);

        StringBuilder log = new StringBuilder();
        var future = updateService.updateYtDlpAsync(log::append);

        Boolean result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertFalse(result, "La actualización debe fallar / bloquearse cuando hay descargas activas");
        assertTrue(log.toString().contains("descargas activas"), "El mensaje de log debe indicar descargas activas");
    }

    @Test
    void testUpdateYtDlpAsyncExecutesWithoutException() throws Exception {
        YtDlpUpdateService updateService = new YtDlpUpdateService();
        updateService.setActiveDownloadChecker(() -> false);
        var future = updateService.updateYtDlpAsync(line -> {
            assertNotNull(line);
        });

        Boolean result = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result);
    }
}
