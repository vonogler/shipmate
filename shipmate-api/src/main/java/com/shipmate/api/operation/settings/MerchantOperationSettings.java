package com.shipmate.api.operation.settings;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.shippingorder.CourierCsvTemplate;

import java.time.OffsetDateTime;

public record MerchantOperationSettings(
        String merchantId,
        String defaultMerchantStaffId,
        CourierCsvTemplate defaultCourierCsvTemplate,
        BusinessChannel defaultBusinessChannel,
        OffsetDateTime updatedAt
) {
}
