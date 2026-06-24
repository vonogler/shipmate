package com.shipmate.api.shippingorder;

public record CourierCsvResponse(
        String fileName,
        String content
) {
}
