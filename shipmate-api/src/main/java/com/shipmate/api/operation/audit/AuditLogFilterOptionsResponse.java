package com.shipmate.api.operation.audit;

import java.util.List;

public record AuditLogFilterOptionsResponse(
        List<String> actionTypes,
        List<String> targetTypes,
        List<String> merchantIds,
        List<String> metadataKeys
) {
}
