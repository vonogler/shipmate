package com.shipmate.api.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertCustomerRequest(
        String customerId,
        @NotBlank String merchantId,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 50) String customerPhone,
        @Size(max = 100) String defaultRecipientName,
        @Size(max = 50) String defaultRecipientPhone,
        @Size(max = 20) String defaultPostalCode,
        @Size(max = 500) String defaultAddress,
        @Size(max = 500) String defaultAddressDetail,
        @Size(max = 200) List<@Size(max = 200) String> favoriteItemNames,
        @Valid @Size(max = 500) List<CustomerPriceItem> priceItems
) {
}
