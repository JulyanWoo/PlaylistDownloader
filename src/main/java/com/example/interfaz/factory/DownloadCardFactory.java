package com.example.interfaz.factory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.kordamp.ikonli.javafx.FontIcon;

import com.example.interfaz.util.ThumbnailUtils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class DownloadCardFactory {

    public static class ActiveCardElements {
        private Label titleLabel;
        private Label subtitleLabel;
        private ProgressBar progressBar;
        private Label speedLabel;
        private Label etaLabel;

        public Label getTitleLabel() { return titleLabel; }
        public Label getSubtitleLabel() { return subtitleLabel; }
        public ProgressBar getProgressBar() { return progressBar; }
        public Label getSpeedLabel() { return speedLabel; }
        public Label getEtaLabel() { return etaLabel; }
    }

    public static VBox createDownloadingCard(
            String title,
            String subtitle,
            double progress,
            String speed,
            String eta,
            String urlOrTitle,
            boolean isPrioritized,
            Runnable onPause,
            Runnable onCancel,
            ActiveCardElements elements
    ) {
        VBox card = new VBox(10);
        card.getStyleClass().add("download-card");
        if (isPrioritized) {
            card.getStyleClass().add("priority-active-card");
        } else {
            card.getStyleClass().add("active-card");
        }

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane artBox = createCardCoverBox(urlOrTitle != null ? urlOrTitle : title, 50, "mdi2m-music", "#6b7280");

        VBox titleBox = new VBox(3);
        elements.titleLabel = new Label(title);
        elements.titleLabel.getStyleClass().add("card-title-text");

        elements.subtitleLabel = new Label(subtitle);
        elements.subtitleLabel.getStyleClass().add("card-subtitle-text");
        titleBox.getChildren().addAll(elements.titleLabel, elements.subtitleLabel);

        if (isPrioritized) {
            Label priorityBadge = new Label("⚡ Prioritaria");
            priorityBadge.getStyleClass().add("badge-priority");
            titleBox.getChildren().add(priorityBadge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(artBox, titleBox, spacer);

        elements.progressBar = new ProgressBar(progress);
        elements.progressBar.setMaxWidth(Double.MAX_VALUE);
        elements.progressBar.setPrefHeight(20);
        elements.progressBar.getStyleClass().add("card-progress-bar");

        HBox metaRow = new HBox();
        metaRow.setAlignment(Pos.CENTER_LEFT);

        elements.speedLabel = new Label(String.format("%.0f%%", progress * 100));
        elements.speedLabel.getStyleClass().add("card-progress-text");

        elements.etaLabel = new Label();
        elements.etaLabel.setVisible(false);
        elements.etaLabel.setManaged(false);

        metaRow.getChildren().add(elements.speedLabel);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button cardPauseBtn = new Button("Pausar");
        cardPauseBtn.getStyleClass().add("btn-card-small");
        FontIcon pauseIcon = new FontIcon("mdi2p-pause");
        pauseIcon.setIconSize(12);
        cardPauseBtn.setGraphic(pauseIcon);
        if (onPause != null) {
            cardPauseBtn.setOnAction(e -> onPause.run());
        }

        Button cardCancelBtn = new Button("Cancelar");
        cardCancelBtn.getStyleClass().add("btn-card-cancel");
        Label cancelIcon = new Label("✕");
        cancelIcon.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: bold;");
        cardCancelBtn.setGraphic(cancelIcon);
        if (onCancel != null) {
            cardCancelBtn.setOnAction(e -> onCancel.run());
        }

        actionRow.getChildren().addAll(cardPauseBtn, cardCancelBtn);
        card.getChildren().addAll(topRow, elements.progressBar, metaRow, actionRow);

        return card;
    }

    public static VBox createWaitingCard(
            String title,
            String subtitle,
            String urlOrTitle,
            java.util.function.BiConsumer<VBox, String> onPrioritize
    ) {
        VBox card = new VBox(8);
        card.getStyleClass().add("download-card");
        card.setUserData(urlOrTitle);

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane artBox = createCardCoverBox(urlOrTitle != null ? urlOrTitle : title, 44, "mdi2p-playlist-music", "#6b7280");

        VBox titleBox = new VBox(3);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title-text");

        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.getStyleClass().add("card-subtitle-text");

        Label statusBadge = new Label("En cola");
        statusBadge.getStyleClass().add("badge-in-queue");

        titleBox.getChildren().addAll(titleLbl, subtitleLbl, statusBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cardPrioritizeBtn = new Button();
        cardPrioritizeBtn.getStyleClass().add("btn-card-small");
        cardPrioritizeBtn.setStyle("-fx-padding: 5 8;");
        cardPrioritizeBtn.setTooltip(new Tooltip("Priorizar descarga"));
        FontIcon swapIcon = new FontIcon("mdi2s-swap-vertical");
        swapIcon.setIconSize(14);
        cardPrioritizeBtn.setGraphic(swapIcon);
        if (onPrioritize != null) {
            cardPrioritizeBtn.setOnAction(e -> onPrioritize.accept(card, urlOrTitle));
        }

        topRow.getChildren().addAll(artBox, titleBox, spacer, cardPrioritizeBtn);
        card.getChildren().add(topRow);

        return card;
    }

    public static VBox createCompletedCard(String title, String urlOrTitle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("download-card");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane artBox = createCardCoverBox(urlOrTitle != null ? urlOrTitle : title, 44, "mdi2c-check-circle", "#10b981");

        VBox titleBox = new VBox(3);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-title-text");

        Label statusLbl = new Label("Descarga exitosa");
        statusLbl.getStyleClass().add("badge-completed");

        titleBox.getChildren().addAll(titleLbl, statusLbl);

        topRow.getChildren().addAll(artBox, titleBox);
        card.getChildren().add(topRow);

        return card;
    }

    public static StackPane createCardCoverBox(String urlOrTitle, double size, String defaultIconLiteral, String iconColorHex) {
        StackPane artBox = new StackPane();
        artBox.setPrefSize(size, size);
        artBox.setMinSize(size, size);
        artBox.setMaxSize(size, size);
        artBox.getStyleClass().add("card-cover-art");

        FontIcon defaultIcon = new FontIcon(defaultIconLiteral);
        defaultIcon.setIconSize((int) (size * 0.48));
        if (iconColorHex != null) {
            defaultIcon.setStyle("-fx-icon-color: " + iconColorHex + ";");
        }
        artBox.getChildren().add(defaultIcon);

        if (urlOrTitle == null || urlOrTitle.isBlank()) {
            return artBox;
        }

        String videoId = ThumbnailUtils.extractVideoId(urlOrTitle);
        if (videoId != null) {
            setSingleThumbnail(artBox, videoId, size, defaultIcon);
        } else if (ThumbnailUtils.isPlaylistUrl(urlOrTitle)) {
            String ytDlpPath = new com.example.interfaz.service.download.BinaryResolver().resolveYtDlpPath();
            CompletableFuture.supplyAsync(() -> ThumbnailUtils.fetchPlaylistVideoIds(urlOrTitle, ytDlpPath))
                    .thenAccept(videoIds -> {
                        if (!videoIds.isEmpty()) {
                            Platform.runLater(() -> setPlaylistCollage(artBox, videoIds, size, defaultIcon));
                        }
                    });
        }

        return artBox;
    }

    private static void setSingleThumbnail(StackPane artBox, String videoId, double size, FontIcon fallback) {
        String thumbUrl = ThumbnailUtils.getThumbnailUrl(videoId);
        ImageView imageView = new ImageView();
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imageView.setClip(clip);

        Image image = new Image(thumbUrl, size * 2, size * 2, true, true, true);
        image.errorProperty().addListener((obs, oldVal, isErr) -> {
            if (isErr) {
                Platform.runLater(() -> artBox.getChildren().setAll(fallback));
            }
        });

        imageView.setImage(image);
        artBox.getChildren().setAll(imageView);
    }

    private static void setPlaylistCollage(StackPane artBox, List<String> videoIds, double size, FontIcon fallback) {
        if (videoIds.isEmpty()) {
            return;
        }

        if (videoIds.size() == 1) {
            setSingleThumbnail(artBox, videoIds.get(0), size, fallback);
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPrefSize(size, size);

        double tileSize = (size - 2) / 2.0;
        int maxTiles = Math.min(videoIds.size(), 4);

        for (int i = 0; i < maxTiles; i++) {
            String vid = videoIds.get(i);
            String url = ThumbnailUtils.getThumbnailUrl(vid);

            ImageView tileView = new ImageView();
            tileView.setFitWidth(tileSize);
            tileView.setFitHeight(tileSize);
            tileView.setPreserveRatio(false);

            Image img = new Image(url, tileSize * 2, tileSize * 2, true, true, true);
            tileView.setImage(img);

            int col = i % 2;
            int row = i / 2;
            grid.add(tileView, col, row);
        }

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        grid.setClip(clip);

        artBox.getChildren().setAll(grid);
    }
}
