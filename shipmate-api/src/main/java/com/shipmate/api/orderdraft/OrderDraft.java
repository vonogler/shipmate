package com.shipmate.api.orderdraft;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.inboundmessage.SourceChannel;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderDraft(
        String orderDraftId,
        String sourceInboundMessageId,
        String merchantId,
        String merchantStaffId,
        String customerId,
        SourceChannel sourceChannel,
        BusinessChannel businessChannel,
        String rawText,
        List<OrderDraftItemCandidate> itemCandidates,
        String deliveryRecipientName,
        String deliveryRecipientPhone,
        String deliveryPostalCode,
        String deliveryAddress,
        String deliveryAddressDetail,
        String deliveryRequestMemo,
        String deliveryMemo,
        String courierMemo,
        OrderDraftStatus orderDraftStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public OrderDraft {
        itemCandidates = itemCandidates == null ? List.of() : List.copyOf(itemCandidates);
    }

    public OrderDraft withReview(
            List<OrderDraftItemCandidate> itemCandidates,
            String deliveryRecipientName,
            String deliveryRecipientPhone,
            String deliveryPostalCode,
            String deliveryAddress,
            String deliveryAddressDetail,
            String deliveryRequestMemo,
            String deliveryMemo,
            String courierMemo,
            OffsetDateTime updatedAt
    ) {
        return new OrderDraft(
                orderDraftId,
                sourceInboundMessageId,
                merchantId,
                merchantStaffId,
                customerId,
                sourceChannel,
                businessChannel,
                rawText,
                itemCandidates,
                deliveryRecipientName,
                deliveryRecipientPhone,
                deliveryPostalCode,
                deliveryAddress,
                deliveryAddressDetail,
                deliveryRequestMemo,
                deliveryMemo,
                courierMemo,
                orderDraftStatus == OrderDraftStatus.NEEDS_REVIEW ? OrderDraftStatus.CREATED : orderDraftStatus,
                createdAt,
                updatedAt
        );
    }

    public OrderDraft withStatus(OrderDraftStatus orderDraftStatus, OffsetDateTime updatedAt) {
        return new OrderDraft(
                orderDraftId,
                sourceInboundMessageId,
                merchantId,
                merchantStaffId,
                customerId,
                sourceChannel,
                businessChannel,
                rawText,
                itemCandidates,
                deliveryRecipientName,
                deliveryRecipientPhone,
                deliveryPostalCode,
                deliveryAddress,
                deliveryAddressDetail,
                deliveryRequestMemo,
                deliveryMemo,
                courierMemo,
                orderDraftStatus,
                createdAt,
                updatedAt
        );
    }
}
