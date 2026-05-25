package com.shipmate.api.callrecording;

import java.time.OffsetDateTime;

public record CallRecording(
        String recordingId,
        String tenantId,
        String userId,
        String fileName,
        String contentType,
        long fileSize,
        boolean uploadConsent,
        String source,
        String objectKey,
        CallRecordingStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime uploadedAt,
        String errorCode
) {
    CallRecording withUploadUrlIssued() {
        return new CallRecording(
                recordingId,
                tenantId,
                userId,
                fileName,
                contentType,
                fileSize,
                uploadConsent,
                source,
                objectKey,
                CallRecordingStatus.UPLOAD_URL_ISSUED,
                createdAt,
                uploadedAt,
                errorCode
        );
    }

    CallRecording withUploaded() {
        return new CallRecording(
                recordingId,
                tenantId,
                userId,
                fileName,
                contentType,
                fileSize,
                uploadConsent,
                source,
                objectKey,
                CallRecordingStatus.ANALYZING,
                createdAt,
                OffsetDateTime.now(),
                null
        );
    }
}
