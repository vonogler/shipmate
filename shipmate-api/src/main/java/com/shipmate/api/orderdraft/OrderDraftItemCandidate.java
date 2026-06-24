package com.shipmate.api.orderdraft;

import java.math.BigDecimal;

public record OrderDraftItemCandidate(
        String orderDraftItemCandidateId,
        String itemName,
        String itemSpec,
        String itemQuantity,
        String itemUnit,
        BigDecimal matchedUnitPrice,
        String packagingWidth,
        String packagingDepth,
        String packagingHeight,
        String materialName,
        Boolean printRequired,
        Boolean designFileProvided,
        String productionQuantity,
        String requestedDueDate,
        Boolean sampleRequested,
        String customProductType,
        String imprintMethod,
        String itemColor,
        String customOptionText,
        Boolean proofConfirmed,
        Boolean productionConfirmed,
        String lineMemo
) {
}
