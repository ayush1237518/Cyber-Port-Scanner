package com.portscanner.ui;

import com.portscanner.utils.AppLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX application entry point. Responsible only for bootstrapping the
 * primary stage; all interactive behaviour lives in {@link ScannerController}.
 */
public class MainApp extends Application {

    private static final String WINDOW_TITLE = "Network Port Scanner — Cybersecurity Toolkit";
    private static final double WINDOW_WIDTH = 1180;
    private static final double WINDOW_HEIGHT = 780;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/dark-theme.css")).toExternalForm());

            primaryStage.setTitle(WINDOW_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);

            ScannerController controller = loader.getController();
            primaryStage.setOnCloseRequest(event -> controller.shutdown());

            primaryStage.show();
            AppLogger.info("Network Port Scanner GUI started.");

        } catch (IOException ex) {
            AppLogger.error("Failed to load application UI.", ex);
            throw new IllegalStateException("Could not start application", ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
