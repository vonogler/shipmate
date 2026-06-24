package com.shipmate.api.operation.audit;

import com.shipmate.api.auth.RequestAuthContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_OFFSET = 100_000;

    private final AuditLogRepository repository;
    private final RequestAuthContext requestAuthContext;

    public AuditLogService(AuditLogRepository repository, RequestAuthContext requestAuthContext) {
        this.repository = repository;
        this.requestAuthContext = requestAuthContext;
    }

    public AuditLog append(String merchantId, String actorId, String actionType, String targetType, String targetId, String summary) {
        return append(merchantId, actorId, actionType, targetType, targetId, summary, null);
    }

    public AuditLog append(
            String merchantId,
            String actorId,
            String actionType,
            String targetType,
            String targetId,
            String summary,
            String auditMetadataJson
    ) {
        return repository.append(new AuditLog(
                "aud_" + UUID.randomUUID().toString().replace("-", ""),
                merchantId,
                actorId,
                actionType,
                targetType,
                targetId,
                summary,
                auditMetadataJson,
                OffsetDateTime.now()
        ));
    }

    public AuditLog appendForCurrentActor(
            String merchantId,
            String fallbackActorId,
            String actionType,
            String targetType,
            String targetId,
            String summary
    ) {
        return append(
                requestAuthContext.currentMerchantIdOr(merchantId),
                requestAuthContext.currentActorIdOr(fallbackActorId),
                actionType,
                targetType,
                targetId,
                summary
        );
    }

    public AuditLog appendForCurrentActor(
            String merchantId,
            String fallbackActorId,
            String actionType,
            String targetType,
            String targetId,
            String summary,
            String auditMetadataJson
    ) {
        return append(
                requestAuthContext.currentMerchantIdOr(merchantId),
                requestAuthContext.currentActorIdOr(fallbackActorId),
                actionType,
                targetType,
                targetId,
                summary,
                auditMetadataJson
        );
    }

    public AuditLog appendSystem(String actionType, String targetType, String targetId, String summary) {
        return append("operation", null, actionType, targetType, targetId, summary);
    }

    public List<AuditLog> recent(Integer limit) {
        return repository.findRecent(normalizeLimit(limit));
    }

    public AuditLogPageResponse search(AuditLogSearchCriteria criteria) {
        AuditLogSearchCriteria normalized = new AuditLogSearchCriteria(
                normalizeFilter(criteria.actionType()),
                normalizeFilter(criteria.targetType()),
                normalizeFilter(criteria.merchantId()),
                normalizeFilter(criteria.metadataKey()),
                normalizeFilter(criteria.searchText()),
                normalizeDays(criteria.days()),
                normalizeLimit(criteria.limit()),
                normalizeOffset(criteria.offset())
        );
        return new AuditLogPageResponse(
                repository.search(normalized),
                repository.count(normalized),
                normalized.limit(),
                normalized.offset()
        );
    }

    public AuditLogFilterOptionsResponse filterOptions() {
        return repository.filterOptions();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null || offset < 1) {
            return 0;
        }
        return Math.min(offset, MAX_OFFSET);
    }

    private Integer normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return null;
        }
        return Math.min(days, 365);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || value.equals("ALL")) {
            return null;
        }
        return value.strip();
    }
}
