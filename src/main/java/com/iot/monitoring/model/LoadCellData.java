package com.iot.monitoring.model;

/**
 * Model data untuk masing-masing sensor Load Cell
 */
public class LoadCellData {
    private double berat;
    private double gaya;

    public LoadCellData() {}

    public LoadCellData(double berat, double gaya) {
        this.berat = berat;
        this.gaya = gaya;
    }

    public double getBerat() {
        return berat;
    }

    public void setBerat(double berat) {
        this.berat = berat;
    }

    public double getGaya() {
        return gaya;
    }

    public void setGaya(double gaya) {
        this.gaya = gaya;
    }

    @Override
    public String toString() {
        return String.format("%.2f kg (%.1f N)", berat, gaya);
    }
}

