package com.argus.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ArgusCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(ArgusCoreApplication.class, args);
    }
}