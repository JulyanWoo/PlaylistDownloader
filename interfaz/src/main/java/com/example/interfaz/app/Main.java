package com.example.interfaz.app;

import com.example.interfaz.service.LogService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de la aplicación - Punto de entrada
 */
public class Main extends Application {
    
    private static final String MAIN_VIEW_FXML = "/main-view.fxml";
    private static final String APP_TITLE = "YouTube Downloader - Descargador de Música";
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    @Override
    public void start(Stage stage) throws IOException {
        try {
            LogService.getInstance();
            LogService.log("Aplicación YouTube Downloader iniciada");
            
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(MAIN_VIEW_FXML));
            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
            
            com.example.interfaz.controller.MainController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.setStage(stage);
            }
            
            stage.setTitle(APP_TITLE);
            stage.setScene(scene);
            stage.setMinWidth(600);
            stage.setMinHeight(400);
            stage.show();
            
            stage.setOnCloseRequest(event -> {
                LogService.log("Aplicación cerrada por el usuario");
                LogService.getInstance().stopCapturing();
                System.exit(0);
            });
            
            LogService.log("Interfaz gráfica cargada correctamente");
            
        } catch (IOException e) {
            System.err.println("Error al cargar la interfaz: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Método principal de entrada de la aplicación
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Lanzar la aplicación JavaFX
        launch(args);
    }
    
    /**
     * Método para obtener la instancia de la aplicación
     * @return instancia de Main
     */
    public static Main getInstance() {
        return new Main();
    }
}