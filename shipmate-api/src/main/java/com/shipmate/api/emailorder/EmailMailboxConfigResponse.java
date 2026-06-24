package com.shipmate.api.emailorder;

import java.time.OffsetDateTime;

public record EmailMailboxConfigResponse(
        String merchantId,
        String host,
        int port,
        String username,
        boolean sslEnabled,
        String collectionStatus,
        OffsetDateTime updatedAt
) {
}
