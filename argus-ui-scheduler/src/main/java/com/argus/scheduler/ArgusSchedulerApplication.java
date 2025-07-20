package com.argus.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArgusSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArgusSchedulerApplication.class, args);
    }
}