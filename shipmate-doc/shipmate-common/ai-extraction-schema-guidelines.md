# AI 추출 스키마 지침

## 목적

AI가 주문 원문에서 추출한 결과를 API, DB, Merchant 검수 화면이 동일하게 해석하도록 스키마를 정의한다.

## 기본 원칙

- AI 결과는 확정 주문이 아니라 후보 데이터다.
- 원문 `rawText`는 항상 보관한다.
- 추출 결과에는 confidence와 확인 필요 사유를 함께 둔다.
- 업종 공통 필드와 업종 전용 필드를 분리한다.
- 필드명은 `field-dictionary.md`를 따른다.

## 공통 스키마

```json
{
  "businessChannel": "PACKAGING_CUSTOM",
  "businessType": "PACKAGING",
  "customerCandidate": {
    "customerId": null,
    "customerNameText": "한국유통",
    "confidence": 0.72
  },
  "requestedDeliveryDate": "2026-06-20",
  "lineItems": [],
  "needsReviewReasons": [],
  "extractionConfidence": 0.81
}
```

## Line item 공통 필드

```json
{
  "lineItemIndex": 1,
  "rawLineText": "지난번 쇼핑백 1,000장 다시 주세요",
  "itemCandidateName": "쇼핑백",
  "itemId": null,
  "quantityValue": 1000,
  "itemUnit": "EA",
  "needsReviewReasons": ["DESIGN_FILE_REQUIRED"],
  "confidence": 0.84
}
```

## PACKAGING_CUSTOM 확장 필드

```json
{
  "packagingProductType": "SHOPPING_BAG",
  "packagingFinishedSize": null,
  "packagingMaterialName": null,
  "packagingPrintType": "COLOR",
  "packagingDesignFileStatus": "NEW_FILE_REQUIRED",
  "packagingPreviousOrderReference": "LAST_ORDER"
}
```

## 확인 필요 사유

| 값 | 의미 |
|---|---|
| `CUSTOMER_NOT_MATCHED` | 거래처 후보를 확정하지 못함 |
| `ITEM_NOT_MATCHED` | 내부 품목과 매칭하지 못함 |
| `QUANTITY_AMBIGUOUS` | 수량 해석이 불명확함 |
| `UNIT_AMBIGUOUS` | 단위 해석이 불명확함 |
| `PRICE_NOT_MATCHED` | 단가를 확정하지 못함 |
| `DELIVERY_DATE_MISSING` | 희망 납품일이 없음 |
| `DESIGN_FILE_REQUIRED` | 디자인 파일 확인 필요 |
| `DUPLICATE_ORDER_SUSPECTED` | 중복 주문 가능성 있음 |

## 저장 방식

MVP에서는 `OrderDraft.extractedFields`에 JSON으로 저장한다.

구조가 안정화되면 자주 검색하는 필드만 별도 column으로 승격한다.

승격 후보:

- `business_channel`
- `business_type`
- `customer_id`
- `requested_delivery_date`
- `review_status`

## 금지 사항

- AI 결과만으로 `reviewStatus=APPROVED`를 만들지 않는다.
- AI가 생성한 품목명을 내부 표준 품목으로 즉시 확정하지 않는다.
- confidence가 없는 추출 결과를 저장하지 않는다.
- 원문 없이 추출 결과만 저장하지 않는다.

## API 검증 기준

`shipmate-api`는 AI 응답을 바로 반영하지 않고 다음 조건을 통과한 후보만 출고표 초안에 반영한다.

- 최상위 객체 응답은 `extractionConfidence`가 0 이상 1 이하 숫자여야 한다.
- `lineItems`는 배열이어야 한다.
- 각 line item은 `itemCandidateName` 또는 `itemName`이 있어야 한다.
- 품목명은 300자 이하로 제한한다.
- 수량 텍스트는 80자 이하로 제한한다.
- `requestedDueDate`가 있으면 `YYYY-MM-DD` 형식이어야 한다.
- `confidence`가 있으면 0 이상 1 이하 숫자여야 한다.
- `needsReviewReasons`는 이 문서의 확인 필요 사유 값만 허용한다.

검증에 실패한 전체 응답 또는 line item은 반영하지 않는다. 반영할 후보가 없으면 기존 규칙 기반 후보를 사용한다.
