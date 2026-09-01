@echo off
TITLE IoT Sensor Monitoring Dashboard (Load Cell & Thermocouple)
COLOR 0B

echo ==============================================================================
echo   IoT Sensor Monitoring Dashboard Launcher (Windows - Portable)
echo ==============================================================================
echo.

:: 1. Jika executable native SensorMonitoring.exe ada, prioritaskan langsung
if exist "%~dp0SensorMonitoring.exe" (
    echo [INFO] Menjalankan SensorMonitoring.exe portabel bawaan...
    start "" "%~dp0SensorMonitoring.exe"
    exit /b 0
)

:: 2. Cari Java Executable Portabel Bawaan Folder (Tidak butuh install JDK)
set "JAVA_CMD="

if exist "%~dp0runtime\bin\javaw.exe" (
    set "JAVA_CMD=%~dp0runtime\bin\javaw.exe"
    echo [INFO] Menggunakan Runtime Java portabel (runtime\bin\javaw.exe)...
) else if exist "%~dp0jre\bin\javaw.exe" (
    set "JAVA_CMD=%~dp0jre\bin\javaw.exe"
    echo [INFO] Menggunakan JRE portabel (jre\bin\javaw.exe)...
) else if exist "%~dp0runtime\bin\java.exe" (
    set "JAVA_CMD=%~dp0runtime\bin\java.exe"
    echo [INFO] Menggunakan Runtime Java portabel (runtime\bin\java.exe)...
) else if exist "%~dp0jre\bin\java.exe" (
    set "JAVA_CMD=%~dp0jre\bin\java.exe"
    echo [INFO] Menggunakan JRE portabel (jre\bin\java.exe)...
) else (
    :: Cek apakah Java terinstall di Windows PATH
    java -version >nul 2>&1
    if %errorlevel% equ 0 (
        set "JAVA_CMD=javaw"
        echo [INFO] Menggunakan Java bawaan sistem Windows...
    )
)

:: Cari berkas JAR aplikasi
set "JAR_FILE=%~dp0target\load-cell-and-termokopel-monitoring-1.0.0.jar"
if not exist "%JAR_FILE%" (
    set "JAR_FILE=%~dp0load-cell-and-termokopel-monitoring-1.0.0.jar"
)

if "%JAVA_CMD%"=="" (
    echo [ERROR] Java runtime tidak ditemukan di folder lokal maupun sistem Windows!
    echo.
    echo Anda TIDAK PERLU menginstall JDK secara manual di laptop ini.
    echo Cukup letakkan folder 'runtime' atau 'jre' portabel ke dalam folder ini,
    echo atau gunakan berkas SensorMonitoring.exe yang sudah dibundel.
    echo.
    pause
    exit /b 1
)

if exist "%JAR_FILE%" (
    echo [INFO] Menjalankan aplikasi monitoring: %JAR_FILE%
    start "" "%JAVA_CMD%" -jar "%JAR_FILE%"
) else (
    echo [ERROR] File aplikasi JAR tidak ditemukan!
    echo Pastikan file target\load-cell-and-termokopel-monitoring-1.0.0.jar ada.
    pause
    exit /b 1
)
