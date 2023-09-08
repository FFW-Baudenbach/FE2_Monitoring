package com.odin568.monitoring.software;

import com.odin568.helper.HealthState;
import com.odin568.helper.HttpHelper;
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

            String switchState = getEntryFromHealth(health.get(), "switchDeviceService", "switchState");
            String switchException = getEntryFromHealth(health.get(), "switchDeviceService", "exception");
            String lastMotion = getEntryFromHealth(health.get(), "motionDetectorService", "lastMotionDetected");

            if (switchException != null) {
                result.Information = switchException;
                result.HealthState = HealthState.Error;
            }
            else {
                result.Information = "Actor is " + switchState + ".";
                if (lastMotion != null)
                    result.Information += " Last motion: " + lastMotion;

                result.HealthState = "none".equalsIgnoreCase(lastMotion) ? HealthState.Warning : HealthState.Healthy;
            }
        }

        return List.of(result);
    }

    private String getEntryFromHealth(JSONObject input, String component, String detailKey)
    {
        try {
            return input.getJSONObject("components").getJSONObject(component).getJSONObject("details").getString(detailKey);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private Optional<String> getHealth(String url)
    {
        try {
            RestTemplate restTemplate = HttpHelper.getRestTemplate();
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
