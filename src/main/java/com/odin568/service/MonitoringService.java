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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Service
public class MonitoringService
{
    private final PushoverService pushoverService;
    private final int requiredNrRetries = 5;

    private long errorCounter = 0;
    private boolean errorNotified = false;
    private LocalDateTime lastErrorOccurred = null;

    private long restoredCounter = 0;
    private boolean restoredNotified = false;
    private LocalDateTime lastSuccessOccurred = null;

    private final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    @Autowired
    public MonitoringService(PushoverService pushoverService) {
        this.pushoverService = pushoverService;
    }

    @Scheduled(cron = "${alive.cron:0 0 6 * * *}")
    private void sendDailyAlive() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String msg = "";
        msg += "Status:       " + (somethingHappenedLastDay() ? "WARNING" : "OK") + System.lineSeparator();
        msg += "Last Error:   " + (lastErrorOccurred == null ? "None" : lastErrorOccurred.format(formatter)) + System.lineSeparator();
        msg += "Last Success: " + (lastSuccessOccurred == null ? "None" : lastSuccessOccurred.format(formatter));

        pushoverService.sendToPushover("FE2_Monitoring Status", msg, (somethingHappenedLastDay() ? "0" : "-1"));
    }

    @Scheduled(initialDelayString = "${initialDelay:10000}", fixedDelayString = "${fixedDelay:60000}")
    private void runChecks()
    {
        //sendDailyAlive(); //DEBUG

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

        //pushoverService.sendToPushover("TestMessage", buildMessage(results), "0"); //DEBUG

        boolean allUp = results.stream().allMatch(i -> i.Healthy);
        if (allUp) {
            lastSuccessOccurred = LocalDateTime.now();
            errorCounter = 0;
            if (errorNotified && ++restoredCounter >= requiredNrRetries) {
                logger.info("Sending Pushover restored message...");
                if (pushoverService.sendToPushover("Problems resolved", buildMessage(results), "0")) {
                    restoredNotified = false;
                    restoredCounter = 0;
                    errorNotified = false;
                    errorCounter = 0;
                }
                restoredCounter--; //Avoid potential overflow
            }
        }
        else {
            lastErrorOccurred = LocalDateTime.now();
            if (!errorNotified && ++errorCounter >= requiredNrRetries) {
                logger.error("Sending Pushover error message...");
                errorNotified = pushoverService.sendToPushover("Problem detected", buildMessage(results), "1");
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
    }

    private String buildMessage(ArrayList<MonitoringResult> results)
    {
        StringBuilder sb = new StringBuilder();
        for(var result : results) {
            sb.append((result.Healthy ? "✅" : "❌"));
            sb.append(" ");
            sb.append(result.Device);
            if (result.Information != null && result.Information.length() > 0) {
                sb.append(": ");
                sb.append(result.Information);
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString().trim();
    }

    private boolean somethingHappenedLastDay() {
        if (lastSuccessOccurred == null) {
            return true;
        }

        if (lastErrorOccurred == null) {
            return false;
        }

        return LocalDateTime.now().minusDays(1).isBefore(lastErrorOccurred);
    }

}
