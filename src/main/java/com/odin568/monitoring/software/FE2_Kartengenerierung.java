package com.odin568.monitoring.software;

import com.odin568.helper.HttpHelper;
import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;

import java.util.ArrayList;
import java.util.List;

public class FE2_Kartengenerierung implements Monitoring
{
    @Override
    public List<MonitoringResult> check()
    {
        var result = new ArrayList<MonitoringResult>();

        // Health check already checks the availability of the icons - no need to re-test.
        result.add(HttpHelper.isSiteUpViaHttp("FE2_Kartengenerierung - Health", "http://192.168.112.200:8080/actuator/health", true));
        // Check if maps are reachable from outside
        result.add(HttpHelper.isSiteUpViaHttps("FE2_Kartengenerierung - Maps", "https://haus.ffw-baudenbach.de/maps/index.html", true, false));

        return result;
    }
}
