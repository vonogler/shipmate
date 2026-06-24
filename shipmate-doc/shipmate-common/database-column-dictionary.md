# DB 컬럼 딕셔너리

## 목적

DB table, column, type, nullable, index, FK, code set을 통일한다.

DB 컬럼 딕셔너리는 `field-dictionary.md`와 함께 관리한다. API field와 DB column은 서로 역참조할 수 있어야 한다.

## DB 접근 방식 결정

현재 DB 접근 방식은 `MySQL 8.4 + Flyway SQL migration + Spring JDBC JdbcTemplate`로 결정한다.

JPA 또는 QueryDSL을 우선 적용하지 않는 이유:

- MVP 단계에서는 테이블/컬럼이 자주 조정되므로 SQL 마이그레이션과 명시적 row mapping이 변경 영향을 가장 좁게 드러낸다.
- JSON 파일 저장소에서 MySQL repository로 순차 전환하는 중이므로, 기존 저장소 계약을 유지한 채 SQL만 교체하는 방식이 안전하다.
- 내부관리자 조회, 백업 export, 운영 로그처럼 집계/운영 SQL이 늘어나는 영역은 실제 실행 SQL을 문서와 코드에서 바로 확인하는 편이 운영 검증에 유리하다.
- 현재 도메인은 복잡한 객체 그래프보다 명확한 업무 테이블과 값 객체 중심이므로 ORM 영속성 컨텍스트의 이점이 크지 않다.

JPA/QueryDSL 재검토 기준:

- 주문/출고/고객 간 연관 조회가 반복되고 동적 검색 조건이 5개 이상으로 늘어나는 경우
- 페이징/정렬/필터 조합이 여러 화면에서 중복되어 QueryDSL이 중복 제거 효과를 갖는 경우
- DB repository 전환이 끝나고 테이블 구조가 안정화된 뒤, 유지보수 비용이 명시적 SQL보다 낮아지는 경우

## 기본 명명 규칙

| 구분 | 규칙 |
|---|---|
| table | `tb_` prefix + `snake_case` 복수형. 단, 공통 코드 마스터 테이블은 `tb_code` 단일 테이블 |
| column | `snake_case` |
| FK column | `{entity}_id` |
| code column | field에 대응하는 `snake_case` |
| audit column | `created_at`, `updated_at` |
| enum/code value | `UPPER_SNAKE_CASE` |

## 일반명 사용 제한

`phone`, `name`, `address`, `status`, `type`, `date`, `amount` 같은 일반명은 단독 column으로 사용하지 않는다.

예외:

- 각 테이블의 primary key인 `id`
- audit column인 `created_at`, `updated_at`

금지 예:

```text
phone
name
address
status
type
date
amount
```

허용 예:

```text
customer_phone_number
recipient_name
delivery_address
review_status
document_type
requested_delivery_date
order_total_amount
```

## ID 컬럼 규칙

- 테이블 자신의 primary key는 `id`를 사용한다.
- 외부 참조는 반드시 `{entity}_id`를 사용한다.
- `user_id`는 사용하지 않는다.

| 대상 | DB column | API field |
|---|---|---|
| 출고비서 이용자 | `merchant_id` | `merchantId` |
| 이용자 직원 | `merchant_staff_id` | `merchantStaffId` |
| 고객/거래처 | `customer_id` | `customerId` |
| 수취인 | `recipient_id` | `recipientId` |
| 주문 원문 | `inbound_message_id` | `inboundMessageId` |
| 주문 초안 | `order_draft_id` | `orderDraftId` |
| 업무문서 | `business_document_id` | `businessDocumentId` |
| 작업지시서 | `work_order_id` | `workOrderId` |
| 출고표 | `shipment_sheet_id` | `shipmentSheetId` |
| 납품서 | `delivery_note_id` | `deliveryNoteId` |
| 내부 운영자 | `admin_user_id` | `adminUserId` |
| AI 작업 | `ai_job_id` | `aiJobId` |

## 코드 테이블 연계 규칙

선택값/코드값 column은 `code_set`을 명시한다. `code_set`은 `master-codes.md`의 `code_group`과 같은 의미다.

