package com.shipmate.api.operation.error;

import java.time.OffsetDateTime;

public record ErrorLog(
        String errorLogId,
        String code,
        int status,
        String message,
        String exceptionType,
        String errorMetadataJson,
        String handlingStatus,
        String assignedActorId,
        String handlingMemo,
        OffsetDateTime handledAt,
        OffsetDateTime createdAt
) {
}
