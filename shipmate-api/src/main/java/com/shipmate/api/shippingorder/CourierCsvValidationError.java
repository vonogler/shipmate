package com.shipmate.api.shippingorder;

public record CourierCsvValidationError(
        String orderDraftId,
        String fieldName,
        String csvColumnName,
        String message
) {
}
