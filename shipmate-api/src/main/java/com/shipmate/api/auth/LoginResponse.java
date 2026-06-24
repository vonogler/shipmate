package com.shipmate.api.auth;

public record LoginResponse(
        String merchantUserId,
        String merchantId,
        String loginId,
        String displayName,
        String role,
        String accessToken
) {
    static LoginResponse from(MerchantUser user, String accessToken) {
        return new LoginResponse(
                user.merchantUserId(),
                user.merchantId(),
                user.loginId(),
                user.displayName(),
                user.role(),
                accessToken
        );
    }
}
