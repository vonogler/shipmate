package com.shipmate.api.aiextraction;

import com.shipmate.api.orderdraft.OrderDraftResponse;

public record AiExtractionResponse(
        String extractionMode,
        OrderDraftResponse orderDraft
) {
}
