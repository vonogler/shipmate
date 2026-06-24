package com.shipmate.api.customer;

import java.math.BigDecimal;

public record CustomerPriceItem(
        String customerPriceItemId,
        String itemNameKeyword,
        String itemSpecKeyword,
        String itemUnit,
        BigDecimal unitPrice
) {
}
