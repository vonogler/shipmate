package com.shipmate.api.emailorder;

import com.shipmate.api.inboundmessage.SourceChannel;

import java.time.OffsetDateTime;

public record EmailAttachmentResponse(
        String emailAttachmentId,
        String merchantId,
        String inboundMessageId,
        SourceChannel sourceChannel,
        String sourceMessageId,
        String fileName,
        String contentType,
        long fileSizeBytes,
        OffsetDateTime createdAt
) {
    static EmailAttachmentResponse from(EmailAttachment attachment) {
        return new EmailAttachmentResponse(
                attachment.emailAttachmentId(),
                attachment.merchantId(),
                attachment.inboundMessageId(),
                attachment.sourceChannel(),
                attachment.sourceMessageId(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.fileSizeBytes(),
                attachment.createdAt()
        );
    }
}
