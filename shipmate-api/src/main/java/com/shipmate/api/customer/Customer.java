package com.shipmate.api.customer;

import java.time.OffsetDateTime;
import java.util.List;

public record Customer(
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
    public Customer {
        favoriteItemNames = favoriteItemNames == null ? List.of() : List.copyOf(favoriteItemNames);
        priceItems = priceItems == null ? List.of() : List.copyOf(priceItems);
    }

    public Customer markDeleted(OffsetDateTime deletedAt) {
        return new Customer(
                customerId,
                merchantId,
                customerName,
                customerPhone,
                defaultRecipientName,
                defaultRecipientPhone,
                defaultPostalCode,
                defaultAddress,
                defaultAddressDetail,
                favoriteItemNames,
                priceItems,
                deletedAt,
                createdAt,
                deletedAt
        );
    }
}
