package com.example.interfaz.service.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeService.class);

    private boolean isDarkMode = true;

    public void applySavedTheme() {
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }

    public void toggleTheme() {
        toggleTheme(null);
    }

    public void toggleTheme(FontIcon themeIcon) {
        isDarkMode = !isDarkMode;
        applySavedTheme();
        if (themeIcon != null) {
            themeIcon.setIconLiteral(isDarkMode ? "mdi2m-moon-waning-crescent" : "mdi2w-weather-sunny");
        }
        LOGGER.info("Cambiado a Modo {}", isDarkMode ? "Oscuro (Primer Dark)" : "Claro (Primer Light)");
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }
}
