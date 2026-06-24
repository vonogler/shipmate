package com.shipmate.api.inboundmessage;

import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.operation.audit.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InboundMessageService {
    private static final Logger log = LoggerFactory.getLogger(InboundMessageService.class);
    private static final int DEFAULT_RECENT_LIMIT = 50;
    private static final int MAX_RECENT_LIMIT = 200;

    private final InboundMessageRepository repository;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;

    public InboundMessageService(
            InboundMessageRepository repository,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
    }

    public InboundMessageResponse create(CreateInboundMessageRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        String inboundMessageId = "inmsg_" + UUID.randomUUID().toString().replace("-", "");

        InboundMessage inboundMessage = new InboundMessage(
                inboundMessageId,
                request.merchantId(),
                blankToNull(request.merchantStaffId()),
                blankToNull(request.customerId()),
                request.sourceChannel(),
                request.businessChannel(),
                request.rawText().strip(),
                InboundMessageStatus.RECEIVED,
                request.receivedAt() == null ? now : request.receivedAt(),
                now,
                now
        );

        repository.save(inboundMessage);
        auditLogService.append(
                inboundMessage.merchantId(),
                requestAuthContext.currentActorIdOr(inboundMessage.merchantStaffId()),
                "INBOUND_MESSAGE_CREATED",
                "INBOUND_MESSAGE",
                inboundMessage.inboundMessageId(),
                "주문 원문이 접수되었습니다."
        );
        log.info(
                "INBOUND_MESSAGE_CREATED inboundMessageId={} merchantId={} merchantStaffId={} customerId={} sourceChannel={} businessChannel={}",
                inboundMessage.inboundMessageId(),
                inboundMessage.merchantId(),
                inboundMessage.merchantStaffId(),
                inboundMessage.customerId(),
                inboundMessage.sourceChannel(),
                inboundMessage.businessChannel()
        );

        return InboundMessageResponse.from(inboundMessage);
    }

    public List<InboundMessageResponse> recent(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.findRecent(normalizedLimit).stream()
                .filter(this::canRead)
                .map(InboundMessageResponse::from)
                .toList();
    }

    public InboundMessageResponse get(String inboundMessageId) {
        return InboundMessageResponse.from(findModel(inboundMessageId));
    }

    public InboundMessage findModel(String inboundMessageId) {
        InboundMessage inboundMessage = repository.findById(inboundMessageId)
                .orElseThrow(() -> new ApiRequestException(
                        "SM-INB-4001",
                        HttpStatus.NOT_FOUND,
                        "주문 원문을 찾을 수 없습니다."
                ));
        if (!canRead(inboundMessage)) {
            throw new ApiRequestException("SM-INB-4002", HttpStatus.FORBIDDEN, "주문 원문에 접근할 권한이 없습니다.");
        }
        return inboundMessage;
    }

    public void markDrafted(String inboundMessageId) {
        InboundMessage inboundMessage = findModel(inboundMessageId);
        InboundMessage drafted = inboundMessage.withStatus(InboundMessageStatus.DRAFTED, OffsetDateTime.now());
        repository.save(drafted);
        auditLogService.append(
                drafted.merchantId(),
                requestAuthContext.currentActorIdOr(drafted.merchantStaffId()),
                "INBOUND_MESSAGE_DRAFTED",
                "INBOUND_MESSAGE",
                drafted.inboundMessageId(),
                "주문 원문에서 출고표 초안이 생성되었습니다."
        );
        log.info(
                "INBOUND_MESSAGE_DRAFTED inboundMessageId={} merchantId={} customerId={} sourceChannel={} businessChannel={}",
                drafted.inboundMessageId(),
                drafted.merchantId(),
                drafted.customerId(),
                drafted.sourceChannel(),
                drafted.businessChannel()
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_RECENT_LIMIT;
        }
        if (limit < 1) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private boolean canRead(InboundMessage inboundMessage) {
        return requestAuthContext.currentUser()
                .map(user -> user.merchantId().equals(inboundMessage.merchantId()))
                .orElse(true);
    }
}
