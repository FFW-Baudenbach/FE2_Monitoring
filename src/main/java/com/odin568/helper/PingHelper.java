package com.odin568.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class PingHelper
{
    private static Logger logger = LoggerFactory.getLogger(PingHelper.class);

    public static MonitoringResult pingDevice(String device, String ip)
    {
        MonitoringResult result = new MonitoringResult(device);

        try{
            InetAddress address = InetAddress.getByName(ip);
            result.Healthy = address.isReachable(5000);
            if (!result.Healthy)
                result.Information = "Unreachable";
        }
        catch (IOException e){
            logger.error("Error pinging device " + device, e);
            result.Healthy = false;
            result.Information = e.getMessage();
        }

        return result;
    }
}
