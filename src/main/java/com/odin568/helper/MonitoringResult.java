package com.odin568.helper;

public class MonitoringResult {

    public final String Device;
    public HealthState HealthState;
    public String Information;

    public MonitoringResult(String device) {
        Device = device;
        HealthState = com.odin568.helper.HealthState.Error;
        Information = "";
    }
}
