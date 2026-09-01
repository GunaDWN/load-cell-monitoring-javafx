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
        lblZeroStatus.setText("⚪ Belum dinolkan");
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
            showAlert("Port Belum Terhubung", "Silakan hubungkan koneksi serial di dashboard utama terlebih dahulu.");
            return;
        }

        progressZero.setVisible(true);
        lblZeroStatus.setText("⏳ Sedang menyetel titik nol...");
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
            showAlert("Port Belum Terhubung", "Silakan hubungkan koneksi serial di dashboard utama terlebih dahulu.");
            return;
        }

        String text = txtKnownWeight.getText();
        if (text == null || text.trim().isEmpty()) {
            showAlert("Input Kosong", "Silakan masukkan berat beban uji referensi.");
            return;
        }

        double val;
        try {
            val = Double.parseDouble(text.replace(',', '.').trim());
            if (val <= 0) {
                showAlert("Nilai Tidak Valid", "Berat beban uji harus lebih besar dari 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Format Salah", "Masukkan angka desimal yang valid (contoh: 100 atau 500).");
            return;
        }

        // Konversi ke satuan KG untuk firmware Arduino
        double massInKg = cmbWeightUnit.getValue().startsWith("gram") ? (val / 1000.0) : val;

        progressCalib.setVisible(true);
        lblCalibResult.setText("⏳ Menunggu stabilisasi mekanik & menyimpan ke EEPROM...");
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
                lblZeroStatus.setText("✅ Titik nol (Tare) berhasil disetel!");
                lblZeroStatus.setStyle("-fx-text-fill: #00e676; -fx-font-weight: bold;");
            } else if (line.contains("[CAL_STATUS] CAL1_SUCCESS:") || line.contains("[CAL_STATUS] CAL2_SUCCESS:")) {
                progressCalib.setVisible(false);
                String factor = line.substring(line.indexOf("SUCCESS:") + 8).trim();
                lblCalibResult.setText("✅ Kalibrasi Sukses!\nFaktor: " + factor + " (Tersimpan permanen di EEPROM Arduino)");
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

