# 공통 마스터 코드

## 문서 정보

| 항목 | 값 |
|---|---|
| 저장소 | `shipmate-common` |
| 담당 | common |
| 갱신일 | 2026-06-13 |
| 기준 형식 | Markdown 원본과 HTML 보기 파일 |

## 관리 원칙

공유 마스터 코드는 문서 자산으로 관리한다. RDB seed data와 master code table은 이 문서를 기준으로 만든다.

모든 enum과 status를 마스터 코드로 관리하지 않는다. 다만 출고비서 MVP에서는 화면 표시명, 필터, 운영 통계에 직접 쓰이는 상태값은 `tb_code`에 seed 하고, 실제 상태 전이는 application enum으로 검증한다.

## 필드/컬럼 딕셔너리 연계

`field-dictionary.md`의 `codeSet`과 `database-column-dictionary.md`의 `code_set`은 이 문서의 `code_group`과 연결한다.

| API field | DB column | code group | 관리 방식 |
|---|---|---|---|
| `sourceChannel` | `source_channel` | `SOURCE_CHANNEL` | master table |
| `businessChannel` | `business_channel` | `BUSINESS_CHANNEL` | master table |
| `itemUnit` | `item_unit` | `ITEM_UNIT` | master table |
| `defaultCourierCsvTemplate` | `default_courier_csv_template` | `SHIPPING_CARRIER` | master table |
| `orderDraftStatus` | `order_draft_status` | `ORDER_DRAFT_STATUS` | master table 표시명 + application enum |
| `inboundMessageStatus` | `inbound_message_status` | `INBOUND_MESSAGE_STATUS` | master table 표시명 + application enum |
| `role` | `role` | `MERCHANT_USER_ROLE` | master table |
| `callRecordingStatus` | `call_recording_status` | `CALL_RECORDING_STATUS` | master table 표시명 + application enum |

## RDB 전환 기준

### `tb_code`

| Column | Type | Nullable | 설명 |
|---|---|---|---|
| `id` | bigint | false | PK |
| `code_group` | varchar(80) | false | 부모 code set key |
| `code` | varchar(80) | false | code value |
| `label_ko` | varchar(120) | false | 한국어 표시명 |
| `description` | varchar(500) | true | 설명 |
| `active` | boolean | false | 사용 여부 |
| `sort_order` | integer | false | 정렬 순서 |
| `created_at` | timestamp | false | 생성 시각 |
| `updated_at` | timestamp | false | 수정 시각 |

전환 규칙:

- 문서와 RDB seed data가 다르면 문서를 먼저 갱신하고 migration을 맞춘다.
- code set key와 code value는 릴리스 이후 이름을 바꾸지 않고 비활성 처리한다.
- 업무 테이블보다 `tb_code`를 먼저 생성한다.
- 코드 그룹 설명은 `code='__GROUP__'` row로 관리한다.

## 코드 목록

### `SOURCE_CHANNEL`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `KAKAO_PASTE` | 카카오톡 붙여넣기 | 사용 | 10 |
| `SMS_SHARE` | 문자 공유 | 사용 | 20 |
| `EMAIL` | 이메일 | 사용 | 30 |
| `ORDER_PAGE` | 주문 페이지 | 사용 | 40 |

### `BUSINESS_CHANNEL`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `PACKAGING_CUSTOM` | 포장재 주문제작 | 사용 | 10 |
| `BEAUTY_SUPPLIES` | 미용재료/화장품 도매 | 사용 | 20 |
| `MRO_SUPPLIES` | 소모성 자재 | 사용 | 30 |
| `APPAREL_WHOLESALE` | 의류 도매 | 사용 | 40 |
| `METAL_MATERIALS` | 금속 자재 | 사용 | 50 |
| `PHARMACY_SUPPLIES` | 약국/의약외품 | 사용 | 60 |

### `SHIPPING_CARRIER`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `GENERIC` | 기본 양식 | 사용 | 10 |
| `CJ_LOGISTICS` | CJ대한통운 | 사용 | 20 |
| `LOTTE` | 롯데택배 | 사용 | 30 |
| `HANJIN` | 한진택배 | 사용 | 40 |

### `ITEM_UNIT`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `EA` | 개 | 사용 | 10 |
| `SHEET` | 장 | 사용 | 20 |
| `BOX` | 박스 | 사용 | 30 |
| `SET` | 세트 | 사용 | 40 |

### `INBOUND_MESSAGE_STATUS`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `RECEIVED` | 접수됨 | 사용 | 10 |
| `DRAFTING` | 초안 생성 중 | 사용 | 20 |
| `DRAFTED` | 초안 생성됨 | 사용 | 30 |
| `IGNORED` | 제외됨 | 사용 | 80 |
| `FAILED` | 실패 | 사용 | 90 |

### `ORDER_DRAFT_STATUS`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `CREATED` | 초안 | 사용 | 10 |
| `NEEDS_REVIEW` | 검수 필요 | 사용 | 15 |
| `SUBMITTED` | 제출됨 | 사용 | 20 |
| `CANCELED` | 취소됨 | 사용 | 90 |

### `MERCHANT_USER_ROLE`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `ADMIN` | 관리자 | 사용 | 10 |
| `OPERATOR` | 운영자 | 사용 | 20 |
| `VIEWER` | 조회자 | 사용 | 30 |

### `CALL_RECORDING_STATUS`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `CREATED` | 생성됨 | 사용 | 10 |
| `UPLOAD_URL_ISSUED` | 업로드 URL 발급됨 | 사용 | 20 |
| `UPLOADING` | 업로드 중 | 사용 | 30 |
| `UPLOADED` | 업로드 완료 | 사용 | 40 |
| `ANALYZING` | 분석 중 | 사용 | 50 |
| `ANALYSIS_DONE` | 분석 완료 | 사용 | 60 |
| `FAILED` | 실패 | 사용 | 90 |

### `CONSENT_TYPE`

| Code | 표시명 | 사용 여부 | 정렬 |
|---|---|---|---|
| `AUDIO_UPLOAD_ANALYSIS` | 오디오 업로드 및 AI 분석 | 사용 | 10 |
| `AUTO_UPLOAD` | 자동 업로드 | 미사용 | 20 |
