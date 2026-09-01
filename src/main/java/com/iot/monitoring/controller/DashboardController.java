package com.iot.monitoring.controller;

import com.iot.monitoring.model.SensorData;
import com.iot.monitoring.service.SerialService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller utama untuk dashboard monitoring sensor dengan fitur pemilihan
 * unit (kg/Newton, °C/°F) dan filter visibilitas sensor (LC1/LC2, TC1/TC2).
 */
public class DashboardController {

    // Controls Top Bar
    @FXML private ComboBox<String> portComboBox;
    @FXML private ComboBox<Integer> baudRateComboBox;
    @FXML private Button refreshPortsButton;
    @FXML private Button connectButton;
    @FXML private Label statusBadge;

    // Command Buttons
    @FXML private Button tareButton;
    @FXML private Button calibButton;
    @FXML private Button toggleJsonButton;
    @FXML private Button clearChartButton;
    @FXML private Button exportCsvButton;

    // Metric Cards & Labels - Load Cell
    @FXML private VBox cardLc1;
    @FXML private Label lblLc1Berat;
    @FXML private Label lblLc1Gaya;
    @FXML private Label lblLc1Status;

    @FXML private VBox cardLc2;
    @FXML private Label lblLc2Berat;
    @FXML private Label lblLc2Gaya;
    @FXML private Label lblLc2Status;

    @FXML private VBox cardLcCombined;
    @FXML private Label lblCombinedBerat;
    @FXML private Label lblCombinedGaya;
    @FXML private Label lblCombinedStatus;

    // Metric Cards & Labels - Termokopel
    @FXML private VBox cardTc1;
    @FXML private Label lblTc1Suhu;
    @FXML private Label lblTc1Fahrenheit;
    @FXML private Label lblTc1Status;

    @FXML private VBox cardTc2;
    @FXML private Label lblTc2Suhu;
    @FXML private Label lblTc2Fahrenheit;
    @FXML private Label lblTc2Status;

    @FXML private VBox cardTcCombined;
    @FXML private Label lblCombinedSuhu;
    @FXML private Label lblCombinedFahrenheit;
    @FXML private Label lblCombinedTcStatus;

    // Charts & Axes
    @FXML private Label lblWeightChartTitle;
    @FXML private LineChart<Number, Number> weightChart;
    @FXML private NumberAxis weightXAxis;
    @FXML private NumberAxis weightYAxis;

    @FXML private Label lblTempChartTitle;
    @FXML private LineChart<Number, Number> tempChart;
    @FXML private NumberAxis tempXAxis;
    @FXML private NumberAxis tempYAxis;

    // Unit Toggles
    @FXML private ToggleButton btnUnitKg;
    @FXML private ToggleButton btnUnitNewton;
    @FXML private ToggleGroup weightUnitGroup;

    @FXML private ToggleButton btnUnitCelsius;
    @FXML private ToggleButton btnUnitFahrenheit;
    @FXML private ToggleGroup tempUnitGroup;

    // Sensor Visibility Radios
    @FXML private RadioButton rbLcAll;
    @FXML private RadioButton rbLc1Only;
    @FXML private RadioButton rbLc2Only;
    @FXML private RadioButton rbLcCombined;
    @FXML private ToggleGroup lcVisibilityGroup;

    @FXML private RadioButton rbTcAll;
    @FXML private RadioButton rbTc1Only;
    @FXML private RadioButton rbTc2Only;
    @FXML private RadioButton rbTcCombined;
    @FXML private ToggleGroup tcVisibilityGroup;

    // Console Log
    @FXML private TextArea consoleTextArea;

    // Serial Service & Chart Series
    private final SerialService serialService = new SerialService();
    private XYChart.Series<Number, Number> seriesLc1;
    private XYChart.Series<Number, Number> seriesLc2;
    private XYChart.Series<Number, Number> seriesLcCombined;
    private XYChart.Series<Number, Number> seriesTc1;
    private XYChart.Series<Number, Number> seriesTc2;
    private XYChart.Series<Number, Number> seriesTcCombined;

    private int dataPointIndex = 0;
    private static final int MAX_CHART_POINTS = 60; // Riwayat 60 titik (~30 detik)

