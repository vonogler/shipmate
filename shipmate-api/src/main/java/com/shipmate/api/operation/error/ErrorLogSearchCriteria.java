package com.shipmate.api.operation.error;

public record ErrorLogSearchCriteria(
        String metadataKey,
        String searchText,
        String handlingStatus,
        Integer days,
        Integer limit,
        Integer offset
) {
}
