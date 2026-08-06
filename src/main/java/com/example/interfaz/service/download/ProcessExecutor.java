package com.example.interfaz.service.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessExecutor implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecutor.class);

    private final Object pauseLock = new Object();
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldStop = new AtomicBoolean(false);

    private volatile Process currentProcess;

    public boolean execute(List<String> cmd, Consumer<String> lineProcessor, Consumer<String> logNotifier) throws IOException, InterruptedException {
        if (cmd == null || cmd.isEmpty() || cmd.stream().allMatch(String::isBlank)) {
            throw new IllegalArgumentException("El comando no puede estar vacío");
        }

        synchronized (this) {
            if (shouldStop.get()) {
                notifySafely(logNotifier, "Descarga cancelada por el usuario");
                return false;
            }
            this.shouldStop.set(false);
            this.isPaused.set(false);
        }

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();
            synchronized (this) {
                this.currentProcess = process;
            }

            if (shouldStop.get()) {
                process.destroyForcibly();
                notifySafely(logNotifier, "Descarga cancelada por el usuario");
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && !shouldStop.get()) {
                    handlePauseState();
                    if (shouldStop.get()) {
                        break;
                    }
                    notifySafely(logNotifier, line);
                    notifySafely(lineProcessor, line);
                }
            }

            int exitCode;
            if (process.isAlive()) {
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    LOGGER.warn("El proceso no terminó en el tiempo esperado (10s). Forzando cierre...");
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                exitCode = process.isAlive() ? -1 : process.exitValue();
            } else {
                exitCode = process.exitValue();
            }

            boolean success = exitCode == 0 && !shouldStop.get();

            if (success) {
                notifySafely(logNotifier, "Descarga completada exitosamente");
            } else if (shouldStop.get()) {
                notifySafely(logNotifier, "Descarga cancelada por el usuario");
            } else {
                notifySafely(logNotifier, "Error en la descarga (código: " + exitCode + ")");
            }

            return success;
        } finally {
            resetState(process);
        }
    }

    private void notifySafely(Consumer<String> consumer, String message) {
        if (consumer != null) {
            try {
                consumer.accept(message);
            } catch (Exception e) {
                LOGGER.error("Error al notificar callback de proceso/log: {}", message, e);
            }
        }
    }

    public void pause() {
        if (!isAlive()) {
            return;
        }
        this.isPaused.set(true);
        LOGGER.info("Proceso pausado");
    }

    public void resume() {
        if (!isPaused.get()) {
            return;
        }
        this.isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        LOGGER.info("Proceso reanudado");
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
            try {
                proc.descendants().forEach(ProcessHandle::destroyForcibly);
            } catch (Exception e) {
                LOGGER.warn("Error cancelando subprocesos hijos: {}", e.getMessage());
            }
            proc.destroyForcibly();
            try {
                proc.onExit().get(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                LOGGER.warn("Error esperando cierre de proceso: {}", e.getMessage());
            }
            synchronized (this) {
                if (this.currentProcess == proc) {
                    this.currentProcess = null;
                }
            }
            LOGGER.info("Proceso terminado forzosamente");
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

    private synchronized void resetState(Process process) {
        if (process != null && this.currentProcess == process) {
            this.currentProcess = null;
            this.isPaused.set(false);
            this.shouldStop.set(false);
        }
    }

    synchronized void clearState() {
        this.currentProcess = null;
        this.isPaused.set(false);
        this.shouldStop.set(false);
    }

    @Override
    public void close() {
        stop();
        clearState();
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
}