| DB column | API field | code_set | 관리 방식 | 설명 |
|---|---|---|---|---|
| `source_channel` | `sourceChannel` | `SOURCE_CHANNEL` | master table | 주문 원문 입력 경로 |
| `business_channel` | `businessChannel` | `BUSINESS_CHANNEL` | master table | 업종/업무 채널 |
| `business_type` | `businessType` | `BUSINESS_TYPE` | master table | 채널 내부 세부 유형 |
| `document_type` | `documentType` | `DOCUMENT_TYPE` | master table | 업무문서 유형 |
| `item_unit` | `itemUnit` | `ITEM_UNIT` | master table | 품목 수량 단위 |
| `shipping_carrier` | `shippingCarrier` | `SHIPPING_CARRIER` | master table | 택배사 |
| `order_draft_status` | `orderDraftStatus` | - | application enum | 주문 초안 내부 상태 |
| `review_status` | `reviewStatus` | - | application enum 우선 | 검수 상태 |
| `ai_job_status` | `aiJobStatus` | - | application enum | AI 작업 내부 상태 |

모든 상태값을 master table에 넣지 않는다. 시스템 로직 전이와 강하게 묶인 상태는 application enum으로 관리한다.

## Master table 적용 기준

master table에 넣는 값:

- 화면 표시명, 정렬, 활성 여부가 필요한 값
- 운영자가 선택하거나 필터링하는 값
- 리포트/통계에서 의미가 있는 값
- 나중에 추가 또는 비활성 가능성이 있는 값

application enum으로 둘 값:

- 내부 처리 흐름 상태
- 배치/업로드/AI job처럼 시스템 로직 전이에 강하게 묶인 값
- 사용자가 직접 선택하지 않는 값

## 권장 Master table

### `tb_code`

| Column | Type | Nullable | 설명 |
|---|---|---|---|
| `id` | bigint | false | PK |
| `code_group` | varchar(80) | false | code set key |
| `code` | varchar(80) | false | code value |
| `label_ko` | varchar(120) | false | 한국어 표시명 |
| `description` | varchar(500) | true | 설명 |
| `active` | boolean | false | 사용 여부 |
| `sort_order` | integer | false | 표시 순서 |
| `created_at` | timestamp | false | 생성 시각 |
| `updated_at` | timestamp | false | 수정 시각 |

권장 제약:

- `tb_code`는 `(code_group, code)`를 unique로 둔다.
- 그룹 설명 row는 별도 테이블로 분리하지 않고 `code='__GROUP__'` row로 관리한다.
- 릴리스 이후 code value는 삭제하지 않고 `active=false`로 비활성 처리한다.

