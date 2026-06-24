package com.shipmate.api.operation.audit;

import java.time.OffsetDateTime;

public record AuditLog(
        String auditLogId,
        String merchantId,
        String actorId,
        String actionType,
        String targetType,
        String targetId,
        String summary,
        String auditMetadataJson,
        OffsetDateTime createdAt
) {
}
