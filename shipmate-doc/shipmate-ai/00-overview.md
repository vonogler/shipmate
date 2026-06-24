# 00. 개요

## 목표

ShipMate ShipMate AI는 두 가지 AI 기능을 독립적으로 개발한다.

1. `asr-lab`: 통화녹음 음성 인식과 전사문 품질 개선.
2. `order-review-ai`: 전사문 기반 출고표 초안 생성과 검수 데이터 학습.

초기 제품 목표는 고객 통화로부터 출고표 초안을 생성하면서, 나중에 ASR 모델과 출고정보 추출 모델을 개선할 수 있는 검수 데이터를 축적하는 것이다.

## 전체 흐름

```text
Android 통화녹음 업로드
-> MinIO 음성 객체 저장
-> shipmate-api 업로드 완료 처리
-> ASR worker
-> Clova Speech 전사문 생성
-> 전사문 검수
-> 출고정보 추출 worker
-> 출고표 초안 JSON 생성
-> Admin 사람 검수
-> 출고표 확정
-> 학습 데이터 저장
-> 주기적 모델 평가와 파인튜닝
```

## 권장 워크스페이스 구조

```text
shipmate-ai/
  asr-lab/
    workers/
    datasets/
    evaluation/
    notebooks/
  order-review-ai/
    prompts/
    workers/
    schemas/
    evaluation/
  datasets/
    raw/
    reviewed/
    training/
    evaluation/
  experiments/
    asr/
    order-review/
  model-registry/
    asr/
    order-review/
```

## 연동 원칙

- `shipmate-ai`는 고객 마스터 데이터를 직접 소유하지 않는다.
- `shipmate-ai`는 필요한 업무 데이터를 `shipmate-api` 또는 승인된 export를 통해 읽는다.
- AI 출력은 사람 검수 또는 서버 검증 전까지 항상 초안이다.
- 사람이 검수한 결과를 향후 학습의 기준 데이터로 사용한다.
- 학습은 통화마다 실시간으로 수행하지 않고, 주기적으로 수행한다.

## AI 기능 분리

| 기능 | 입력 | 출력 | 모델 유형 |
|---|---|---|---|
| ASR/STT | 음성 파일 | 전사문 | 음성 인식 |
| 전사문 검수 | 전사문 + 음성 | 수정 전사문 | 사람 검수 흐름 |
| 출고정보 추출 | 전사문 + 고객/상품 데이터 | 출고표 초안 JSON | LLM/NLP |
| 출고표 검수 | AI 초안 + 업무 데이터 | 확정 출고표 JSON | 사람 검수 흐름 |
