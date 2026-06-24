package com.shipmate.api.emailorder;

public record EmailAttachmentDownloadUrlResponse(
        String emailAttachmentId,
        String fileName,
        String downloadUrl,
        int expiresInSeconds,
        int retentionDays
) {
}
