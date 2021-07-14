package com.odin568.monitoring.software;

import com.odin568.helper.HttpHelper;
import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;

import java.util.ArrayList;
import java.util.List;

public class FE2 implements Monitoring
{
    @Override
    public List<MonitoringResult> check()
    {
        var result = new ArrayList<MonitoringResult>();

        result.add(HttpHelper.isSiteUpViaHttp("FE2 directly", "http://192.168.112.1:83/app.css", true));
        result.add(HttpHelper.isSiteRedirectedToHttps("FE2 reverse proxy redirect", "http://192.168.112.200/app.css"));
        result.add(HttpHelper.isSiteUpViaHttps("FE2 via reverse proxy", "https://192.168.112.200/app.css", true, true));
        result.add(HttpHelper.isSiteUpViaHttps("FE2 via web", "https://haus.ffw-baudenbach.de/app.css", true, false));

        return result;
    }
}
