package com.example.interfaz.service.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WindowManagerTest {

    private WindowManager windowManager;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        windowManager = new WindowManager();
    }

    @Test
    void testStageDecoratorNotNull() {
        assertNotNull(windowManager.getStageDecorator(), "WindowStageDecorator should be initialized");
        assertNull(windowManager.getPrimaryStage(), "PrimaryStage should initially be null");
    }
}
