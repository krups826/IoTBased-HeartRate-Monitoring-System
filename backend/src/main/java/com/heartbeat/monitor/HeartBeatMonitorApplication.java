package com.heartbeat.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HeartBeatMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartBeatMonitorApplication.class, args);
    }

}
