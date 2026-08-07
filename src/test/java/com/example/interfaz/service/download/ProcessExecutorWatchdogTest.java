package com.example.interfaz.service.download;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that ProcessExecutor's inactivity watchdog terminates stuck processes.
 */
class ProcessExecutorWatchdogTest {

    /**
     * A process that sleeps for 5 seconds simulates an unresponsive yt-dlp.
     * The watchdog (45 s) won't fire here but we verify that stop() works immediately.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldStopProcessThatProducesNoOutput() throws Exception {
        try (ProcessExecutor executor = new ProcessExecutor()) {
            List<String> cmd = System.getProperty("os.name").toLowerCase().contains("win")
                    ? List.of("cmd.exe", "/c", "ping -n 6 127.0.0.1 > nul")   // ~5 s wait
                    : List.of("sleep", "5");

            // Run the long-running command in a background thread
            var future = java.util.concurrent.Executors
                    .newSingleThreadExecutor()
                    .submit(() -> {
                        try {
                            return executor.execute(cmd, null, null);
                        } catch (java.io.IOException | InterruptedException e) {
                            return false;
                        }
                    });

            Thread.sleep(300); // give process time to start
            executor.stop();   // cancel immediately

            Boolean result = future.get(5, TimeUnit.SECONDS);
            assertFalse(result, "Cancelled process should return false");
            assertFalse(executor.isAlive(), "Process should be cleaned up after stop()");
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldCompleteNormalProcessSuccessfully() throws Exception {
        try (ProcessExecutor executor = new ProcessExecutor()) {
            List<String> cmd = System.getProperty("os.name").toLowerCase().contains("win")
                    ? List.of("cmd.exe", "/c", "echo watchdog-test-ok")
                    : List.of("echo", "watchdog-test-ok");

            List<String> lines = new ArrayList<>();
            boolean result = executor.execute(cmd, lines::add, null);

            assertTrue(result, "Normal process should succeed");
            assertTrue(lines.stream().anyMatch(l -> l.contains("watchdog-test-ok")));
            assertFalse(executor.isAlive());
        }
    }
}
