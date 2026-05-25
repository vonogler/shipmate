package com.shipmate.api.callrecording;

public record CompleteUploadResponse(
        String recordingId,
        CallRecordingStatus status
) {
}
