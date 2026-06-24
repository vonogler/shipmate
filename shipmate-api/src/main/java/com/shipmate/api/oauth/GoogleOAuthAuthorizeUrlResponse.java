package com.shipmate.api.oauth;

import java.time.OffsetDateTime;

public record GoogleOAuthAuthorizeUrlResponse(
        String authorizationUrl,
        String state,
        OffsetDateTime expiresAt
) {
}