## MVP 컬럼 목록

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_inbound_messages` | `id` | `id` | bigint | false | - | PK |
| `tb_inbound_messages` | `merchant_id` | `merchantId` | bigint | false | - | 출고비서 이용자 |
| `tb_inbound_messages` | `merchant_staff_id` | `merchantStaffId` | bigint | true | - | 접수한 이용자 직원 |
| `tb_inbound_messages` | `customer_id` | `customerId` | bigint | true | - | 거래처 |
| `tb_inbound_messages` | `source_channel` | `sourceChannel` | varchar(40) | false | `SOURCE_CHANNEL` | 입력 경로 |
| `tb_inbound_messages` | `business_channel` | `businessChannel` | varchar(60) | false | `BUSINESS_CHANNEL` | 업종/업무 채널 |
| `tb_inbound_messages` | `raw_text` | `rawText` | text | false | - | 주문 원문 |
| `tb_inbound_messages` | `received_at` | `receivedAt` | timestamp | false | - | 원문 수신 시각 |
| `tb_inbound_messages` | `created_at` | `createdAt` | timestamp | false | - | 생성 시각 |
| `tb_order_drafts` | `id` | `id` | bigint | false | - | PK |
| `tb_order_drafts` | `inbound_message_id` | `inboundMessageId` | bigint | false | - | 주문 원문 |
| `tb_order_drafts` | `merchant_id` | `merchantId` | bigint | false | - | 출고비서 이용자 |
| `tb_order_drafts` | `customer_id` | `customerId` | bigint | true | - | 거래처 |
| `tb_order_drafts` | `business_channel` | `businessChannel` | varchar(60) | false | `BUSINESS_CHANNEL` | 업종/업무 채널 |
| `tb_order_drafts` | `business_type` | `businessType` | varchar(60) | false | `BUSINESS_TYPE` | 세부 업무 유형 |
| `tb_order_drafts` | `extracted_fields` | `extractedFields` | json | true | - | AI 추출 결과 |
| `tb_order_drafts` | `review_status` | `reviewStatus` | varchar(40) | false | - | 검수 상태 |
| `tb_order_drafts` | `requested_delivery_date` | `requestedDeliveryDate` | date | true | - | 희망 납품일 |
| `tb_order_drafts` | `created_at` | `createdAt` | timestamp | false | - | 생성 시각 |
| `tb_order_drafts` | `updated_at` | `updatedAt` | timestamp | false | - | 수정 시각 |

## 현재 구현 기준 추가 컬럼 후보

MySQL 전환은 아래 순서로 진행한다.

1. `tb_code`를 가장 먼저 생성한다.
2. 코드값이 필요한 상태/선택 필드를 `tb_code`에 seed 한다.
3. 업무 테이블은 필드 딕셔너리와 이 DB 컬럼 딕셔너리가 일치한 뒤 생성한다.
4. JSON 파일 저장소를 DB repository로 전환할 때도 아래 table 이름과 column 이름을 그대로 사용한다.

현재 생성 대상 업무 테이블:

| 생성 순서 | Table | 역할 |
|---|---|---|
| 1 | `tb_merchant_users` | 로그인/권한 |
| 2 | `tb_customers` | 고객/거래처 |
| 3 | `tb_customer_price_items` | 고객별 단가표 |
| 4 | `tb_inbound_messages` | 주문 원문 |
| 5 | `tb_order_drafts` | 출고표 초안 |
| 6 | `tb_order_draft_item_candidates` | 출고표 품목 후보 |
| 7 | `tb_email_mailbox_configs` | 이메일 수집 설정 |
| 8 | `tb_audit_logs` | 변경 이력 |
| 9 | `tb_error_logs` | 오류 로그 |
| 10 | `tb_merchant_operation_settings` | 이용자 운영 설정 |
| 11 | `tb_merchant_admin_profiles` | 내부관리자용 이용자 운영 상태/메모 |
| 12 | `tb_email_attachments` | 이메일 수집 첨부 원본 메타데이터 |
| 13 | `tb_gmail_polling_runs` | Gmail OAuth polling 실행 이력 |
| 14 | `tb_courier_csv_validation_rules` | 택배 CSV 검증 규칙 |

### 마스터 코드 선행 생성

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_code` | `id` | `id` | bigint | false | - | PK |
| `tb_code` | `code_group` | `codeGroup` | varchar(80) | false | - | 코드 그룹 |
| `tb_code` | `code` | `code` | varchar(80) | false | - | 코드값 |
| `tb_code` | `label_ko` | `labelKo` | varchar(120) | false | - | 한국어 표시명 |
| `tb_code` | `description` | `description` | varchar(500) | true | - | 설명 |
| `tb_code` | `active` | `active` | boolean | false | - | 사용 여부 |
| `tb_code` | `sort_order` | `sortOrder` | integer | false | - | 정렬 순서 |

### 인증/권한

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_merchant_users` | `id` | `id` | bigint | false | - | PK |
| `tb_merchant_users` | `merchant_user_id` | `merchantUserId` | varchar(80) | false | - | 이용자 계정 ID |
| `tb_merchant_users` | `merchant_id` | `merchantId` | varchar(80) | false | - | 출고비서 이용자 |
| `tb_merchant_users` | `login_id` | `loginId` | varchar(120) | false | - | 로그인 ID |
| `tb_merchant_users` | `password_hash` | `passwordHash` | varchar(255) | false | - | 비밀번호 해시 |
| `tb_merchant_users` | `display_name` | `displayName` | varchar(120) | false | - | 표시 이름 |
| `tb_merchant_users` | `role` | `role` | varchar(40) | false | `MERCHANT_USER_ROLE` | 권한 |
| `tb_merchant_users` | `locked_at` | `lockedAt` | timestamp | true | - | 계정 잠금 시각 |

### 내부관리자 이용자 운영 프로필

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_merchant_admin_profiles` | `id` | `id` | bigint | false | - | PK |
| `tb_merchant_admin_profiles` | `merchant_id` | `merchantId` | varchar(80) | false | - | 출고비서 이용자 |
| `tb_merchant_admin_profiles` | `merchant_status` | `status` | varchar(40) | false | - | 내부관리자 운영 상태 |
| `tb_merchant_admin_profiles` | `operation_memo` | `operationMemo` | varchar(1000) | true | - | 운영 메모 |
| `tb_merchant_admin_profiles` | `created_at` | `createdAt` | timestamp | false | - | 생성 시각 |
| `tb_merchant_admin_profiles` | `updated_at` | `updatedAt` | timestamp | false | - | 수정 시각 |

