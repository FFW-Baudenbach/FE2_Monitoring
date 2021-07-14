package com.odin568.monitoring.hardware;

import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;
import com.odin568.helper.PingHelper;

import java.util.List;

public class Router extends PingHelper implements Monitoring
{
    @Override
    public List<MonitoringResult> check() { return List.of(pingDevice("FritzBox", "192.168.112.254")); }

}
