# 문자 공유 기반 출고표 생성 구조

## 목적

이용자가 자신의 휴대폰 문자로 받은 발주 내용을 출고비서에 전달하여 주문 초안과 출고표로 이어지게 한다.

문자는 이용자 개인 휴대폰 안에 도착하는 경우가 많다. 따라서 MVP에서는 Android 앱이 문자 입력 창구가 되고, API 서버가 주문 초안 생성과 출고표 생성의 기준 시스템이 된다.

## 결론

문자 기반 출고표 생성은 `shipmate-android`와 `shipmate-api`가 모두 필요하다.

```text
shipmate-android: 이용자 휴대폰 안의 문자 내용을 출고비서로 전달하는 입력 창구
shipmate-api: 전달된 문자를 저장, 분석, 검수 상태로 관리하고 출고표로 연결하는 업무 서버
shipmate-merchant: 이용자가 주문 초안을 검수하고 출고표 생성을 승인하는 화면
```

Android만으로는 문자 내용을 보여주거나 임시 입력하는 수준까지는 가능하다. 하지만 출고표 생성, 이력 관리, 여러 기기 공유, 고객/가맹점 화면 연동까지 고려하면 API가 반드시 필요하다.

## 권장 MVP 흐름

```text
이용자 문자 앱
-> 문자 앱에서 출고비서로 공유
-> shipmate-android가 공유 텍스트 수신
-> 이용자가 내용 미리보기와 거래처 후보 확인
-> shipmate-api로 문자 원문 업로드
-> API가 주문 초안 생성
-> shipmate-merchant에서 검수/수정/승인
-> 출고표 생성
```

이 방식은 Android SMS 권한을 직접 요구하지 않고도 시작할 수 있다. 이용자가 문자 앱에서 직접 공유하기 때문에 정책 리스크와 개인정보 수집 부담을 줄일 수 있다.

## 프로젝트별 역할

### shipmate-android

- Android 공유 시트에서 문자 텍스트를 수신한다.
- 수신한 문자 원문을 이용자에게 미리 보여준다.
- 거래처 후보, 주문일, 메모 등 최소 정보를 함께 확인하게 한다.
- API 업로드 성공 여부를 이용자에게 표시한다.
- 자동 문자 읽기보다 수동 공유를 MVP 기본 방식으로 둔다.

### shipmate-api

- 공유된 문자 원문을 `InboundMessage`로 저장한다.
- 문자 출처를 `SMS_SHARE`로 구분한다.
- 원문에서 품목, 수량, 요청일, 거래처 후보를 추출한다.
- 추출 결과를 `OrderDraft` 주문 초안으로 만든다.
- 검수 상태와 출고표 생성 이력을 관리한다.
- 추후 이메일, 카카오톡, 웹 주문과 같은 다른 입력 채널도 같은 주문 초안 흐름으로 합류시킨다.

### shipmate-merchant

- 이용자가 문자 기반 주문 초안을 목록으로 확인한다.
- 추출이 애매한 항목을 수정하거나 거래처를 지정한다.
- 승인하면 출고표를 생성한다.
- 원문 문자와 추출 결과를 함께 보여주어 검수 근거를 남긴다.

## 권장 도메인 모델

```text
message
  InboundMessage
  InboundMessageSource = SMS_SHARE | EMAIL | WEB_ORDER | KAKAO
  InboundMessageService

order
  OrderDraft
  OrderDraftExtractionService
  OrderReviewService
```

상태는 다음처럼 시작한다.

```text
RECEIVED
-> EXTRACTED
-> NEEDS_REVIEW
-> APPROVED
-> ORDER_CREATED
```

처리하지 않을 문자는 `REJECTED`로 종료한다.

## API 예시

```text
POST /api/merchant/inbound-messages
source: SMS_SHARE
rawText: 문자 원문
receivedAt: 이용자 휴대폰 수신 시각 또는 공유 시각
merchantId: 출고비서 이용자 식별자
```

응답은 생성된 주문 초안 식별자를 돌려준다.

```text
inboundMessageId
orderDraftId
status
```

## 확장 방향

### 자동 문자 읽기

Android에서 SMS를 자동으로 읽는 방식은 정책과 개인정보 리스크가 크다. 기본 문자 앱 권한, SMS 권한 심사, Google Play 정책 확인이 필요하므로 MVP 기본 구조로 두지 않는다.

### 알림 접근 기반 수집

문자 알림을 읽어 주문을 자동 수집하는 방식도 가능하지만 개인정보 노출 범위가 넓고 사용자가 불안하게 느낄 수 있다. 운영 정책과 권한 안내가 충분히 준비된 뒤 검토한다.

### 공통 입력 파이프라인

문자 공유로 만든 구조는 이메일 발주, 웹 주문, 카카오톡 주문에도 재사용한다.

```text
입력 채널
-> InboundMessage
-> OrderDraft
-> 검수
-> 출고표
```

## 결론

문자 발주를 출고표로 연결하려면 Android와 API를 분리해서 설계하는 것이 맞다.

Android는 이용자 휴대폰에서 문자 원문을 안전하게 전달하는 역할에 집중하고, API는 주문 초안 생성과 출고표 생성의 기준 데이터를 관리한다. 이용자는 `shipmate-merchant`에서 초안을 검수하고 승인하는 흐름으로 사용한다.
