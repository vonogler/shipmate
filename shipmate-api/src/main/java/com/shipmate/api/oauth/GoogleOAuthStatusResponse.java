package com.shipmate.api.oauth;

import java.time.OffsetDateTime;

public record GoogleOAuthStatusResponse(
        boolean connected,
        String merchantId,
        String googleEmail,
        boolean hasRefreshToken,
        OffsetDateTime expiresAt,
        OffsetDateTime updatedAt
) {
    static GoogleOAuthStatusResponse disconnected(String merchantId) {
        return new GoogleOAuthStatusResponse(false, merchantId, null, false, null, null);
    }

    static GoogleOAuthStatusResponse connected(GoogleOAuthToken token) {
        return new GoogleOAuthStatusResponse(
                true,
                token.merchantId(),
                token.googleEmail(),
                token.refreshToken() != null && !token.refreshToken().isBlank(),
                token.expiresAt(),
                token.updatedAt()
        );
    }
}
