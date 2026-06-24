# ShipMate AI 문서

`shipmate-ai`는 API, Admin, Android와 분리된 AI 전용 프로젝트로 둔다.
이 문서는 `shipmate-ai`의 개발 구조, 데이터 흐름, 학습 데이터 축적 방식, 운영 정책을 정의한다.

## 목표 프로젝트 구조

```text
shipmate_workspace/
  shipmate-ai/
    asr-lab/
    order-review-ai/
    datasets/
    experiments/
    model-registry/
```

## 문서 목록

- [00. 개요](00-overview.md)
- [01. ASR Lab](01-asr-lab.md)
- [02. 출고표 검수 AI](02-order-review-ai.md)
- [03. 학습 데이터](03-training-data.md)
- [04. 개인정보와 보관 정책](04-privacy-retention.md)
- [05. 로드맵](05-roadmap.md)

## 프로젝트 경계

`shipmate-api`는 업로드 API, 업무 데이터 API, 운영 상태 업데이트를 담당한다.
`shipmate-ai`는 AI worker, 실험, 데이터셋, 모델 평가, 모델 산출물을 담당한다.
`shipmate-admin`은 사람 검수 화면과 최종 승인 흐름을 담당한다.
`shipmate-doc`은 아키텍처와 정책 문서를 담당한다.
