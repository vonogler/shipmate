# 01. ASR Lab

## 목적

`asr-lab`는 통화녹음의 STT 처리와 ASR 모델 개선을 담당한다.

초기 운영 STT는 Clova Speech를 기준 provider로 사용한다.
사람이 검수한 전사문을 저장해 두면, 나중에 ShipMate 업무 도메인에 특화된 ASR 모델을 파인튜닝하거나 평가할 수 있다.

## 담당 범위

- 업로드된 음성을 Clova Speech에 전달한다.
- 원본 전사 결과를 저장한다.
- STT provider, 상태, confidence, 오류 metadata를 기록한다.
- Admin 검수 화면에 전사 데이터를 제공한다.
- ASR 학습/평가 데이터셋을 생성한다.
- 향후 자체 ASR 모델을 Clova Speech와 비교 평가한다.

## 제외 범위

- 출고표 초안 생성.
- 고객/상품 마스터 데이터 소유.
- 최종 출고 확정.

## ASR 데이터 흐름

```text
call_recording.status = UPLOADED
-> asr_worker가 작업 선택
-> MinIO에서 음성 다운로드 또는 스트리밍
-> Clova Speech 호출
-> transcript_raw 저장
-> status = TRANSCRIBED 로 변경
-> Admin에서 전사문 검수
-> transcript_corrected 저장
-> 학습 가능 sample로 표시
```

## 데이터셋 예시

```json
{
  "recordingId": "rec_...",
  "audioObjectKey": "call-recordings/tenant/user/file.m4a",
  "provider": "clova",
  "transcriptRaw": "Clova Speech 원본 전사문",
  "transcriptCorrected": "사람이 수정한 전사문",
  "reviewedBy": "검수자 id",
  "reviewedAt": "2026-06-03T00:00:00Z",
  "trainingConsent": true,
  "piiMasked": false
}
```

## 평가 지표

- `CER`: 문자 오류율.
- `WER`: 단어 오류율.
- 도메인 용어 정확도: 고객명, 상품명, 수량 표현, 주소.
- 후속 출고표 필드 정확도.

## 학습 전략

처음부터 학습하지 않는다. 먼저 검수된 전사문 데이터를 축적한다.

권장 기준:

- 10~50시간: 실험과 기준 성능 비교.
- 100~500시간: 도메인 파인튜닝 검토 가능.
- 500시간 이상: Clova Speech 대체 또는 보완 가능성 평가.
