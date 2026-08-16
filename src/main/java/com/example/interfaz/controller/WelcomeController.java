package com.example.interfaz.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.interfaz.factory.ServiceFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;

@SuppressWarnings({"unused", "FXML"})
public class WelcomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeController.class);

    @FXML private TextField welcomeInputField;
    @FXML private Button welcomeAddButton;
    @FXML private Button welcomePasteButton;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        LOGGER.info("WelcomeController inicializado.");
    }

    @FXML
    void onWelcomeAddToQueue() {
        if (welcomeInputField == null) return;
        String input = welcomeInputField.getText().trim();
        if (input.isEmpty()) {
            ServiceFactory.getInstance().getDialogService().showWarning("URL Vacía", "Por favor introduce una URL válida de YouTube.");
            return;
        }

        String normalizedInput = input.replaceAll("(?i)(https?://)", " $1").trim();
        String[] urls = normalizedInput.split("[\\s,]+");
        var downloadFacade = ServiceFactory.getInstance().getMainDownloadFacade();
        boolean addedAny = false;

        for (String urlStr : urls) {
            String url = urlStr.trim();
            if (url.isEmpty()) continue;
            if (downloadFacade.addToQueue(url)) {
                addedAny = true;
                if (mainController != null && mainController.getProgressController() != null) {
                    mainController.getProgressController().addWaitingCard(url, "En cola");
                }
            }
        }

        if (addedAny) {
            if (mainController != null) {
                mainController.onNavDownloads();
            }
            welcomeInputField.clear();
        } else {
            ServiceFactory.getInstance().getDialogService().showError("Error al encolar", "No se pudo encolar la descarga. Verifica la URL o la configuración.");
        }
    }

    @FXML
    void onWelcomePaste() {
        try {
            String clipboard = Clipboard.getSystemClipboard().getString();
            if (clipboard != null && !clipboard.isBlank()) {
                if (welcomeInputField != null) {
                    welcomeInputField.setText(clipboard.trim());
                }
            } else {
                ServiceFactory.getInstance().getDialogService().showWarning("Portapapeles Vacío", "No hay texto plano en el portapapeles.");
            }
        } catch (Exception e) {
            LOGGER.error("Error al acceder al portapapeles", e);
        }
    }

    @FXML
    void onWelcomePasteAndAdd() {
        onWelcomePaste();
        onWelcomeAddToQueue();
    }
}
