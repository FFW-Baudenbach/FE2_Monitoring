package com.odin568.helper;

public class MonitoringResult {

    public final String Device;
    public boolean Healthy;
    public String Information;

    public MonitoringResult(String device) {
        Device = device;
        Healthy = false;
        Information = "";
    }
}
