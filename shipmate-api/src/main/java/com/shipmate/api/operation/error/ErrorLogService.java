package com.shipmate.api.operation.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ErrorLogService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final ErrorLogRepository repository;
    private final ObjectMapper objectMapper;

    public ErrorLogService(ErrorLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ErrorLog append(String code, int status, String message, Throwable throwable) {
        return append(code, status, message, throwable, null);
    }

    public ErrorLog append(String code, int status, String message, Throwable throwable, ErrorRequestContext context) {
        return repository.append(new ErrorLog(
                "err_" + UUID.randomUUID().toString().replace("-", ""),
                code,
                status,
                message,
                throwable == null ? null : throwable.getClass().getName(),
                metadataJson(code, status, throwable, context),
                "OPEN",
                null,
                null,
                null,
                OffsetDateTime.now()
        ));
    }

    private String metadataJson(String code, int status, Throwable throwable, ErrorRequestContext context) {
        try {
            return objectMapper.writeValueAsString(new ErrorMetadata(
                    code,
                    status,
                    throwable == null ? null : throwable.getClass().getName(),
                    context == null ? null : context.method(),
                    context == null ? null : context.path(),
                    context == null ? null : context.merchantId(),
                    context == null ? null : context.actorId()
            ));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    public List<ErrorLog> recent(Integer limit) {
        return repository.findRecent(normalizeLimit(limit));
    }

    public List<ErrorLog> search(String metadataKey, String searchText, Integer days, Integer limit) {
        return repository.search(new ErrorLogSearchCriteria(
                blankToNull(metadataKey),
                blankToNull(searchText),
                null,
                days,
                normalizeLimit(limit),
                0
        ));
    }

    public ErrorLogPageResponse searchPage(String metadataKey, String searchText, String handlingStatus, Integer days, Integer limit, Integer offset) {
        int normalizedLimit = normalizeLimit(limit);
        int normalizedOffset = offset == null ? 0 : Math.max(0, offset);
        ErrorLogSearchCriteria criteria = new ErrorLogSearchCriteria(
                blankToNull(metadataKey),
                blankToNull(searchText),
                blankToNull(handlingStatus),
                days,
                normalizedLimit,
                normalizedOffset
        );
        return new ErrorLogPageResponse(
                repository.search(criteria),
                repository.count(criteria),
                normalizedLimit,
                normalizedOffset
        );
    }

    public ErrorLog updateHandling(String errorLogId, String handlingStatus, String assignedActorId, String handlingMemo) {
        String normalizedStatus = normalizeHandlingStatus(handlingStatus);
        OffsetDateTime handledAt = normalizedStatus.equals("RESOLVED") ? OffsetDateTime.now() : null;
        return repository.updateHandling(
                errorLogId,
                normalizedStatus,
                blankToNull(assignedActorId),
                blankToNull(handlingMemo),
                handledAt
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeHandlingStatus(String handlingStatus) {
        String normalized = handlingStatus == null ? "" : handlingStatus.strip();
        if (normalized.equals("OPEN") || normalized.equals("ACKNOWLEDGED") || normalized.equals("RESOLVED")) {
            return normalized;
        }
        return "OPEN";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public record ErrorRequestContext(
            String method,
            String path,
            String merchantId,
            String actorId
    ) {
    }

    private record ErrorMetadata(
            String code,
            int status,
            String exceptionType,
            String requestMethod,
            String requestPath,
            String merchantId,
            String actorId
    ) {
    }
}
