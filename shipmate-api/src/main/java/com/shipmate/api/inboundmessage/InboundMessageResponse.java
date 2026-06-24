package com.shipmate.api.inboundmessage;

import java.time.OffsetDateTime;

public record InboundMessageResponse(
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
    static InboundMessageResponse from(InboundMessage inboundMessage) {
        return new InboundMessageResponse(
                inboundMessage.inboundMessageId(),
                inboundMessage.merchantId(),
                inboundMessage.merchantStaffId(),
                inboundMessage.customerId(),
                inboundMessage.sourceChannel(),
                inboundMessage.businessChannel(),
                inboundMessage.rawText(),
                inboundMessage.inboundMessageStatus(),
                inboundMessage.receivedAt(),
                inboundMessage.createdAt(),
                inboundMessage.updatedAt()
        );
    }
}
