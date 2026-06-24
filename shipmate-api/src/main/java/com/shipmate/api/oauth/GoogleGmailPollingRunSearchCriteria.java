package com.shipmate.api.oauth;

public record GoogleGmailPollingRunSearchCriteria(
        String merchantId,
        Boolean success,
        Integer days,
        int limit
) {
}
