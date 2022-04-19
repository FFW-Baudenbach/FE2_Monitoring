package com.odin568.monitoring.hardware;

import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;
import com.odin568.helper.PingHelper;

import java.util.List;

public class RaspberryPi implements Monitoring
{
    @Override
    public List<MonitoringResult> check() {
        var dockerRaspi = PingHelper.pingDevice("RaspberryPi Docker - Ping", "192.168.112.200");
        var montitorRaspi = PingHelper.pingDevice("RaspberryPi Monitor - Ping", "192.168.112.201");
        return List.of(dockerRaspi, montitorRaspi);
    }

}
