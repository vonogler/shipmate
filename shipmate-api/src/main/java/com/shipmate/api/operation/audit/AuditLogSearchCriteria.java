package com.shipmate.api.operation.audit;

public record AuditLogSearchCriteria(
        String actionType,
        String targetType,
        String merchantId,
        String metadataKey,
        String searchText,
        Integer days,
        Integer limit,
        Integer offset
) {
}
