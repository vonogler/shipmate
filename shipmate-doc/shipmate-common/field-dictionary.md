# 필드 딕셔너리

## 목적

API JSON field, Kotlin/Java/TypeScript field, enum, 화면 개념명을 통일한다.

필드 딕셔너리는 `database-column-dictionary.md`와 함께 관리한다. 업무 필드는 DB column, code set, 화면 문구와 연결되어야 한다.

## 기본 명명 규칙

| 구분 | 규칙 |
|---|---|
| API JSON | `camelCase` |
| Kotlin/Java/TypeScript | `camelCase` |
| DB column 연결 | `snake_case` |
| DB table 연결 | 모든 업무 table은 `tb_` prefix를 붙인다. 공통 코드 마스터는 `tb_code` |
| enum value | `UPPER_SNAKE_CASE` |
| 화면 문구 | 한국어 |

## 일반명 사용 제한

`id`, `name`, `phone`, `address`, `status`, `type`, `date`, `amount` 같은 일반명은 단독으로 사용하지 않는다.

예외:

- 엔티티 자신의 primary key로 쓰는 `id`
- audit field인 `createdAt`, `updatedAt`
- 같은 엔티티 내부에서 의미가 명확한 제한된 내부 변수

금지 예:

```json
{
  "id": "123",
  "phone": "010-1111-2222",
  "status": "APPROVED"
}
```

허용 예:

```json
{
  "orderDraftId": "draft_123",
  "merchantId": "merchant_456",
  "customerPhoneNumber": "010-1111-2222",
  "reviewStatus": "APPROVED"
}
```

## 식별자 명명 규칙

- `id`는 엔티티 자신의 primary key로만 사용한다.
- API 요청/응답, DTO, event payload, FK 참조, 검색 조건에서는 `id` 단독 사용을 금지한다.
- 외부 참조 식별자는 반드시 `{entity}Id` 형식으로 작성한다.
- 하나의 payload에 여러 ID가 들어가면 모든 ID는 역할이 드러나는 이름을 사용한다.
- `userId`는 사용하지 않는다.

| 대상 | API field | 의미 |
|---|---|---|
| 출고비서 이용자 | `merchantId` | 출고비서를 쓰는 업체/사업자 |
| 이용자 직원 | `merchantStaffId` | 이용자 업체에 소속된 직원 |
| 고객/거래처 | `customerId` | 출고비서 이용자에게 주문하는 거래처 |
| 수취인 | `recipientId` | 실제 배송을 받는 사람 또는 장소 |
| 주문 원문 | `inboundMessageId` | 카카오톡, 문자, 이메일 등에서 들어온 원문 |
| 주문 초안 | `orderDraftId` | AI 또는 이용자 입력으로 만든 주문 후보 |
| 업무문서 | `businessDocumentId` | 출고표, 작업지시서, 납품서 같은 업무 산출물 |
| 작업지시서 | `workOrderId` | 제작/작업 지시 문서 |
| 출고표 | `shipmentSheetId` | 출고 작업 기준 문서 |
| 납품서 | `deliveryNoteId` | 납품 확인 문서 |
| 내부 운영자 | `adminUserId` | 출고비서 서비스를 운영하는 관리자 |
| AI 작업 | `aiJobId` | AI 추출 또는 분석 작업 |

## 역할 기반 연락처 규칙

`phone`은 단독으로 사용하지 않는다. 전화번호는 반드시 역할을 붙인다.

| API field | 의미 |
|---|---|
| `customerPhoneNumber` | 거래처 대표 또는 담당 전화번호 |
| `recipientPhoneNumber` | 실제 수취인 전화번호 |
| `merchantPhoneNumber` | 출고비서 이용자 업체 전화번호 |
| `merchantStaffPhoneNumber` | 이용자 직원 전화번호 |
| `senderPhoneNumber` | 발신자 전화번호 |
| `billingPhoneNumber` | 세금계산서/결제 담당 전화번호 |
| `deliveryContactPhoneNumber` | 배송지 현장 연락처 |

