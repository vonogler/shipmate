package com.shipmate.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMerchantUserRequest(
        @NotBlank String merchantId,
        @NotBlank @Size(max = 100) String loginId,
        @NotBlank @Size(min = 4, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 50) String role
) {
}
