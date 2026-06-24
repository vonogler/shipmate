# 이용자 전용 이메일 기반 발주 수집 구조

## 방향

이메일로 출고표를 생성하는 MVP는 이용자별 전용 발주 이메일을 발급하는 방식으로 진행한다.

```text
출고비서 이용자: (주)한국유통
이용자 전용 발주 이메일: korea-distribution@orders.shipmate.co.kr
고객 행동: 평소처럼 이메일로 발주서 발송
출고비서 처리: 이메일 수신, 원문 저장, 주문 초안 생성, 출고표 생성
```

고객은 별도 앱이나 새로운 주문 도구를 쓰지 않아도 된다. 이용자는 자신의 고객에게 전용 발주 이메일만 안내하면 되고, 출고비서는 수신된 이메일을 주문 초안으로 바꾼다.

## 권장 결론

초기에는 이용자 전용 이메일 주소를 출고비서가 발급한다.

```text
order+merchant_123@shipmate.co.kr
korea-distribution@orders.shipmate.co.kr
order@korea-distribution.orders.shipmate.co.kr
```

이 방식은 기존 이메일 계정 연동보다 구현과 운영이 단순하고, 고객별 주문 출처를 안정적으로 구분할 수 있다.

## 기본 흐름

```text
고객
-> 이용자 전용 발주 이메일로 발주서 발송
-> 출고비서 메일 수신 서버 또는 email worker 수신
-> 원본 이메일과 첨부파일 저장
-> 이용자와 고객 후보 식별
-> 제목, 본문, 첨부파일에서 발주 정보 추출
-> 주문 초안 생성
-> shipmate-merchant에서 검수/수정/승인
-> 출고표 생성
```

고객에게 보여주는 정보는 원본 이메일이 아니라 출고비서가 처리한 주문 접수 결과와 진행 상태다.

## 장점

- 고객은 평소처럼 이메일만 보내면 된다.
- 이용자별 주문 출처를 수신 주소만으로 구분할 수 있다.
- 메일 원문, 첨부파일, 수신 시각, 발신자를 출고 근거로 보관할 수 있다.
- 이용자에게 전용 주문 채널을 제공하는 느낌을 줄 수 있다.
- 이후 이용자 자체 도메인 이메일이나 기존 이메일 계정 연동으로 확장하기 쉽다.

## 대안과 비교

### 이용자 기존 이메일 계정 연동

```text
order@korea-distribution.co.kr
-> 출고비서가 IMAP 또는 provider API로 읽기
```

고객 경험은 가장 자연스럽지만, MVP로는 복잡하다. Gmail, Microsoft 365, 네이버, 회사 메일 등 제공자별 인증 방식과 권한 정책이 다르고, 이용자가 계정 접근 권한을 출고비서에 허용해야 한다.

이 방식은 2단계 이후에 제공한다.

### 이용자 자체 도메인 연결

```text
order@korea-distribution.co.kr
-> MX 또는 forwarding 설정
-> 출고비서 수신
```

브랜드 경험은 좋지만 DNS 설정과 도메인 소유권 확인이 필요하다. 초기 이용자에게는 설정 부담이 크므로 MVP 기본값으로 두지 않는다.

### 웹 주문 페이지 첨부

```text
(주)한국유통 주문 접수 페이지
-> 엑셀, PDF, 이미지 첨부
-> 주문 초안 생성
```

메일 서버 구현 부담은 줄지만 고객에게 새 행동을 요구한다. 이메일 발주가 이미 익숙한 고객에게는 전용 이메일 방식이 더 자연스럽다.

## MVP 위치 결정

초기에는 새 프로젝트를 만들지 않고 `shipmate-api`에 기능으로 추가한다.

이메일 발주 수집은 아직 독립 서비스로 분리할 만큼 처리량과 경계가 확정되지 않았다. 현재 필요한 것은 이용자 전용 이메일로 받은 발주를 주문 초안으로 만드는 MVP이므로, 먼저 API 안에 도메인 패키지로 넣고 나중에 처리량이나 복잡도가 커지면 별도 worker 또는 `shipmate-email`로 분리한다.

## 추천 패키지 구조

