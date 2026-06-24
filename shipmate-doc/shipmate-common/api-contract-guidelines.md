# API 계약 지침

## 목적

`shipmate-api`, `shipmate-android`, `shipmate-merchant`, `shipmate-admin` 사이의 API 계약을 통일한다.

## 기본 원칙

- API field는 `field-dictionary.md`를 따른다.
- DB column은 `database-column-dictionary.md`를 따른다.
- 화면 문구는 한국어로 작성하지만 API field, enum, path는 영어 계약을 유지한다.
- request/response에서 `userId`, `phone`, `status`, `type` 같은 일반명 단독 사용을 금지한다.
- 여러 ID가 함께 들어가는 payload는 모두 역할 기반 이름을 사용한다.

## 경로 규칙

REST path는 복수형 resource를 사용한다.

```text
/api/inbound-messages
/api/order-drafts
/api/business-documents
/api/customers
/api/items
```

업종 템플릿은 path보다 request field로 우선 표현한다.

```json
{
  "businessChannel": "PACKAGING_CUSTOM",
  "businessType": "PACKAGING"
}
```

## 공통 응답 형식

단건:

```json
{
  "data": {
    "orderDraftId": "draft_123"
  }
}
```

목록:

```json
{
  "data": [],
  "page": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 0
  }
}
```

오류:

```json
{
  "errorCode": "SM-COM-0001",
  "message": "잘못된 요청입니다.",
  "details": []
}
```

## MVP API 후보

### 주문 원문 생성

```text
POST /api/inbound-messages
```

```json
{
  "merchantId": "merchant_001",
  "merchantStaffId": "staff_001",
  "customerId": null,
  "sourceChannel": "KAKAO_PASTE",
  "businessChannel": "PACKAGING_CUSTOM",
  "rawText": "지난번 쇼핑백 1,000장 다시 주세요.",
  "receivedAt": "2026-06-13T10:30:00+09:00"
}
```

### 주문 초안 목록

```text
GET /api/order-drafts?merchantId=merchant_001&businessChannel=PACKAGING_CUSTOM
```

### 주문 초안 검수 저장

```text
PATCH /api/order-drafts/{orderDraftId}/review
```

```json
{
  "merchantStaffId": "staff_001",
  "reviewStatus": "APPROVED",
  "reviewMemo": "수량 확인 완료"
}
```

## 금지 사항

- path에 업종명을 먼저 고정하지 않는다. 예: `/api/packaging/order-drafts`는 MVP에서 사용하지 않는다.
- `id`, `userId`, `phone`, `status` 단독 field를 사용하지 않는다.
- AI 추출 결과를 확정 주문 API와 같은 응답으로 섞지 않는다.
