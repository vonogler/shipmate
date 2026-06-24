package com.shipmate.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ShipmateApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShipmateApiApplication.class, args);
    }
}
