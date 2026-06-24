package com.shipmate.api.emailorder;

import java.time.OffsetDateTime;

public record EmailAttachmentCleanupResponse(
        int retentionDays,
        OffsetDateTime cutoffCreatedAt,
        int expiredAttachmentCount,
        int deletedObjectCount,
        int deletedMetadataCount,
        int failedCount,
        String status
) {
}
