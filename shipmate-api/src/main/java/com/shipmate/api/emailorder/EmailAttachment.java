package com.shipmate.api.emailorder;

import com.shipmate.api.inboundmessage.SourceChannel;

import java.time.OffsetDateTime;

public record EmailAttachment(
        String emailAttachmentId,
        String merchantId,
        String inboundMessageId,
        SourceChannel sourceChannel,
        String sourceMessageId,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String objectKey,
        OffsetDateTime createdAt
) {
}
