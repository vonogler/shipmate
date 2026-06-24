package com.shipmate.api.emailorder;

import com.shipmate.api.inboundmessage.BusinessChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmailOrderRequest(
        @NotBlank String merchantId,
        String merchantStaffId,
        String customerId,
        @NotNull BusinessChannel businessChannel,
        @Email String fromEmail,
        @Size(max = 500) String subject,
        @NotBlank @Size(max = 20000) String body
) {
}
