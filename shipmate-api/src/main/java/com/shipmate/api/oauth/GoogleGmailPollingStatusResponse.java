package com.shipmate.api.oauth;

import java.time.OffsetDateTime;
import java.util.Map;

public record GoogleGmailPollingStatusResponse(
        boolean pollingEnabled,
        long pollingDelayMs,
        int pollingMaxMessages,
        boolean autoCreateOrderDrafts,
        int accountCount,
        int collectedMessageCount,
        OffsetDateTime latestCollectedAt,
        OffsetDateTime latestReceivedAt,
        int attachmentStoredCount,
        int recentRunCount,
        int recentFailureCount,
        int recentSkippedNonOrderCount,
        OffsetDateTime latestSuccessAt,
        OffsetDateTime latestFailureAt,
        String latestFailureCode,
        String latestFailureMessage,
        String latestFailureInboundMessageId,
        Map<String, Long> recentFailureCodeCounts
) {
}
