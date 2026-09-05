#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# 1. Cek aplikasi macOS .app
if [ -d "$DIR/SensorMonitor.app" ]; then
    echo "[INFO] Membuka SensorMonitor.app portabel..."
    open "$DIR/SensorMonitor.app"
    exit 0
fi

# 2. Cek runtime portabel lokal (atau sistem Java)
JAVA_BIN=""
if [ -f "$DIR/runtime/bin/java" ]; then
    JAVA_BIN="$DIR/runtime/bin/java"
elif [ -f "$DIR/jre/bin/java" ]; then
    JAVA_BIN="$DIR/jre/bin/java"
elif command -v java &> /dev/null; then
    JAVA_BIN="java"
fi

if [ -z "$JAVA_BIN" ]; then
    echo "=================================================================="
    echo " [ERROR] Java Runtime tidak ditemukan di Mac ini!"
    echo "=================================================================="
    echo " Solusi Cepat:"
    echo " 1. Pasang Java 17 gratis melalui Homebrew:"
    echo "    brew install openjdk@17"
    echo " ATAU"
    echo " 2. Unduh installer Java 17 untuk Mac dari: https://adoptium.net/"
    echo "=================================================================="
    read -p "Tekan ENTER untuk keluar..."
    exit 1
fi

# 3. Cari berkas JAR aplikasi
JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/load-cell-and-termokopel-monitoring-1.0.0.jar"
fi

if [ ! -f "$JAR_FILE" ]; then
    if command -v mvn &> /dev/null; then
        echo "[INFO] Berkas JAR belum ada, melakukan kompilasi dengan Maven..."
        mvn package -DskipTests
        JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"
    else
        echo "=================================================================="
        echo " [ERROR] Berkas JAR aplikasi belum ditemukan!"
        echo "=================================================================="
        echo " Pastikan berkas 'load-cell-and-termokopel-monitoring-1.0.0.jar'"
        echo " sudah disalin ke dalam folder ini."
        echo "=================================================================="
        read -p "Tekan ENTER untuk keluar..."
        exit 1
    fi
fi

echo "[INFO] Menjalankan IoT Sensor Monitoring Dashboard di macOS..."
"$JAVA_BIN" -jar "$JAR_FILE"
