package com.shipmate.api.operation.audit;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLog> rows,
        long total,
        int limit,
        int offset
) {
}
