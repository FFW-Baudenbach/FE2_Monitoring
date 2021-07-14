package com.odin568.service;

import com.odin568.helper.MonitoringResult;
import com.odin568.monitoring.hardware.Printer;
import com.odin568.monitoring.hardware.RaspberryPi;
import com.odin568.monitoring.hardware.Router;
import com.odin568.monitoring.hardware.WindowsPC;
import com.odin568.monitoring.software.FE2;
import com.odin568.monitoring.software.FE2_Kartengenerierung;
import com.odin568.monitoring.software.Website;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MonitoringService
{
    private final PushoverService pushoverService;
    private final int requiredNrRetries = 5;

    private long errorCounter = 0;
    private boolean errorNotified = false;
    private long restoredCounter = 0;
    private boolean restoredNotified = false;

    private Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    public MonitoringService(PushoverService pushoverService) {
        this.pushoverService = pushoverService;
    }

    @Scheduled(initialDelayString = "${initialDelay:10000}", fixedDelayString = "${fixedDelay:1000}")
    private List<MonitoringResult> checkEverything() {

        logger.debug("Started checking devices and services.");

        var results = new ArrayList<MonitoringResult>();

        results.addAll(new Router().check());
        results.addAll(new WindowsPC().check());
        results.addAll(new RaspberryPi().check());
        results.addAll(new FE2().check());
        results.addAll(new FE2_Kartengenerierung().check());
        results.addAll(new Printer().check());
        results.addAll(new Website().check());

        if (logger.isDebugEnabled())
            logger.debug("Result of run:" + System.lineSeparator() + buildMessage(results));

        boolean allUp = results.stream().allMatch(i -> i.Healthy);
        if (allUp) {
            if (errorNotified && ++restoredCounter >= requiredNrRetries) {
                String message = buildMessage(results);
                logger.info("Sending Pushover restored message...");

                if (pushoverService.sendToPushover("Mobile alarm restored", message, "0")) {
                    restoredNotified = false;
                    restoredCounter = 0;
                    errorNotified = false;
                    errorCounter = 0;
                }
                restoredCounter--; //Avoid potential overflow
            }
        }
        else {
            if (!errorNotified && ++errorCounter >= requiredNrRetries) {
                String message = buildMessage(results);
                logger.error("Sending Pushover error message...");
                errorNotified = pushoverService.sendToPushover("Mobile alarm broken", message, "1");
                errorCounter--; // Avoid potential overflow
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("======================================");
            logger.debug("Status: " + (allUp ? "UP" : "DOWN"));
            logger.debug("ErrorCounter: " + errorCounter);
            logger.debug("ErrorNotified: " + errorNotified);
            logger.debug("RestoredCounter: " + restoredCounter);
            logger.debug("RestoredNotified: " + restoredNotified);
            logger.debug("======================================");
        }

        logger.debug("Finished checking devices and services.");

        return results;
    }

    private String buildMessage(ArrayList<MonitoringResult> results) {
        StringBuilder sb = new StringBuilder();
        for(var result : results) {
            sb.append((result.Healthy ? "✅" : "❌"));
            sb.append(" | ");
            sb.append(result.Device);
            if (result.Information != null && result.Information.length() > 0) {
                sb.append(" | ");
                sb.append(result.Information);
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

}
