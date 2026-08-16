package com.example.interfaz.service.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a child process (yt-dlp) with:
 * <ul>
 * <li>Pause / resume / stop lifecycle.</li>
 * <li>Inactivity watchdog: if no stdout line arrives for
 * {@value #INACTIVITY_TIMEOUT_SECONDS} seconds the process is forcibly killed
 * and the download is reported as failed — preventing thread freeze.</li>
 * </ul>
 */
public class ProcessExecutor implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    /**
     * Kill unresponsive yt-dlp after this many seconds of silence.
     */
    private static final int INACTIVITY_TIMEOUT_SECONDS = 45;

    private final Object pauseLock = new Object();
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    /**
     * Epoch-ms of the last line received from stdout.
     */
    private final AtomicLong lastActivityMs = new AtomicLong(0);

    private volatile Process currentProcess;

    public boolean execute(List<String> cmd,
            Consumer<String> lineProcessor,
            Consumer<String> logNotifier) throws IOException, InterruptedException {

        if (cmd == null || cmd.isEmpty() || cmd.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException("Command cannot be empty");
        }

        synchronized (this) {
            // Always reset flags — a new execute() call means a new download.
            // The old stop() may have set shouldStop=true and then nulled currentProcess,
            // preventing resetState() from clearing it (race condition).
            this.shouldStop.set(false);
            this.isPaused.set(false);
        }

        Process process = null;
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "ProcessWatchdog");
                    t.setDaemon(true);
                    return t;
                });
        ScheduledFuture<?> watchdogFuture = null;

        try {
            LOGGER.info("[DIAG] Executing command: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.redirectErrorStream(true);

            process = pb.start();
            LOGGER.info("[DIAG] Process started (pid={})", process.pid());
            synchronized (this) {
                this.currentProcess = process;
            }

            if (shouldStop.get()) {
                process.destroyForcibly();
                notifySafely(logNotifier, "Download cancelled by user");
                return false;
            }

            lastActivityMs.set(System.currentTimeMillis());
            final Process procRef = process;
            watchdogFuture = watchdog.scheduleAtFixedRate(() -> {
                long silence = System.currentTimeMillis() - lastActivityMs.get();
                if (silence > INACTIVITY_TIMEOUT_SECONDS * 1000L && procRef.isAlive()) {
                    LOGGER.warn("yt-dlp silent for {}s — forcing termination", silence / 1000);
                    shouldStop.set(true);
                    procRef.descendants().forEach(ProcessHandle::destroyForcibly);
                    procRef.destroyForcibly();
                    synchronized (pauseLock) {
                        pauseLock.notifyAll();
                    }
                }
            }, INACTIVITY_TIMEOUT_SECONDS, INACTIVITY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null && !shouldStop.get()) {
                    lineCount++;
                    lastActivityMs.set(System.currentTimeMillis());
                    LOGGER.debug("[DIAG] stdout[{}]: {}", lineCount, line);
                    handlePauseState();
                    if (shouldStop.get()) {
                        break;
                    }
                    notifySafely(logNotifier, line);
                    notifySafely(lineProcessor, line);
                }
                LOGGER.info("[DIAG] Read loop ended. Lines read: {}, shouldStop={}", lineCount, shouldStop.get());
            }

            int exitCode;
            if (process.isAlive()) {
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    LOGGER.warn("[DIAG] Process did not exit in 10 s — forcing kill");
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                exitCode = process.isAlive() ? -1 : process.exitValue();
            } else {
                exitCode = process.exitValue();
            }

            boolean success = (exitCode == 0) && !shouldStop.get();

            LOGGER.info("[DIAG] Process finished. exitCode={}, shouldStop={}, success={}",
                    exitCode, shouldStop.get(), success);

            if (success) {
                notifySafely(logNotifier, "Download completed successfully");
            } else if (shouldStop.get()) {
                notifySafely(logNotifier, "Download cancelled or timed out (exitCode=" + exitCode + ")");
            } else {
                notifySafely(logNotifier, "Download error (exit code: " + exitCode + ")");
            }

            return success;

        } finally {
            if (watchdogFuture != null) {
                watchdogFuture.cancel(false);
            }
            watchdog.shutdownNow();
            resetState(process);
        }
    }

    public void pause() {
        if (!isAlive()) {
            return;
        }
        isPaused.set(true);
        LOGGER.info("Process paused");
    }

    public void resume() {
        if (!isPaused.get()) {
            return;
        }
        isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        LOGGER.info("Process resumed");
    }

    public void stop() {
        Process proc;
        synchronized (this) {
            proc = this.currentProcess;
            if (proc == null) {
                return;
            }
            this.shouldStop.set(true);
            this.isPaused.set(false);
        }

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        if (proc.isAlive()) {
            killProcessTree(proc);
            try {
                proc.onExit().get(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.warn("Error waiting for process shutdown: {}", e.getMessage());
            }
            synchronized (this) {
                if (this.currentProcess == proc) {
                    this.currentProcess = null;
                }
            }
        }
        LOGGER.info("Process stopped");
    }

    public static void killProcessTree(Process proc) {
        if (proc == null) {
            return;
        }
        long pid = proc.pid();
        try {
            proc.descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (Exception ignored) {
        }
        try {
            proc.destroyForcibly();
        } catch (Exception ignored) {
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win") && pid > 0) {
            try {
                new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isAlive() {
        Process proc = this.currentProcess;
        return proc != null && proc.isAlive();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public boolean isStopping() {
        return shouldStop.get();
    }

    @Override
    public void close() {
        stop();
        clearState();
    }

    private void notifySafely(Consumer<String> consumer, String message) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.accept(message);
        } catch (Exception e) {
            LOGGER.error("Error in progress callback: {}", message, e);
        }
    }

    private void handlePauseState() {
        synchronized (pauseLock) {
            while (isPaused.get() && !shouldStop.get()) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    shouldStop.set(true);
                    break;
                }
            }
        }
    }

    private synchronized void resetState(Process process) {
        if (process != null && this.currentProcess == process) {
            this.currentProcess = null;
        }
        // Always clear flags — even if stop() already nulled currentProcess
        this.isPaused.set(false);
        this.shouldStop.set(false);
    }

    synchronized void clearState() {
        this.currentProcess = null;
        this.isPaused.set(false);
        this.shouldStop.set(false);
    }
}
