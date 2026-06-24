package com.shipmate.api.shippingorder;

public record CourierCsvValidationRule(
        String courierCsvValidationRuleId,
        CourierCsvTemplate courierCsvTemplate,
        String fieldName,
        String csvColumnName,
        boolean required,
        String errorMessage,
        int sortOrder,
        boolean active
) {
}
