package com.shipmate.api.oauth;

import java.time.OffsetDateTime;

record GoogleOAuthState(
        String state,
        String merchantId,
        String actorId,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt
) {
    boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
