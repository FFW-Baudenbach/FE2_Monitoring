package com.odin568.monitoring.software;

import com.odin568.helper.HealthState;
import com.odin568.helper.HttpHelper;
import com.odin568.helper.MonitoringResult;
import com.odin568.monitoring.Monitoring;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class FE2 implements Monitoring
{
    private final String apiKey;
    private final Logger logger = LoggerFactory.getLogger(FE2.class);

    public FE2(final String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public List<MonitoringResult> check()
    {
        var result = new ArrayList<MonitoringResult>();

        result.add(HttpHelper.isSiteUpViaHttp("FE2 Web - Direct", "http://192.168.112.1:83/favicon.ico", true));
        result.add(HttpHelper.isSiteUpViaHttps("FE2 Web - Reverse proxy", "https://192.168.112.200/favicon.ico", true, true));
        result.add(HttpHelper.isSiteRedirectedToHttps("FE2 Web - Http redirect", "http://haus.ffw-baudenbach.de/favicon.ico"));
        result.add(HttpHelper.isSiteUpViaHttps("FE2 Web - External", "https://haus.ffw-baudenbach.de/favicon.ico", true, false));

        result.add(checkApi());
        result.add(checkStatus());
        result.addAll(checkInputs());
        result.add(checkCloud());
        result.add(checkMqtt());
        result.add(checkSystem());
        result.add(checkAmWeb());

        return result;
    }

    private MonitoringResult checkApi() {
        var result = new MonitoringResult("FE2 Rest - External Status");
        var output = readObjectFromFe2Api("https://haus.ffw-baudenbach.de/rest/status");

        /*
        {
           "fe2":"OK",
           "gae":"OK"
        }
         */
        if (output.isPresent()) {
            String fe2 = output.get().getString("fe2");
            String gae = output.get().getString("gae");
            if (fe2.equalsIgnoreCase("OK") && gae.equalsIgnoreCase("OK")) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                result.Information = "FE2: " + fe2 + ", GAE: " + gae;
            }
        }

        return result;
    }

    private MonitoringResult checkStatus() {
        var result = new MonitoringResult("FE2 Rest - Monitoring Status");

        var output = readObjectFromFe2Api("http://192.168.112.1:83/rest/monitoring/status");

        /*
        {
          "state": "OK",
          "message": "",
          "nbrOfLoggedErrors": 0,
          "redundancyState": {
            "state": "OK",
            "current": "STANDALONE",
            "configured": "STANDALONE"
          }
        }
         */
        if (output.isPresent()) {
            switch(output.get().getString("state"))
            {
                case "OK":
                    result.HealthState = HealthState.Healthy;
                    break;
                case "WARN":
                    result.HealthState = HealthState.Warning;
                    break;
                default:
                    result.HealthState = HealthState.Error;
                    break;
            }

            String msg = output.get().getString("message");

            result.Information = msg.trim();

            if (output.get().has("nbrOfLoggedErrors"))
            {
                int nrErrors = output.get().getInt("nbrOfLoggedErrors");
                if (nrErrors > 0) {
                    result.Information += " (" + nrErrors + " Errors in Log)";
                }
            }

        }

        return result;
    }

    private List<MonitoringResult> checkInputs() {

        /*
        [
          {
            "name": "Lebenszeichen Probealarm",
            "id": "290253998",
            "state": "OK"
          },
          {
            "name": "FE2 KBI Alarmierung und Status",
            "id": "699925490",
            "state": "OK"
          },
          {
            "name": "Leitstellen-Fax TEST",
            "id": "1843322222",
            "state": "OK"
          },
          {
            "name": "Monitoring (FE2)",
            "id": "951cf8af9e5d116619245dbc473e147a2b83b4febe508820db3b8928a93c31b4",
            "state": "OK"
          },
          {
            "name": "Leitstellen-Fax PRODUKTIV",
            "id": "790147395",
            "state": "OK"
          },
          {
            "name": "Grasland-Feuerindex",
            "id": "1838781858",
            "state": "OK"
          },
          {
            "name": "Mail-Überwachung TEST",
            "id": "1711724625",
            "state": "OK"
          },
          {
            "name": "Waldbrand-Feuerindex",
            "id": "1751594556",
            "state": "OK"
          },
          {
            "name": "Wetterwarnungen",
            "id": "1619357678",
            "state": "OK"
          }
        ]
         */

        var output = readArrayFromFe2Api("http://192.168.112.1:83/rest/monitoring/input");
        if (output.isEmpty()) {
            return List.of(new MonitoringResult("FE2 Rest - Monitoring Inputs"));
        }

        List<MonitoringResult> errorResults = new ArrayList<>();
        for (int i = 0; i < output.get().length(); i++) {
            JSONObject currInput = (JSONObject)output.get().get(i);

            String name = currInput.getString("name");
            String id = currInput.getString("id");
            String state = currInput.getString("state");

            if (!Arrays.asList("OK", "NOT_USED", "WARN").contains(state.toUpperCase())) {
                MonitoringResult error = new MonitoringResult("FE2 Rest - Monitoring Input " + name);

                var detailedInput = readObjectFromFe2Api("http://192.168.112.1:83/rest/monitoring/input/" + id);
                detailedInput.ifPresent(jsonObject -> error.Information = jsonObject.isNull("message") ? state : jsonObject.getString("message"));

                errorResults.add(error);
            }
        }

        if (!errorResults.isEmpty()) {
            return errorResults;
        }

        var successResult = new MonitoringResult("FE2 Rest - Monitoring Inputs");
        successResult.HealthState = HealthState.Healthy;

        return List.of(successResult);
    }

    private MonitoringResult checkCloud() {
        var result = new MonitoringResult("FE2 Rest - Monitoring Cloud");

        var cloudServices = readArrayFromFe2Api("http://192.168.112.1:83/rest/monitoring/cloud");

        /*
        [
          {
            "state": "OK",
            "service": "Kalender"
          },
          {
            "state": "OK",
            "service": "Alarmierung"
          },
          {
            "state": "OK",
            "service": "Maps"
          },
          {
            "state": "OK",
            "service": "Verfügbarkeit"
          }
        ]
         */
        if (cloudServices.isPresent()) {

            List<String> faultyServices = new ArrayList<>();
            for (int i = 0; i < cloudServices.get().length(); i++) {
                JSONObject cloudService = (JSONObject) cloudServices.get().get(i);
                String service = cloudService.getString("service");
                String state = cloudService.getString("state");

                if (!"OK".equalsIgnoreCase(state)) {
                    faultyServices.add(service);
                }
            }

            if (faultyServices.isEmpty()) {
                result.HealthState = HealthState.Healthy;
            }
            else if (faultyServices.size() == 1 && faultyServices.contains("Kalender")) {
                result.HealthState = HealthState.Warning;
                result.Information = "Error in Kalender";
            }
            else {
                result.HealthState = HealthState.Error;
                result.Information = "Error in " + String.join(",", faultyServices);
            }
        }

        return result;
    }

    private MonitoringResult checkMqtt() {
        var result = new MonitoringResult("FE2 Rest - Monitoring MQTT");

        var brokerStates = readObjectFromFe2Api("http://192.168.112.1:83/rest/monitoring/mqtt");

        /*
        {
          "defaultBroker": "OK",
          "kubernetes": "OK"
        }
         */
        if (brokerStates.isPresent()) {
            boolean defaultBroker = "OK".equalsIgnoreCase(brokerStates.get().getString("defaultBroker"));
            boolean kubernetes = "OK".equalsIgnoreCase(brokerStates.get().getString("kubernetes"));

            if (defaultBroker && kubernetes) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                List<String> mqttErrors = new ArrayList<>();
                if (!defaultBroker)
                    mqttErrors.add("defaultBroker");
                if (!kubernetes) {
                    mqttErrors.add("kubernetes");
                }
                result.Information = "Error in " + String.join(",", mqttErrors);
            }
        }

        return result;
    }

    private MonitoringResult checkSystem() {
        var result = new MonitoringResult("FE2 Rest - Monitoring System");

        var systemState = readObjectFromFe2Api("http://192.168.112.1:83/rest/monitoring/system");

        /*
        {
          "freeMemory": 5266,
          "disks": [
            {
              "disk": "C:\\",
              "freeSpace": 177
            },
            {
              "disk": "E:\\",
              "freeSpace": 15
            }
          ]
        }
         */
        if (systemState.isPresent()) {
            List<String> systemErrors = new ArrayList<>();
            int freeSystemSpace = -1;
            for(var disk : systemState.get().getJSONArray("disks")) {
                String driveLetter = ((JSONObject) disk).getString("disk");
                if (driveLetter.startsWith("C")) {
                    freeSystemSpace = ((JSONObject) disk).getInt("freeSpace");
                    break;
                }
            }
            if (freeSystemSpace < 10) {
                systemErrors.add("Free DiskSpace:" + (freeSystemSpace > 0 ? String.valueOf(freeSystemSpace) : "??") + "GB");
            }

            if (systemState.get().has("freeMemory"))
            {
                int freeMemory = systemState.get().getInt("freeMemory");
                if (freeMemory < 1000) {
                    systemErrors.add("Free Memory:" + freeMemory + "GB");
                }
            }

            if (systemErrors.isEmpty()) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                result.Information = String.join(",", systemErrors);
            }
        }

        return result;
    }

    private MonitoringResult checkAmWeb() {
        var result = new MonitoringResult("FE2 Rest - Monitoring AMweb");

        var amWebStates = readArrayFromFe2Api("http://192.168.112.1:83/rest/monitoring/amweb");

        /*
        [
           {
              "identifier":"6266ed621abde574fa6fa26c",
              "connectionState":"OK",
              "organization":"Admin",
              "name":"Monitor",
              "connectionType":"WEBSOCKET",
              "nbrOfWebSocketConnections":1
           }
        ]
         */

        if (amWebStates.isPresent()) {

            List<String> faultyAmWebs = new ArrayList<>();
            for (int i = 0; i < amWebStates.get().length(); i++) {
                JSONObject amWebState = (JSONObject) amWebStates.get().get(i);
                String name = amWebState.getString("name");
                String connectionState = amWebState.getString("connectionState");
                String connectionType = amWebState.getString("connectionType");
                int nbrOfWebSocketConnections = amWebState.getInt("nbrOfWebSocketConnections");

                if (!"OK".equals(connectionState) || !connectionType.equals("WEBSOCKET") || nbrOfWebSocketConnections < 1) {
                    faultyAmWebs.add("AMWeb '" + name + "' in state '" + connectionState + "' with type '" + connectionType + "' with " + nbrOfWebSocketConnections + " connection(s)");
                }
            }

            if (faultyAmWebs.isEmpty()) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                result.HealthState = HealthState.Warning; // WebSocket connection can break after a while
                result.Information = String.join(",", faultyAmWebs);
            }
        }

        return result;
    }

    private Optional<JSONObject> readObjectFromFe2Api(String url) {
        var result = readFromFe2Api(url);
        return result.map(JSONObject::new);
    }

    private Optional<JSONArray> readArrayFromFe2Api(String url) {
        var result = readFromFe2Api(url);
        return result.map(JSONArray::new);
    }

    private Optional<String> readFromFe2Api(String url)
    {
        try {
            RestTemplate restTemplate = HttpHelper.getRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<String>(headers),
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.trace("Request Successful.");
                logger.trace(response.getBody());
            }
            else {
                logger.debug("Request Failed");
                logger.debug("Statuscode: " + response.getStatusCode());
            }

            return Optional.ofNullable(response.getBody());
        }
        catch (Exception ex) {
            logger.debug("Exception on request", ex);
            return Optional.empty();
        }
    }
}
