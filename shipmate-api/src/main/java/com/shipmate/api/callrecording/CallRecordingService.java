package com.shipmate.api.callrecording;

import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.config.ShipmateStorageProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class CallRecordingService {
    private final CallRecordingRepository repository;
    private final MinioClient internalMinioClient;
    private final MinioClient publicMinioClient;
    private final ShipmateStorageProperties storageProperties;

    public CallRecordingService(
            CallRecordingRepository repository,
            @Qualifier("internalMinioClient") MinioClient internalMinioClient,
            @Qualifier("publicMinioClient") MinioClient publicMinioClient,
            ShipmateStorageProperties storageProperties
    ) {
        this.repository = repository;
        this.internalMinioClient = internalMinioClient;
        this.publicMinioClient = publicMinioClient;
        this.storageProperties = storageProperties;
    }

    public UploadSessionResponse createUploadSession(CreateUploadSessionRequest request) {
        if (!request.uploadConsent()) {
            throw new ApiRequestException("SM-UPL-2001", HttpStatus.BAD_REQUEST, "Upload consent is required.");
        }
        if (!request.contentType().toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new ApiRequestException("SM-UPL-2002", HttpStatus.BAD_REQUEST, "Unsupported audio content type.");
        }

        String recordingId = "rec_" + UUID.randomUUID().toString().replace("-", "");
        String tenantId = valueOrDefault(request.tenantId(), "local-tenant");
        String userId = valueOrDefault(request.userId(), "local-user");
        String objectKey = objectKey(tenantId, userId, recordingId, request.fileName());

        CallRecording recording = new CallRecording(
                recordingId,
                tenantId,
                userId,
                request.fileName(),
                request.contentType(),
                request.fileSize(),
                request.uploadConsent(),
                request.source(),
                objectKey,
                CallRecordingStatus.CREATED,
                OffsetDateTime.now(),
                null,
                null
        ).withUploadUrlIssued();

        repository.save(recording);

        return new UploadSessionResponse(
                recordingId,
                presignedPutUrl(objectKey),
                objectKey,
                storageProperties.presignedUrlExpirySeconds()
        );
    }

    public CompleteUploadResponse completeUpload(String recordingId) {
        CallRecording recording = repository.findById(recordingId)
                .orElseThrow(() -> new ApiRequestException("SM-UPL-2006", HttpStatus.NOT_FOUND, "Recording not found."));

        verifyObjectExists(recording.objectKey());
        CallRecording uploaded = recording.withUploaded();
        repository.save(uploaded);
        return new CompleteUploadResponse(uploaded.recordingId(), uploaded.status());
    }

    public java.util.List<RecentCallRecordingResponse> recent() {
        return repository.findRecent(50).stream()
                .map(RecentCallRecordingResponse::from)
                .toList();
    }

    private String presignedPutUrl(String objectKey) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .expiry(storageProperties.presignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            throw new ApiRequestException("SM-UPL-2007", HttpStatus.BAD_GATEWAY, "Unable to create upload URL.");
        }
    }

    private void verifyObjectExists(String objectKey) {
        try {
            internalMinioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new ApiRequestException("SM-UPL-2005", HttpStatus.BAD_GATEWAY, "Object storage upload verification failed.");
        }
    }

    private String objectKey(String tenantId, String userId, String recordingId, String fileName) {
        return "call-recordings/%s/%s/%s/original.%s".formatted(
                sanitizePathPart(tenantId),
                sanitizePathPart(userId),
                sanitizePathPart(recordingId),
                extension(fileName)
        );
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "audio";
        }
        return sanitizePathPart(fileName.substring(index + 1).toLowerCase(Locale.ROOT));
    }

    private String sanitizePathPart(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
