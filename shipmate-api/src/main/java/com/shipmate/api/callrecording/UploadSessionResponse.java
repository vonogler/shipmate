package com.shipmate.api.callrecording;

public record UploadSessionResponse(
        String recordingId,
        String uploadUrl,
        String objectKey,
        int expiresInSeconds
) {
}
