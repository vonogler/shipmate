package com.shipmate.api.oauth;

import java.time.OffsetDateTime;

record GoogleOAuthToken(
        String merchantId,
        String googleEmail,
        String accessToken,
        String refreshToken,
        String scope,
        String tokenType,
        OffsetDateTime expiresAt,
        OffsetDateTime updatedAt
) {
}