```text
shipmate-api
  src/main/java/com/shipmate/api/email
    MerchantEmailAddress
    EmailMessage
    EmailAttachment
    EmailIngestionService
    EmailAddressResolver
    EmailParsingService
    EmailController

  src/main/java/com/shipmate/api/order
    OrderDraft
    OrderDraftService
    OrderReviewService
```

## 주요 처리 단계

1. 이용자별 발주 이메일 주소 생성
2. 이용자가 고객에게 전용 이메일 주소 안내
3. 고객 발주 이메일 수신
4. 수신 주소로 이용자 식별
5. 발신자, 제목, 본문, 첨부파일 저장
6. 첨부파일이 있으면 PDF, 엑셀, 이미지 등 형식별 추출
7. 품목, 수량, 배송일, 주소, 메모 추출
8. 주문 초안 생성
9. `shipmate-merchant`에서 이용자가 검수
10. 승인 후 출고표 생성

## 상태 모델 초안

```text
RECEIVED        이메일 수신
STORED          원문과 첨부파일 저장 완료
PARSED          발주 정보 추출 완료
NEEDS_REVIEW    이용자 확인 필요
APPROVED        이용자 승인
ORDER_CREATED   출고표 생성 완료
REJECTED        발주 제외
FAILED          처리 실패
```

## 데이터 구조 초안

```text
merchant_email_address
- id
- merchantId
- emailAddress
- displayName
- status
- createdAt

email_message
- id
- merchantId
- merchantEmailAddressId
- fromAddress
- toAddress
- subject
- receivedAt
- rawBody
- rawMessageStorageKey
- attachmentIds
- status

email_attachment
- id
- emailMessageId
- fileName
- contentType
- storageKey
- size

order_draft
- id
- sourceType
- sourceId
- merchantId
- customerId
- status
- extractedJson
- reviewedJson
- confidence
```

## 이메일 주소 발급 규칙

초기에는 운영과 충돌 방지를 위해 시스템 식별자를 포함한 주소를 우선 사용한다.

```text
order+merchant_123@shipmate.co.kr
```

고객에게 보여주는 주소가 너무 시스템처럼 보이면 이용자별 별칭을 제공한다.

```text
korea-distribution@orders.shipmate.co.kr
```

별칭 주소는 중복, 금칙어, 변경 이력, 해지 후 재사용 정책을 관리해야 한다.

## 보관 정책

이메일 원문과 첨부파일은 출고 근거가 될 수 있으므로 주문 초안과 연결해서 보관한다.

다만 개인정보가 포함될 수 있으므로 다음 기준이 필요하다.

- 원본 이메일 보관 기간
- 첨부파일 보관 기간
- 이용자 탈퇴 시 삭제 또는 익명화 정책
- 고객 개인정보 마스킹 기준
- 처리 실패 이메일 재시도와 폐기 기준

## 기존 메일 계정 연동 운영 정책

현재 구현은 두 흐름을 병행한다.

- 기존 IMAP 수집: `shipmate-api`의 이메일 수집 설정으로 IMAP 서버에 로그인해 읽지 않은 최근 메일을 수집한다.
- Gmail OAuth 수집: Google OAuth로 Gmail 읽기 권한을 연결한 뒤 서버 스케줄러가 Gmail API를 주기적으로 polling한다.

운영 기준:

