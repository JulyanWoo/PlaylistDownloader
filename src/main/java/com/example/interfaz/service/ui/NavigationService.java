package com.example.interfaz.service.ui;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class NavigationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationService.class);

    private StackPane contentArea;
    private Button currentActiveButton;
    private Node downloadView;
    private Node libraryAnalyzerView;
    private Node logsView;

    public void initialize(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void showDownloadView() {
        if (downloadView == null) {
            downloadView = loadView("/download-view.fxml");
        }
        setContent(downloadView);
    }

    public void showLibraryAnalyzerView() {
        if (libraryAnalyzerView == null) {
            libraryAnalyzerView = loadView("/library-analyzer-view.fxml");
        }
        setContent(libraryAnalyzerView);
    }

    public void showLogsView() {
        if (logsView == null) {
            logsView = loadView("/logs-view.fxml");
        }
        setContent(logsView);
    }

    private void setContent(Node node) {
        if (contentArea != null && node != null) {
            contentArea.getChildren().setAll(node);
        }
    }

    private Node loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            return loader.load();
        } catch (IOException e) {
            LOGGER.error("Error al cargar la vista FXML: {}", fxmlPath, e);
            return null;
        }
    }

    public void navigateTo(Button navButton, Node targetView, Node... otherViews) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
        }

        if (navButton != null) {
            if (!navButton.getStyleClass().contains("active")) {
                navButton.getStyleClass().add("active");
            }
            currentActiveButton = navButton;
        }

        if (targetView != null) {
            targetView.setVisible(true);
            targetView.setManaged(true);
        }

        if (otherViews != null) {
            for (Node other : otherViews) {
                if (other != null && other != targetView) {
                    other.setVisible(false);
                    other.setManaged(false);
                }
            }
        }
    }
}
