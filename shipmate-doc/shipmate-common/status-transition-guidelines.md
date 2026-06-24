# 상태 전이 지침

## 목적

주문 원문, 주문 초안, 검수, 업무문서 생성의 상태를 통일한다.

## 기본 원칙

- 상태값은 시스템 로직에 강하게 묶인 경우 application enum으로 둔다.
- 화면 표시명, 정렬, 활성 여부가 필요한 선택값만 master table에 둔다.
- 상태 전이는 한 방향을 기본으로 하며, 되돌림은 명시된 경우에만 허용한다.
- 상태 변경은 `merchantStaffId` 또는 시스템 작업 주체를 남긴다.

## InboundMessage 상태

| 상태 | 의미 |
|---|---|
| `RECEIVED` | 원문 수신 완료 |
| `DRAFTING` | 주문 초안 생성 중 |
| `DRAFTED` | 주문 초안 생성 완료 |
| `IGNORED` | 주문으로 처리하지 않음 |
| `FAILED` | 원문 처리 실패 |

## OrderDraft 상태

| 상태 | 의미 |
|---|---|
| `CREATED` | 초안 생성 |
| `NEEDS_REVIEW` | 이용자 확인 필요 |
| `SUBMITTED` | 출고표 제출 완료 |
| `CANCELED` | 초안 취소 |

## BusinessDocument 상태

| 상태 | 의미 |
|---|---|
| `CREATED` | 문서 초안 생성 |
| `CONFIRMED` | 문서 확정 |
| `EXPORTED` | CSV/PDF 등 외부 산출물 생성 |
| `CANCELLED` | 문서 취소 |

## 기본 전이

```text
InboundMessage
RECEIVED -> DRAFTING -> DRAFTED
RECEIVED -> IGNORED
RECEIVED -> FAILED

OrderDraft
CREATED -> SUBMITTED
CREATED -> NEEDS_REVIEW -> CREATED -> SUBMITTED
CREATED -> CANCELED
NEEDS_REVIEW -> CANCELED
SUBMITTED -> CREATED

BusinessDocument
CREATED -> CONFIRMED -> EXPORTED
CREATED -> CANCELLED
CONFIRMED -> CANCELLED
```

## 출고표 수정 정책

- 제출된 출고표는 직접 수정하지 않는다.
- 제출 후 수정이 필요하면 `POST /api/shipping-orders/{orderDraftId}/reopen-review`로 `SUBMITTED -> CREATED` 재검수 전환을 수행한다.
- 재검수 전환 요청 본문에는 `reopenReason`을 넣어야 하며, 입력한 사유는 변경 이력 summary와 `tb_audit_logs.metadata_json`에 기록한다.
- 재검수 전환 후 이용자는 초안 검수 화면에서 배송 정보, 품목, 단가 후보, 메모를 수정하고 다시 제출한다.
- 재검수 전환, 검수 저장, 재제출은 변경 이력에 각각 `ORDER_DRAFT_REOPENED`, `ORDER_DRAFT_REVIEW_UPDATED`, `ORDER_DRAFT_SUBMITTED`로 기록한다.
- 택배 CSV 생성과 검증은 `SUBMITTED` 상태의 출고표만 대상으로 한다.

## 검수 사유

`needsReviewReasons`는 배열로 저장한다.

예:

- `ITEM_NOT_MATCHED`
- `QUANTITY_AMBIGUOUS`
- `PRICE_NOT_MATCHED`
- `DELIVERY_DATE_MISSING`
- `DESIGN_FILE_REQUIRED`
- `DUPLICATE_ORDER_SUSPECTED`

## 금지 사항

- AI가 만든 결과를 검수 없이 `APPROVED`로 만들지 않는다.
- `FAILED`와 `CANCELED`를 같은 의미로 사용하지 않는다.
- 상태 표시명을 코드값에 직접 의존하지 않는다.