## 용어 정의

| 한국어 | 코드 용어 | 의미 |
|---|---|---|
| 출고비서 이용자 | `merchant` | 출고비서를 쓰는 업체/사업자 |
| 이용자 직원 | `merchantStaff` | 이용자 업체에 소속된 직원 |
| 고객/거래처 | `customer` | 출고비서 이용자에게 주문하는 거래처 |
| 수취인 | `recipient` | 실제 배송을 받는 사람 또는 장소 |
| 내부 운영자 | `adminUser` | 출고비서 서비스를 운영하는 관리자 |
| 시스템 계정 | `account` | 로그인/인증 단위가 필요할 때만 사용 |

`user`라는 용어는 단독으로 사용하지 않는다.

## 업종 전용 필드 규칙

공통 필드는 짧고 안정적인 이름을 사용할 수 있다. 업종 전용 필드는 업종 prefix를 붙여 충돌을 막는다.

| 업종 | Prefix | 예 |
|---|---|---|
| 포장재/판촉물/맞춤 제작 | `packaging` | `packagingPrintType`, `packagingDesignFileStatus` |
| 미용재료/화장품 | `beauty` | `beautyColorCode`, `beautyProductLineName` |
| 공구/MRO | `mro` | `mroPartNumber`, `mroPackageUnitQuantity` |
| 의류/패션 | `apparel` | `apparelColorName`, `apparelSizeCode` |
| 금속/자재 | `metal` | `metalMaterialGrade`, `metalThicknessValue` |
| 약국 소모품 | `pharmacy` | `pharmacyLotNumber`, `pharmacyExpiryDate` |

## 코드 테이블 연계 규칙

필드가 선택값 또는 코드값이면 `codeSet`을 명시한다.

| API field | DB column | codeSet | 관리 방식 | 설명 |
|---|---|---|---|---|
| `sourceChannel` | `source_channel` | `SOURCE_CHANNEL` | master table | 주문 원문 입력 경로 |
| `businessChannel` | `business_channel` | `BUSINESS_CHANNEL` | master table | 업종/업무 채널 |
| `businessType` | `business_type` | `BUSINESS_TYPE` | master table | 채널 내부 세부 유형 |
| `documentType` | `document_type` | `DOCUMENT_TYPE` | master table | 업무문서 유형 |
| `itemUnit` | `item_unit` | `ITEM_UNIT` | master table | 품목 수량 단위 |
| `shippingCarrier` | `shipping_carrier` | `SHIPPING_CARRIER` | master table | 택배사 |
| `orderDraftStatus` | `order_draft_status` | - | application enum | 주문 초안 내부 상태 |
| `reviewStatus` | `review_status` | - | application enum 우선 | 검수 상태. 화면 표시가 필요하면 master table 표시명만 병행 |
| `aiJobStatus` | `ai_job_status` | - | application enum | AI 작업 내부 상태 |

모든 상태값을 master table에 넣지 않는다. 운영자가 선택하거나 화면 표시, 정렬, 활성 여부 관리가 필요한 값만 master table로 관리한다.

## DB 저장 테이블 연결 규칙

API field가 DB에 저장되는 경우 필드 딕셔너리와 DB 컬럼 딕셔너리 양쪽에서 같은 table, column을 가리켜야 한다.

