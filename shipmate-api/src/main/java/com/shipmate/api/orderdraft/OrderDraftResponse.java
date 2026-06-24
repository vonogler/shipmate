package com.shipmate.api.orderdraft;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.inboundmessage.SourceChannel;

import java.time.OffsetDateTime;
import java.util.List;

public record OrderDraftResponse(
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
    public static OrderDraftResponse from(OrderDraft orderDraft) {
        return new OrderDraftResponse(
                orderDraft.orderDraftId(),
                orderDraft.sourceInboundMessageId(),
                orderDraft.merchantId(),
                orderDraft.merchantStaffId(),
                orderDraft.customerId(),
                orderDraft.sourceChannel(),
                orderDraft.businessChannel(),
                orderDraft.rawText(),
                orderDraft.itemCandidates(),
                orderDraft.deliveryRecipientName(),
                orderDraft.deliveryRecipientPhone(),
                orderDraft.deliveryPostalCode(),
                orderDraft.deliveryAddress(),
                orderDraft.deliveryAddressDetail(),
                orderDraft.deliveryRequestMemo(),
                orderDraft.deliveryMemo(),
                orderDraft.courierMemo(),
                orderDraft.orderDraftStatus(),
                orderDraft.createdAt(),
                orderDraft.updatedAt()
        );
    }
}
