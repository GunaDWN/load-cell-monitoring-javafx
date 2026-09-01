#!/bin/bash
# ==============================================================================
# Script untuk menjalankan JavaFX Dashboard Monitoring IoT
# ==============================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$DIR/target/load-cell-and-termokopel-monitoring-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "[INFO] Berkas JAR belum dibuat, melakukan kompilasi terlebih dahulu..."
    mvn package -DskipTests
fi

echo "[INFO] Menjalankan Dashboard JavaFX..."
java -jar "$JAR_FILE" "$@"