| 업무 영역 | DB table | 대표 API field | 설명 |
|---|---|---|---|
| 공통 코드 | `tb_code` | `sourceChannel`, `businessChannel`, `itemUnit` | 화면 표시명과 선택값 |
| 인증/권한 | `tb_merchant_users` | `merchantUserId`, `loginId`, `role` | 출고비서 이용자 계정 |
| 주문 원문 | `tb_inbound_messages` | `inboundMessageId`, `rawText`, `inboundMessageStatus` | 카카오톡, 문자, 이메일 주문 원문 |
| 출고표 초안 | `tb_order_drafts` | `orderDraftId`, `deliveryRecipientName`, `orderDraftStatus` | 검수 전/후 출고표 초안 |
| 출고표 품목 후보 | `tb_order_draft_item_candidates` | `orderDraftItemCandidateId`, `itemName`, `matchedUnitPrice` | AI/규칙 기반 품목 후보 |
| 고객/거래처 | `tb_customers` | `customerId`, `customerName`, `customerPhoneNumber` | 고객 기본 배송지와 보관 상태 |
| 고객 단가표 | `tb_customer_price_items` | `customerPriceItemId`, `itemNameKeyword`, `unitPrice` | 고객별 단가 매칭 |
| 이메일 수집 설정 | `tb_email_mailbox_configs` | `host`, `port`, `username`, `sslEnabled` | 이용자별 메일함 설정 |
| 이메일 첨부 원본 | `tb_email_attachments` | `emailAttachmentId`, `fileName`, `fileSizeBytes` | 이메일 수집 첨부 원본 메타데이터와 다운로드 연결 |
| Gmail polling 실행 이력 | `tb_gmail_polling_runs` | `gmailPollingRunId`, `triggerType`, `success`, `latestInboundMessageId` | Gmail 자동/수동 수집 실행 결과 |
| 변경 이력 | `tb_audit_logs` | `auditLogId`, `actorId`, `actionType` | 데이터 변경/운영 행위 기록 |
| 오류 로그 | `tb_error_logs` | `errorLogId`, `code`, `httpStatus`, `errorMetadataJson` | API 오류 기록 |
| 운영 설정 | `tb_merchant_operation_settings` | `defaultMerchantStaffId`, `defaultCourierCsvTemplate` | 파일럿 기본 설정 |

저장 대상이 아닌 응답 전용 field는 DB table을 연결하지 않는다. 예: `accessToken`, `priceItems`, `inboundMessageCount`, `orderDraftStatusCounts`.

## MVP 필드 목록

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `inboundMessageId` | `inbound_message_id` | - | 주문 원문 ID |
| `orderDraftId` | `order_draft_id` | - | 주문 초안 ID |
| `merchantId` | `merchant_id` | - | 출고비서 이용자 ID |
| `merchantStaffId` | `merchant_staff_id` | - | 이용자 직원 ID |
| `customerId` | `customer_id` | - | 거래처 ID |
| `sourceChannel` | `source_channel` | `SOURCE_CHANNEL` | 입력 경로 |
| `businessChannel` | `business_channel` | `BUSINESS_CHANNEL` | 업종/업무 채널 |
| `businessType` | `business_type` | `BUSINESS_TYPE` | 세부 업무 유형 |
| `rawText` | `raw_text` | - | 주문 원문 텍스트 |
| `extractedFields` | `extracted_fields` | - | AI 추출 결과 JSON |
| `customerNameSnapshot` | `customer_name_snapshot` | - | 주문 시점 거래처명 |
| `requestedDeliveryDate` | `requested_delivery_date` | - | 희망 납품일 |
| `reviewStatus` | `review_status` | - | 검수 상태 |
| `createdAt` | `created_at` | - | 생성 시각 |
| `updatedAt` | `updated_at` | - | 수정 시각 |

## 현재 구현 필드 확장 목록

아래 목록은 2026-06-13 기준 `shipmate-api`, `shipmate-merchant`, `shipmate-android` 구현에 맞춰 추가한 필드다. DB 전환 시에는 이 목록을 기준으로 컬럼명을 확정한다.

### 인증/권한

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `merchantUserId` | `merchant_user_id` | - | 이용자 계정 ID |
| `loginId` | `login_id` | - | 로그인 ID |
| `passwordHash` | `password_hash` | - | 비밀번호 해시 |
| `displayName` | `display_name` | - | 화면 표시 이름 |
| `role` | `role` | `MERCHANT_USER_ROLE` | 이용자 계정 권한 |
| `accessToken` | - | - | 로그인 응답 토큰. DB 저장 대상 아님 |

