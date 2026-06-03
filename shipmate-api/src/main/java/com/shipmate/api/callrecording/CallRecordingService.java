package com.shipmate.api.callrecording;

import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.config.ShipmateStorageProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class CallRecordingService {
    private static final Logger log = LoggerFactory.getLogger(CallRecordingService.class);

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
            log.warn(
                    "UPLOAD_SESSION_REJECTED code=SM-UPL-2001 fileName={} source={} reason=missing_consent",
                    request.fileName(),
                    request.source()
            );
            throw new ApiRequestException("SM-UPL-2001", HttpStatus.BAD_REQUEST, "Upload consent is required.");
        }
        if (!request.contentType().toLowerCase(Locale.ROOT).startsWith("audio/")) {
            log.warn(
                    "UPLOAD_SESSION_REJECTED code=SM-UPL-2002 fileName={} contentType={} source={} reason=unsupported_content_type",
                    request.fileName(),
                    request.contentType(),
                    request.source()
            );
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
        log.info(
                "UPLOAD_SESSION_CREATED recordingId={} fileName={} source={} fileSize={} contentType={} objectKey={}",
                recording.recordingId(),
                recording.fileName(),
                recording.source(),
                recording.fileSize(),
                recording.contentType(),
                recording.objectKey()
        );

        return new UploadSessionResponse(
                recordingId,
                presignedPutUrl(objectKey),
                objectKey,
                storageProperties.presignedUrlExpirySeconds()
        );
    }

    public CompleteUploadResponse completeUpload(String recordingId) {
        log.info("UPLOAD_COMPLETE_REQUESTED recordingId={}", recordingId);
        CallRecording recording = repository.findById(recordingId)
                .orElseThrow(() -> {
                    log.warn("UPLOAD_COMPLETE_FAILED code=SM-UPL-2006 recordingId={} reason=not_found", recordingId);
                    return new ApiRequestException("SM-UPL-2006", HttpStatus.NOT_FOUND, "Recording not found.");
                });

        verifyObjectExists(recording.objectKey());
        CallRecording uploaded = recording.withUploaded();
        repository.save(uploaded);
        log.info(
                "UPLOAD_COMPLETED recordingId={} fileName={} source={} objectKey={} uploadedAt={}",
                uploaded.recordingId(),
                uploaded.fileName(),
                uploaded.source(),
                uploaded.objectKey(),
                uploaded.uploadedAt()
        );
        return new CompleteUploadResponse(uploaded.recordingId(), uploaded.status());
    }

    public java.util.List<RecentCallRecordingResponse> recent() {
        return repository.findRecent(50).stream()
                .map(RecentCallRecordingResponse::from)
                .toList();
    }

    public void delete(String recordingId) {
        log.info("UPLOAD_DELETE_REQUESTED recordingId={}", recordingId);
        CallRecording recording = repository.findById(recordingId)
                .orElseThrow(() -> {
                    log.warn("UPLOAD_DELETE_FAILED code=SM-UPL-2006 recordingId={} reason=not_found", recordingId);
                    return new ApiRequestException("SM-UPL-2006", HttpStatus.NOT_FOUND, "Recording not found.");
                });

        deleteObjectIfExists(recording.objectKey());
        repository.deleteById(recordingId);
        log.info(
                "UPLOAD_DELETED recordingId={} fileName={} source={} objectKey={}",
                recording.recordingId(),
                recording.fileName(),
                recording.source(),
                recording.objectKey()
        );
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
            log.warn("UPLOAD_SESSION_FAILED code=SM-UPL-2007 objectKey={} reason={}", objectKey, exception.getMessage());
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
            log.warn("UPLOAD_VERIFY_FAILED code=SM-UPL-2005 objectKey={} reason={}", objectKey, exception.getMessage());
            throw new ApiRequestException("SM-UPL-2005", HttpStatus.BAD_GATEWAY, "Object storage upload verification failed.");
        }
    }

    private void deleteObjectIfExists(String objectKey) {
        try {
            internalMinioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            log.warn("UPLOAD_DELETE_FAILED code=SM-UPL-2008 objectKey={} reason={}", objectKey, exception.getMessage());
            throw new ApiRequestException("SM-UPL-2008", HttpStatus.BAD_GATEWAY, "Object storage delete failed.");
        }
    }

    private String objectKey(String tenantId, String userId, String recordingId, String fileName) {
        return "call-recordings/%s/%s/%s_%s.%s".formatted(
                sanitizePathPart(tenantId),
                sanitizePathPart(userId),
                sanitizeFileNamePart(baseName(fileName)),
                shortRecordingId(recordingId),
                extension(fileName)
        );
    }

    private String baseName(String fileName) {
        String normalized = fileName == null || fileName.isBlank() ? "call-recording" : fileName;
        String nameOnly = normalized.replace("\\", "/");
        int pathIndex = nameOnly.lastIndexOf('/');
        if (pathIndex >= 0) {
            nameOnly = nameOnly.substring(pathIndex + 1);
        }

        int extensionIndex = nameOnly.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return nameOnly;
        }
        return nameOnly.substring(0, extensionIndex);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "audio";
        }
        return sanitizePathPart(fileName.substring(index + 1).toLowerCase(Locale.ROOT));
    }

    private String sanitizeFileNamePart(String value) {
        String sanitized = value
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\p{Cntrl}", "-")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
        return sanitized.isBlank() ? "call-recording" : sanitized;
    }

    private String shortRecordingId(String recordingId) {
        String sanitized = sanitizePathPart(recordingId.replaceFirst("^rec_", ""));
        return sanitized.substring(0, Math.min(8, sanitized.length()));
    }

    private String sanitizePathPart(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
