package com.shipmate.api.shippingorder;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkCourierCsvRequest(
        @NotEmpty List<String> orderDraftIds,
        CourierCsvTemplate courierCsvTemplate
) {
}
