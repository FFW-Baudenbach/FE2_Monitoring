package com.odin568.monitoring.software;

import com.odin568.helper.HttpHelper;
import com.odin568.helper.MonitoringResult;
import com.odin568.monitoring.Monitoring;

import java.util.ArrayList;
import java.util.List;


public class Website implements Monitoring
{
    @Override
    public List<MonitoringResult> check()
    {
        var result = new ArrayList<MonitoringResult>();

        result.add(HttpHelper.isSiteUpViaHttps("Website - Access", "https://www.ffw-baudenbach.de", true, false));
        result.add(HttpHelper.isSiteRedirectedToHttps("Website - Http redirect", "http://www.ffw-baudenbach.de"));

        return result;
    }
}
