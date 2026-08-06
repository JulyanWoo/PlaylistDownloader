package com.example.interfaz.service.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavigationServiceTest {

    @Test
    void testNavigationServiceInstantiation() {
        NavigationService service = new NavigationService();
        assertNotNull(service);
    }
}
