package com.shipmate.api.orderdraft;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateOrderDraftItemCandidateRequest(
        String orderDraftItemCandidateId,
        @Size(max = 300) String itemName,
        @Size(max = 500) String itemSpec,
        @Size(max = 100) String itemQuantity,
        @Size(max = 50) String itemUnit,
        BigDecimal matchedUnitPrice,
        @Size(max = 100) String packagingWidth,
        @Size(max = 100) String packagingDepth,
        @Size(max = 100) String packagingHeight,
        @Size(max = 200) String materialName,
        Boolean printRequired,
        Boolean designFileProvided,
        @Size(max = 100) String productionQuantity,
        @Size(max = 50) String requestedDueDate,
        Boolean sampleRequested,
        @Size(max = 200) String customProductType,
        @Size(max = 200) String imprintMethod,
        @Size(max = 100) String itemColor,
        @Size(max = 500) String customOptionText,
        Boolean proofConfirmed,
        Boolean productionConfirmed,
        @Size(max = 1000) String lineMemo
) {
}
