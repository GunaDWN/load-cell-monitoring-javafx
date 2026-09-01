@echo off
TITLE IoT Sensor Monitoring Dashboard (Load Cell & Thermocouple)
COLOR 0B

echo ==============================================================================
echo   IoT Sensor Monitoring Dashboard Launcher (Windows)
echo ==============================================================================
echo.

:: Cek apakah Java terinstall
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java runtime tidak ditemukan di sistem Anda!
    echo Silakan install Java 17 (JRE / JDK) terlebih dahulu.
    echo Anda dapat mengunduh Java dari: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: Cari berkas JAR
set "JAR_FILE=%~dp0target\load-cell-and-termokopel-monitoring-1.0.0.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%~dp0load-cell-and-termokopel-monitoring-1.0.0.jar"
)

if exist "%JAR_FILE%" (
    echo [INFO] Menjalankan aplikasi monitoring: %JAR_FILE%
    echo [INFO] Menghubungkan ke Java Virtual Machine...
    start "" javaw -jar "%JAR_FILE%"
) else (
    if exist "%~dp0SensorMonitoring.exe" (
        echo [INFO] Menjalankan SensorMonitoring.exe...
        start "" "%~dp0SensorMonitoring.exe"
    ) else (
        echo [ERROR] File aplikasi JAR tidak ditemukan!
        echo Pastikan file target\load-cell-and-termokopel-monitoring-1.0.0.jar ada.
        pause
        exit /b 1
    )
)
