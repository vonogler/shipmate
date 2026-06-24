# 02. 출고표 검수 AI

## 목적

`order-review-ai`는 전사문과 업무 context를 출고표 초안 JSON으로 변환한다.

이 기능은 ASR이 아니다. LLM/NLP 기반 정보 추출 흐름이다.

## 담당 범위

- 전사문, 고객 후보, 상품 후보, 단가 데이터, 배송 context로 prompt 입력을 구성한다.
- 정해진 schema의 출고표 초안 JSON을 생성한다.
- 생성된 JSON을 서버 schema로 검증한다.
- AI 초안, 사람 수정본, 최종 승인 JSON을 기록한다.
- LLM 파인튜닝/평가 데이터셋을 생성한다.

## 제외 범위

- 음성 전사.
- 고객/상품 마스터 데이터 소유.
- 검수 또는 서버 검증 없는 직접 출고 확정.

## 입력 Context

```json
{
  "recordingId": "rec_...",
  "transcript": "수정 전사문 또는 원본 전사문",
  "customerCandidates": [],
  "productCandidates": [],
  "knownDeliveryAddresses": [],
  "pricePolicies": [],
  "today": "2026-06-03"
}
```

## 출력 Schema

```json
{
  "customerId": "C001",
  "customerConfidence": 0.92,
  "items": [
    {
      "productId": "P001",
      "productName": "샘플 상품",
      "quantity": 3,
      "unit": "box",
      "confidence": 0.88
    }
  ],
  "deliveryDate": "2026-06-04",
  "deliveryTimeWindow": "AM",
  "deliveryAddressId": "ADDR001",
  "memo": "고객 요청사항",
  "missingFields": [],
  "warnings": []
}
```

## 서버 검증

LLM 출력은 출고표 초안이 되기 전에 반드시 검증해야 한다.

검증 항목:

- 고객 ID가 존재하는지 확인한다.
- 상품 ID가 존재하는지 확인한다.
- 수량과 단위가 유효한지 확인한다.
- 단가 정책을 적용할 수 있는지 확인한다.
- 배송지가 기존 주소인지 신규 주소인지 확인한다.
- 누락 필드 또는 낮은 confidence 필드를 검수자에게 표시한다.

## 학습 전략

처음에는 외부 LLM과 엄격한 schema 검증을 사용한다.
사람이 수정한 검수 결과를 모두 저장한다.
검수 예시가 충분히 쌓이면 나중에 파인튜닝을 검토한다.

권장 기준:

- 검수된 초안 1,000건: prompt와 검증 로직 개선.
- 검수된 초안 5,000~10,000건: 파인튜닝 테스트.
- 검수된 초안 10,000건 이상: 로컬 모델 또는 private deployment 평가.
