package com.odin568.monitoring.software;

import com.odin568.helper.MonitoringResult;
import com.odin568.monitoring.Monitoring;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FE2_SmartHome implements Monitoring
{
    private final Logger logger = LoggerFactory.getLogger(FE2_SmartHome.class);

    @Override
    public List<MonitoringResult> check()
    {
        MonitoringResult result = new MonitoringResult("FE2_SmartHome - Health");

        Optional<String> output = getHealth("http://192.168.112.200:8082/actuator/health");
        if (output.isPresent()) {
            var health = output.map(JSONObject::new);

            var obj = health.get()
                    .getJSONObject("components")
                    .getJSONObject("switchDeviceService")
                    .getJSONObject("details");

            if (obj.has("switchState")) {
                result.Information = "Actor is " + obj.get("switchState").toString();
                result.Healthy = true;
            }
            else if (obj.has("exception")) {
                result.Information = obj.get("exception").toString();
                result.Healthy = false;
            }
        }

        return List.of(result);
    }

    private Optional<String> getHealth(String url)
    {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

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
