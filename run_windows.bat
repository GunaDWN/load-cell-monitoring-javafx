@echo off
TITLE IoT Sensor Monitoring Dashboard (Load Cell & Thermocouple)
COLOR 0B

echo ==============================================================================
echo   IoT Sensor Monitoring Dashboard Launcher (Windows - Portable)
echo ==============================================================================
echo.

:: 1. Prioritas Utama: Gunakan JRE portabel lokal yang dibundel di folder ini
set "JAVA_CMD="

if exist "%~dp0jre\bin\javaw.exe" (
    set "JAVA_CMD=%~dp0jre\bin\javaw.exe"
    echo [INFO] Menggunakan Runtime JRE portabel (jre\bin\javaw.exe)...
) else if exist "%~dp0runtime\bin\javaw.exe" (
    set "JAVA_CMD=%~dp0runtime\bin\javaw.exe"
    echo [INFO] Menggunakan Runtime Java portabel (runtime\bin\javaw.exe)...
) else if exist "%~dp0dist\SensorMonitoring-Windows-Portable\jre\bin\javaw.exe" (
    set "JAVA_CMD=%~dp0dist\SensorMonitoring-Windows-Portable\jre\bin\javaw.exe"
    echo [INFO] Menggunakan Runtime JRE dari folder dist...
) else (
    :: Cek apakah Java terinstall di Windows PATH
    java -version >nul 2>&1
    if %errorlevel% equ 0 (
        set "JAVA_CMD=javaw"
        echo [INFO] Menggunakan Java bawaan sistem Windows...
    )
)

:: Cari berkas JAR aplikasi
set "JAR_FILE=%~dp0load-cell-and-termokopel-monitoring-1.0.0.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%~dp0target\load-cell-and-termokopel-monitoring-1.0.0.jar"
)

if not "%JAVA_CMD%"=="" if exist "%JAR_FILE%" (
    echo [INFO] Menjalankan Dashboard: %JAR_FILE%
    start "" "%JAVA_CMD%" -jar "%JAR_FILE%"
    exit /b 0
)

:: 2. Alternatif: Jika ada SensorMonitoring.exe
if exist "%~dp0SensorMonitoring.exe" (
    echo [INFO] Mencoba menjalankan SensorMonitoring.exe...
    start "" "%~dp0SensorMonitoring.exe"
    exit /b 0
)

echo [ERROR] Java runtime tidak ditemukan di folder lokal maupun sistem Windows!
echo.
echo Panduan:
echo 1. Pastikan folder 'jre' ada di samping file ini, ATAU
echo 2. Buka folder 'dist\SensorMonitoring-Windows-Portable' dan jalankan 'run_windows.bat'.
echo.
pause
exit /b 1
