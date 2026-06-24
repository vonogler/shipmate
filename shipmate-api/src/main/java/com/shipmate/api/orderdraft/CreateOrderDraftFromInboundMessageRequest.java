package com.shipmate.api.orderdraft;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderDraftFromInboundMessageRequest(
        @NotBlank String sourceInboundMessageId
) {
}
