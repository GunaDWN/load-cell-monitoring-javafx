#!/bin/bash
# ==============================================================================
# Script untuk menjalankan JavaFX Dashboard Monitoring IoT (Linux / Portable)
# ==============================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Cek apakah ada binary native hasil jpackage
if [ -f "$DIR/dist/linux/SensorMonitor/bin/SensorMonitor" ]; then
    echo "[INFO] Menjalankan binary standalone portabel (tanpa butuh Java sistem)..."
    "$DIR/dist/linux/SensorMonitor/bin/SensorMonitor" "$@"
    exit 0
fi

# 2. Cek runtime Java portabel lokal (jre / runtime bawaan folder)
JAVA_BIN=""
if [ -f "$DIR/runtime/bin/java" ]; then
    JAVA_BIN="$DIR/runtime/bin/java"
    echo "[INFO] Menggunakan Java runtime portabel: $JAVA_BIN"
elif [ -f "$DIR/jre/bin/java" ]; then
    JAVA_BIN="$DIR/jre/bin/java"
    echo "[INFO] Menggunakan JRE portabel: $JAVA_BIN"
elif command -v java &> /dev/null; then
    JAVA_BIN="java"
    echo "[INFO] Menggunakan Java sistem..."
fi

if [ -z "$JAVA_BIN" ]; then
    echo "[ERROR] Java runtime tidak ditemukan!"
    echo "Anda tidak perlu menginstall JDK penuh. Cukup letakkan folder 'runtime' atau 'jre' portabel di folder ini."
    exit 1
fi

JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/load-cell-and-termokopel-monitoring-1.0.0.jar"
fi

if [ ! -f "$JAR_FILE" ]; then
    if command -v mvn &> /dev/null; then
        echo "[INFO] Berkas JAR belum dibuat, melakukan kompilasi dengan Maven..."
        mvn package -DskipTests
    else
        echo "[ERROR] Berkas JAR tidak ditemukan!"
        exit 1
    fi
fi

echo "[INFO] Menjalankan Dashboard JavaFX..."
"$JAVA_BIN" -jar "$JAR_FILE" "$@"
