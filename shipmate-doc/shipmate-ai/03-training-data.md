# 03. 학습 데이터

## 목적

검수 데이터는 핵심 자산이다. 당장 모델을 학습하지 않더라도 ShipMate는 처음부터 검수 데이터를 학습 가능한 형태로 수집해야 한다.

## 데이터 분류

| 분류 | 사용처 | 설명 |
|---|---|---|
| 원본 음성 | ASR | 업로드된 통화녹음 |
| 원본 전사문 | ASR, LLM | Clova Speech 결과 |
| 수정 전사문 | ASR | 사람이 수정한 전사문 |
| AI 초안 JSON | LLM | 최초 출고표 초안 |
| 수정 초안 JSON | LLM | 사람이 수정한 출고표 초안 |
| 최종 출고표 JSON | LLM, 업무 분석 | 확정된 출고 데이터 |
| 검수 metadata | 평가 | 검수자, 검수 시각, 수정 사유 |

## 필요한 Record

```text
call_recording
transcript
shipment_draft
shipment_draft_item
review_result
training_sample
model_evaluation
```

## 학습 Sample 규칙

- AI 입력과 사람이 최종 승인한 출력을 함께 저장한다.
- 추론 시 사용한 후보 목록을 저장한다.
- 가능하면 수정 전후 차이를 저장한다.
- 동의 상태와 보관 만료일을 저장한다.
- 검수되지 않은 AI 출력은 학습 가능 상태로 표시하지 않는다.

## ASR 학습 Pair

```text
입력: 음성 파일
정답: 사람이 수정한 전사문
```

## LLM 학습 Pair

```text
입력: 전사문 + 고객 후보 + 상품 후보 + 업무 context
정답: 사람이 승인한 출고표 초안 JSON
```

## 데이터셋 Versioning

모든 학습용 export에는 데이터셋 version이 있어야 한다.

```text
datasetVersion: asr-2026-06-01
datasetVersion: order-review-2026-06-01
```

과거 학습 데이터셋을 덮어쓰지 않는다. 재현 가능한 snapshot을 유지한다.
