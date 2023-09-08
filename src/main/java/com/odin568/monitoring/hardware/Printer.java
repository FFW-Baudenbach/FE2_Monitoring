package com.odin568.monitoring.hardware;

import com.odin568.helper.HealthState;
import com.odin568.monitoring.Monitoring;
import com.odin568.helper.MonitoringResult;
import com.odin568.helper.HttpHelper;
import com.odin568.helper.PingHelper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Printer implements Monitoring
{

    @Override
    public List<MonitoringResult> check() {
        var result = new ArrayList<MonitoringResult>();

        result.add(PingHelper.pingDevice("Printer - Ping", "192.168.112.10"));
        result.add(HttpHelper.isSiteUpViaHttp("Printer - WebUI", "http://192.168.112.10/sws/index.html", false));
        result.addAll(getPrinterStatus("http://192.168.112.10/sws/app/information/home/home.json"));

        return result;
    }

    /**
     * Documentation about API: https://github.com/nielstron/pysyncthru/issues/35#issuecomment-673392310
     */
    private List<MonitoringResult> getPrinterStatus(String apiUrl) {

        var resultList = new ArrayList<MonitoringResult>();

        JSONObject response;
        try {
            response = readJsonFromUrl(apiUrl);
        }
        catch (IOException ex) {
            MonitoringResult result = new MonitoringResult("Printer - API");
            result.Information = ex.getClass().getName() + ": " + ex.getMessage();
            return List.of(result);
        }

        // Check Status
        MonitoringResult status = new MonitoringResult("Printer - Device Status");
        int deviceStatus = (Integer)((JSONObject)response.get("status")).get("hrDeviceStatus");
        status.Information = getDeviceStatus(deviceStatus);
        if (isDeviceStatusNormal(deviceStatus)) {
            status.HealthState = HealthState.Healthy;
        }
        resultList.add(status);

        // Check black printer cartridge:
        MonitoringResult toner = new MonitoringResult("Printer - Black Toner");
        int remaining = (Integer)((JSONObject)response.get("toner_black")).get("remaining");
        if (remaining > 20)
            toner.HealthState = HealthState.Healthy;
        else if (remaining > 10)
            toner.HealthState = HealthState.Warning;
        else
            toner.HealthState = HealthState.Error;
        toner.Information = remaining + "% remaining";
        resultList.add(toner);

        return resultList;
    }

    private boolean isDeviceStatusNormal(int hrDeviceStatus) {
        return switch (hrDeviceStatus) {
            case 1, 2, 4 -> true;
            default -> false;
        };
    }

    private String getDeviceStatus(int hrDeviceStatus) {
        return switch (hrDeviceStatus) {
            case 1 -> "unknown(1)";
            case 2 -> "running(2)";
            case 3 -> "warning(3)";
            case 4 -> "testing(4)";
            case 5 -> "down(5)";
            default -> "invalid(" + hrDeviceStatus + ")";
        };
    }

    private JSONObject readJsonFromUrl(String url) throws IOException {
        try (InputStream is = new URL(url).openStream())
        {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            return new JSONObject(jsonText);
        }
    }
}
