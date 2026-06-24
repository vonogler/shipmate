package com.shipmate.api.oauth;

public record GoogleGmailPollingResponse(
        int accountCount,
        int scannedCount,
        int collectedCount,
        int orderDraftCreatedCount,
        int attachmentStoredCount,
        int skippedDuplicateCount,
        int skippedNonOrderCount,
        String status
) {
}
