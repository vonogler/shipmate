package com.shipmate.api.aiextraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipmate.api.operation.audit.AuditLogService;
import com.shipmate.api.orderdraft.OrderDraftItemCandidate;
import com.shipmate.api.orderdraft.OrderDraftResponse;
import com.shipmate.api.orderdraft.OrderDraftService;
import com.shipmate.api.orderdraft.UpdateOrderDraftItemCandidateRequest;
import com.shipmate.api.orderdraft.UpdateOrderDraftReviewRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AiExtractionService {
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final double NEEDS_REVIEW_CONFIDENCE_THRESHOLD = 0.75;
    private static final Set<String> ALLOWED_REVIEW_REASONS = Set.of(
            "CUSTOMER_NOT_MATCHED",
            "ITEM_NOT_MATCHED",
            "QUANTITY_AMBIGUOUS",
            "UNIT_AMBIGUOUS",
            "PRICE_NOT_MATCHED",
            "DELIVERY_DATE_MISSING",
            "DESIGN_FILE_REQUIRED",
            "DUPLICATE_ORDER_SUSPECTED"
    );

    private final OrderDraftService orderDraftService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AuditLogService auditLogService;

    public AiExtractionService(
            OrderDraftService orderDraftService,
            ObjectMapper objectMapper,
            AuditLogService auditLogService
    ) {
        this.orderDraftService = orderDraftService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
        this.auditLogService = auditLogService;
    }

    public AiExtractionResponse extract(String orderDraftId) {
        return extractAndApply(orderDraftId, true);
    }

    public AiExtractionResponse extractAndApply(String orderDraftId, boolean markLowConfidenceAsNeedsReview) {
        OrderDraftResponse orderDraft = orderDraftService.get(orderDraftId);
        ExtractionResult extractionResult = extractWithOpenAi(orderDraft);
        List<UpdateOrderDraftItemCandidateRequest> extracted = extractionResult.candidates();
        String extractionMode = extractionResult.mode();
        if (extracted.isEmpty()) {
            extracted = fallbackFromCurrent(orderDraft);
            extractionMode = "RULE";
            extractionResult = new ExtractionResult(extractionMode, extracted, 1.0, List.of(), false);
        }

        OrderDraftResponse updated = orderDraftService.updateReview(
                orderDraftId,
                new UpdateOrderDraftReviewRequest(
                        extracted,
                        orderDraft.deliveryRecipientName(),
                        orderDraft.deliveryRecipientPhone(),
                        orderDraft.deliveryPostalCode(),
                        orderDraft.deliveryAddress(),
                        orderDraft.deliveryAddressDetail(),
                        orderDraft.deliveryRequestMemo(),
                        orderDraft.deliveryMemo(),
                        orderDraft.courierMemo()
                )
        );
        if (markLowConfidenceAsNeedsReview && extractionResult.needsReview()) {
            updated = orderDraftService.markNeedsReview(
                    updated.orderDraftId(),
                    "AI 주문 후보 추출 신뢰도가 낮아 검수 필요 상태로 전환되었습니다. confidence=" + extractionResult.confidence()
            );
        }
        auditLogService.appendForCurrentActor(
                updated.merchantId(),
                updated.merchantStaffId(),
                "ORDER_DRAFT_AI_EXTRACTED",
                "ORDER_DRAFT",
                updated.orderDraftId(),
                extractionMode.equals("AI") ? "AI 주문 후보 추출 결과가 반영되었습니다." : "규칙 기반 주문 후보가 반영되었습니다."
        );
        return new AiExtractionResponse(extractionMode, updated);
    }

    private ExtractionResult extractWithOpenAi(OrderDraftResponse orderDraft) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return ExtractionResult.emptyAi();
        }

        try {
            String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5.2");
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", prompt(orderDraft.rawText())
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ExtractionResult.emptyAi();
            }
            return parseCandidates(extractOutputText(response.body()));
        } catch (Exception exception) {
            return ExtractionResult.emptyAi();
        }
    }

    private String prompt(String rawText) {
        return """
                다음 주문 원문에서 주문 후보를 JSON 객체로만 추출하세요.
                최상위 키: businessChannel, requestedDeliveryDate, lineItems, needsReviewReasons, extractionConfidence.
                lineItems 항목 키: itemCandidateName, itemSpec, quantityValue, itemUnit, packagingWidth, packagingDepth, packagingHeight, packagingMaterialName, productionQuantity, requestedDueDate, customProductType, imprintMethod, itemColor, customOptionText, rawLineText, needsReviewReasons, confidence.
                confidence와 extractionConfidence는 0부터 1 사이 숫자여야 합니다.
                needsReviewReasons는 CUSTOMER_NOT_MATCHED, ITEM_NOT_MATCHED, QUANTITY_AMBIGUOUS, UNIT_AMBIGUOUS, PRICE_NOT_MATCHED, DELIVERY_DATE_MISSING, DESIGN_FILE_REQUIRED, DUPLICATE_ORDER_SUSPECTED 중에서만 사용하세요.
                품목명을 확정할 수 없으면 해당 lineItem은 만들지 마세요. 설명 문장은 쓰지 마세요.

                주문 원문:
                %s
                """.formatted(rawText);
    }

    private String extractOutputText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode outputText = root.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }

        StringBuilder builder = new StringBuilder();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode outputItem : output) {
                JsonNode content = outputItem.get("content");
                if (content == null || !content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual()) {
                        builder.append(text.asText());
                    }
                }
            }
        }
        return builder.toString();
    }

    private ExtractionResult parseCandidates(String jsonText) throws Exception {
        if (jsonText == null || jsonText.isBlank()) {
            return ExtractionResult.emptyAi();
        }
        String normalized = jsonText.strip();
        int start = normalized.indexOf('[');
        int end = normalized.lastIndexOf(']');
        if (start >= 0 && end > start) {
            normalized = normalized.substring(start, end + 1);
        }
        JsonNode root = objectMapper.readTree(normalized);
        JsonNode itemRoot = root;
        if (root.isObject()) {
            if (!isConfidence(root.get("extractionConfidence")) || !validReviewReasons(root.get("needsReviewReasons"))) {
                return ExtractionResult.emptyAi();
            }
            itemRoot = root.get("lineItems");
        }
        if (itemRoot == null || !itemRoot.isArray()) {
            return ExtractionResult.emptyAi();
        }

        List<UpdateOrderDraftItemCandidateRequest> candidates = new ArrayList<>();
        double minimumConfidence = root.isObject() ? root.get("extractionConfidence").doubleValue() : 1.0;
        List<String> reviewReasons = root.isObject() ? reviewReasons(root.get("needsReviewReasons")) : List.of();
        for (JsonNode node : itemRoot) {
            if (!validLineItem(node)) {
                continue;
            }
            JsonNode lineConfidence = node.get("confidence");
            if (lineConfidence != null && lineConfidence.isNumber()) {
                minimumConfidence = Math.min(minimumConfidence, lineConfidence.doubleValue());
            }
            reviewReasons = mergeReviewReasons(reviewReasons, reviewReasons(node.get("needsReviewReasons")));
            UpdateOrderDraftItemCandidateRequest candidate = new UpdateOrderDraftItemCandidateRequest(
                    null,
                    firstText(node, "itemName", "itemCandidateName"),
                    text(node, "itemSpec"),
                    firstText(node, "itemQuantity", "quantityValue"),
                    text(node, "itemUnit"),
                    decimal(node, "matchedUnitPrice"),
                    text(node, "packagingWidth"),
                    text(node, "packagingDepth"),
                    text(node, "packagingHeight"),
                    firstText(node, "materialName", "packagingMaterialName"),
                    null,
                    null,
                    text(node, "productionQuantity"),
                    text(node, "requestedDueDate"),
                    null,
                    text(node, "customProductType"),
                    text(node, "imprintMethod"),
                    text(node, "itemColor"),
                    text(node, "customOptionText"),
                    null,
                    null,
                    text(node, "lineMemo")
            );
            if (!isBlank(candidate.itemName())) {
                candidates.add(candidate);
            }
        }
        boolean needsReview = candidates.isEmpty()
                || minimumConfidence < NEEDS_REVIEW_CONFIDENCE_THRESHOLD
                || !reviewReasons.isEmpty();
        return new ExtractionResult("AI", candidates, minimumConfidence, reviewReasons, needsReview);
    }

    private List<UpdateOrderDraftItemCandidateRequest> fallbackFromCurrent(OrderDraftResponse orderDraft) {
        return orderDraft.itemCandidates().stream()
                .map(this::fromCurrentItem)
                .toList();
    }

    private UpdateOrderDraftItemCandidateRequest fromCurrentItem(OrderDraftItemCandidate item) {
        return new UpdateOrderDraftItemCandidateRequest(
                item.orderDraftItemCandidateId(),
                item.itemName(),
                item.itemSpec(),
                item.itemQuantity(),
                item.itemUnit(),
                item.matchedUnitPrice(),
                item.packagingWidth(),
                item.packagingDepth(),
                item.packagingHeight(),
                item.materialName(),
                item.printRequired(),
                item.designFileProvided(),
                item.productionQuantity(),
                item.requestedDueDate(),
                item.sampleRequested(),
                item.customProductType(),
                item.imprintMethod(),
                item.itemColor(),
                item.customOptionText(),
                item.proofConfirmed(),
                item.productionConfirmed(),
                item.lineMemo()
        );
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return blankToNull(value.asText());
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return null;
    }

    private String firstText(JsonNode node, String firstFieldName, String secondFieldName) {
        String first = text(node, firstFieldName);
        return first == null ? text(node, secondFieldName) : first;
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.decimalValue();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private boolean validLineItem(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        String itemName = firstText(node, "itemName", "itemCandidateName");
        if (isBlank(itemName) || itemName.length() > 300) {
            return false;
        }
        String quantity = firstText(node, "itemQuantity", "quantityValue");
        if (quantity != null && quantity.length() > 80) {
            return false;
        }
        String requestedDueDate = text(node, "requestedDueDate");
        if (requestedDueDate != null && !ISO_DATE_PATTERN.matcher(requestedDueDate).matches()) {
            return false;
        }
        JsonNode confidence = node.get("confidence");
        return (confidence == null || isConfidence(confidence)) && validReviewReasons(node.get("needsReviewReasons"));
    }

    private boolean isConfidence(JsonNode value) {
        return value != null && value.isNumber() && value.doubleValue() >= 0 && value.doubleValue() <= 1;
    }

    private boolean validReviewReasons(JsonNode reasons) {
        if (reasons == null || reasons.isNull()) {
            return true;
        }
        if (!reasons.isArray()) {
            return false;
        }
        for (JsonNode reason : reasons) {
            if (!reason.isTextual() || !ALLOWED_REVIEW_REASONS.contains(reason.asText())) {
                return false;
            }
        }
        return true;
    }

    private List<String> reviewReasons(JsonNode reasons) {
        if (reasons == null || reasons.isNull() || !reasons.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode reason : reasons) {
            if (reason.isTextual() && ALLOWED_REVIEW_REASONS.contains(reason.asText())) {
                values.add(reason.asText());
            }
        }
        return values;
    }

    private List<String> mergeReviewReasons(List<String> left, List<String> right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        List<String> values = new ArrayList<>(left);
        for (String value : right) {
            if (!values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private record ExtractionResult(
            String mode,
            List<UpdateOrderDraftItemCandidateRequest> candidates,
            double confidence,
            List<String> reviewReasons,
            boolean needsReview
    ) {
        private static ExtractionResult emptyAi() {
            return new ExtractionResult("AI", List.of(), 0.0, List.of(), false);
        }
    }
}
