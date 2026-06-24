package com.shipmate.api.admin;

import java.time.OffsetDateTime;

public record AdminMerchantProfile(
        String merchantId,
        String merchantStatus,
        String operationMemo,
        OffsetDateTime updatedAt
) {
}