### 주문 원문/출고표

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_inbound_messages` | `inbound_message_status` | `inboundMessageStatus` | varchar(40) | false | `INBOUND_MESSAGE_STATUS` | 원문 처리 상태 |
| `tb_order_drafts` | `source_inbound_message_id` | `sourceInboundMessageId` | varchar(80) | false | - | 원문 ID |
| `tb_order_drafts` | `merchant_staff_id` | `merchantStaffId` | varchar(80) | true | - | 담당자 ID |
| `tb_order_drafts` | `source_channel` | `sourceChannel` | varchar(40) | false | `SOURCE_CHANNEL` | 입력 채널 |
| `tb_order_drafts` | `raw_text` | `rawText` | text | false | - | 주문 원문 |
| `tb_order_drafts` | `delivery_recipient_name` | `deliveryRecipientName` | varchar(120) | true | - | 수취인 |
| `tb_order_drafts` | `delivery_recipient_phone_number` | `deliveryRecipientPhoneNumber` | varchar(40) | true | - | 수취인 연락처 |
| `tb_order_drafts` | `delivery_postal_code` | `deliveryPostalCode` | varchar(20) | true | - | 우편번호 |
| `tb_order_drafts` | `delivery_address` | `deliveryAddress` | varchar(500) | true | - | 주소 |
| `tb_order_drafts` | `delivery_address_detail` | `deliveryAddressDetail` | varchar(500) | true | - | 상세주소 |
| `tb_order_drafts` | `delivery_request_memo` | `deliveryRequestMemo` | varchar(1000) | true | - | 배송 요청사항 |
| `tb_order_drafts` | `delivery_memo` | `deliveryMemo` | varchar(1000) | true | - | 납품 메모 |
| `tb_order_drafts` | `courier_memo` | `courierMemo` | varchar(1000) | true | - | 택배 메모 |
| `tb_order_drafts` | `order_draft_status` | `orderDraftStatus` | varchar(40) | false | `ORDER_DRAFT_STATUS` | 초안 상태 |

### 출고표 품목 후보

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_order_draft_item_candidates` | `id` | `id` | bigint | false | - | PK |
| `tb_order_draft_item_candidates` | `order_draft_item_candidate_id` | `orderDraftItemCandidateId` | varchar(80) | false | - | 품목 후보 ID |
| `tb_order_draft_item_candidates` | `order_draft_id` | `orderDraftId` | varchar(80) | false | - | 출고표 초안 ID |
| `tb_order_draft_item_candidates` | `item_name` | `itemName` | varchar(300) | true | - | 품목명 |
| `tb_order_draft_item_candidates` | `item_spec` | `itemSpec` | varchar(300) | true | - | 규격 |
| `tb_order_draft_item_candidates` | `item_quantity` | `itemQuantity` | varchar(80) | true | - | 수량 |
| `tb_order_draft_item_candidates` | `item_unit` | `itemUnit` | varchar(40) | true | `ITEM_UNIT` | 단위 |
| `tb_order_draft_item_candidates` | `matched_unit_price` | `matchedUnitPrice` | decimal(15,2) | true | - | 매칭 단가 |
| `tb_order_draft_item_candidates` | `packaging_width` | `packagingWidth` | varchar(80) | true | - | 포장재 가로 |
| `tb_order_draft_item_candidates` | `packaging_depth` | `packagingDepth` | varchar(80) | true | - | 포장재 세로 |
| `tb_order_draft_item_candidates` | `packaging_height` | `packagingHeight` | varchar(80) | true | - | 포장재 높이 |
| `tb_order_draft_item_candidates` | `packaging_material_name` | `packagingMaterialName` | varchar(200) | true | - | 포장재 재질명 |
| `tb_order_draft_item_candidates` | `print_required` | `printRequired` | boolean | true | - | 인쇄 필요 여부 |
| `tb_order_draft_item_candidates` | `design_file_provided` | `designFileProvided` | boolean | true | - | 디자인 파일 여부 |
| `tb_order_draft_item_candidates` | `production_quantity` | `productionQuantity` | varchar(80) | true | - | 제작 수량 |
| `tb_order_draft_item_candidates` | `requested_due_date` | `requestedDueDate` | date | true | - | 납기 희망일 |
| `tb_order_draft_item_candidates` | `sample_requested` | `sampleRequested` | boolean | true | - | 샘플 요청 여부 |
| `tb_order_draft_item_candidates` | `custom_product_type` | `customProductType` | varchar(200) | true | - | 맞춤 제품 종류 |
| `tb_order_draft_item_candidates` | `imprint_method` | `imprintMethod` | varchar(200) | true | - | 각인/인쇄 방식 |
| `tb_order_draft_item_candidates` | `item_color` | `itemColor` | varchar(120) | true | - | 색상 |
| `tb_order_draft_item_candidates` | `custom_option_text` | `customOptionText` | varchar(1000) | true | - | 맞춤 옵션 |
| `tb_order_draft_item_candidates` | `proof_confirmed` | `proofConfirmed` | boolean | true | - | 시안 확인 |
| `tb_order_draft_item_candidates` | `production_confirmed` | `productionConfirmed` | boolean | true | - | 제작 확정 |
| `tb_order_draft_item_candidates` | `line_memo` | `lineMemo` | varchar(1000) | true | - | 행 메모 |

