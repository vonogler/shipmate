package com.shipmate.api.inboundmessage;

import java.time.OffsetDateTime;

public record InboundMessage(
        String inboundMessageId,
        String merchantId,
        String merchantStaffId,
        String customerId,
        SourceChannel sourceChannel,
        BusinessChannel businessChannel,
        String rawText,
        InboundMessageStatus inboundMessageStatus,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public InboundMessage withStatus(InboundMessageStatus inboundMessageStatus, OffsetDateTime updatedAt) {
        return new InboundMessage(
                inboundMessageId,
                merchantId,
                merchantStaffId,
                customerId,
                sourceChannel,
                businessChannel,
                rawText,
                inboundMessageStatus,
                receivedAt,
                createdAt,
                updatedAt
        );
    }
}