### 주문 원문

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `inboundMessageStatus` | `inbound_message_status` | `INBOUND_MESSAGE_STATUS` | 주문 원문 처리 상태 |
| `receivedAt` | `received_at` | - | 주문 원문 수신 시각 |

### 변경 이력

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `auditMetadataJson` | `metadata_json` | - | 변경 이력 부가 metadata JSON. 재검수 사유처럼 화면 필터와 상세 표시가 필요한 보조 정보를 저장한다. |
| `metadataKey` | - | - | 변경 이력 metadata JSON key 빠른 필터 요청 field. DB 저장 대상 아님 |
| `metadataKeys` | - | - | 변경 이력 필터 옵션 응답 field. DB 저장 대상 아님 |
| `itemSummary` | `tb_audit_logs.metadata_json` | - | 제출 변경 이력에 저장하는 품목 요약 snapshot |
| `quantitySummary` | `tb_audit_logs.metadata_json` | - | 제출 변경 이력에 저장하는 수량 요약 snapshot |
| `itemCandidates` | `tb_audit_logs.metadata_json` | - | 제출 변경 이력에 저장하는 품목 후보 snapshot 배열. 품목명, 규격, 수량, 단위, 단가 비교에 사용한다. |
| `errorMetadataJson` | `tb_error_logs.metadata_json` | - | 오류 발생 시 요청 method/path, 이용자, 작업자 context를 저장하는 오류 로그 부가 metadata JSON |
| `latestInboundMessageId` | `tb_gmail_polling_runs.latest_inbound_message_id` | - | Gmail polling 실행 후 최근 수집 원문 ID. 실패 상세에서 원문 화면 이동에 사용한다. |
| `latestFailureInboundMessageId` | - | - | Gmail polling 상태 응답의 최근 실패 관련 원문 ID. DB 저장 대상 아님 |
| `gmailPollingLatestFailureInboundMessageId` | - | - | 운영 대시보드 응답의 최근 Gmail 실패 관련 원문 ID. DB 저장 대상 아님 |
| `csvColumnName` | - | - | 택배 CSV 검증 오류의 실제 양식 컬럼명. DB 저장 대상 아님 |
| `courierCsvValidationRuleId` | `tb_courier_csv_validation_rules.courier_csv_validation_rule_id` | - | 택배 CSV 검증 규칙 ID |
| `courierCsvTemplate` | `tb_courier_csv_validation_rules.courier_csv_template` | `SHIPPING_CARRIER` | 택배 CSV 검증 규칙이 적용되는 템플릿 |
| `fieldName` | `tb_courier_csv_validation_rules.field_name` | - | 검증 대상 API field 이름 |
| `required` | `tb_courier_csv_validation_rules.required` | - | 필수값 검증 여부 |
| `errorMessage` | `tb_courier_csv_validation_rules.error_message` | - | 필수값 누락 시 표시할 메시지 |
| `sortOrder` | `tb_courier_csv_validation_rules.sort_order` | - | CSV 검증 규칙 표시/실행 순서 |
| `active` | `tb_courier_csv_validation_rules.active` | - | CSV 검증 규칙 사용 여부 |

