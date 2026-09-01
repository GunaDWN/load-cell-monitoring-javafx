package com.iot.monitoring;

import com.iot.monitoring.controller.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main Application JavaFX
 */
public class MainApp extends Application {

    private DashboardController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/iot/monitoring/view/dashboard.fxml"));
            Parent root = loader.load();
            controller = loader.getController();

            Scene scene = new Scene(root, 1200, 850);

            primaryStage.setTitle("IoT Sensor Monitoring Dashboard (2x Load Cell & 2x Termokopel MAX6675)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(650);

            // Bersihkan koneksi serial saat jendela aplikasi ditutup
            primaryStage.setOnCloseRequest(event -> {
                if (controller != null) {
                    controller.shutdown();
                }
                System.exit(0);
            });

            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Gagal memuat file FXML antarmuka dashboard: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

