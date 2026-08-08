package com.example.interfaz.service.ui;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WindowStageDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowStageDecorator.class);

    private Stage stage;
    private HBox customTitleBar;
    private Node rootNode;
    private FontIcon iconMaximize;

    private double xOffset = 0;
    private double yOffset = 0;
    private double normalX;
    private double normalY;
    private double normalWidth;
    private double normalHeight;
    private boolean isCustomMaximized = false;

    public void attach(Stage stage, HBox customTitleBar, Node rootNode, FontIcon iconMaximize) {
        this.stage = stage;
        this.customTitleBar = customTitleBar;
        this.rootNode = rootNode;
        this.iconMaximize = iconMaximize;

        setupTitleBarDragAndControls();
        setupWindowEdgeResizing();

        LOGGER.info("WindowStageDecorator configurado correctamente en el stage.");
    }

    public void minimize() {
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    public void toggleMaximize() {
        if (stage == null) {
            return;
        }

        if (isCustomMaximized) {
            stage.setX(normalX);
            stage.setY(normalY);
            stage.setWidth(normalWidth);
            stage.setHeight(normalHeight);
            isCustomMaximized = false;
            if (iconMaximize != null) {
                iconMaximize.setIconLiteral("mdi2w-window-maximize");
            }
        } else {
            normalX = stage.getX();
            normalY = stage.getY();
            normalWidth = stage.getWidth();
            normalHeight = stage.getHeight();

            var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1);
            Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
            Rectangle2D bounds = screen.getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            isCustomMaximized = true;

            if (iconMaximize != null) {
                iconMaximize.setIconLiteral("mdi2w-window-restore");
            }
        }
    }

    public void closeWindow() {
        if (stage != null) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        } else {
            Platform.exit();
        }
    }

    private void setupTitleBarDragAndControls() {
        if (customTitleBar == null || stage == null) {
            return;
        }

        customTitleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        customTitleBar.setOnMouseDragged(event -> {
            if (isCustomMaximized) {
                double mouseXRatio = event.getSceneX() / stage.getWidth();
                toggleMaximize();
                xOffset = normalWidth * mouseXRatio;
                yOffset = event.getSceneY();
            }
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        customTitleBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });

        customTitleBar.setOnMouseReleased(event -> {
            var screens = Screen.getScreensForRectangle(event.getScreenX(), event.getScreenY(), 1, 1);
            if (!screens.isEmpty()) {
                Screen screen = screens.get(0);
                if (event.getScreenY() <= screen.getVisualBounds().getMinY() + 5) {
                    if (!isCustomMaximized) {
                        toggleMaximize();
                    }
                }
            }
        });
    }

    private void setupWindowEdgeResizing() {
        if (rootNode == null || stage == null) {
            return;
        }
        final int border = 8;
        rootNode.setOnMouseMoved(event -> {
            if (isCustomMaximized || stage.isMaximized()) {
                rootNode.setCursor(Cursor.DEFAULT);
                return;
            }
            double x = event.getX();
            double y = event.getY();
            double width = rootNode.getBoundsInLocal().getWidth();
            double height = rootNode.getBoundsInLocal().getHeight();

            if (x < border && y < border) {
                rootNode.setCursor(Cursor.NW_RESIZE);
            } else if (x < border && y > height - border) {
                rootNode.setCursor(Cursor.SW_RESIZE);
            } else if (x > width - border && y < border) {
                rootNode.setCursor(Cursor.NE_RESIZE);
            } else if (x > width - border && y > height - border) {
                rootNode.setCursor(Cursor.SE_RESIZE);
            } else if (x < border) {
                rootNode.setCursor(Cursor.W_RESIZE);
            } else if (x > width - border) {
                rootNode.setCursor(Cursor.E_RESIZE);
            } else if (y < border) {
                rootNode.setCursor(Cursor.N_RESIZE);
            } else if (y > height - border) {
                rootNode.setCursor(Cursor.S_RESIZE);
            } else {
                rootNode.setCursor(Cursor.DEFAULT);
            }
        });

        final double[] startMousePos = new double[2];
        final double[] startStagePos = new double[4];

        rootNode.setOnMousePressed(event -> {
            startMousePos[0] = event.getScreenX();
            startMousePos[1] = event.getScreenY();
            startStagePos[0] = stage.getX();
            startStagePos[1] = stage.getY();
            startStagePos[2] = stage.getWidth();
            startStagePos[3] = stage.getHeight();
        });

        rootNode.setOnMouseDragged(event -> {
            if (isCustomMaximized || stage.isMaximized()) {
                return;
            }
            Cursor cursor = rootNode.getCursor();
            if (cursor == Cursor.DEFAULT) {
                return;
            }

            double deltaX = event.getScreenX() - startMousePos[0];
            double deltaY = event.getScreenY() - startMousePos[1];

            if (cursor == Cursor.E_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.SE_RESIZE) {
                stage.setWidth(Math.max(stage.getMinWidth(), startStagePos[2] + deltaX));
            }
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SW_RESIZE || cursor == Cursor.SE_RESIZE) {
                stage.setHeight(Math.max(stage.getMinHeight(), startStagePos[3] + deltaY));
            }
            if (cursor == Cursor.W_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
                double newWidth = Math.max(stage.getMinWidth(), startStagePos[2] - deltaX);
                if (newWidth > stage.getMinWidth()) {
                    stage.setX(startStagePos[0] + deltaX);
                    stage.setWidth(newWidth);
                }
            }
            if (cursor == Cursor.N_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.NE_RESIZE) {
                double newHeight = Math.max(stage.getMinHeight(), startStagePos[3] - deltaY);
                if (newHeight > stage.getMinHeight()) {
                    stage.setY(startStagePos[1] + deltaY);
                    stage.setHeight(newHeight);
                }
            }
        });
    }

    public boolean isCustomMaximized() {
        return isCustomMaximized;
    }
}
