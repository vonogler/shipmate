package com.shipmate.api.callrecording;

import java.time.OffsetDateTime;

public record RecentCallRecordingResponse(
        String recordingId,
        String fileName,
        String source,
        CallRecordingStatus status,
        long fileSize,
        String objectKey,
        OffsetDateTime uploadedAt,
        OffsetDateTime createdAt,
        String errorCode
) {
    static RecentCallRecordingResponse from(CallRecording recording) {
        return new RecentCallRecordingResponse(
                recording.recordingId(),
                recording.fileName(),
                recording.source(),
                recording.status(),
                recording.fileSize(),
                recording.objectKey(),
                recording.uploadedAt(),
                recording.createdAt(),
                recording.errorCode()
        );
    }
}
