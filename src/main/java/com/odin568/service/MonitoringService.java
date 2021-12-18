package com.odin568.service;

import com.odin568.helper.Mode;
import com.odin568.helper.MonitoringResult;
import com.odin568.monitoring.hardware.Printer;
import com.odin568.monitoring.hardware.RaspberryPi;
import com.odin568.monitoring.hardware.Router;
import com.odin568.monitoring.hardware.WindowsPC;
import com.odin568.monitoring.software.FE2;
import com.odin568.monitoring.software.FE2_Kartengenerierung;
import com.odin568.monitoring.software.FE2_Monitoring;
import com.odin568.monitoring.software.Website;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonitoringService implements HealthIndicator
{
    @Value("${fe2.apiKey}")
    private String apiKey;

    @Value("${mode}")
    private Mode mode;

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

    @PostConstruct
    private void onStartup() {
        logger.info("Application was started. Mode = " + mode);
        pushoverService.sendToPushover("FE2_Monitoring " + mode + " Status", "FE2_Monitoring started.", "0");
    }

    @PreDestroy
    public void onExit() {
        logger.info("Application is stopping.");
        pushoverService.sendToPushover("FE2_Monitoring " + mode + " Status", "FE2_Monitoring stopped.", "0");
    }

    @Scheduled(cron = "${alive.cron:0 0 6 * * *}")
    private void sendDailyAlive() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        String msg = "";
        msg += "Status:       " + (isActiveIssue() ? "ERROR" : (somethingHappenedLastDay() ? "WARNING" : "OK")) + System.lineSeparator();
        msg += "Last Error:   " + (lastErrorOccurred == null ? "None" : lastErrorOccurred.format(formatter)) + System.lineSeparator();
        msg += "Last Success: " + (lastSuccessOccurred == null ? "None" : lastSuccessOccurred.format(formatter));

        pushoverService.sendToPushover("FE2_Monitoring " + mode + " Status", msg, (somethingHappenedLastDay() ? "0" : "-1"));
    }

    @Scheduled(initialDelayString = "${initialDelay:10000}", fixedDelayString = "${fixedDelay:60000}")
    private void runChecks()
    {
        logger.info("Starting " + mode + " checking devices and services.");

        var results = executeChecks();

        boolean allUp = results.stream().allMatch(i -> i.Healthy);
        if (allUp) {
            if (logger.isDebugEnabled())
                logger.debug("Result of " + mode + " run:" + System.lineSeparator() + buildMessage(results));

            lastSuccessOccurred = LocalDateTime.now();
            errorCounter = 0;
            if (errorNotified && ++restoredCounter >= requiredNrRetries) {
                logger.info("Sending Pushover restored message...");
                if (pushoverService.sendToPushover("FE2_Monitoring " + mode + " Resolved", buildMessage(results), "0")) {
                    restoredNotified = false;
                    restoredCounter = 0;
                    errorNotified = false;
                    errorCounter = 0;
                }
                restoredCounter--; //Avoid potential overflow
            }
        }
        else {
            logger.warn("Result of run:" + System.lineSeparator() + buildMessage(results));
            lastErrorOccurred = LocalDateTime.now();
            if (!errorNotified && ++errorCounter >= requiredNrRetries) {
                logger.error("Sending Pushover error message...");
                errorNotified = pushoverService.sendToPushover("FE2_Monitoring " + mode + " Error", buildMessage(results), "1");
                errorCounter--; // Avoid potential overflow
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("======================================");
            logger.debug("Mode: " + mode);
            logger.debug("Status: " + (allUp ? "UP" : "DOWN"));
            logger.debug("ErrorCounter: " + errorCounter);
            logger.debug("ErrorNotified: " + errorNotified);
            logger.debug("RestoredCounter: " + restoredCounter);
            logger.debug("RestoredNotified: " + restoredNotified);
            logger.debug("======================================");
        }

        logger.info("Finished checking devices and services.");
    }

    private List<MonitoringResult> executeChecks()
    {
        switch (mode) {
            case INTERNAL -> {
                var results = new ArrayList<MonitoringResult>();
                results.addAll(new Router().check());
                results.addAll(new WindowsPC().check());
                results.addAll(new RaspberryPi().check());
                results.addAll(new FE2(apiKey).check());
                results.addAll(new FE2_Kartengenerierung().check());
                results.addAll(new Printer().check());
                results.addAll(new Website().check());
                return results;
            }
            case EXTERNAL -> {
                return new FE2_Monitoring().check();
            }
        }
        return List.of();
    }

    private String buildMessage(List<MonitoringResult> results)
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

    private boolean isActiveIssue() {

        if (lastErrorOccurred == null) {
            return false;
        }

        if (lastSuccessOccurred == null) {
            return true;
        }

        return lastSuccessOccurred.isBefore(lastErrorOccurred);
    }

    @Override
    public Health health() {

        String currentState = (isActiveIssue() ? "ERROR" : (somethingHappenedLastDay() ? "WARNING" : "OK"));

        return Health.up().withDetail("currentState", currentState).build();
    }
}
