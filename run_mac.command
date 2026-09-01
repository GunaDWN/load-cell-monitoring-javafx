#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 1. Cek aplikasi macOS .app
if [ -d "$DIR/SensorMonitor.app" ]; then
    echo "[INFO] Membuka SensorMonitor.app portabel..."
    open "$DIR/SensorMonitor.app"
    exit 0
fi

# 2. Cek runtime portabel lokal
JAVA_BIN=""
if [ -f "$DIR/runtime/bin/java" ]; then
    JAVA_BIN="$DIR/runtime/bin/java"
elif [ -f "$DIR/jre/bin/java" ]; then
    JAVA_BIN="$DIR/jre/bin/java"
elif command -v java &> /dev/null; then
    JAVA_BIN="java"
fi

if [ -z "$JAVA_BIN" ]; then
    echo "[ERROR] Java runtime tidak ditemukan!"
    echo "Anda tidak perlu memasang JDK manual di laptop ini."
    echo "Cukup letakkan folder 'runtime' portabel ke direktori ini atau buka SensorMonitor.app."
    read -p "Tekan ENTER untuk keluar..."
    exit 1
fi

JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/load-cell-and-termokopel-monitoring-1.0.0.jar"
fi

echo "[INFO] Menjalankan IoT Sensor Monitoring Dashboard di macOS..."
"$JAVA_BIN" -jar "$JAR_FILE"
