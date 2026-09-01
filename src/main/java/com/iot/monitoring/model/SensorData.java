package com.iot.monitoring.model;

/**
 * Model utama representasi payload JSON dari Arduino
 */
public class SensorData {
    private LoadCellData loadcell1;
    private LoadCellData loadcell2;
    private ThermocoupleData termokopel1;
    private ThermocoupleData termokopel2;

    public SensorData() {}

    public LoadCellData getLoadcell1() {
        return loadcell1;
    }

    public void setLoadcell1(LoadCellData loadcell1) {
        this.loadcell1 = loadcell1;
    }

    public LoadCellData getLoadcell2() {
        return loadcell2;
    }

    public void setLoadcell2(LoadCellData loadcell2) {
        this.loadcell2 = loadcell2;
    }

    public ThermocoupleData getTermokopel1() {
        return termokopel1;
    }

    public void setTermokopel1(ThermocoupleData termokopel1) {
        this.termokopel1 = termokopel1;
    }

    public ThermocoupleData getTermokopel2() {
        return termokopel2;
    }

    public void setTermokopel2(ThermocoupleData termokopel2) {
        this.termokopel2 = termokopel2;
    }
}