    // Struktur data riwayat untuk re-populasi saat berganti unit
    private static class DataRecord {
        final int index;
        final double berat1, gaya1;
        final double berat2, gaya2;
        final Double tempC1, tempF1;
        final Double tempC2, tempF2;

        DataRecord(int index, double berat1, double gaya1, double berat2, double gaya2,
                   Double tempC1, Double tempF1, Double tempC2, Double tempF2) {
            this.index = index;
            this.berat1 = berat1;
            this.gaya1 = gaya1;
            this.berat2 = berat2;
            this.gaya2 = gaya2;
            this.tempC1 = tempC1;
            this.tempF1 = tempF1;
            this.tempC2 = tempC2;
            this.tempF2 = tempF2;
        }
    }

    private final List<DataRecord> history = new ArrayList<>();

    // Struktur data untuk rekaman log CSV
    public static class CsvRecord {
        final String timestamp;
        final int index;
        final double berat1;
        final double gaya1;
        final double berat2;
        final double gaya2;
        final String tempC1;
        final String tempF1;
        final String tempC2;
        final String tempF2;

        public CsvRecord(int index, double berat1, double gaya1, double berat2, double gaya2,
                         Double tempC1, Double tempF1, Double tempC2, Double tempF2) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            this.index = index;
            this.berat1 = berat1;
            this.gaya1 = gaya1;
            this.berat2 = berat2;
            this.gaya2 = gaya2;
            this.tempC1 = (tempC1 != null) ? String.format(Locale.US, "%.2f", tempC1) : "";
            this.tempF1 = (tempF1 != null) ? String.format(Locale.US, "%.2f", tempF1) : "";
            this.tempC2 = (tempC2 != null) ? String.format(Locale.US, "%.2f", tempC2) : "";
            this.tempF2 = (tempF2 != null) ? String.format(Locale.US, "%.2f", tempF2) : "";
        }
    }

    private final List<CsvRecord> csvRecords = new ArrayList<>();

    @FXML
    public void initialize() {
        // Inisialisasi ComboBox Baud Rate
        baudRateComboBox.setItems(FXCollections.observableArrayList(9600, 19200, 38400, 57600, 115200));
        baudRateComboBox.setValue(57600);

        // Refresh daftar serial port
        handleRefreshPorts();

        // Inisialisasi Chart Series
        seriesLc1 = new XYChart.Series<>();
        seriesLc1.setName("Load Cell 1");

        seriesLc2 = new XYChart.Series<>();
        seriesLc2.setName("Load Cell 2");

        seriesLcCombined = new XYChart.Series<>();
        seriesLcCombined.setName("Total Beban (LC1 + LC2)");

        weightChart.getData().addAll(seriesLc1, seriesLc2);

        seriesTc1 = new XYChart.Series<>();
        seriesTc1.setName("Termokopel 1");

        seriesTc2 = new XYChart.Series<>();
        seriesTc2.setName("Termokopel 2");

        seriesTcCombined = new XYChart.Series<>();
        seriesTcCombined.setName("Total Suhu (TC1 + TC2)");

        tempChart.getData().addAll(seriesTc1, seriesTc2);

        // Konfigurasi sumbu X agar selalu mengisi penuh grafik tanpa ruang kosong di kiri
        weightXAxis.setAutoRanging(false);
        weightXAxis.setForceZeroInRange(false);
        weightXAxis.setLowerBound(0);
        weightXAxis.setUpperBound(MAX_CHART_POINTS);
        weightXAxis.setTickUnit(10);

        tempXAxis.setAutoRanging(false);
        tempXAxis.setForceZeroInRange(false);
        tempXAxis.setLowerBound(0);
        tempXAxis.setUpperBound(MAX_CHART_POINTS);
        tempXAxis.setTickUnit(10);

        // Proteksi ToggleGroup agar minimal satu opsi selalu terpilih
        setupToggleGroupProtection(weightUnitGroup);
        setupToggleGroupProtection(tempUnitGroup);
        setupToggleGroupProtection(lcVisibilityGroup);
        setupToggleGroupProtection(tcVisibilityGroup);

        // Setup Listener Serial Service
        serialService.setOnDataReceived(this::onSensorDataReceived);
        serialService.setOnRawLineReceived(this::onRawLineReceived);
        serialService.setOnConnectionStateChanged(this::onConnectionStateChanged);
        serialService.setOnError(this::onError);
    }

    private void setupToggleGroupProtection(ToggleGroup group) {
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null && oldVal != null) {
                oldVal.setSelected(true);
            }
        });
    }

    /**
     * Memperbarui daftar port serial yang tersedia
     */
    @FXML
    public void handleRefreshPorts() {
        List<String> ports = serialService.getAvailablePortNames();
        portComboBox.setItems(FXCollections.observableArrayList(ports));
        if (!ports.isEmpty()) {
            String selected = ports.contains("ttyUSB0") ? "ttyUSB0" : ports.get(0);
            portComboBox.setValue(selected);
        }
    }

    /**
     * Menghubungkan atau memutuskan koneksi serial
     */
    @FXML
    public void handleToggleConnect() {
        if (serialService.isConnected()) {
            serialService.disconnect();
        } else {
            String selectedPort = portComboBox.getValue();
            if (selectedPort == null || selectedPort.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Port Belum Dipilih", "Silakan pilih port serial terlebih dahulu.");
                return;
            }
            int baudRate = baudRateComboBox.getValue();
            connectButton.setDisable(true);
            connectButton.setText("Menghubungkan...");

            new Thread(() -> {
                boolean success = serialService.connect(selectedPort, baudRate);
                if (!success) {
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        connectButton.setText("Connect");
                    });
                }
            }).start();
        }
    }

    /**
     * Handler pergantian satuan Berat (kg / Newton)
     */
    @FXML
    public void handleWeightUnitChange() {
        boolean isNewton = btnUnitNewton.isSelected();
        lblWeightChartTitle.setText(isNewton ? "Tren Gaya (Newton)" : "Tren Beban (kg)");
        weightYAxis.setLabel(isNewton ? "Gaya (N)" : "Berat (kg)");
        repopulateWeightChart();
    }

    /**
     * Handler pergantian satuan Suhu (°C / °F)
     */
    @FXML
    public void handleTempUnitChange() {
        boolean isFahrenheit = btnUnitFahrenheit.isSelected();
        lblTempChartTitle.setText(isFahrenheit ? "Tren Suhu (°F)" : "Tren Suhu (°C)");
        tempYAxis.setLabel(isFahrenheit ? "Suhu (°F)" : "Suhu (°C)");
        repopulateTempChart();
    }

    /**
     * Handler filter visibilitas Load Cell (Semua / LC 1 Saja / LC 2 Saja / Gabungan)
     */
    @FXML
    public void handleWeightVisibilityChange() {
        weightChart.getData().clear();
        boolean isCombined = rbLcCombined.isSelected();

        // Tampilkan 1 kartu gabungan jika mode gabungan dipilih, atau 2 kartu terpisah jika bukan
        if (cardLc1 != null && cardLc2 != null && cardLcCombined != null) {
            cardLc1.setVisible(!isCombined);
            cardLc1.setManaged(!isCombined);
            cardLc2.setVisible(!isCombined);
            cardLc2.setManaged(!isCombined);
            cardLcCombined.setVisible(isCombined);
            cardLcCombined.setManaged(isCombined);
        }

        if (isCombined) {
            weightChart.getData().add(seriesLcCombined);
            if (seriesLcCombined.getNode() != null) {
                seriesLcCombined.getNode().setStyle("-fx-stroke: #e040fb; -fx-stroke-width: 2.5px;");
            }
        } else {
            if (rbLcAll.isSelected() || rbLc1Only.isSelected()) {
                weightChart.getData().add(seriesLc1);
            }
            if (rbLcAll.isSelected() || rbLc2Only.isSelected()) {
                weightChart.getData().add(seriesLc2);
            }
        }
    }

    /**
     * Handler filter visibilitas Termokopel (Semua / TC 1 Saja / TC 2 Saja / Rata-rata)
     */
    @FXML
    public void handleTempVisibilityChange() {
        tempChart.getData().clear();
        boolean isCombined = rbTcCombined.isSelected();

        // Tampilkan 1 kartu gabungan jika mode rata-rata dipilih, atau 2 kartu terpisah jika bukan
        if (cardTc1 != null && cardTc2 != null && cardTcCombined != null) {
            cardTc1.setVisible(!isCombined);
            cardTc1.setManaged(!isCombined);
            cardTc2.setVisible(!isCombined);
            cardTc2.setManaged(!isCombined);
            cardTcCombined.setVisible(isCombined);
            cardTcCombined.setManaged(isCombined);
        }

        if (isCombined) {
            tempChart.getData().add(seriesTcCombined);
            if (seriesTcCombined.getNode() != null) {
                seriesTcCombined.getNode().setStyle("-fx-stroke: #ffd600; -fx-stroke-width: 2.5px;");
            }
        } else {
            if (rbTcAll.isSelected() || rbTc1Only.isSelected()) {
                tempChart.getData().add(seriesTc1);
            }
            if (rbTcAll.isSelected() || rbTc2Only.isSelected()) {
                tempChart.getData().add(seriesTc2);
            }
        }
    }

    private void repopulateWeightChart() {
        boolean isNewton = btnUnitNewton.isSelected();
        seriesLc1.getData().clear();
        seriesLc2.getData().clear();
        seriesLcCombined.getData().clear();
        for (DataRecord r : history) {
            seriesLc1.getData().add(new XYChart.Data<>(r.index, isNewton ? r.gaya1 : r.berat1));
            seriesLc2.getData().add(new XYChart.Data<>(r.index, isNewton ? r.gaya2 : r.berat2));
            seriesLcCombined.getData().add(new XYChart.Data<>(r.index, isNewton ? (r.gaya1 + r.gaya2) : (r.berat1 + r.berat2)));
        }
        if (rbLcCombined.isSelected() && seriesLcCombined.getNode() != null) {
            seriesLcCombined.getNode().setStyle("-fx-stroke: #e040fb; -fx-stroke-width: 2.5px;");
        }
    }

    private void repopulateTempChart() {
        boolean isFahrenheit = btnUnitFahrenheit.isSelected();
        seriesTc1.getData().clear();
        seriesTc2.getData().clear();
        seriesTcCombined.getData().clear();
        for (DataRecord r : history) {
            if (r.tempC1 != null) {
                seriesTc1.getData().add(new XYChart.Data<>(r.index, isFahrenheit ? r.tempF1 : r.tempC1));
            }
            if (r.tempC2 != null) {
                seriesTc2.getData().add(new XYChart.Data<>(r.index, isFahrenheit ? r.tempF2 : r.tempC2));
            }
            Double totC = calculateCombinedTemp(r.tempC1, r.tempC2);
            Double totF = calculateCombinedTemp(r.tempF1, r.tempF2);
            if (totC != null) {
                seriesTcCombined.getData().add(new XYChart.Data<>(r.index, isFahrenheit ? totF : totC));
            }
        }
        if (rbTcCombined.isSelected() && seriesTcCombined.getNode() != null) {
            seriesTcCombined.getNode().setStyle("-fx-stroke: #ffd600; -fx-stroke-width: 2.5px;");
        }
    }

    private Double calculateCombinedTemp(Double t1, Double t2) {
        if (t1 != null && t2 != null) return t1 + t2;
        if (t1 != null) return t1;
        if (t2 != null) return t2;
        return null;
    }

    /**
     * Perintah Tare ke Arduino Nano
     */
    @FXML
    public void handleTare() {
        if (serialService.sendCommand("t")) {
            appendConsoleMessage("[AKSI] Mengirim perintah Tare ('t')...");
        }
    }

    /**
     * Membuka Wizard Dialog Kalibrasi Load Cell
     */
    @FXML
    public void handleOpenCalibration() {
        if (!serialService.isConnected()) {
            showAlert(Alert.AlertType.WARNING, "Port Belum Terhubung", "Silakan klik Connect pada dashboard terlebih dahulu.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/iot/monitoring/view/calibration_dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Wizard Kalibrasi Load Cell");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(connectButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            CalibrationDialogController controller = loader.getController();
            controller.setSerialService(serialService, dialogStage);

            // Kembalikan listener raw log dashboard saat dialog ditutup
            dialogStage.setOnHidden(e -> {
                serialService.setOnRawLineReceived(this::onRawLineReceived);
            });

            dialogStage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Membuka Dialog", "Terjadi kesalahan: " + e.getMessage());
        }
    }

    /**
     * Perintah Toggle Format JSON (Compact / Indented)
     */
    @FXML
    public void handleToggleJson() {
        if (serialService.sendCommand("j")) {
            appendConsoleMessage("[AKSI] Mengirim perintah toggle format JSON ('j')...");
        }
    }

    /**
     * Membersihkan grafik
     */
    @FXML
    public void handleClearChart() {
        history.clear();
        seriesLc1.getData().clear();
        seriesLc2.getData().clear();
        seriesLcCombined.getData().clear();
        seriesTc1.getData().clear();
        seriesTc2.getData().clear();
        seriesTcCombined.getData().clear();
        dataPointIndex = 0;
        weightXAxis.setLowerBound(0);
        weightXAxis.setUpperBound(MAX_CHART_POINTS);
        tempXAxis.setLowerBound(0);
        tempXAxis.setUpperBound(MAX_CHART_POINTS);
    }

    /**
     * Membersihkan teks log konsol
     */
    @FXML
    public void handleClearLog() {
        consoleTextArea.clear();
    }

    /**
     * Mengekspor seluruh log data yang terekam ke file CSV
     */
    @FXML
    public void handleExportCsv() {
        if (csvRecords.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Data Masih Kosong", "Belum ada data sensor yang terekam untuk diunduh ke CSV.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Simpan Data Log Sensor ke CSV");
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName("sensor_log_" + timeStamp + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Window window = connectButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(window);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                // Header CSV
                writer.write("Timestamp,Sample_Index,LoadCell1_Berat_kg,LoadCell1_Gaya_N,LoadCell2_Berat_kg,LoadCell2_Gaya_N,Total_Berat_kg,Total_Gaya_N,Termokopel1_Suhu_C,Termokopel1_Fahrenheit_F,Termokopel2_Suhu_C,Termokopel2_Fahrenheit_F,Total_Suhu_C,Total_Fahrenheit_F");
                writer.newLine();

                for (CsvRecord r : csvRecords) {
                    double totB = r.berat1 + r.berat2;
                    double totG = r.gaya1 + r.gaya2;

                    String totCStr = "";
                    String totFStr = "";
                    try {
                        Double valC1 = (!r.tempC1.isEmpty()) ? Double.parseDouble(r.tempC1) : null;
                        Double valC2 = (!r.tempC2.isEmpty()) ? Double.parseDouble(r.tempC2) : null;
                        Double totCVal = calculateCombinedTemp(valC1, valC2);
                        if (totCVal != null) totCStr = String.format(Locale.US, "%.2f", totCVal);

                        Double valF1 = (!r.tempF1.isEmpty()) ? Double.parseDouble(r.tempF1) : null;
                        Double valF2 = (!r.tempF2.isEmpty()) ? Double.parseDouble(r.tempF2) : null;
                        Double totFVal = calculateCombinedTemp(valF1, valF2);
                        if (totFVal != null) totFStr = String.format(Locale.US, "%.2f", totFVal);
                    } catch (Exception ignored) {}

                    writer.write(String.format(Locale.US, "%s,%d,%.3f,%.2f,%.3f,%.2f,%.3f,%.2f,%s,%s,%s,%s,%s,%s",
                            r.timestamp, r.index,
                            r.berat1, r.gaya1,
                            r.berat2, r.gaya2,
                            totB, totG,
                            r.tempC1, r.tempF1,
                            r.tempC2, r.tempF2,
                            totCStr, totFStr));
                    writer.newLine();
                }
                writer.flush();

                showAlert(Alert.AlertType.INFORMATION, "Unduhan Berhasil",
                        "Berhasil mengekspor " + csvRecords.size() + " baris data ke:\n" + file.getAbsolutePath());
                appendConsoleMessage("[CSV] Berhasil mengunduh " + csvRecords.size() + " baris log ke: " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Gagal Menyimpan CSV", "Terjadi kesalahan saat menulis file:\n" + e.getMessage());
            }
        }
    }

    /**
     * Callback saat payload SensorData baru berhasil di-parse dari JSON
     */
    private void onSensorDataReceived(SensorData data) {
        dataPointIndex++;

        // Geser jendela sumbu X dinamis (sliding window) agar grafik selalu 100% penuh dari tepi kiri ke kanan
        int lower = Math.max(0, dataPointIndex - MAX_CHART_POINTS);
        int upper = Math.max(MAX_CHART_POINTS, dataPointIndex);
        weightXAxis.setLowerBound(lower);
        weightXAxis.setUpperBound(upper);
        tempXAxis.setLowerBound(lower);
        tempXAxis.setUpperBound(upper);

        boolean isNewton = btnUnitNewton.isSelected();
        boolean isFahrenheit = btnUnitFahrenheit.isSelected();

        double b1 = 0, g1 = 0, b2 = 0, g2 = 0;
        Double c1 = null, f1 = null, c2 = null, f2 = null;

        // 1. Update Kartu Load Cell 1
        if (data.getLoadcell1() != null) {
            b1 = data.getLoadcell1().getBerat();
            g1 = data.getLoadcell1().getGaya();

            // Proteksi outlier ekstrim (misal lonjakan noise ADC atau pin tak terhubung)
            if (Math.abs(g1) > 2000.0 || Math.abs(b1) > 200.0) {
                b1 = 0; g1 = 0;
            }

            if (isNewton) {
                lblLc1Berat.setText(String.format(Locale.US, "%.1f N", g1));
                lblLc1Gaya.setText(String.format(Locale.US, "Massa: %.2f kg", b1));
            } else {
                lblLc1Berat.setText(String.format(Locale.US, "%.2f kg", b1));
                lblLc1Gaya.setText(String.format(Locale.US, "Gaya: %.1f N", g1));
            }
            lblLc1Status.setText(Math.abs(b1) > 0.05 ? "TERTIMBANG" : "KOSONG");
            lblLc1Status.setStyle(Math.abs(b1) > 0.05 ? "-fx-text-fill: #00e5ff;" : "-fx-text-fill: #90a4ae;");

            addChartPoint(seriesLc1, dataPointIndex, isNewton ? g1 : b1);
        }

        // 2. Update Kartu Load Cell 2
        if (data.getLoadcell2() != null) {
            b2 = data.getLoadcell2().getBerat();
            g2 = data.getLoadcell2().getGaya();

            // Proteksi outlier ekstrim (misal lonjakan noise ADC atau pin tak terhubung)
            if (Math.abs(g2) > 2000.0 || Math.abs(b2) > 200.0) {
                b2 = 0; g2 = 0;
            }

            if (isNewton) {
                lblLc2Berat.setText(String.format(Locale.US, "%.1f N", g2));
                lblLc2Gaya.setText(String.format(Locale.US, "Massa: %.2f kg", b2));
            } else {
                lblLc2Berat.setText(String.format(Locale.US, "%.2f kg", b2));
                lblLc2Gaya.setText(String.format(Locale.US, "Gaya: %.1f N", g2));
            }
            lblLc2Status.setText(Math.abs(b2) > 0.05 ? "TERTIMBANG" : "KOSONG");
            lblLc2Status.setStyle(Math.abs(b2) > 0.05 ? "-fx-text-fill: #ff9100;" : "-fx-text-fill: #90a4ae;");

            addChartPoint(seriesLc2, dataPointIndex, isNewton ? g2 : b2);
        }

        // 3. Update Data & Kartu Gabungan Load Cell (LC 1 + LC 2)
        double totalB = b1 + b2;
        double totalG = g1 + g2;
        addChartPoint(seriesLcCombined, dataPointIndex, isNewton ? totalG : totalB);
        if (rbLcCombined.isSelected() && seriesLcCombined.getNode() != null) {
            seriesLcCombined.getNode().setStyle("-fx-stroke: #e040fb; -fx-stroke-width: 2.5px;");
        }

        if (lblCombinedBerat != null && lblCombinedGaya != null) {
            if (isNewton) {
                lblCombinedBerat.setText(String.format(Locale.US, "%.1f N", totalG));
                lblCombinedGaya.setText(String.format(Locale.US, "Total Massa: %.2f kg", totalB));
            } else {
                lblCombinedBerat.setText(String.format(Locale.US, "%.2f kg", totalB));
                lblCombinedGaya.setText(String.format(Locale.US, "Total Gaya: %.1f N", totalG));
            }
            if (lblCombinedStatus != null) {
                lblCombinedStatus.setText(Math.abs(totalB) > 0.05 ? "TERTIMBANG" : "KOSONG");
                lblCombinedStatus.setStyle(Math.abs(totalB) > 0.05 ? "-fx-text-fill: #e040fb; -fx-font-weight: bold;" : "-fx-text-fill: #90a4ae;");
            }
        }

        // 4. Update Kartu Termokopel 1
        if (data.getTermokopel1() != null) {
            if (data.getTermokopel1().isConnected()) {
                c1 = data.getTermokopel1().getSuhu();
                f1 = data.getTermokopel1().getFahrenheit();

                if (isFahrenheit) {
                    lblTc1Suhu.setText(String.format(Locale.US, "%.1f °F", f1));
                    lblTc1Fahrenheit.setText(String.format(Locale.US, "Celcius: %.1f °C", c1));
                } else {
                    lblTc1Suhu.setText(String.format(Locale.US, "%.1f °C", c1));
                    lblTc1Fahrenheit.setText(String.format(Locale.US, "Fahrenheit: %.1f °F", f1));
                }
                if (lblTc1Status != null) {
                    lblTc1Status.setText("AKTIF");
                    lblTc1Status.setStyle("-fx-text-fill: #00e676;");
                }
                addChartPoint(seriesTc1, dataPointIndex, isFahrenheit ? f1 : c1);
            } else {
                lblTc1Suhu.setText("TERPUTUS");
                lblTc1Fahrenheit.setText("Probe lepas");
                if (lblTc1Status != null) {
                    lblTc1Status.setText("TERPUTUS");
                    lblTc1Status.setStyle("-fx-text-fill: #90a4ae;");
                }
            }
        }

        // 5. Update Kartu Termokopel 2
        if (data.getTermokopel2() != null) {
            if (data.getTermokopel2().isConnected()) {
                c2 = data.getTermokopel2().getSuhu();
                f2 = data.getTermokopel2().getFahrenheit();

                if (isFahrenheit) {
                    lblTc2Suhu.setText(String.format(Locale.US, "%.1f °F", f2));
                    lblTc2Fahrenheit.setText(String.format(Locale.US, "Celcius: %.1f °C", c2));
                } else {
                    lblTc2Suhu.setText(String.format(Locale.US, "%.1f °C", c2));
                    lblTc2Fahrenheit.setText(String.format(Locale.US, "Fahrenheit: %.1f °F", f2));
                }
                if (lblTc2Status != null) {
                    lblTc2Status.setText("AKTIF");
                    lblTc2Status.setStyle("-fx-text-fill: #ff5252;");
                }
                addChartPoint(seriesTc2, dataPointIndex, isFahrenheit ? f2 : c2);
            } else {
                lblTc2Suhu.setText("TERPUTUS");
                lblTc2Fahrenheit.setText("Probe lepas");
                if (lblTc2Status != null) {
                    lblTc2Status.setText("TERPUTUS");
                    lblTc2Status.setStyle("-fx-text-fill: #90a4ae;");
                }
            }
        }

        // 6. Update Data & Kartu Gabungan Termokopel (TC 1 + TC 2)
        Double totC = calculateCombinedTemp(c1, c2);
        Double totF = calculateCombinedTemp(f1, f2);

        if (totC != null) {
            addChartPoint(seriesTcCombined, dataPointIndex, isFahrenheit ? totF : totC);
            if (rbTcCombined.isSelected() && seriesTcCombined.getNode() != null) {
                seriesTcCombined.getNode().setStyle("-fx-stroke: #ffd600; -fx-stroke-width: 2.5px;");
            }
        }

        if (lblCombinedSuhu != null && lblCombinedFahrenheit != null) {
            if (totC != null) {
                if (isFahrenheit) {
                    lblCombinedSuhu.setText(String.format(Locale.US, "%.1f °F", totF));
                    lblCombinedFahrenheit.setText(String.format(Locale.US, "Celcius: %.1f °C", totC));
                } else {
                    lblCombinedSuhu.setText(String.format(Locale.US, "%.1f °C", totC));
                    lblCombinedFahrenheit.setText(String.format(Locale.US, "Fahrenheit: %.1f °F", totF));
                }
                if (lblCombinedTcStatus != null) {
                    lblCombinedTcStatus.setText("AKTIF");
                    lblCombinedTcStatus.setStyle("-fx-text-fill: #ffd600; -fx-font-weight: bold;");
                }
            } else {
                lblCombinedSuhu.setText("TERPUTUS");
                lblCombinedFahrenheit.setText("Probe lepas");
                if (lblCombinedTcStatus != null) {
                    lblCombinedTcStatus.setText("TERPUTUS");
                    lblCombinedTcStatus.setStyle("-fx-text-fill: #90a4ae;");
                }
            }
        }

        // Simpan titik ke history buffer (untuk grafik)
        history.add(new DataRecord(dataPointIndex, b1, g1, b2, g2, c1, f1, c2, f2));
        if (history.size() > MAX_CHART_POINTS) {
            history.remove(0);
        }

        // Rekam baris data untuk ekspor CSV
        csvRecords.add(new CsvRecord(dataPointIndex, b1, g1, b2, g2, c1, f1, c2, f2));
        if (exportCsvButton != null) {
            exportCsvButton.setText("📥 Download CSV (" + csvRecords.size() + ")");
        }
    }

    private void addChartPoint(XYChart.Series<Number, Number> series, int x, double y) {
        series.getData().add(new XYChart.Data<>(x, y));
        if (series.getData().size() > MAX_CHART_POINTS) {
            series.getData().remove(0);
        }
    }

    /**
     * Menampilkan raw string serial pada teks area konsol
     */
    private void onRawLineReceived(String line) {
        appendConsoleMessage(line);
    }

    private void appendConsoleMessage(String message) {
        consoleTextArea.appendText(message + "\n");
        if (consoleTextArea.getText().length() > 20000) {
            consoleTextArea.deleteText(0, 5000);
        }
    }

    /**
     * Handler perubahan status koneksi serial
     */
    private void onConnectionStateChanged(boolean connected) {
        connectButton.setDisable(false);
        if (connected) {
            connectButton.setText("Disconnect");
            connectButton.getStyleClass().removeAll("button-primary");
            connectButton.getStyleClass().add("button-danger");

            statusBadge.setText("TERHUBUNG");
            statusBadge.getStyleClass().removeAll("status-badge-disconnected");
            statusBadge.getStyleClass().add("status-badge-connected");

            portComboBox.setDisable(true);
            baudRateComboBox.setDisable(true);
            refreshPortsButton.setDisable(true);
            tareButton.setDisable(false);
            calibButton.setDisable(false);
            toggleJsonButton.setDisable(false);

            appendConsoleMessage("[SISTEM] Koneksi serial berhasil terhubung.");
        } else {
            connectButton.setText("Connect");
            connectButton.getStyleClass().removeAll("button-danger");
            connectButton.getStyleClass().add("button-primary");

            statusBadge.setText("TERPUTUS");
            statusBadge.getStyleClass().removeAll("status-badge-connected");
            statusBadge.getStyleClass().add("status-badge-disconnected");

            portComboBox.setDisable(false);
            baudRateComboBox.setDisable(false);
            refreshPortsButton.setDisable(false);
            tareButton.setDisable(true);
            calibButton.setDisable(true);
            toggleJsonButton.setDisable(true);

            appendConsoleMessage("[SISTEM] Koneksi serial terputus.");
        }
    }

    private void onError(String errorMessage) {
        appendConsoleMessage("[ERROR] " + errorMessage);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        if (serialService != null) {
            serialService.disconnect();
        }
    }
}
