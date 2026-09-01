package com.iot.monitoring.model;

/**
 * Model data untuk masing-masing sensor Termokopel MAX6675
 */
public class ThermocoupleData {
    private Double suhu;       // Celcius (bisa null jika probe terputus)
    private Double fahrenheit;  // Fahrenheit

    public ThermocoupleData() {}

    public ThermocoupleData(Double suhu, Double fahrenheit) {
        this.suhu = suhu;
        this.fahrenheit = fahrenheit;
    }

    public Double getSuhu() {
        return suhu;
    }

    public void setSuhu(Double suhu) {
        this.suhu = suhu;
    }

    public Double getFahrenheit() {
        return fahrenheit;
    }

    public void setFahrenheit(Double fahrenheit) {
        this.fahrenheit = fahrenheit;
    }

    public boolean isConnected() {
        return suhu != null && !suhu.isNaN();
    }

    @Override
    public String toString() {
        if (!isConnected()) {
            return "Terputus";
        }
        return String.format("%.1f °C / %.1f °F", suhu, fahrenheit);
    }
}

