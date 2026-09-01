package com.iot.monitoring.service;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.iot.monitoring.model.SensorData;
import javafx.application.Platform;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service komunikasi serial menggunakan library jSerialComm
 * Dilengkapi streaming real-time per karakter/baris tanpa buffer blocking besar.
 */
public class SerialService {
    private SerialPort activePort;
    private Thread readerThread;
    private volatile boolean isRunning = false;
    private final Gson gson = new Gson();

    private Consumer<SensorData> onDataReceived;
    private Consumer<String> onRawLineReceived;
    private Consumer<Boolean> onConnectionStateChanged;
    private Consumer<String> onError;

    // Buffer internal fallback untuk format multi-line
    private final StringBuilder jsonBuffer = new StringBuilder();
    private int braceDepth = 0;
    private boolean insideJson = false;

    public SerialService() {}

    /**
     * Mengambil daftar nama serial port yang tersedia pada sistem
     */
    public List<String> getAvailablePortNames() {
        List<String> portNames = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portNames.add(port.getSystemPortName());
        }
        return portNames;
    }

    /**
     * Membuka koneksi serial ke port yang ditentukan
     */
    public synchronized boolean connect(String portName, int baudRate) {
        disconnect();

        activePort = SerialPort.getCommPort(portName);
        activePort.setBaudRate(baudRate);
        activePort.setNumDataBits(8);
        activePort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        activePort.setParity(SerialPort.NO_PARITY);
        // Timeout semi-blocking 100ms agar setiap data baris langsung diproses seketika tanpa tertahan di buffer OS
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (!activePort.openPort()) {
            if (onError != null) {
                onError.accept("Gagal membuka port " + portName + ". Pastikan port tidak sedang digunakan.");
            }
            return false;
        }

        // Beri jeda 1 detik agar Arduino selesai reset DTR
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        isRunning = true;
        jsonBuffer.setLength(0);
        braceDepth = 0;
        insideJson = false;

        readerThread = new Thread(this::readLoop, "Serial-Reader-Thread");
        readerThread.setDaemon(true);
        readerThread.start();

        if (onConnectionStateChanged != null) {
            Platform.runLater(() -> onConnectionStateChanged.accept(true));
        }

        return true;
    }

    /**
     * Menutup koneksi serial
     */
    public synchronized void disconnect() {
        isRunning = false;

        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }

        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
        }
        activePort = null;

        if (onConnectionStateChanged != null) {
            Platform.runLater(() -> onConnectionStateChanged.accept(false));
        }
    }

    public boolean isConnected() {
        return activePort != null && activePort.isOpen();
    }

    /**
     * Mengirim perintah string ke Arduino (misalnya 't', '1', '2', 'j')
     */
    public boolean sendCommand(String command) {
        if (!isConnected()) return false;
        try {
            OutputStream out = activePort.getOutputStream();
            out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            return true;
        } catch (Exception e) {
            if (onError != null) {
                Platform.runLater(() -> onError.accept("Gagal mengirim perintah: " + e.getMessage()));
            }
            return false;
        }
    }

    /**
     * Loop pembacaan data serial secara real-time streaming
     */
    private void readLoop() {
        try {
            InputStream in = activePort.getInputStream();
            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            byte[] readBuffer = new byte[256];

            while (isRunning) {
                int bytesRead;
                try {
                    bytesRead = in.read(readBuffer);
                } catch (Exception ex) {
                    if (!isRunning) break;
                    continue; // Timeout semi-blocking normal, lanjutkan
                }

                if (bytesRead > 0) {
                    for (int i = 0; i < bytesRead; i++) {
                        byte b = readBuffer[i];
                        if (b == '\n') {
                            String line = lineBuffer.toString(StandardCharsets.UTF_8).trim();
                            lineBuffer.reset();
                            if (!line.isEmpty()) {
                                processLine(line);
                            }
                        } else if (b != '\r') {
                            lineBuffer.write(b);
                        }
                    }
                } else if (bytesRead == -1) {
                    if (!isRunning) break;
                    try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                }
            }
        } catch (Exception e) {
            if (isRunning && onError != null) {
                Platform.runLater(() -> onError.accept("Koneksi terputus: " + e.getMessage()));
            }
        } finally {
            if (isRunning) {
                disconnect();
            }
        }
    }

    /**
     * Memproses 1 baris teks yang baru saja selesai diterima
     */
    private void processLine(String line) {
        // 1. Format Utama: 1 baris JSON utuh (Single-Line JSON)
        if (line.startsWith("{") && line.endsWith("}")) {
            parseAndDispatch(line);
            if (onRawLineReceived != null) {
                final String raw = line;
                Platform.runLater(() -> onRawLineReceived.accept(raw));
            }
            return;
        }

        // 2. Pesan Log / Status Non-JSON (misal [INFO], [STATUS], menu)
        if (!line.contains("{") && !insideJson) {
            if (onRawLineReceived != null) {
                final String raw = line;
                Platform.runLater(() -> onRawLineReceived.accept(raw));
            }
            return;
        }

        // 3. Fallback Multi-line JSON: Mulai akumulasi baru jika menemukan '{' pembuka
        if (line.startsWith("{")) {
            jsonBuffer.setLength(0);
            braceDepth = 0;
            insideJson = true;
        }

        if (insideJson) {
            jsonBuffer.append(line).append(" ");
            for (char c : line.toCharArray()) {
                if (c == '{') braceDepth++;
                else if (c == '}') braceDepth--;
            }

            // Jika kurung kurawal seimbang, paket lengkap!
            if (braceDepth <= 0 && jsonBuffer.length() > 0) {
                String completeJson = jsonBuffer.toString().trim();
                parseAndDispatch(completeJson);
                if (onRawLineReceived != null) {
                    Platform.runLater(() -> onRawLineReceived.accept(completeJson));
                }
                jsonBuffer.setLength(0);
                braceDepth = 0;
                insideJson = false;
            } else if (jsonBuffer.length() > 2000) {
                // Reset buffer jika terjadi data corrupt
                jsonBuffer.setLength(0);
                braceDepth = 0;
                insideJson = false;
            }
        }
    }

    /**
     * Parsing JSON dan kirim hasil ke listener UI
     */
    private void parseAndDispatch(String jsonString) {
        try {
            SensorData data = gson.fromJson(jsonString, SensorData.class);
            if (data != null && (data.getLoadcell1() != null || data.getTermokopel1() != null)) {
                if (onDataReceived != null) {
                    Platform.runLater(() -> onDataReceived.accept(data));
                }
            }
        } catch (JsonSyntaxException ignored) {
            // Abaikan string non-JSON yang mungkin tercampur
        }
    }

    public void setOnDataReceived(Consumer<SensorData> onDataReceived) {
        this.onDataReceived = onDataReceived;
    }

    public void setOnRawLineReceived(Consumer<String> onRawLineReceived) {
        this.onRawLineReceived = onRawLineReceived;
    }

    public void setOnConnectionStateChanged(Consumer<Boolean> onConnectionStateChanged) {
        this.onConnectionStateChanged = onConnectionStateChanged;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }
}
