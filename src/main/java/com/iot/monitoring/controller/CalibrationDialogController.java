package com.iot.monitoring.controller;

import com.iot.monitoring.service.SerialService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Locale;

/**
 * Controller untuk wizard dialog kalibrasi Load Cell 1 & 2
 */
public class CalibrationDialogController {

    @FXML private RadioButton rbCalibLc1;
    @FXML private RadioButton rbCalibLc2;
    @FXML private ToggleGroup sensorCalibGroup;

    @FXML private Button btnZero;
    @FXML private ProgressIndicator progressZero;
    @FXML private Label lblZeroStatus;

    @FXML private TextField txtKnownWeight;
    @FXML private ComboBox<String> cmbWeightUnit;
    @FXML private Button btnCalibrate;
    @FXML private ProgressIndicator progressCalib;
    @FXML private Label lblCalibResult;

    private SerialService serialService;
    private Stage stage;

    @FXML
    public void initialize() {
        cmbWeightUnit.setItems(FXCollections.observableArrayList("gram (g)", "kg"));
        cmbWeightUnit.setValue("gram (g)");

        sensorCalibGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            resetStepStatus();
        });
    }

    public void setSerialService(SerialService serialService, Stage stage) {
        this.serialService = serialService;
        this.stage = stage;

        // Listener untuk pesan status kalibrasi dari serial
        this.serialService.setOnRawLineReceived(this::onSerialLineReceived);
    }

    private void resetStepStatus() {
        lblZeroStatus.setText("Not zeroed yet");
        lblZeroStatus.setStyle("-fx-text-fill: #90a4ae; -fx-font-weight: bold;");
        progressZero.setVisible(false);

        lblCalibResult.setText("");
        progressCalib.setVisible(false);
    }

    /**
     * Langkah 1: Nolkan sensor (Tare)
     */
    @FXML
    public void handleZeroSensor() {
        if (serialService == null || !serialService.isConnected()) {
            showAlert("Port Disconnected", "Please connect the serial port on the main dashboard first.");
            return;
        }

        progressZero.setVisible(true);
        lblZeroStatus.setText("Setting zero point (Tare)...");
        lblZeroStatus.setStyle("-fx-text-fill: #ff9100; -fx-font-weight: bold;");

        boolean isLc1 = rbCalibLc1.isSelected();
        serialService.sendCommand(isLc1 ? "CAL1_ZERO" : "CAL2_ZERO");
    }

    /**
     * Langkah 2: Hitung & Simpan Kalibrasi dengan Beban Uji
     */
    @FXML
    public void handleCalibrate() {
        if (serialService == null || !serialService.isConnected()) {
            showAlert("Port Disconnected", "Please connect the serial port on the main dashboard first.");
            return;
        }

        String text = txtKnownWeight.getText();
        if (text == null || text.trim().isEmpty()) {
            showAlert("Empty Input", "Please enter reference test load weight.");
            return;
        }

        double val;
        try {
            val = Double.parseDouble(text.replace(',', '.').trim());
            if (val <= 0) {
                showAlert("Invalid Value", "Test load weight must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Format", "Please enter a valid decimal number (e.g. 100 or 500).");
            return;
        }

        // Konversi ke satuan KG untuk firmware Arduino
        double massInKg = cmbWeightUnit.getValue().startsWith("gram") ? (val / 1000.0) : val;

        progressCalib.setVisible(true);
        lblCalibResult.setText("Waiting for mechanical stabilization & saving to EEPROM...");
        lblCalibResult.setStyle("-fx-text-fill: #ff9100; -fx-font-weight: bold;");

        boolean isLc1 = rbCalibLc1.isSelected();
        String cmd = (isLc1 ? "CAL1_LOAD:" : "CAL2_LOAD:") + String.format(Locale.US, "%.4f", massInKg);
        serialService.sendCommand(cmd);
    }

    /**
     * Parsing pesan balasan kalibrasi dari serial
     */
    public void onSerialLineReceived(String line) {
        if (line == null) return;

        Platform.runLater(() -> {
            if (line.contains("[CAL_STATUS] ZERO1_DONE") || line.contains("[CAL_STATUS] ZERO2_DONE")) {
                progressZero.setVisible(false);
                lblZeroStatus.setText("Zero point (Tare) set successfully!");
                lblZeroStatus.setStyle("-fx-text-fill: #00e676; -fx-font-weight: bold;");
            } else if (line.contains("[CAL_STATUS] CAL1_SUCCESS:") || line.contains("[CAL_STATUS] CAL2_SUCCESS:")) {
                progressCalib.setVisible(false);
                String factor = line.substring(line.indexOf("SUCCESS:") + 8).trim();
                lblCalibResult.setText("Calibration Successful!\nFactor: " + factor + " (Saved permanently to Arduino EEPROM)");
                lblCalibResult.setStyle("-fx-text-fill: #00e676; -fx-font-weight: bold;");
            }
        });
    }

    @FXML
    public void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

