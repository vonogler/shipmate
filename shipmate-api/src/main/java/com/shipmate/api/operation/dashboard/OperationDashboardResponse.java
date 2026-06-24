package com.shipmate.api.operation.dashboard;

import java.util.Map;

public record OperationDashboardResponse(
        long inboundMessageCount,
        long orderDraftCount,
        long submittedShippingOrderCount,
        long errorLogCount,
        Map<String, Long> orderDraftStatusCounts,
        Map<String, Long> sourceChannelCounts,
        Map<String, Long> errorCodeCounts,
        boolean gmailPollingEnabled,
        long gmailPollingDelayMs,
        int gmailPollingAccountCount,
        int gmailPollingRecentRunCount,
        int gmailPollingRecentFailureCount,
        int gmailPollingRecentSkippedNonOrderCount,
        String gmailPollingLatestFailureMessage,
        String gmailPollingLatestFailureInboundMessageId,
        Map<String, Long> gmailPollingFailureCodeCounts
) {
}
