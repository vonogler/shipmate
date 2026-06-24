package com.shipmate.api.auth;

import java.time.OffsetDateTime;

public record MerchantUser(
        String merchantUserId,
        String merchantId,
        String loginId,
        String passwordHash,
        String displayName,
        String role,
        OffsetDateTime lockedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
