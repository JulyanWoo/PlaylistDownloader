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

    public void toggleTheme(FontIcon themeIcon) {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            if (themeIcon != null) {
                themeIcon.setIconLiteral("mdi2m-moon-waning-crescent");
            }
            LOGGER.info("Cambiado a Modo Oscuro (Primer Dark)");
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            if (themeIcon != null) {
                themeIcon.setIconLiteral("mdi2w-weather-sunny");
            }
            LOGGER.info("Cambiado a Modo Claro (Primer Light)");
        }
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }
}
