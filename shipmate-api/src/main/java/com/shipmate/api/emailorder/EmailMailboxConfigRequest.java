package com.shipmate.api.emailorder;

import jakarta.validation.constraints.NotBlank;

public record EmailMailboxConfigRequest(
        @NotBlank String merchantId,
        @NotBlank String host,
        int port,
        @NotBlank String username,
        @NotBlank String encryptedPasswordMemo,
        boolean sslEnabled
) {
}
