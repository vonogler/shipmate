package com.shipmate.api.inboundmessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateInboundMessageRequest(
        @NotBlank
        String merchantId,
        String merchantStaffId,
        String customerId,
        @NotNull
        SourceChannel sourceChannel,
        @NotNull
        BusinessChannel businessChannel,
        @NotBlank
        @Size(max = 20000)
        String rawText,
        OffsetDateTime receivedAt
) {
}
