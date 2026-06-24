package com.shipmate.api.orderdraft;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOrderDraftReviewRequest(
        @Valid @Size(max = 200) List<UpdateOrderDraftItemCandidateRequest> itemCandidates,
        @Size(max = 100) String deliveryRecipientName,
        @Size(max = 50) String deliveryRecipientPhone,
        @Size(max = 20) String deliveryPostalCode,
        @Size(max = 500) String deliveryAddress,
        @Size(max = 500) String deliveryAddressDetail,
        @Size(max = 1000) String deliveryRequestMemo,
        @Size(max = 2000) String deliveryMemo,
        @Size(max = 2000) String courierMemo
) {
}
