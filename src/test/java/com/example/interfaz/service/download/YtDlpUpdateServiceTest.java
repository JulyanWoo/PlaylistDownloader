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
    void testUpdateYtDlpAsyncExecutesWithoutException() throws Exception {
        YtDlpUpdateService updateService = new YtDlpUpdateService();
        var future = updateService.updateYtDlpAsync(line -> {
            assertNotNull(line);
        });

        Boolean result = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(result);
    }
}
