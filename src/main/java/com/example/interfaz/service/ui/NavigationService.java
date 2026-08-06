package com.example.interfaz.service.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;

public class NavigationService {

    private Button currentActiveButton;

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