### 출고표 초안/배송

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `sourceInboundMessageId` | `source_inbound_message_id` | - | 출고표 초안 생성 원문 ID |
| `orderDraftStatus` | `order_draft_status` | `ORDER_DRAFT_STATUS` | 출고표 초안 상태 |
| `deliveryRecipientName` | `delivery_recipient_name` | - | 배송 수취인명 |
| `deliveryRecipientPhone` | `delivery_recipient_phone` | - | 현재 구현 필드. DB 전환 시 `deliveryRecipientPhoneNumber`로 정규화 검토 |
| `deliveryRecipientPhoneNumber` | `delivery_recipient_phone_number` | - | 권장 정규 필드명 |
| `deliveryPostalCode` | `delivery_postal_code` | - | 배송 우편번호 |
| `deliveryAddress` | `delivery_address` | - | 배송 주소 |
| `deliveryAddressDetail` | `delivery_address_detail` | - | 배송 상세주소 |
| `deliveryRequestMemo` | `delivery_request_memo` | - | 배송 요청사항 |
| `deliveryMemo` | `delivery_memo` | - | 납품/배송 업무 메모 |
| `courierMemo` | `courier_memo` | - | 택배 출력용 메모 |
| `reopenReason` | `tb_audit_logs.metadata_json` | - | 제출 출고표 재검수 전환 사유 요청 field. 변경 이력 summary와 metadata JSON에 반영한다. |

### 택배 CSV 검증 응답

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `valid` | - | - | 택배 CSV 필수값 검증 통과 여부. 응답 전용 field |
| `errors` | - | - | 이전 화면 호환을 위한 문자열 오류 목록. 응답 전용 field |
| `errorDetails` | - | - | 구조화된 CSV 검증 오류 목록. 응답 전용 field |
| `fieldName` | - | - | CSV 검증 오류가 연결된 API field 이름. 응답 전용 field |

### 품목 후보

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `orderDraftItemCandidateId` | `order_draft_item_candidate_id` | - | 출고표 초안 품목 후보 ID |
| `itemName` | `item_name` | - | 품목명 |
| `itemSpec` | `item_spec` | - | 품목 규격 |
| `itemQuantity` | `item_quantity` | - | 품목 수량 |
| `matchedUnitPrice` | `matched_unit_price` | - | 매칭된 단가 후보 |
| `lineMemo` | `line_memo` | - | 품목 행 메모 |

### 포장재/판촉물/맞춤 제작

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `packagingWidth` | `packaging_width` | - | 포장재 가로 |
| `packagingDepth` | `packaging_depth` | - | 포장재 세로 |
| `packagingHeight` | `packaging_height` | - | 포장재 높이 |
| `materialName` | `material_name` | - | 현재 구현 필드. DB 전환 시 `packagingMaterialName` 권장 |
| `packagingMaterialName` | `packaging_material_name` | - | 포장재 재질명 권장 필드 |
| `printRequired` | `print_required` | - | 인쇄 필요 여부 |
| `designFileProvided` | `design_file_provided` | - | 디자인 파일 제공 여부 |
| `productionQuantity` | `production_quantity` | - | 제작 수량 |
| `requestedDueDate` | `requested_due_date` | - | 납기 희망일 |
| `sampleRequested` | `sample_requested` | - | 샘플 요청 여부 |
| `customProductType` | `custom_product_type` | - | 맞춤/판촉 제품 종류 |
| `imprintMethod` | `imprint_method` | - | 각인/인쇄 방식 |
| `itemColor` | `item_color` | - | 품목 색상 |
| `customOptionText` | `custom_option_text` | - | 맞춤 옵션 설명 |
| `proofConfirmed` | `proof_confirmed` | - | 시안 확인 여부 |
| `productionConfirmed` | `production_confirmed` | - | 제작 확정 여부 |

