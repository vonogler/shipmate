# ShipMate Merchant

출고비서 이용자가 주문 원문을 접수하고 출고표 초안을 검수하는 가맹점 화면입니다.

## 현재 범위

첫 화면은 포장재 주문제작 흐름을 기준으로 주문 원문과 출고표 초안을 다룹니다.

```text
카카오톡/문자/이메일 원문 -> 주문 원문 저장 -> 출고표 초안 생성 -> 이용자 검수
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
http://localhost:3001
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

- 주문 원문 접수
- 주문 원문 목록
- 선택 원문 상세
- 출고표 초안 생성
- 출고표 초안 목록
- 품목 후보, 수량, 규격, 단가 후보, 납품 메모, 택배 메모 검수 저장
- 검수된 출고표 초안 제출
- 제출된 출고표 목록과 상세 조회
- 배송지, 수취인, 연락처, 배송 요청사항 검수 저장
- 포장재 주문제작 필드 검수 저장: 가로, 세로, 높이, 재질, 인쇄 여부, 디자인 파일 여부, 제작 수량, 납기 희망일, 샘플 요청 여부
- 판촉물/맞춤 제작 필드 검수 저장: 제품 종류, 각인/인쇄 방식, 색상, 옵션, 시안 확인 여부, 제작 확정 여부
- 주문 원문에서 규격과 수량을 품목 후보로 자동 추출하는 기본 규칙
- 제출된 출고표 택배 CSV 다운로드
- 기본, CJ대한통운, 롯데택배, 한진택배 CSV 시작 템플릿 선택
- 고객 목록, 고객별 기본 배송지, 자주 쓰는 품목, 고객별 단가표 저장
- 초안 품목 후보 저장 시 고객별 단가표 기준 단가 후보 자동 보강
- 제출된 출고표 기준 출고표, 작업지시서, 납품서 인쇄 화면
- AI 주문 후보 추출 버튼
- 제출된 출고표 일괄 택배 CSV 다운로드
- 변경 이력, 오류 로그, 백업 미리보기 운영 패널

호출 API:

```http
POST /api/inbound-messages
GET  /api/inbound-messages
POST /api/order-drafts/from-inbound-message
GET  /api/order-drafts
PUT  /api/order-drafts/{orderDraftId}/review
POST /api/order-drafts/{orderDraftId}/submit
GET  /api/shipping-orders
GET  /api/shipping-orders/{orderDraftId}
GET  /api/shipping-orders/{orderDraftId}/courier-csv?courierCsvTemplate=GENERIC
POST /api/shipping-orders/courier-csv
POST /api/customers
GET  /api/customers
GET  /api/customers/{customerId}
POST /api/order-drafts/{orderDraftId}/ai-extract
GET  /api/operation/audit-logs
GET  /api/operation/error-logs
GET  /api/operation/backups/export
```
