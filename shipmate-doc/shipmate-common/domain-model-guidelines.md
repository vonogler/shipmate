# 도메인 모델 지침

## 목적

카카오톡, 문자, 이메일, 주문 페이지에서 들어온 주문 원문을 주문 초안, 검수, 업무문서로 연결하는 공통 도메인 모델을 정의한다.

## 핵심 원칙

- `shipmate-merchant`는 업종별 프로젝트로 분리하지 않고 `businessChannel` 템플릿으로 확장한다.
- 입력 경로는 `sourceChannel`, 업종/업무 유형은 `businessChannel`로 분리한다.
- AI 결과는 확정 주문이 아니라 검수 가능한 후보로 저장한다.
- 출고표만 전제로 하지 않고 작업지시서, 견적요청서, 납품서, 택배 CSV까지 확장 가능하게 둔다.

## 주요 엔티티

| 엔티티 | 역할 |
|---|---|
| `InboundMessage` | 카카오톡/문자/이메일/주문 페이지에서 들어온 주문 원문 |
| `OrderDraft` | 원문에서 만든 주문 또는 견적 후보 |
| `OrderDraftLineItem` | 주문 초안의 품목/수량/옵션 후보 |
| `ReviewResult` | 이용자의 검수 결과 |
| `BusinessDocument` | 작업지시서, 출고표, 납품서, 택배 CSV 같은 업무 산출물의 공통 부모 |
| `WorkOrder` | 제작/작업 지시서 |
| `ShipmentSheet` | 출고 작업 기준 문서 |
| `DeliveryNote` | 납품 확인 문서 |
| `Customer` | 출고비서 이용자에게 주문하는 거래처 |
| `Item` | 내부 표준 품목 |
| `ItemAlias` | 거래처별 품목 별칭 |
| `PriceRule` | 거래처별 단가, 특가, 등급가 규칙 |

## 기본 관계

```text
InboundMessage 1 -> N OrderDraft
OrderDraft 1 -> N OrderDraftLineItem
OrderDraft 1 -> N ReviewResult
OrderDraft 1 -> N BusinessDocument
BusinessDocument 1 -> 0..1 WorkOrder
BusinessDocument 1 -> 0..1 ShipmentSheet
BusinessDocument 1 -> 0..1 DeliveryNote
Customer 1 -> N ItemAlias
Customer 1 -> N PriceRule
```

## MVP 모델 범위

1차 구현에는 다음만 필수로 둔다.

- `InboundMessage`
- `OrderDraft`
- `OrderDraftLineItem`
- `BusinessDocument`
- `Customer`

2차 이후에 추가한다.

- `Item`
- `ItemAlias`
- `PriceRule`
- `WorkOrder`
- `ShipmentSheet`
- `DeliveryNote`

## 포장재 주문제작 기준 필드

`PACKAGING_CUSTOM`의 첫 구현은 포장재 주문제작을 기준으로 한다.

| 구분 | 필드 예 |
|---|---|
| 공통 | `merchantId`, `customerId`, `sourceChannel`, `businessChannel`, `rawText`, `reviewStatus` |
| 포장재 | `packagingProductType`, `packagingFinishedSize`, `packagingMaterialName`, `packagingPrintType`, `packagingDesignFileStatus` |
| 일정 | `requestedDeliveryDate`, `requestedWorkDueDate` |
| 배송 | `recipientName`, `deliveryAddress`, `deliveryContactPhoneNumber` |

## 금지 사항

- `OrderDraft`를 곧바로 확정 주문으로 취급하지 않는다.
- `rawText`를 삭제하지 않는다.
- `sourceChannel`과 `businessChannel`을 같은 필드로 합치지 않는다.
- 업종별 필드를 공통 필드처럼 이름 짓지 않는다.