### 고객/단가표

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_customers` | `customer_name` | `customerName` | varchar(200) | false | - | 고객명 |
| `tb_customers` | `customer_phone_number` | `customerPhoneNumber` | varchar(40) | true | - | 고객 연락처 |
| `tb_customers` | `default_recipient_name` | `defaultRecipientName` | varchar(120) | true | - | 기본 수취인 |
| `tb_customers` | `default_recipient_phone_number` | `defaultRecipientPhoneNumber` | varchar(40) | true | - | 기본 수취인 연락처 |
| `tb_customers` | `default_postal_code` | `defaultPostalCode` | varchar(20) | true | - | 기본 우편번호 |
| `tb_customers` | `default_address` | `defaultAddress` | varchar(500) | true | - | 기본 주소 |
| `tb_customers` | `default_address_detail` | `defaultAddressDetail` | varchar(500) | true | - | 기본 상세주소 |
| `tb_customers` | `favorite_item_names` | `favoriteItemNames` | json | false | - | 자주 쓰는 품목 |
| `tb_customers` | `deleted_at` | `deletedAt` | timestamp | true | - | 보관 처리 시각 |
| `tb_customer_price_items` | `customer_price_item_id` | `customerPriceItemId` | varchar(80) | false | - | 단가표 항목 ID |
| `tb_customer_price_items` | `item_name_keyword` | `itemNameKeyword` | varchar(200) | true | - | 품목 키워드 |
| `tb_customer_price_items` | `item_spec_keyword` | `itemSpecKeyword` | varchar(200) | true | - | 규격 키워드 |
| `tb_customer_price_items` | `unit_price` | `unitPrice` | decimal(15,2) | false | - | 단가 |

### 운영/이메일/백업

| Table | Column | API field | Type | Nullable | code_set | 설명 |
|---|---|---|---|---|---|---|
| `tb_email_mailbox_configs` | `host` | `host` | varchar(255) | false | - | 메일 서버 |
| `tb_email_mailbox_configs` | `port` | `port` | integer | false | - | 포트 |
| `tb_email_mailbox_configs` | `username` | `username` | varchar(255) | false | - | 메일 계정 |
| `tb_email_mailbox_configs` | `encrypted_password_memo` | `encryptedPasswordMemo` | varchar(500) | false | - | 비밀번호 보관 메모 |
| `tb_email_mailbox_configs` | `ssl_enabled` | `sslEnabled` | boolean | false | - | SSL 여부 |
| `tb_email_mailbox_configs` | `collection_status` | `collectionStatus` | varchar(500) | true | - | 최근 수집/연결 상태 |
| `tb_email_attachments` | `email_attachment_id` | `emailAttachmentId` | varchar(64) | false | - | 이메일 첨부 원본 ID |
| `tb_email_attachments` | `merchant_id` | `merchantId` | varchar(64) | false | - | 출고비서 이용자 |
| `tb_email_attachments` | `inbound_message_id` | `inboundMessageId` | varchar(64) | false | - | 주문 원문 ID |
| `tb_email_attachments` | `source_channel` | `sourceChannel` | varchar(40) | false | `SOURCE_CHANNEL` | 입력 채널 |
| `tb_email_attachments` | `source_message_id` | `sourceMessageId` | varchar(200) | true | - | Gmail message ID 등 외부 메일 원문 ID |
| `tb_email_attachments` | `file_name` | `fileName` | varchar(500) | false | - | 첨부 파일명 |
| `tb_email_attachments` | `content_type` | `contentType` | varchar(200) | false | - | 첨부 콘텐츠 타입 |
| `tb_email_attachments` | `file_size_bytes` | `fileSizeBytes` | bigint | false | - | 첨부 파일 크기 byte |
| `tb_email_attachments` | `object_key` | `objectKey` | varchar(800) | false | - | 객체 저장소 내부 키 |
| `tb_email_attachments` | `created_at` | `createdAt` | datetime(6) | false | - | 생성 시각 |
| `tb_gmail_polling_runs` | `gmail_polling_run_id` | `gmailPollingRunId` | varchar(64) | false | - | Gmail polling 실행 이력 ID |
| `tb_gmail_polling_runs` | `trigger_type` | `triggerType` | varchar(40) | false | - | 실행 트리거 |
| `tb_gmail_polling_runs` | `merchant_id` | `merchantId` | varchar(64) | true | - | 특정 이용자 수동 실행 대상 |
| `tb_gmail_polling_runs` | `account_count` | `accountCount` | integer | false | - | 연결 계정 수 |
| `tb_gmail_polling_runs` | `scanned_count` | `scannedCount` | integer | false | - | 조회 메시지 수 |
| `tb_gmail_polling_runs` | `collected_count` | `collectedCount` | integer | false | - | 신규 수집 수 |
| `tb_gmail_polling_runs` | `order_draft_created_count` | `orderDraftCreatedCount` | integer | false | - | 초안 자동 생성 수 |
| `tb_gmail_polling_runs` | `attachment_stored_count` | `attachmentStoredCount` | integer | false | - | 첨부 저장 수 |
| `tb_gmail_polling_runs` | `skipped_duplicate_count` | `skippedDuplicateCount` | integer | false | - | 중복 제외 수 |
| `tb_gmail_polling_runs` | `skipped_non_order_count` | `skippedNonOrderCount` | integer | false | - | 비주문 메일 제외 수 |
| `tb_gmail_polling_runs` | `success` | `success` | boolean | false | - | 실행 성공 여부 |
| `tb_gmail_polling_runs` | `failure_code` | `failureCode` | varchar(80) | true | - | 실패 코드 |
| `tb_gmail_polling_runs` | `failure_message` | `failureMessage` | varchar(1000) | true | - | 실패 메시지 |
| `tb_gmail_polling_runs` | `latest_inbound_message_id` | `latestInboundMessageId` | varchar(80) | true | `ix_tb_gmail_polling_runs_latest_inbound` | 최근 수집 원문 ID |
| `tb_gmail_polling_runs` | `started_at` | `startedAt` | datetime(6) | false | - | 시작 시각 |
| `tb_gmail_polling_runs` | `finished_at` | `finishedAt` | datetime(6) | false | - | 종료 시각 |
| `tb_audit_logs` | `audit_log_id` | `auditLogId` | varchar(80) | false | - | 변경 이력 ID |
| `tb_audit_logs` | `merchant_id` | `merchantId` | varchar(80) | false | - | 출고비서 이용자 |
| `tb_audit_logs` | `actor_id` | `actorId` | varchar(80) | true | - | 행위자 ID |
| `tb_audit_logs` | `action_type` | `actionType` | varchar(80) | false | - | 행위 유형 |
| `tb_audit_logs` | `target_type` | `targetType` | varchar(80) | false | - | 대상 유형 |
| `tb_audit_logs` | `target_id` | `targetId` | varchar(80) | true | - | 대상 ID |
| `tb_audit_logs` | `summary` | `summary` | varchar(1000) | false | - | 요약 |
| `tb_audit_logs` | `metadata_json` | `auditMetadataJson` | json | true | `ix_tb_audit_logs_action_created`, `ix_tb_audit_logs_target_created`, `ix_tb_audit_logs_merchant_created` | 변경 이력 부가 metadata JSON |
| `tb_error_logs` | `error_log_id` | `errorLogId` | varchar(80) | false | - | 오류 로그 ID |
| `tb_error_logs` | `code` | `code` | varchar(80) | false | - | 오류 코드 |
| `tb_error_logs` | `http_status` | `httpStatus` | integer | false | - | HTTP 상태 코드 |
| `tb_error_logs` | `message` | `message` | varchar(1000) | false | - | 오류 메시지 |
| `tb_error_logs` | `exception_type` | `exceptionType` | varchar(255) | true | - | 예외 유형 |
| `tb_error_logs` | `metadata_json` | `errorMetadataJson` | json | true | - | 오류 요청 context metadata JSON |
| `tb_error_logs` | `handling_status` | `handlingStatus` | varchar(40) | false | - | 운영 처리 상태 |
| `tb_error_logs` | `assigned_actor_id` | `assignedActorId` | varchar(80) | true | - | 처리 담당자 |
| `tb_error_logs` | `handling_memo` | `handlingMemo` | varchar(1000) | true | - | 처리 메모 |
| `tb_error_logs` | `handled_at` | `handledAt` | timestamp | true | - | 조치 완료 시각 |
| `tb_courier_csv_validation_rules` | `courier_csv_validation_rule_id` | `courierCsvValidationRuleId` | varchar(80) | false | PK | 택배 CSV 검증 규칙 ID |
| `tb_courier_csv_validation_rules` | `courier_csv_template` | `courierCsvTemplate` | varchar(40) | false | `uk_tb_courier_csv_validation_rules_template_field`, `ix_tb_courier_csv_validation_rules_template_active`, `SHIPPING_CARRIER` | 택배 CSV 템플릿 |
| `tb_courier_csv_validation_rules` | `field_name` | `fieldName` | varchar(120) | false | `uk_tb_courier_csv_validation_rules_template_field` | 검증 대상 API field 이름 |
| `tb_courier_csv_validation_rules` | `csv_column_name` | `csvColumnName` | varchar(120) | true | - | 택배사 업로드 양식 컬럼명 |
| `tb_courier_csv_validation_rules` | `required` | `required` | boolean | false | - | 필수값 검증 여부 |
| `tb_courier_csv_validation_rules` | `error_message` | `errorMessage` | varchar(500) | false | - | 필수값 누락 메시지 |
| `tb_courier_csv_validation_rules` | `sort_order` | `sortOrder` | integer | false | - | 검증 순서 |
| `tb_courier_csv_validation_rules` | `active` | `active` | boolean | false | `ix_tb_courier_csv_validation_rules_template_active` | 사용 여부 |
| `tb_courier_csv_validation_rules` | `updated_at` | `updatedAt` | timestamp | false | - | 수정 시각 |
| `tb_merchant_operation_settings` | `default_merchant_staff_id` | `defaultMerchantStaffId` | varchar(80) | true | - | 기본 담당자 |
| `tb_merchant_operation_settings` | `default_courier_csv_template` | `defaultCourierCsvTemplate` | varchar(40) | false | `SHIPPING_CARRIER` | 기본 택배사 |
| `tb_merchant_operation_settings` | `default_business_channel` | `defaultBusinessChannel` | varchar(60) | false | `BUSINESS_CHANNEL` | 기본 업종 |
