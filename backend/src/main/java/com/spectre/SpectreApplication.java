package com.spectre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
public class SpectreApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpectreApplication.class, args);
    }
}