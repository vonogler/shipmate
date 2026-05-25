package com.shipmate.api.callrecording;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateUploadSessionRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Min(0) long fileSize,
        @AssertTrue boolean uploadConsent,
        @NotBlank String source,
        String tenantId,
        String userId
) {
}
