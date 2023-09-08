package com.odin568.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;

public class PingHelper
{
    private static final Logger logger = LoggerFactory.getLogger(PingHelper.class);

    public static MonitoringResult pingDevice(String device, String ip)
    {
        MonitoringResult result = new MonitoringResult(device);

        try{
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(5000))
                result.HealthState = HealthState.Healthy;
            else
            {
                result.HealthState = HealthState.Error;
                result.Information = "Unreachable";
            }
        }
        catch (IOException e){
            logger.error("Error pinging device " + device, e);
            result.HealthState = HealthState.Error;
            result.Information = e.getMessage();
        }

        return result;
    }
}
