package com.shipmate.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shipmate.google-oauth")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        boolean gmailPollingEnabled,
        long gmailPollingDelayMs,
        int gmailPollingMaxMessages,
        boolean gmailAutoCreateOrderDrafts,
        String tokenEncryptionSecret
) {
}
