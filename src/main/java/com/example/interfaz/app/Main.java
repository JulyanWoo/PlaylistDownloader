package com.example.interfaz.app;

import atlantafx.base.theme.PrimerDark;
import com.example.interfaz.factory.ServiceFactory;
import com.example.interfaz.service.LogService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    private static final String MAIN_VIEW_FXML = "/main-view.fxml";
    private static final String APP_TITLE = "Playlist Downloader";
    private static final int WINDOW_WIDTH = 920;
    private static final int WINDOW_HEIGHT = 650;

    @Override
    public void start(Stage stage) throws IOException {
        try {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

            LogService.getInstance();
            LogService.log("Aplicación Playlist Downloader iniciada con tema AtlantaFX Primer Dark");

            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(MAIN_VIEW_FXML));
            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);

            com.example.interfaz.controller.MainController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }

            stage.setTitle(APP_TITLE);
            try {
                stage.getIcons().add(new javafx.scene.image.Image(Main.class.getResourceAsStream("/app.png")));
            } catch (Exception e) {
                System.err.println("No se pudo cargar el icono del stage: " + e.getMessage());
            }
            stage.setScene(scene);
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            stage.show();

            stage.setOnCloseRequest(event -> {
                LogService.log("Aplicación cerrada por el usuario");
                ServiceFactory.getInstance().shutdown();
                LogService.getInstance().stopCapturing();
                System.exit(0);
            });

            LogService.log("Interfaz gráfica cargada correctamente");

        } catch (IOException e) {
            System.err.println("Error al cargar la interfaz: " + e.getMessage());
            LogService.log("Error crítico al cargar interfaz: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void stop() {
        ServiceFactory.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Main getInstance() {
        return new Main();
    }
}
