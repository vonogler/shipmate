package com.shipmate.api.operation.settings;

import com.shipmate.api.inboundmessage.BusinessChannel;
import com.shipmate.api.shippingorder.CourierCsvTemplate;
import jakarta.validation.constraints.NotBlank;

public record MerchantOperationSettingsRequest(
        @NotBlank String merchantId,
        String defaultMerchantStaffId,
        CourierCsvTemplate defaultCourierCsvTemplate,
        BusinessChannel defaultBusinessChannel
) {
}
