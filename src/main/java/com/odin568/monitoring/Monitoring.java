package com.odin568.monitoring;

import com.odin568.helper.MonitoringResult;

import java.util.List;

public interface Monitoring
{
    List<MonitoringResult> check();
}
