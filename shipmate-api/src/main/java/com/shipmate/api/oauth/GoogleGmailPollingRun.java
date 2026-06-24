package com.shipmate.api.oauth;

import java.time.OffsetDateTime;

public record GoogleGmailPollingRun(
        String gmailPollingRunId,
        String triggerType,
        String merchantId,
        int accountCount,
        int scannedCount,
        int collectedCount,
        int orderDraftCreatedCount,
        int attachmentStoredCount,
        int skippedDuplicateCount,
        int skippedNonOrderCount,
        boolean success,
        String failureCode,
        String failureMessage,
        String latestInboundMessageId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {
}