### 고객/단가표

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `customerName` | `customer_name` | - | 고객/거래처명 |
| `customerPhone` | `customer_phone` | - | 현재 구현 필드. DB 전환 시 `customerPhoneNumber`로 정규화 검토 |
| `customerPhoneNumber` | `customer_phone_number` | - | 권장 정규 필드명 |
| `defaultRecipientName` | `default_recipient_name` | - | 고객 기본 수취인 |
| `defaultRecipientPhone` | `default_recipient_phone` | - | 현재 구현 필드. DB 전환 시 `defaultRecipientPhoneNumber` 권장 |
| `defaultRecipientPhoneNumber` | `default_recipient_phone_number` | - | 고객 기본 수취인 연락처 권장 필드 |
| `defaultPostalCode` | `default_postal_code` | - | 고객 기본 우편번호 |
| `defaultAddress` | `default_address` | - | 고객 기본 주소 |
| `defaultAddressDetail` | `default_address_detail` | - | 고객 기본 상세주소 |
| `favoriteItemNames` | `favorite_item_names` | - | 자주 쓰는 품목명 목록 |
| `priceItems` | - | - | 고객별 단가표 품목 목록 |
| `customerPriceItemId` | `customer_price_item_id` | - | 고객 단가표 항목 ID |
| `itemNameKeyword` | `item_name_keyword` | - | 품목명 매칭 키워드 |
| `itemSpecKeyword` | `item_spec_keyword` | - | 규격 매칭 키워드 |
| `unitPrice` | `unit_price` | - | 단가 |
| `deletedAt` | `deleted_at` | - | 보관 처리 시각 |

### 이메일/운영/백업/대시보드

