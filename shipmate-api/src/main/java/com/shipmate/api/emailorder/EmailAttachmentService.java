package com.shipmate.api.emailorder;

import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.config.ShipmateStorageProperties;
import com.shipmate.api.inboundmessage.SourceChannel;
import com.shipmate.api.operation.audit.AuditLogService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailAttachmentService {
    private final EmailAttachmentRepository repository;
    private final MinioClient internalMinioClient;
    private final MinioClient publicMinioClient;
    private final ShipmateStorageProperties storageProperties;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;

    public EmailAttachmentService(
            EmailAttachmentRepository repository,
            @Qualifier("internalMinioClient") MinioClient internalMinioClient,
            @Qualifier("publicMinioClient") MinioClient publicMinioClient,
            ShipmateStorageProperties storageProperties,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.internalMinioClient = internalMinioClient;
        this.publicMinioClient = publicMinioClient;
        this.storageProperties = storageProperties;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
    }

    public EmailAttachment save(
            String merchantId,
            String inboundMessageId,
            SourceChannel sourceChannel,
            String sourceMessageId,
            String fileName,
            String contentType,
            byte[] bytes
    ) {
        String attachmentId = "emla_" + UUID.randomUUID().toString().replace("-", "");
        String normalizedFileName = normalizeFileName(fileName);
        String normalizedContentType = normalizeContentType(contentType);
        String objectKey = objectKey(merchantId, inboundMessageId, attachmentId, normalizedFileName);
        putObject(objectKey, normalizedContentType, bytes);

        EmailAttachment attachment = new EmailAttachment(
                attachmentId,
                merchantId,
                inboundMessageId,
                sourceChannel,
                sourceMessageId,
                normalizedFileName,
                normalizedContentType,
                bytes.length,
                objectKey,
                OffsetDateTime.now()
        );
        repository.save(attachment);
        return attachment;
    }

    public List<EmailAttachmentResponse> listByInboundMessageId(String inboundMessageId) {
        return repository.findByInboundMessageId(inboundMessageId).stream()
                .filter(this::canRead)
                .map(EmailAttachmentResponse::from)
                .toList();
    }

    public EmailAttachmentDownloadUrlResponse downloadUrl(String emailAttachmentId) {
        EmailAttachment attachment = repository.findById(emailAttachmentId)
                .orElseThrow(() -> new ApiRequestException("SM-EML-7201", HttpStatus.NOT_FOUND, "이메일 첨부파일을 찾을 수 없습니다."));
        if (!canRead(attachment)) {
            throw new ApiRequestException("SM-EML-7202", HttpStatus.FORBIDDEN, "이메일 첨부파일에 접근할 권한이 없습니다.");
        }
        String downloadUrl = presignedGetUrl(attachment.objectKey());
        auditLogService.appendForCurrentActor(
                attachment.merchantId(),
                null,
                "EMAIL_ATTACHMENT_DOWNLOAD_URL_CREATED",
                "EMAIL_ATTACHMENT",
                attachment.emailAttachmentId(),
                "이메일 첨부파일 다운로드 URL이 발급되었습니다. fileName=" + attachment.fileName()
        );
        return new EmailAttachmentDownloadUrlResponse(
                attachment.emailAttachmentId(),
                attachment.fileName(),
                downloadUrl,
                storageProperties.presignedUrlExpirySeconds(),
                storageProperties.emailAttachmentRetentionDays()
        );
    }

    @Scheduled(cron = "${shipmate.storage.email-attachment-cleanup-cron:0 30 3 * * *}")
    public void cleanupExpiredAttachmentsOnSchedule() {
        cleanupExpiredAttachments(200);
    }

    public EmailAttachmentCleanupResponse cleanupExpiredAttachments(Integer limit) {
        int retentionDays = storageProperties.emailAttachmentRetentionDays();
        if (retentionDays < 1) {
            return new EmailAttachmentCleanupResponse(
                    retentionDays,
                    OffsetDateTime.now(),
                    0,
                    0,
                    0,
                    0,
                    "이메일 첨부파일 보관 기간 정리가 비활성화되어 있습니다."
            );
        }
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        List<EmailAttachment> expired = repository.findExpired(cutoff, limit == null ? 200 : limit);
        int deletedObjectCount = 0;
        int deletedMetadataCount = 0;
        int failedCount = 0;
        for (EmailAttachment attachment : expired) {
            try {
                removeObject(attachment.objectKey());
                deletedObjectCount++;
                deletedMetadataCount += repository.deleteById(attachment.emailAttachmentId());
            } catch (Exception exception) {
                failedCount++;
            }
        }
        if (!expired.isEmpty()) {
            auditLogService.appendSystem(
                    "EMAIL_ATTACHMENT_RETENTION_CLEANED",
                    "EMAIL_ATTACHMENT",
                    null,
                    "만료 이메일 첨부파일 정리를 실행했습니다. expired=%d, deleted=%d, failed=%d"
                            .formatted(expired.size(), deletedMetadataCount, failedCount)
            );
        }
        return new EmailAttachmentCleanupResponse(
                retentionDays,
                cutoff,
                expired.size(),
                deletedObjectCount,
                deletedMetadataCount,
                failedCount,
                "이메일 첨부파일 보관 기간 정리 완료"
        );
    }

    private void putObject(String objectKey, String contentType, byte[] bytes) {
        try {
            internalMinioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .contentType(contentType)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("이메일 첨부 원본을 객체 저장소에 저장하지 못했습니다.", exception);
        }
    }

    private String presignedGetUrl(String objectKey) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .expiry(storageProperties.presignedUrlExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            throw new ApiRequestException("SM-EML-7203", HttpStatus.BAD_GATEWAY, "이메일 첨부파일 다운로드 URL을 생성하지 못했습니다.");
        }
    }

    private void removeObject(String objectKey) throws Exception {
        internalMinioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(storageProperties.bucket())
                        .object(objectKey)
                        .build()
        );
    }

    private boolean canRead(EmailAttachment attachment) {
        return requestAuthContext.currentUser()
                .map(user -> user.merchantId().equals(attachment.merchantId()))
                .orElse(true);
    }

    private String objectKey(String merchantId, String inboundMessageId, String attachmentId, String fileName) {
        return "email-attachments/%s/%s/%s_%s.%s".formatted(
                sanitizePathPart(merchantId),
                sanitizePathPart(inboundMessageId),
                sanitizeFileNamePart(baseName(fileName)),
                attachmentId.substring("emla_".length(), "emla_".length() + 12),
                extension(fileName)
        );
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "email-attachment.bin";
        }
        String nameOnly = fileName.replace("\\", "/");
        int pathIndex = nameOnly.lastIndexOf('/');
        return pathIndex >= 0 ? nameOnly.substring(pathIndex + 1) : nameOnly;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private String baseName(String fileName) {
        String normalized = normalizeFileName(fileName);
        int extensionIndex = normalized.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return normalized;
        }
        return normalized.substring(0, extensionIndex);
    }

    private String extension(String fileName) {
        String normalized = normalizeFileName(fileName);
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            return "bin";
        }
        return sanitizePathPart(normalized.substring(index + 1).toLowerCase(Locale.ROOT));
    }

    private String sanitizeFileNamePart(String value) {
        String sanitized = value
                .replaceAll("[\\\\/:*?\"<>|]", "-")
                .replaceAll("\\p{Cntrl}", "-")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("-+", "-");
        return sanitized.isBlank() ? "email-attachment" : sanitized;
    }

    private String sanitizePathPart(String value) {
        String sanitized = value == null ? "" : value
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }
}
