package com.example.interfaz.service.download;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessExecutorTest {

    @Test
    void testProcessExecutorExecutionAndStateReset() throws Exception {
        ProcessExecutor executor = new ProcessExecutor();
        assertFalse(executor.isAlive());
        assertFalse(executor.isStopping());

        List<String> outputLines = new ArrayList<>();
        boolean result;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            result = executor.execute(List.of("cmd.exe", "/c", "echo test output"), outputLines::add, null);
        } else {
            result = executor.execute(List.of("echo", "test output"), outputLines::add, null);
        }

        assertTrue(result);
        assertFalse(executor.isAlive(), "ProcessExecutor debe limpiar currentProcess y retornar false para isAlive() tras completar");
        assertFalse(outputLines.isEmpty());
        assertTrue(outputLines.get(0).contains("test output"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnEmptyCommand() {
        ProcessExecutor executor = new ProcessExecutor();
        assertThrows(IllegalArgumentException.class, () -> executor.execute(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(List.of(""), null, null));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(List.of("   "), null, null));
    }

    @Test
    void shouldBeResilientToConsumerExceptions() {
        ProcessExecutor executor = new ProcessExecutor();
        List<String> cmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? List.of("cmd.exe", "/c", "echo resilient test")
                : List.of("echo", "resilient test");

        assertDoesNotThrow(() -> {
            boolean success = executor.execute(cmd, line -> {
                throw new RuntimeException("Consumer Error Simulation");
            }, null);
            assertTrue(success, "La ejecución debe completar exitosamente a pesar de fallas en el consumer de líneas");
        });
    }

    @Test
    void testAutoCloseableAndClearState() {
        try (ProcessExecutor executor = new ProcessExecutor()) {
            executor.stop();
            assertFalse(executor.isStopping(), "stop() sin un proceso activo debe retornar sin activar isStopping");
            executor.clearState();
            assertFalse(executor.isStopping());
            assertFalse(executor.isAlive());
        }
    }
}
