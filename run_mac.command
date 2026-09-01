#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    JAR_FILE="$DIR/load-cell-and-termokopel-monitoring-1.0.0.jar"
fi

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 17 tidak ditemukan. Silakan pasang Java 17 dari https://adoptium.net/ atau jalankan: brew install openjdk@17"
    read -p "Tekan ENTER untuk keluar..."
    exit 1
fi

echo "[INFO] Menjalankan IoT Sensor Monitoring Dashboard di macOS..."
java -jar "$JAR_FILE"