- Gmail은 일반 계정 비밀번호가 아니라 앱 비밀번호를 우선 사용한다.
- 네이버는 IMAP 사용 설정을 켠 뒤 메일 비밀번호 또는 앱 비밀번호 정책을 따른다.
- 로컬 테스트 계정 정보는 `shipmate-api/requests/http-client.private.env.json`에만 둔다.
- `http-client.private.env.json`은 Git 추적 대상에서 제외한다.
- 수집 성공/실패 결과는 `tb_email_mailbox_configs.collection_status`에 기록한다.
- 수집 실패는 `SM-EML-7102` 오류 로그로 남긴다.
- 읽지 않은 메일만 수집하고, 주문 원문 저장 후 IMAP `Seen` 플래그를 설정해 중복 수집을 줄인다.
- Gmail OAuth polling은 `GOOGLE_GMAIL_POLLING_DELAY_MS` 주기로 실행하고 기본값은 5분이다.
- Gmail OAuth polling은 `in:inbox is:unread newer_than:14d` 조건으로 읽지 않은 최근 메일을 조회한다.
- Gmail message ID는 `tb_google_gmail_collected_messages`에 기록해 중복 수집을 막는다.
- Gmail OAuth polling으로 수집한 메일은 주문 원문 저장 후 Gmail API `modify`로 `UNREAD` 라벨 제거를 시도한다.
- 기존 `gmail.readonly` 토큰처럼 읽음 처리 권한이 부족한 경우 수집은 성공으로 유지하고 `SM-GML-9108` 오류 로그만 남긴다.
- Gmail OAuth polling으로 신규 수집한 주문 원문은 기본적으로 출고표 초안을 자동 생성한다.
- 출고표 초안 생성 직후 `OPENAI_API_KEY`가 있으면 제목/본문/첨부 파일명 후보를 포함한 원문을 AI 추출에 자동 전달한다.
- AI 응답의 `extractionConfidence` 또는 line item `confidence`가 0.75 미만이거나 `needsReviewReasons`가 있으면 초안을 `NEEDS_REVIEW` 상태로 분리한다.
- `OPENAI_API_KEY`가 없거나 AI 응답 검증에 실패하면 현재 규칙 기반 후보를 유지한다.
- Google 보안 알림, 인증 코드, 개인정보 안내, `no-reply` 계열 안내 메일처럼 주문 가능성이 낮은 메일은 주문 원문으로 저장하지 않고 읽음 처리 후 비주문 제외 건수로 기록한다.
- 자동 초안 생성을 끄려면 `GOOGLE_GMAIL_AUTO_CREATE_ORDER_DRAFTS=false`로 설정한다.
- 자동 초안 생성 실패는 메일 수집을 롤백하지 않고 `SM-GML-9106` 오류 로그로 기록한다.
- Gmail OAuth polling으로 발견한 첨부 원본은 `email-attachments/{merchantId}/{inboundMessageId}/...` 객체 키로 저장하고 `tb_email_attachments`에 메타데이터를 기록한다.
- 첨부 원본 저장 실패는 메일 수집과 초안 생성을 롤백하지 않고 `SM-GML-9107` 오류 로그로 기록한다.
- Gmail OAuth polling 실행 결과는 `tb_gmail_polling_runs`에 성공/실패, 실행 트리거, 조회/수집/중복/비주문 제외/첨부 저장 건수, 실패 코드와 메시지를 기록한다.
- Gmail OAuth polling 운영 상태는 `/api/oauth/google/gmail-polling/status`에서 설정값, 연결 계정 수, 누적 수집 건수, 최신 수집 시각, 첨부 저장 건수, 최근 7일 실행/실패/비주문 제외 건수와 실패 코드별 집계를 조회한다.
- 이메일 첨부 목록은 `/api/email-orders/attachments?inboundMessageId={id}`에서 조회하고, 다운로드는 `/api/email-orders/attachments/{emailAttachmentId}/download-url`에서 짧은 만료 시간을 가진 presigned URL을 발급받아 수행한다.
- 이메일 첨부 다운로드 URL 발급은 `EMAIL_ATTACHMENT_DOWNLOAD_URL_CREATED` 변경 이력으로 기록한다.

첨부 원본 저장 정책:

- Gmail OAuth 수집은 첨부 파일명을 주문 원문에 후보로 남기고, 첨부 원본은 객체 저장소에 보관한다.
- IMAP 수집은 일반적인 `multipart/*`와 base64 첨부 원본을 객체 저장소에 보관하고, 파일명은 주문 원문에 후보로 남긴다.
- IMAP 첨부 저장 실패는 메일 수집과 주문 원문 저장을 롤백하지 않고 `SM-EML-7204` 오류 로그로 기록한다.
- 첨부 원본 기본 보관 기간은 `SHIPMATE_EMAIL_ATTACHMENT_RETENTION_DAYS`로 설정하고 기본값은 365일이다.
- 첨부 원본은 개인정보와 거래 문서가 포함될 수 있으므로 `SHIPMATE_EMAIL_ATTACHMENT_RETENTION_DAYS`가 지난 원본을 매일 새벽 정리한다.
- 운영자는 `/api/email-orders/attachments/cleanup-expired`를 호출해 만료 첨부파일 정리를 수동 실행할 수 있다. 객체 삭제에 실패한 메타데이터는 삭제하지 않아 다음 실행에서 재시도할 수 있게 한다.

OAuth 적용 기준:

