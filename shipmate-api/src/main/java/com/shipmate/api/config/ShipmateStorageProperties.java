package com.shipmate.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shipmate.storage")
public record ShipmateStorageProperties(
        String bucket,
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        int presignedUrlExpirySeconds
) {
}
