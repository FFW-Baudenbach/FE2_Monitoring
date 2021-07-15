package com.odin568.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Fix potential issue with scheduled tasks:
 * https://community.pivotal.io/s/article/methods-annotated-with-scheduled-stops-working?language=en_US
 */
@Configuration
public class ScheduledTaskConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler (){
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(25);
        return taskScheduler;
    }
}

