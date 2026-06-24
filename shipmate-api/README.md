# ShipMate API

로컬 업로드 테스트를 위한 Java Spring Boot API 서버입니다.

## 현재 범위

이 서버는 통화 녹음 업로드 테스트 흐름과 주문 원문 저장 흐름을 제공합니다.

```text
Android -> shipmate-api -> MinIO -> shipmate-api -> shipmate-admin
카카오톡/문자/이메일 원문 -> shipmate-api -> 주문 원문 저장 -> 출고표 초안 생성
```

현재 업무 데이터 구현은 로컬 JSON 저장소를 함께 사용합니다. Docker profile에서는 MySQL 8.4에 연결하고, Flyway로 공통 마스터 코드 테이블 `tb_code`를 먼저 생성합니다.

## Java / 빌드

현재 로컬 장비에는 Java 25가 설치되어 있어 프로젝트도 Java 25를 대상으로 합니다.

프로젝트는 Gradle Kotlin DSL을 사용합니다.

## 로컬 환경

먼저 `shipmate-infra`에서 MinIO를 실행합니다.

환경 예시:

```text
.env.example
```

같은 Wi-Fi의 휴대폰에서 테스트할 때는 `localhost`를 PC Wi-Fi IP로 바꿉니다.

```powershell
$env:SHIPMATE_MINIO_ENDPOINT='http://192.168.0.25:9000'
$env:SHIPMATE_MINIO_PUBLIC_ENDPOINT='http://192.168.0.25:9000'
gradle bootRun
```

Android 에뮬레이터 테스트에서는 API 서버 내부 MinIO 엔드포인트는 호스트 기준으로 두고, presigned URL은 에뮬레이터 호스트 별칭으로 노출합니다.

```powershell
$env:SHIPMATE_MINIO_ENDPOINT='http://localhost:9000'
$env:SHIPMATE_MINIO_PUBLIC_ENDPOINT='http://10.0.2.2:9000'
gradle bootRun
```

`SHIPMATE_MINIO_ENDPOINT`는 `shipmate-api` 자체가 사용합니다. `SHIPMATE_MINIO_PUBLIC_ENDPOINT`는 Android용 presigned URL 생성에 사용합니다.

## Docker MySQL 환경

`shipmate-infra/docker-compose.local.yml`로 실행하면 `shipmate-api`는 Docker profile로 기동되고 MySQL 스키마 `shipmate`에 연결합니다.

개발 기본값:

```text
database: shipmate
username: shipmate
password: shipmate
```

DB 테이블명은 모든 업무 테이블에 `tb_` prefix를 붙입니다. 공통 마스터 코드 테이블은 `tb_code` 단일 테이블을 사용합니다.

## API

```http
POST /api/call-recordings/upload-sessions
POST /api/call-recordings/{recordingId}/complete
GET  /api/call-recordings/recent
POST /api/inbound-messages
GET  /api/inbound-messages
GET  /api/inbound-messages/{inboundMessageId}
POST /api/order-drafts/from-inbound-message
GET  /api/order-drafts
GET  /api/order-drafts/{orderDraftId}
PUT  /api/order-drafts/{orderDraftId}/review
POST /api/order-drafts/{orderDraftId}/submit
DELETE /api/order-drafts/{orderDraftId}
GET  /api/shipping-orders
GET  /api/shipping-orders/{orderDraftId}
GET  /api/shipping-orders/{orderDraftId}/courier-csv?courierCsvTemplate=GENERIC
POST /api/shipping-orders/courier-csv
POST /api/shipping-orders/courier-csv/validate
POST /api/customers
GET  /api/customers
GET  /api/customers/{customerId}
DELETE /api/customers/{customerId}
POST /api/email-orders
POST /api/email-orders/mailbox-config
POST /api/email-orders/collect-mailbox
POST /api/order-drafts/{orderDraftId}/ai-extract
POST /api/auth/register
POST /api/auth/login
GET  /api/operation/dashboard
GET  /api/operation/settings?merchantId={merchantId}
POST /api/operation/settings
GET  /api/operation/audit-logs
GET  /api/operation/error-logs
GET  /api/operation/backups/export
POST /api/operation/backups/restore-stage
POST /api/operation/backups/restore-approve
GET  /api/admin/overview
GET  /api/admin/merchants
GET  /api/admin/users
POST /api/admin/users
GET  /api/admin/inbound-messages
GET  /api/admin/order-drafts
GET  /api/admin/shipping-orders
GET  /api/admin/call-recordings
GET  /api/admin/audit-logs
GET  /api/admin/error-logs
GET  /api/admin/settings
GET  /api/admin/backup
GET  /api/health
```

`/api/auth/register`, `/api/auth/login`, `POST /api/inbound-messages`, `/api/health`를 제외한 주요 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

요청 샘플 위치:

```text
requests/call-recordings.local.http
requests/inbound-orders.local.http
```

## MinIO 버킷

```text
shipmate-call-recordings
```
