package com.shipmate.api.emailorder;

import java.time.OffsetDateTime;

public record EmailMailboxConfig(
        String merchantId,
        String host,
        int port,
        String username,
        String encryptedPasswordMemo,
        boolean sslEnabled,
        String collectionStatus,
        OffsetDateTime updatedAt
) {
    EmailMailboxConfigResponse toResponse(String collectionStatus) {
        return new EmailMailboxConfigResponse(merchantId, host, port, username, sslEnabled, collectionStatus == null ? this.collectionStatus : collectionStatus, updatedAt);
    }
}
