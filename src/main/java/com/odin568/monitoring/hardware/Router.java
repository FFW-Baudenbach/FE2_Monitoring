package com.odin568.monitoring.hardware;

import com.odin568.helper.HttpHelper;
import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;
import com.odin568.helper.PingHelper;

import java.util.ArrayList;
import java.util.List;

public class Router implements Monitoring
{
    @Override
    public List<MonitoringResult> check() {
        var result = new ArrayList<MonitoringResult>();

        result.add(PingHelper.pingDevice("FritzBox - Ping", "192.168.112.254"));
        result.add(HttpHelper.isSiteUpViaHttp("FritzBox - WebUI", "http://192.168.112.254", true));

        return result;
    }

}
