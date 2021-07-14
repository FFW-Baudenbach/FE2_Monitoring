package com.odin568.monitoring.hardware;

import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;
import com.odin568.helper.PingHelper;

import java.util.List;

public class RaspberryPi extends PingHelper implements Monitoring
{
    @Override
    public List<MonitoringResult> check() { return List.of(pingDevice("RaspberryPi", "192.168.112.200")); }

}