- Gmail/Google Workspace 운영 계정이 늘어나면 OAuth를 우선 검토한다.
- 네이버 등 국내 메일은 제공자별 OAuth/앱 비밀번호 지원 범위를 확인한 뒤 계정 연결 방식을 분기한다.
- OAuth 토큰은 `GOOGLE_OAUTH_TOKEN_ENCRYPTION_SECRET` 기반 AES-GCM 암호화 값으로 DB에 저장한다.
- 신규 OAuth 연결은 Gmail 읽기와 읽음 처리를 위해 `gmail.modify` scope를 요청한다.
- 현재 OAuth 시작/콜백 API는 `/api/oauth/google/authorize-url`, `/api/oauth/google/callback`을 사용한다.
- OAuth 연결 상태 API는 `/api/oauth/google/status`를 사용하며 토큰 원문은 반환하지 않는다.
- 수동 수집 검증은 `/api/oauth/google/collect-gmail`로 실행한다.
- 이미 수집된 Gmail을 읽음 처리해야 할 때는 `/api/oauth/google/mark-latest-collected-read?limit=2`로 최신 수집 이력 기준 읽음 처리를 실행한다.
- 소규모 운영에서는 Gmail watch/Pub/Sub보다 주기적 polling을 먼저 사용한다.

### Gmail polling 실패 조치 체크리스트

| 실패 코드/상황 | 운영 조치 |
|---|---|
| `SM-GML-9102` | Google Gmail 연결 토큰이 없으므로 Merchant 설정 화면에서 Gmail 연결을 다시 완료한다. |
| `SM-GML-9103` 또는 refresh token 없음 | OAuth 동의 화면 테스트 사용자, 승인된 redirect URI, `access_type=offline` 흐름을 확인한 뒤 Gmail 연결을 다시 수행한다. |
| `SM-GML-9104`, `SM-GML-9105` | Google API 응답, OAuth client ID/secret, 네트워크, Google API 활성화 상태를 확인한다. |
| `SM-GML-9106` | 메일 원문은 보존되므로 주문 원문 화면에서 원문을 열고 초안 생성 로그와 AI 추출 결과를 확인한다. |
| `SM-GML-9107` | MinIO/S3 객체 저장소 상태, 첨부 용량, 객체 키 권한을 확인한 뒤 수동 수집을 재실행한다. |
| `SM-GML-9108` | OAuth scope가 `gmail.modify`인지 확인하고 Gmail 연결을 다시 승인한다. 기존 `gmail.readonly` 토큰은 읽음 처리를 할 수 없다. |
| 최근 원문 ID 있음 | 내부관리자 Gmail 수집 이력 상세의 원문 보기 또는 주문 원문 검색으로 이동해 실제 저장 상태를 확인한다. |
| 반복 실패 | 내부관리자 오류 로그에서 같은 코드/요청 context를 검색하고 담당자, 처리 메모, 조치 완료 상태를 기록한다. |
- 공인 DNS 등록 전 외부 HTTPS OAuth 검증이 필요하면 Cloudflare Tunnel 또는 ngrok 임시 URL을 사용한다.
- 임시 URL 사용 시 Google Console 승인된 redirect URI에 `https://{temporary-host}/api/oauth/google/callback`을 추가하고 `STAGING_GOOGLE_OAUTH_REDIRECT_URI`를 같은 값으로 설정한다.

## 단계별 진행

```text
1단계: 이용자 전용 이메일 주소 발급
2단계: shipmate-api 안에 이메일 수신과 주문 초안 생성 기능 추가
3단계: shipmate-merchant에서 이메일 주문 초안 검수 화면 제공
4단계: 이용자별 이메일 별칭과 브랜드 표시 강화
5단계: 이용자 기존 이메일 계정 연동 제공
6단계: 이용자 자체 도메인 이메일 연결 제공
7단계: 처리량 증가 시 shipmate-email 또는 worker로 분리
```

## 결론

이메일 발주 MVP는 이용자 전용 이메일 발급 방식이 가장 적합하다.

고객은 이메일 발송이라는 익숙한 행동을 유지하고, 출고비서는 수신 주소를 기준으로 이용자와 발주 출처를 안정적으로 식별한다. 이후 기존 이메일 계정 연동, 자체 도메인 연결, 웹 주문 채널과 통합하는 방향으로 확장한다.
