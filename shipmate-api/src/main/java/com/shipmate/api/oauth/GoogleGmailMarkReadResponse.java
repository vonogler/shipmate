package com.shipmate.api.oauth;

public record GoogleGmailMarkReadResponse(
        String merchantId,
        int requestedCount,
        int markedReadCount,
        String status
) {
}
