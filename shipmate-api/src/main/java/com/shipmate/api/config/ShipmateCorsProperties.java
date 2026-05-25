package com.shipmate.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "shipmate.cors")
public record ShipmateCorsProperties(
        List<String> allowedOrigins
) {
}
