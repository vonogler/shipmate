package com.shipmate.api.operation.dashboard;

import com.shipmate.api.inboundmessage.InboundMessageResponse;
import com.shipmate.api.oauth.GoogleGmailPollingService;
import com.shipmate.api.oauth.GoogleGmailPollingStatusResponse;
import com.shipmate.api.operation.error.ErrorLogService;
import com.shipmate.api.orderdraft.OrderDraftResponse;
import com.shipmate.api.orderdraft.OrderDraftService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OperationDashboardService {
    private final com.shipmate.api.inboundmessage.InboundMessageService inboundMessageService;
    private final OrderDraftService orderDraftService;
    private final ErrorLogService errorLogService;
    private final GoogleGmailPollingService googleGmailPollingService;

    public OperationDashboardService(
            com.shipmate.api.inboundmessage.InboundMessageService inboundMessageService,
            OrderDraftService orderDraftService,
            ErrorLogService errorLogService,
            GoogleGmailPollingService googleGmailPollingService
    ) {
        this.inboundMessageService = inboundMessageService;
        this.orderDraftService = orderDraftService;
        this.errorLogService = errorLogService;
        this.googleGmailPollingService = googleGmailPollingService;
    }

    public OperationDashboardResponse snapshot(Integer days) {
        OffsetDateTime from = OffsetDateTime.now().minusDays(normalizeDays(days));
        var inboundMessages = inboundMessageService.recent(500);
        var orderDrafts = orderDraftService.recent(500);
        var submitted = orderDraftService.submitted(500);
        var errors = errorLogService.recent(500);

        inboundMessages = inboundMessages.stream()
                .filter(row -> !row.receivedAt().isBefore(from))
                .toList();
        orderDrafts = orderDrafts.stream()
                .filter(row -> !row.createdAt().isBefore(from))
                .toList();
        submitted = submitted.stream()
                .filter(row -> !row.updatedAt().isBefore(from))
                .toList();
        errors = errors.stream()
                .filter(row -> !row.createdAt().isBefore(from))
                .toList();

        Map<String, Long> statusCounts = orderDrafts.stream()
                .collect(Collectors.groupingBy(row -> row.orderDraftStatus().name(), Collectors.counting()));
        Map<String, Long> sourceChannelCounts = inboundMessages.stream()
                .collect(Collectors.groupingBy(row -> row.sourceChannel().name(), Collectors.counting()));
        Map<String, Long> errorCodeCounts = errors.stream()
                .collect(Collectors.groupingBy(row -> row.code(), Collectors.counting()));
        GoogleGmailPollingStatusResponse gmailStatus = googleGmailPollingService.status();

        return new OperationDashboardResponse(
                inboundMessages.size(),
                orderDrafts.size(),
                submitted.size(),
                errors.size(),
                statusCounts,
                sourceChannelCounts,
                errorCodeCounts,
                gmailStatus.pollingEnabled(),
                gmailStatus.pollingDelayMs(),
                gmailStatus.accountCount(),
                gmailStatus.recentRunCount(),
                gmailStatus.recentFailureCount(),
                gmailStatus.recentSkippedNonOrderCount(),
                gmailStatus.latestFailureMessage(),
                gmailStatus.latestFailureInboundMessageId(),
                gmailStatus.recentFailureCodeCounts()
        );
    }

    private long normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return 1;
        }
        return Math.min(days, 90);
    }
}
