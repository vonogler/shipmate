package com.shipmate.api.shippingorder;

import java.util.List;

public record CourierCsvValidationResponse(
        boolean valid,
        List<String> errors,
        List<CourierCsvValidationError> errorDetails
) {
}
