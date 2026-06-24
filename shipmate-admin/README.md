# ShipMate 관리자

출고비서 운영자와 사원이 접속하는 내부관리자 플랫폼입니다.

## 현재 범위

내부 운영자는 전체 가맹점의 주문 처리 흐름과 운영 로그를 조회하고, 내부 직원 계정을 생성할 수 있습니다.

```text
shipmate-api 운영 데이터 -> shipmate-admin 내부관리자
```

## 기술 스택

- React
- TypeScript
- Vite
- Ant Design

## 로컬 실행

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

기본 URL:

```text
http://localhost:3000
```

기본 API URL:

```text
http://localhost:8080
```

API가 다른 장비나 IP에서 실행되면 `.env`를 수정합니다.

```text
VITE_SHIPMATE_API_BASE_URL=http://192.168.0.25:8080
```

## 현재 화면

- 내부관리자 로그인과 내부 계정 생성
- 운영 대시보드
- 이용자/가맹점 관리
- 내부 직원 계정 생성과 목록 조회
- 주문 원문 관리
- 출고표 초안 관리
- 제출 출고표 관리
- 통화 녹음 관리와 삭제
- 오류 로그
- 변경 이력
- 백업/복구 미리보기
- 시스템 설정 조회

호출 API:

```http
POST /api/auth/register
POST /api/auth/login
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
DELETE /api/call-recordings/{recordingId}
```

표시 항목:

- 가맹점 수, 직원 계정 수, 주문 원문 수, 초안 수, 제출 출고표 수, 통화 녹음 수, 오류 수
- 가맹점별 사용자/고객/주문/초안 집계와 최근 활동
- 직원 계정, 로그인 ID, 권한, 생성 시각
- 주문 원문, 출고표 초안, 제출 출고표의 상태와 원문
- 통화 녹음 파일, 상태, 객체 키, 오류 코드
- 오류 로그, 변경 이력, 백업 파일 미리보기, 시스템 설정
