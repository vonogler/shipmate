# 공통 필드명

## 문서 정보

| 항목 | 값 |
|---|---|
| 저장소 | `shipmate-common` |
| 담당 | common |
| 갱신일 | 2026-06-12 |
| 기준 형식 | Markdown 원본과 HTML 보기 파일 |

## 관리 원칙

공유 필드명은 문서 자산으로 관리한다.

| 대상 | 명명 규칙 |
|---|---|
| API JSON | `camelCase` |
| Database | `snake_case` |
| Kotlin | `camelCase` |
| 화면 표시명 | locale별 문구에서 별도 정의 |

## 통화 녹음 필드

| API JSON | Database | Kotlin | Type | 필수 | 설명 |
|---|---|---|---|---|---|
| `recordingId` | `recording_id` | `recordingId` | string | 예 | 통화 녹음 고유 식별자 |
| `fileName` | `file_name` | `fileName` | string | 예 | 원본 또는 정규화된 오디오 파일명 |
| `contentType` | `content_type` | `contentType` | string | 예 | 오디오 MIME type |
| `fileSize` | `file_size` | `fileSize` | long | 예 | 오디오 파일 크기(byte). Android content provider가 크기를 제공하지 못하면 `0`을 사용한다. |
| `uploadConsent` | `upload_consent` | `uploadConsent` | boolean | 예 | 사용자가 업로드와 AI 분석에 동의했는지 여부 |
| `source` | `source` | `source` | string | 예 | `android_manual` 같은 업로드 출처 코드 |
| `status` | `status` | `status` | string | 예 | 현재 업로드 또는 분석 상태 |
| `objectKey` | `object_key` | `objectKey` | string | 아니오 | MinIO object key |
| `uploadedAt` | `uploaded_at` | `uploadedAt` | datetime | 아니오 | 업로드 완료 시각 |
