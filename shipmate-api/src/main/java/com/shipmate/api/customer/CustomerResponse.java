package com.shipmate.api.customer;

import java.time.OffsetDateTime;
import java.util.List;

public record CustomerResponse(
        String customerId,
        String merchantId,
        String customerName,
        String customerPhone,
        String defaultRecipientName,
        String defaultRecipientPhone,
        String defaultPostalCode,
        String defaultAddress,
        String defaultAddressDetail,
        List<String> favoriteItemNames,
        List<CustomerPriceItem> priceItems,
        OffsetDateTime deletedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.customerId(),
                customer.merchantId(),
                customer.customerName(),
                customer.customerPhone(),
                customer.defaultRecipientName(),
                customer.defaultRecipientPhone(),
                customer.defaultPostalCode(),
                customer.defaultAddress(),
                customer.defaultAddressDetail(),
                customer.favoriteItemNames(),
                customer.priceItems(),
                customer.deletedAt(),
                customer.createdAt(),
                customer.updatedAt()
        );
    }
}
