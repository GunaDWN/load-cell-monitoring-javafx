@echo off
TITLE IoT Sensor Monitoring Dashboard (Portable)
COLOR 0A

echo ==============================================================================
echo   MEMBUKA IOT SENSOR MONITORING DASHBOARD (STANDALONE / PORTABLE)
echo ==============================================================================
echo.
echo [INFO] Menyiapkan Java Runtime lokal (Tanpa Perlu Install Java/JDK)...

:: 1. Prioritas utama: gunakan JRE internal yang dibundel
if exist "%~dp0jre\bin\javaw.exe" (
    start "" "%~dp0jre\bin\javaw.exe" -jar "%~dp0load-cell-and-termokopel-monitoring-1.0.0.jar"
    exit /b 0
)

:: 2. Prioritas kedua: gunakan SensorMonitoring.exe
if exist "%~dp0SensorMonitoring.exe" (
    start "" "%~dp0SensorMonitoring.exe"
    exit /b 0
)

:: 3. Prioritas ketiga: gunakan Java yang terpasang di Windows
java -version >nul 2>&1
if %errorlevel% equ 0 (
    start "" javaw -jar "%~dp0load-cell-and-termokopel-monitoring-1.0.0.jar"
    exit /b 0
)

echo [ERROR] Tidak dapat menemukan Java Runtime di folder lokal maupun sistem Windows.
pause
