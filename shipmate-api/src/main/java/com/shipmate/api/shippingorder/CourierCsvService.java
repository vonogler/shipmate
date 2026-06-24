package com.shipmate.api.shippingorder;

import com.shipmate.api.common.ApiRequestException;
import com.shipmate.api.orderdraft.OrderDraftItemCandidate;
import com.shipmate.api.orderdraft.OrderDraftResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CourierCsvService {
    private static final String UTF8_BOM = "\uFEFF";
    private final CourierCsvValidationRuleRepository validationRuleRepository;

    public CourierCsvService(CourierCsvValidationRuleRepository validationRuleRepository) {
        this.validationRuleRepository = validationRuleRepository;
    }

    public CourierCsvResponse createSingleOrderCsv(OrderDraftResponse shippingOrder, CourierCsvTemplate courierCsvTemplate) {
        CourierCsvTemplate template = courierCsvTemplate == null ? CourierCsvTemplate.GENERIC : courierCsvTemplate;
        ensureValid(List.of(shippingOrder), template);
        String fileName = "courier-" + template.name().toLowerCase() + "-" + shippingOrder.orderDraftId() + ".csv";
        StringBuilder csv = new StringBuilder(UTF8_BOM);
        appendRow(csv, header(template));
        appendRow(csv, row(template, shippingOrder));
        return new CourierCsvResponse(fileName, csv.toString());
    }

    public CourierCsvResponse createBulkOrderCsv(List<OrderDraftResponse> shippingOrders, CourierCsvTemplate courierCsvTemplate) {
        CourierCsvTemplate template = courierCsvTemplate == null ? CourierCsvTemplate.GENERIC : courierCsvTemplate;
        ensureValid(shippingOrders, template);
        String fileName = "courier-" + template.name().toLowerCase() + "-bulk.csv";
        StringBuilder csv = new StringBuilder(UTF8_BOM);
        appendRow(csv, header(template));
        shippingOrders.forEach(shippingOrder -> appendRow(csv, row(template, shippingOrder)));
        return new CourierCsvResponse(fileName, csv.toString());
    }

    public CourierCsvValidationResponse validate(List<OrderDraftResponse> shippingOrders, CourierCsvTemplate courierCsvTemplate) {
        CourierCsvTemplate template = courierCsvTemplate == null ? CourierCsvTemplate.GENERIC : courierCsvTemplate;
        List<CourierCsvValidationError> errorDetails = validationErrorDetails(shippingOrders, template);
        List<String> errors = errorDetails.stream()
                .map(error -> error.orderDraftId() == null || error.orderDraftId().isBlank()
                        ? error.message()
                        : error.orderDraftId() + ": " + error.message())
                .toList();
        return new CourierCsvValidationResponse(errorDetails.isEmpty(), errors, errorDetails);
    }

    private void ensureValid(List<OrderDraftResponse> shippingOrders, CourierCsvTemplate courierCsvTemplate) {
        List<String> errors = validate(shippingOrders, courierCsvTemplate).errors();
        if (!errors.isEmpty()) {
            throw new ApiRequestException("SM-SHP-6002", HttpStatus.BAD_REQUEST, String.join(" / ", errors));
        }
    }

    private List<CourierCsvValidationError> validationErrorDetails(List<OrderDraftResponse> shippingOrders, CourierCsvTemplate courierCsvTemplate) {
        List<CourierCsvValidationError> errors = new ArrayList<>();
        if (shippingOrders == null || shippingOrders.isEmpty()) {
            errors.add(new CourierCsvValidationError(null, "orderDraftIds", null, "택배 CSV로 만들 출고표가 없습니다."));
            return errors;
        }
        List<CourierCsvValidationRule> rules = validationRules(courierCsvTemplate);
        for (OrderDraftResponse shippingOrder : shippingOrders) {
            rules.stream()
                    .filter(CourierCsvValidationRule::required)
                    .filter(rule -> fieldValueBlank(shippingOrder, rule.fieldName()))
                    .map(rule -> new CourierCsvValidationError(
                            shippingOrder.orderDraftId(),
                            rule.fieldName(),
                            rule.csvColumnName(),
                            rule.errorMessage()
                    ))
                    .forEach(errors::add);
        }
        return errors;
    }

    private List<CourierCsvValidationRule> validationRules(CourierCsvTemplate courierCsvTemplate) {
        List<CourierCsvValidationRule> configuredRules = validationRuleRepository.findActiveByTemplate(courierCsvTemplate);
        if (!configuredRules.isEmpty()) {
            return configuredRules;
        }
        return defaultValidationRules(courierCsvTemplate);
    }

    private List<CourierCsvValidationRule> defaultValidationRules(CourierCsvTemplate courierCsvTemplate) {
        List<String> fields = new ArrayList<>(List.of(
                "deliveryRecipientName",
                "deliveryRecipientPhone",
                "deliveryAddress",
                "itemCandidates"
        ));
        if (courierCsvTemplate != CourierCsvTemplate.GENERIC) {
            fields.add("deliveryPostalCode");
            fields.add("itemCandidates.itemQuantity");
        }
        List<CourierCsvValidationRule> rules = new ArrayList<>();
        for (int index = 0; index < fields.size(); index += 1) {
            String fieldName = fields.get(index);
            rules.add(new CourierCsvValidationRule(
                    "default-" + courierCsvTemplate.name().toLowerCase() + "-" + fieldName,
                    courierCsvTemplate,
                    fieldName,
                    csvColumnName(courierCsvTemplate, fieldName),
                    true,
                    defaultValidationMessage(courierCsvTemplate, fieldName),
                    index + 1,
                    true
            ));
        }
        return rules;
    }

    private boolean fieldValueBlank(OrderDraftResponse shippingOrder, String fieldName) {
        return switch (fieldName) {
            case "deliveryRecipientName" -> isBlank(shippingOrder.deliveryRecipientName());
            case "deliveryRecipientPhone" -> isBlank(shippingOrder.deliveryRecipientPhone());
            case "deliveryPostalCode" -> isBlank(shippingOrder.deliveryPostalCode());
            case "deliveryAddress" -> isBlank(shippingOrder.deliveryAddress());
            case "itemCandidates" -> shippingOrder.itemCandidates() == null || itemSummary(shippingOrder.itemCandidates()).isBlank();
            case "itemCandidates.itemQuantity" -> quantitySummary(shippingOrder.itemCandidates()).isBlank();
            default -> false;
        };
    }

    private String defaultValidationMessage(CourierCsvTemplate courierCsvTemplate, String fieldName) {
        return switch (fieldName) {
            case "deliveryRecipientName" -> "수취인이 없습니다.";
            case "deliveryRecipientPhone" -> "수취인 연락처가 없습니다.";
            case "deliveryPostalCode" -> "택배사 양식에 필요한 우편번호가 없습니다.";
            case "deliveryAddress" -> "주소가 없습니다.";
            case "itemCandidates" -> "품목명이 없습니다.";
            case "itemCandidates.itemQuantity" -> courierCsvTemplate == CourierCsvTemplate.GENERIC ? "수량이 없습니다." : "택배사 양식에 필요한 수량이 없습니다.";
            default -> "택배 CSV 필수값이 없습니다.";
        };
    }

    private CourierCsvValidationError csvValidationError(
            CourierCsvTemplate courierCsvTemplate,
            String orderDraftId,
            String fieldName,
            String message
    ) {
        return new CourierCsvValidationError(orderDraftId, fieldName, csvColumnName(courierCsvTemplate, fieldName), message);
    }

    private String csvColumnName(CourierCsvTemplate courierCsvTemplate, String fieldName) {
        return switch (courierCsvTemplate) {
            case CJ_LOGISTICS -> switch (fieldName) {
                case "deliveryRecipientName" -> "받는분";
                case "deliveryRecipientPhone" -> "받는분전화번호";
                case "deliveryPostalCode" -> "받는분우편번호";
                case "deliveryAddress" -> "받는분주소";
                case "itemCandidates" -> "품목명";
                case "itemCandidates.itemQuantity" -> "수량";
                default -> null;
            };
            case LOTTE -> switch (fieldName) {
                case "deliveryRecipientName" -> "수취인명";
                case "deliveryRecipientPhone" -> "수취인전화번호";
                case "deliveryPostalCode" -> "수취인우편번호";
                case "deliveryAddress" -> "수취인주소";
                case "itemCandidates" -> "상품명";
                case "itemCandidates.itemQuantity" -> "수량";
                default -> null;
            };
            case HANJIN -> switch (fieldName) {
                case "deliveryRecipientName" -> "받는고객명";
                case "deliveryRecipientPhone" -> "받는고객전화번호";
                case "deliveryPostalCode" -> "받는고객우편번호";
                case "deliveryAddress" -> "받는고객주소";
                case "itemCandidates" -> "상품명";
                case "itemCandidates.itemQuantity" -> "상품수량";
                default -> null;
            };
            case GENERIC -> switch (fieldName) {
                case "deliveryRecipientName" -> "수취인";
                case "deliveryRecipientPhone" -> "연락처";
                case "deliveryPostalCode" -> "우편번호";
                case "deliveryAddress" -> "주소";
                case "itemCandidates" -> "품목요약";
                case "itemCandidates.itemQuantity" -> "수량요약";
                default -> null;
            };
        };
    }

    private List<String> header(CourierCsvTemplate courierCsvTemplate) {
        return switch (courierCsvTemplate) {
            case CJ_LOGISTICS -> List.of("받는분", "받는분전화번호", "받는분기타연락처", "받는분우편번호", "받는분주소", "품목명", "수량", "운임구분", "박스수량", "배송메시지", "고객주문번호");
            case LOTTE -> List.of("수취인명", "수취인전화번호", "수취인우편번호", "수취인주소", "수취인상세주소", "상품명", "수량", "운임구분", "박스수량", "배송요청사항", "주문번호");
            case HANJIN -> List.of("받는고객명", "받는고객전화번호", "받는고객기타연락처", "받는고객우편번호", "받는고객주소", "상품명", "상품수량", "운임구분", "박스수량", "배송메모", "출고표ID");
            case GENERIC -> List.of("출고표ID", "거래처ID", "수취인", "연락처", "우편번호", "주소", "상세주소", "품목요약", "수량요약", "배송요청사항", "납품메모", "택배메모");
        };
    }

    private List<String> row(CourierCsvTemplate courierCsvTemplate, OrderDraftResponse shippingOrder) {
        String fullAddress = joinWithSpace(shippingOrder.deliveryAddress(), shippingOrder.deliveryAddressDetail());
        String quantity = quantitySummaryOrDefault(shippingOrder.itemCandidates());
        return switch (courierCsvTemplate) {
            case CJ_LOGISTICS -> List.of(
                    valueOrBlank(shippingOrder.deliveryRecipientName()),
                    valueOrBlank(shippingOrder.deliveryRecipientPhone()),
                    "",
                    valueOrBlank(shippingOrder.deliveryPostalCode()),
                    fullAddress,
                    itemSummary(shippingOrder.itemCandidates()),
                    quantity,
                    "선불",
                    "1",
                    firstNonBlank(shippingOrder.deliveryRequestMemo(), shippingOrder.courierMemo()),
                    shippingOrder.orderDraftId()
            );
            case LOTTE -> List.of(
                    valueOrBlank(shippingOrder.deliveryRecipientName()),
                    valueOrBlank(shippingOrder.deliveryRecipientPhone()),
                    valueOrBlank(shippingOrder.deliveryPostalCode()),
                    valueOrBlank(shippingOrder.deliveryAddress()),
                    valueOrBlank(shippingOrder.deliveryAddressDetail()),
                    itemSummary(shippingOrder.itemCandidates()),
                    quantity,
                    "선불",
                    "1",
                    firstNonBlank(shippingOrder.deliveryRequestMemo(), shippingOrder.courierMemo()),
                    shippingOrder.orderDraftId()
            );
            case HANJIN -> List.of(
                    valueOrBlank(shippingOrder.deliveryRecipientName()),
                    valueOrBlank(shippingOrder.deliveryRecipientPhone()),
                    "",
                    valueOrBlank(shippingOrder.deliveryPostalCode()),
                    fullAddress,
                    itemSummary(shippingOrder.itemCandidates()),
                    quantity,
                    "선불",
                    "1",
                    firstNonBlank(shippingOrder.deliveryRequestMemo(), shippingOrder.courierMemo()),
                    shippingOrder.orderDraftId()
            );
            case GENERIC -> List.of(
                    shippingOrder.orderDraftId(),
                    valueOrBlank(shippingOrder.customerId()),
                    valueOrBlank(shippingOrder.deliveryRecipientName()),
                    valueOrBlank(shippingOrder.deliveryRecipientPhone()),
                    valueOrBlank(shippingOrder.deliveryPostalCode()),
                    valueOrBlank(shippingOrder.deliveryAddress()),
                    valueOrBlank(shippingOrder.deliveryAddressDetail()),
                    itemSummary(shippingOrder.itemCandidates()),
                    quantitySummary(shippingOrder.itemCandidates()),
                    valueOrBlank(shippingOrder.deliveryRequestMemo()),
                    valueOrBlank(shippingOrder.deliveryMemo()),
                    valueOrBlank(shippingOrder.courierMemo())
            );
        };
    }

    private String itemSummary(List<OrderDraftItemCandidate> itemCandidates) {
        if (itemCandidates == null) {
            return "";
        }
        return itemCandidates.stream()
                .map(itemCandidate -> List.of(
                                valueOrBlank(itemCandidate.itemName()),
                                valueOrBlank(itemCandidate.itemSpec())
                        ).stream()
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.joining(" ")))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private String quantitySummary(List<OrderDraftItemCandidate> itemCandidates) {
        if (itemCandidates == null) {
            return "";
        }
        return itemCandidates.stream()
                .map(itemCandidate -> List.of(
                                valueOrBlank(itemCandidate.itemQuantity()),
                                valueOrBlank(itemCandidate.itemUnit())
                        ).stream()
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.joining("")))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" / "));
    }

    private String quantitySummaryOrDefault(List<OrderDraftItemCandidate> itemCandidates) {
        String summary = quantitySummary(itemCandidates);
        return summary.isBlank() ? "1" : summary;
    }

    private void appendRow(StringBuilder csv, List<String> columns) {
        csv.append(columns.stream()
                .map(this::escape)
                .collect(Collectors.joining(",")));
        csv.append("\r\n");
    }

    private String escape(String value) {
        String normalized = valueOrBlank(value);
        if (!normalized.contains(",") && !normalized.contains("\"") && !normalized.contains("\n") && !normalized.contains("\r")) {
            return normalized;
        }
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private String valueOrBlank(String value) {
        return Objects.toString(value, "");
    }

    private String firstNonBlank(String firstValue, String secondValue) {
        String first = valueOrBlank(firstValue);
        if (!first.isBlank()) {
            return first;
        }
        return valueOrBlank(secondValue);
    }

    private String joinWithSpace(String firstValue, String secondValue) {
        return List.of(valueOrBlank(firstValue), valueOrBlank(secondValue)).stream()
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
