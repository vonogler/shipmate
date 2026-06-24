package com.shipmate.api.orderdraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.auth.RequestAuthContext;
import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.customer.CustomerService;
import com.shipmate.api.inboundmessage.InboundMessage;
import com.shipmate.api.inboundmessage.InboundMessageService;
import com.shipmate.api.operation.audit.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrderDraftService {
    private static final Logger log = LoggerFactory.getLogger(OrderDraftService.class);
    private static final int DEFAULT_RECENT_LIMIT = 50;
    private static final int MAX_RECENT_LIMIT = 200;
    private static final Pattern DIMENSION_PATTERN = Pattern.compile("(\\d{2,5})\\s*[xX*×]\\s*(\\d{2,5})\\s*[xX*×]\\s*(\\d{2,5})");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d[\\d,]*)\\s*(장|개|박스|봉|세트|EA|ea)");
    private static final Pattern ITEM_NAME_FIELD_PATTERN = Pattern.compile("(?m)^\\s*(품목|제품|상품|item)\\s*[:：]\\s*(.+?)\\s*$", Pattern.CASE_INSENSITIVE);

    private final OrderDraftRepository repository;
    private final InboundMessageService inboundMessageService;
    private final CustomerService customerService;
    private final RequestAuthContext requestAuthContext;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public OrderDraftService(
            OrderDraftRepository repository,
            InboundMessageService inboundMessageService,
            CustomerService customerService,
            RequestAuthContext requestAuthContext,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.inboundMessageService = inboundMessageService;
        this.customerService = customerService;
        this.requestAuthContext = requestAuthContext;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public OrderDraftResponse createFromInboundMessage(CreateOrderDraftFromInboundMessageRequest request) {
        InboundMessage inboundMessage = inboundMessageService.findModel(request.sourceInboundMessageId());
        OffsetDateTime now = OffsetDateTime.now();
        String orderDraftId = "odft_" + UUID.randomUUID().toString().replace("-", "");

        OrderDraft orderDraft = new OrderDraft(
                orderDraftId,
                inboundMessage.inboundMessageId(),
                inboundMessage.merchantId(),
                inboundMessage.merchantStaffId(),
                inboundMessage.customerId(),
                inboundMessage.sourceChannel(),
                inboundMessage.businessChannel(),
                inboundMessage.rawText(),
                extractItemCandidates(inboundMessage.rawText(), inboundMessage.customerId()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OrderDraftStatus.CREATED,
                now,
                now
        );

        repository.save(orderDraft);
        inboundMessageService.markDrafted(inboundMessage.inboundMessageId());
        auditLogService.appendForCurrentActor(
                orderDraft.merchantId(),
                orderDraft.merchantStaffId(),
                "ORDER_DRAFT_CREATED",
                "ORDER_DRAFT",
                orderDraft.orderDraftId(),
                "주문 원문에서 출고표 초안이 생성되었습니다."
        );
        log.info(
                "ORDER_DRAFT_CREATED orderDraftId={} sourceInboundMessageId={} merchantId={} customerId={} sourceChannel={} businessChannel={}",
                orderDraft.orderDraftId(),
                orderDraft.sourceInboundMessageId(),
                orderDraft.merchantId(),
                orderDraft.customerId(),
                orderDraft.sourceChannel(),
                orderDraft.businessChannel()
        );

        return OrderDraftResponse.from(orderDraft);
    }

    public List<OrderDraftResponse> recent(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.findRecent(normalizedLimit).stream()
                .filter(this::canRead)
                .map(OrderDraftResponse::from)
                .toList();
    }

    public List<OrderDraftResponse> submitted(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.findRecentByStatus(OrderDraftStatus.SUBMITTED, normalizedLimit).stream()
                .filter(this::canRead)
                .map(OrderDraftResponse::from)
                .toList();
    }

    public OrderDraftResponse get(String orderDraftId) {
        return OrderDraftResponse.from(findReadable(orderDraftId));
    }

    public OrderDraft findReadable(String orderDraftId) {
        OrderDraft orderDraft = repository.findById(orderDraftId)
                .orElseThrow(() -> new ApiRequestException(
                        "SM-ODF-5001",
                        HttpStatus.NOT_FOUND,
                        "출고표 초안을 찾을 수 없습니다."
                ));
        if (!canRead(orderDraft)) {
            throw new ApiRequestException("SM-ODF-5004", HttpStatus.FORBIDDEN, "출고표 초안에 접근할 권한이 없습니다.");
        }
        return orderDraft;
    }

    public OrderDraftResponse getSubmitted(String orderDraftId) {
        OrderDraft orderDraft = repository.findById(orderDraftId)
                .filter(this::canRead)
                .filter(savedOrderDraft -> savedOrderDraft.orderDraftStatus() == OrderDraftStatus.SUBMITTED)
                .orElseThrow(() -> new ApiRequestException(
                        "SM-SHP-6001",
                        HttpStatus.NOT_FOUND,
                        "제출된 출고표를 찾을 수 없습니다."
                ));
        return OrderDraftResponse.from(orderDraft);
    }

    public OrderDraftResponse updateReview(String orderDraftId, UpdateOrderDraftReviewRequest request) {
        OrderDraft orderDraft = findReadable(orderDraftId);

        List<OrderDraftItemCandidate> itemCandidates = normalizeItemCandidates(
                orderDraft.customerId(),
                request.itemCandidates()
        );
        OrderDraft updated = orderDraft.withReview(
                itemCandidates,
                blankToNull(request.deliveryRecipientName()),
                blankToNull(request.deliveryRecipientPhone()),
                blankToNull(request.deliveryPostalCode()),
                blankToNull(request.deliveryAddress()),
                blankToNull(request.deliveryAddressDetail()),
                blankToNull(request.deliveryRequestMemo()),
                blankToNull(request.deliveryMemo()),
                blankToNull(request.courierMemo()),
                OffsetDateTime.now()
        );
        repository.save(updated);
        auditLogService.appendForCurrentActor(
                updated.merchantId(),
                updated.merchantStaffId(),
                "ORDER_DRAFT_REVIEW_UPDATED",
                "ORDER_DRAFT",
                updated.orderDraftId(),
                "출고표 초안 검수 내용이 저장되었습니다."
        );
        log.info(
                "ORDER_DRAFT_REVIEW_UPDATED orderDraftId={} itemCandidateCount={} merchantId={} customerId={}",
                updated.orderDraftId(),
                updated.itemCandidates().size(),
                updated.merchantId(),
                updated.customerId()
        );
        return OrderDraftResponse.from(updated);
    }

    public OrderDraftResponse submit(String orderDraftId) {
        OrderDraft orderDraft = findReadable(orderDraftId);

        if (orderDraft.orderDraftStatus() == OrderDraftStatus.NEEDS_REVIEW) {
            throw new ApiRequestException(
                    "SM-ODF-5005",
                    HttpStatus.BAD_REQUEST,
                    "검수 필요 초안은 검수 내용을 저장한 뒤 제출할 수 있습니다."
            );
        }
        if (orderDraft.orderDraftStatus() == OrderDraftStatus.SUBMITTED) {
            throw new ApiRequestException(
                    "SM-ODF-5002",
                    HttpStatus.CONFLICT,
                    "이미 제출된 출고표 초안입니다."
            );
        }
        if (!hasSubmittableItem(orderDraft)) {
            throw new ApiRequestException(
                    "SM-ODF-5003",
                    HttpStatus.BAD_REQUEST,
                    "제출하려면 품목 후보가 1개 이상 필요합니다."
            );
        }
        List<String> validationErrors = submissionValidationErrors(orderDraft);
        if (!validationErrors.isEmpty()) {
            throw new ApiRequestException(
                    "SM-ODF-5006",
                    HttpStatus.BAD_REQUEST,
                    "출고표 제출 전 필수값을 확인하세요: " + String.join(", ", validationErrors)
            );
        }

        OrderDraft submitted = orderDraft.withStatus(OrderDraftStatus.SUBMITTED, OffsetDateTime.now());
        repository.save(submitted);
        auditLogService.appendForCurrentActor(
                submitted.merchantId(),
                submitted.merchantStaffId(),
                "ORDER_DRAFT_SUBMITTED",
                "ORDER_DRAFT",
                submitted.orderDraftId(),
                "출고표 초안이 제출되었습니다.",
                auditMetadataJson(Map.of(
                        "itemSummary", itemSummary(submitted),
                        "quantitySummary", quantitySummary(submitted),
                        "itemCandidateCount", String.valueOf(submitted.itemCandidates().size()),
                        "itemCandidates", itemCandidatesSnapshotJson(submitted)
                ))
        );
        log.info(
                "ORDER_DRAFT_SUBMITTED orderDraftId={} itemCandidateCount={} merchantId={} customerId={}",
                submitted.orderDraftId(),
                submitted.itemCandidates().size(),
                submitted.merchantId(),
                submitted.customerId()
        );
        return OrderDraftResponse.from(submitted);
    }

    public OrderDraftResponse cancel(String orderDraftId) {
        OrderDraft orderDraft = findReadable(orderDraftId);
        if (orderDraft.orderDraftStatus() == OrderDraftStatus.CANCELED) {
            return OrderDraftResponse.from(orderDraft);
        }

        OrderDraft canceled = orderDraft.withStatus(OrderDraftStatus.CANCELED, OffsetDateTime.now());
        repository.save(canceled);
        auditLogService.appendForCurrentActor(
                canceled.merchantId(),
                canceled.merchantStaffId(),
                "ORDER_DRAFT_CANCELED",
                "ORDER_DRAFT",
                canceled.orderDraftId(),
                "출고표 초안이 취소 처리되었습니다."
        );
        return OrderDraftResponse.from(canceled);
    }

    public OrderDraftResponse reopenSubmitted(String orderDraftId, String reopenReason) {
        OrderDraft orderDraft = findReadable(orderDraftId);
        if (orderDraft.orderDraftStatus() != OrderDraftStatus.SUBMITTED) {
            throw new ApiRequestException(
                    "SM-ODF-5007",
                    HttpStatus.BAD_REQUEST,
                    "제출된 출고표만 재검수로 전환할 수 있습니다."
            );
        }

        OrderDraft reopened = orderDraft.withStatus(OrderDraftStatus.NEEDS_REVIEW, OffsetDateTime.now());
        repository.save(reopened);
        String summary = isBlank(reopenReason)
                ? "제출된 출고표가 재검수 상태로 전환되었습니다."
                : "제출된 출고표가 재검수 상태로 전환되었습니다. 사유: " + reopenReason.strip();
        String auditMetadataJson = isBlank(reopenReason)
                ? null
                : auditMetadataJson(Map.of(
                        "reopenReason", reopenReason.strip(),
                        "previousStatus", orderDraft.orderDraftStatus().name(),
                        "nextStatus", reopened.orderDraftStatus().name()
                ));
        auditLogService.appendForCurrentActor(
                reopened.merchantId(),
                reopened.merchantStaffId(),
                "ORDER_DRAFT_REOPENED",
                "ORDER_DRAFT",
                reopened.orderDraftId(),
                summary,
                auditMetadataJson
        );
        return OrderDraftResponse.from(reopened);
    }

    private String auditMetadataJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            log.warn("변경 이력 metadata JSON 생성 실패", exception);
            return null;
        }
    }

    private String itemCandidatesSnapshotJson(OrderDraft orderDraft) {
        try {
            return objectMapper.writeValueAsString(orderDraft.itemCandidates().stream()
                    .map(itemCandidate -> new ItemCandidateSnapshot(
                            valueOrBlank(itemCandidate.itemName()),
                            valueOrBlank(itemCandidate.itemSpec()),
                            valueOrBlank(itemCandidate.itemQuantity()),
                            valueOrBlank(itemCandidate.itemUnit()),
                            itemCandidate.matchedUnitPrice() == null ? "" : itemCandidate.matchedUnitPrice().stripTrailingZeros().toPlainString()
                    ))
                    .toList());
        } catch (JsonProcessingException exception) {
            log.warn("품목 snapshot JSON 생성 실패", exception);
            return "[]";
        }
    }

    private String itemSummary(OrderDraft orderDraft) {
        return orderDraft.itemCandidates().stream()
                .map(itemCandidate -> List.of(
                                valueOrBlank(itemCandidate.itemName()),
                                valueOrBlank(itemCandidate.itemSpec())
                        ).stream()
                        .filter(value -> !value.isBlank())
                        .reduce((left, right) -> left + " " + right)
                        .orElse(""))
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private String quantitySummary(OrderDraft orderDraft) {
        return orderDraft.itemCandidates().stream()
                .map(itemCandidate -> List.of(
                                valueOrBlank(itemCandidate.itemQuantity()),
                                valueOrBlank(itemCandidate.itemUnit())
                        ).stream()
                        .filter(value -> !value.isBlank())
                        .reduce((left, right) -> left + right)
                        .orElse(""))
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private record ItemCandidateSnapshot(
            String itemName,
            String itemSpec,
            String itemQuantity,
            String itemUnit,
            String matchedUnitPrice
    ) {
    }

    public OrderDraftResponse markNeedsReview(String orderDraftId, String summary) {
        OrderDraft orderDraft = findReadable(orderDraftId);
        if (orderDraft.orderDraftStatus() != OrderDraftStatus.CREATED) {
            return OrderDraftResponse.from(orderDraft);
        }
        OrderDraft updated = orderDraft.withStatus(OrderDraftStatus.NEEDS_REVIEW, OffsetDateTime.now());
        repository.save(updated);
        auditLogService.appendForCurrentActor(
                updated.merchantId(),
                updated.merchantStaffId(),
                "ORDER_DRAFT_NEEDS_REVIEW",
                "ORDER_DRAFT",
                updated.orderDraftId(),
                isBlank(summary) ? "출고표 초안이 검수 필요 상태로 전환되었습니다." : summary
        );
        return OrderDraftResponse.from(updated);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private List<OrderDraftItemCandidate> normalizeItemCandidates(
            String customerId,
            List<UpdateOrderDraftItemCandidateRequest> requests
    ) {
        if (requests == null) {
            return List.of();
        }

        return requests.stream()
                .filter(request -> !isBlank(request.itemName())
                        || !isBlank(request.itemSpec())
                        || !isBlank(request.itemQuantity())
                        || !isBlank(request.itemUnit())
                        || request.matchedUnitPrice() != null
                        || !isBlank(request.packagingWidth())
                        || !isBlank(request.packagingDepth())
                        || !isBlank(request.packagingHeight())
                        || !isBlank(request.materialName())
                        || request.printRequired() != null
                        || request.designFileProvided() != null
                        || !isBlank(request.productionQuantity())
                        || !isBlank(request.requestedDueDate())
                        || request.sampleRequested() != null
                        || !isBlank(request.customProductType())
                        || !isBlank(request.imprintMethod())
                        || !isBlank(request.itemColor())
                        || !isBlank(request.customOptionText())
                        || request.proofConfirmed() != null
                        || request.productionConfirmed() != null
                        || !isBlank(request.lineMemo()))
                .map(request -> applyCustomerPrice(customerId, new OrderDraftItemCandidate(
                                valueOrNewId(request.orderDraftItemCandidateId()),
                                blankToNull(request.itemName()),
                                blankToNull(request.itemSpec()),
                                blankToNull(request.itemQuantity()),
                                blankToNull(request.itemUnit()),
                                request.matchedUnitPrice(),
                                blankToNull(request.packagingWidth()),
                                blankToNull(request.packagingDepth()),
                                blankToNull(request.packagingHeight()),
                                blankToNull(request.materialName()),
                                request.printRequired(),
                                request.designFileProvided(),
                                blankToNull(request.productionQuantity()),
                                blankToNull(request.requestedDueDate()),
                                request.sampleRequested(),
                                blankToNull(request.customProductType()),
                                blankToNull(request.imprintMethod()),
                                blankToNull(request.itemColor()),
                                blankToNull(request.customOptionText()),
                                request.proofConfirmed(),
                                request.productionConfirmed(),
                                blankToNull(request.lineMemo())
                        )
                ))
                .toList();
    }

    private String valueOrNewId(String value) {
        if (!isBlank(value)) {
            return value.strip();
        }
        return "odfi_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.strip();
    }

    private boolean hasSubmittableItem(OrderDraft orderDraft) {
        return orderDraft.itemCandidates().stream()
                .anyMatch(itemCandidate -> !isBlank(itemCandidate.itemName()));
    }

    private List<String> submissionValidationErrors(OrderDraft orderDraft) {
        List<String> errors = new ArrayList<>();
        if (isBlank(orderDraft.deliveryRecipientName())) {
            errors.add("수취인");
        }
        if (isBlank(orderDraft.deliveryRecipientPhone())) {
            errors.add("연락처");
        }
        if (isBlank(orderDraft.deliveryAddress())) {
            errors.add("주소");
        }

        for (int index = 0; index < orderDraft.itemCandidates().size(); index++) {
            OrderDraftItemCandidate itemCandidate = orderDraft.itemCandidates().get(index);
            String itemPrefix = "품목 " + (index + 1) + " ";
            if (isBlank(itemCandidate.itemName())) {
                errors.add(itemPrefix + "품목명");
            }
            if (isBlank(itemCandidate.itemQuantity())) {
                errors.add(itemPrefix + "수량");
            }
            if (isBlank(itemCandidate.itemUnit())) {
                errors.add(itemPrefix + "단위");
            }
        }
        return errors;
    }

    private List<OrderDraftItemCandidate> extractItemCandidates(String rawText, String customerId) {
        if (isBlank(rawText)) {
            return List.of();
        }

        String normalized = rawText.strip();
        Matcher dimensionMatcher = DIMENSION_PATTERN.matcher(normalized);
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(normalized);
        String width = null;
        String depth = null;
        String height = null;
        String itemSpec = null;
        if (dimensionMatcher.find()) {
            width = dimensionMatcher.group(1);
            depth = dimensionMatcher.group(2);
            height = dimensionMatcher.group(3);
            itemSpec = width + "x" + depth + "x" + height;
        }

        String itemQuantity = null;
        String itemUnit = null;
        if (quantityMatcher.find()) {
            itemQuantity = quantityMatcher.group(1).replace(",", "");
            itemUnit = quantityMatcher.group(2);
        }

        String itemName = extractItemName(normalized);

        return List.of(applyCustomerPrice(customerId, new OrderDraftItemCandidate(
                        "odfi_" + UUID.randomUUID().toString().replace("-", ""),
                        itemName,
                        itemSpec,
                        itemQuantity,
                        itemUnit,
                        null,
                        width,
                        depth,
                        height,
                        null,
                        null,
                        null,
                        itemQuantity,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "원문에서 자동 추출한 후보입니다."
                )
        ));
    }

    private String extractItemName(String normalized) {
        Matcher fieldMatcher = ITEM_NAME_FIELD_PATTERN.matcher(normalized);
        if (fieldMatcher.find()) {
            return truncate(fieldMatcher.group(2).strip(), 80);
        }
        return normalized.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("보낸 사람:"))
                .filter(line -> !line.startsWith("제목:"))
                .filter(line -> !line.startsWith("요청사항:"))
                .findFirst()
                .map(line -> truncate(line, 80))
                .orElse(truncate(normalized, 80));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private OrderDraftItemCandidate applyCustomerPrice(String customerId, OrderDraftItemCandidate itemCandidate) {
        if (itemCandidate.matchedUnitPrice() != null) {
            return itemCandidate;
        }
        java.math.BigDecimal matchedUnitPrice = customerService.matchUnitPrice(customerId, itemCandidate);
        if (matchedUnitPrice == null) {
            return itemCandidate;
        }
        return new OrderDraftItemCandidate(
                itemCandidate.orderDraftItemCandidateId(),
                itemCandidate.itemName(),
                itemCandidate.itemSpec(),
                itemCandidate.itemQuantity(),
                itemCandidate.itemUnit(),
                matchedUnitPrice,
                itemCandidate.packagingWidth(),
                itemCandidate.packagingDepth(),
                itemCandidate.packagingHeight(),
                itemCandidate.materialName(),
                itemCandidate.printRequired(),
                itemCandidate.designFileProvided(),
                itemCandidate.productionQuantity(),
                itemCandidate.requestedDueDate(),
                itemCandidate.sampleRequested(),
                itemCandidate.customProductType(),
                itemCandidate.imprintMethod(),
                itemCandidate.itemColor(),
                itemCandidate.customOptionText(),
                itemCandidate.proofConfirmed(),
                itemCandidate.productionConfirmed(),
                itemCandidate.lineMemo()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private boolean canRead(OrderDraft orderDraft) {
        return requestAuthContext.currentUser()
                .map(user -> user.merchantId().equals(orderDraft.merchantId()))
                .orElse(true);
    }
}