| API field | DB column | codeSet | 설명 |
|---|---|---|---|
| `fromEmail` | `from_email` | - | 주문 이메일 발신자 |
| `subject` | `subject` | - | 주문 이메일 제목 |
| `body` | `body` | - | 주문 이메일 본문 |
| `emailAttachmentId` | `email_attachment_id` | - | 이메일 첨부 원본 ID |
| `sourceMessageId` | `source_message_id` | - | Gmail message ID 등 외부 메일 원문 ID |
| `fileName` | `file_name` | - | 첨부 파일명 |
| `contentType` | `content_type` | - | 첨부 콘텐츠 타입 |
| `fileSizeBytes` | `file_size_bytes` | - | 첨부 파일 크기 byte |
| `objectKey` | `object_key` | - | 객체 저장소 내부 키. 일반 응답에서는 숨기고 운영 진단에만 제한적으로 사용 |
| `downloadUrl` | - | - | 첨부 다운로드용 presigned URL. DB 저장 대상 아님 |
| `expiresInSeconds` | - | - | 다운로드 URL 만료 초. DB 저장 대상 아님 |
| `retentionDays` | - | - | 이메일 첨부 원본 보관 기간 일수. 설정 기반 응답 field |
| `gmailPollingRunId` | `gmail_polling_run_id` | - | Gmail polling 실행 이력 ID |
| `triggerType` | `trigger_type` | - | Gmail polling 실행 트리거. `SCHEDULE`, `MANUAL_ALL`, `MANUAL_MERCHANT` |
| `accountCount` | `account_count` | - | Gmail polling 대상 연결 계정 수 |
| `scannedCount` | `scanned_count` | - | Gmail polling 조회 메시지 수 |
| `collectedCount` | `collected_count` | - | Gmail polling 신규 수집 수 |
| `orderDraftCreatedCount` | `order_draft_created_count` | - | Gmail polling 초안 자동 생성 수 |
| `attachmentStoredCount` | `attachment_stored_count` | - | Gmail polling 첨부 원본 저장 수 |
| `skippedDuplicateCount` | `skipped_duplicate_count` | - | Gmail polling 중복 제외 수 |
| `skippedNonOrderCount` | `skipped_non_order_count` | - | Gmail polling 비주문 메일 제외 수 |
| `success` | `success` | - | Gmail polling 실행 성공 여부 |
| `failureCode` | `failure_code` | - | Gmail polling 실패 코드 |
| `failureMessage` | `failure_message` | - | Gmail polling 실패 메시지 |
| `startedAt` | `started_at` | - | Gmail polling 시작 시각 |
| `finishedAt` | `finished_at` | - | Gmail polling 종료 시각 |
| `pollingEnabled` | - | - | Gmail OAuth polling 자동 실행 여부. 설정 기반 응답 field |
| `pollingDelayMs` | - | - | Gmail OAuth polling 실행 간격 millisecond. 설정 기반 응답 field |
| `pollingMaxMessages` | - | - | Gmail OAuth polling 1회 조회 최대 메시지 수. 설정 기반 응답 field |
| `autoCreateOrderDrafts` | - | - | Gmail 수집 후 출고표 초안 자동 생성 여부. 설정 기반 응답 field |
| `collectedMessageCount` | - | - | Gmail 수집 이력 누적 건수. 집계 응답 field |
| `latestCollectedAt` | - | - | Gmail 최신 수집 시각. 집계 응답 field |
| `latestReceivedAt` | - | - | Gmail 최신 수신 시각. 집계 응답 field |
| `recentRunCount` | - | - | 최근 7일 Gmail polling 실행 수. 집계 응답 field |
| `recentFailureCount` | - | - | 최근 7일 Gmail polling 실패 수. 집계 응답 field |
| `recentSkippedNonOrderCount` | - | - | 최근 7일 Gmail polling 비주문 제외 수. 집계 응답 field |
| `latestSuccessAt` | - | - | Gmail polling 최신 성공 시각. 집계 응답 field |
| `latestFailureAt` | - | - | Gmail polling 최신 실패 시각. 집계 응답 field |
| `latestFailureCode` | - | - | Gmail polling 최신 실패 코드. 집계 응답 field |
| `latestFailureMessage` | - | - | Gmail polling 최신 실패 메시지. 집계 응답 field |
| `recentFailureCodeCounts` | - | - | 최근 7일 Gmail polling 실패 코드별 건수. 집계 응답 field |
| `cutoffCreatedAt` | - | - | 이메일 첨부 보관 기간 정리 기준 생성 시각. 정리 응답 field |
| `expiredAttachmentCount` | - | - | 보관 기간이 지난 이메일 첨부 메타데이터 수. 정리 응답 field |
| `deletedObjectCount` | - | - | 객체 저장소에서 삭제한 이메일 첨부 원본 수. 정리 응답 field |
| `deletedMetadataCount` | - | - | DB에서 삭제한 이메일 첨부 메타데이터 수. 정리 응답 field |
| `failedCount` | - | - | 이메일 첨부 정리 실패 수. 정리 응답 field |
| `host` | `host` | - | 이메일 수집 서버 호스트 |
| `port` | `port` | - | 이메일 수집 서버 포트 |
| `username` | `username` | - | 이메일 수집 계정명 |
| `encryptedPasswordMemo` | `encrypted_password_memo` | - | 개발용 비밀번호 보관 메모. 운영 시 비밀 저장소 키로 대체 |
| `sslEnabled` | `ssl_enabled` | - | SSL 연결 여부 |
| `collectionStatus` | `collection_status` | - | 이메일 수집/연결 확인 상태 메시지 |
| `auditLogId` | `audit_log_id` | - | 변경 이력 ID |
| `errorLogId` | `error_log_id` | - | 오류 로그 ID |
| `backupId` | `backup_id` | - | 백업 ID |
| `restoreId` | `restore_id` | - | 복구 승인 ID |
| `defaultMerchantStaffId` | `default_merchant_staff_id` | - | 파일럿 기본 담당자 ID |
| `defaultCourierCsvTemplate` | `default_courier_csv_template` | `SHIPPING_CARRIER` | 기본 택배 CSV 템플릿 |
| `defaultBusinessChannel` | `default_business_channel` | `BUSINESS_CHANNEL` | 기본 업종 채널 |
| `inboundMessageCount` | - | - | 대시보드 주문 원문 수 |
| `orderDraftCount` | - | - | 대시보드 초안 수 |
| `submittedShippingOrderCount` | - | - | 대시보드 제출 출고표 수 |
| `errorLogCount` | - | - | 대시보드 오류 수 |
| `orderDraftStatusCounts` | - | - | 대시보드 출고표 상태별 수 |
| `sourceChannelCounts` | - | - | 대시보드 입력 채널별 수 |
| `errorCodeCounts` | - | - | 대시보드 오류 코드별 수 |
