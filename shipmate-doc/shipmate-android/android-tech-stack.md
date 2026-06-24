# ShipMate Android 기술 스택

## 문서 정보

| 항목 | 값 |
|---|---|
| 서비스 | ShipMate AI |
| 대상 프로젝트 | `shipmate-android` |
| 결정일 | 2026-05-24 |
| 문서 위치 규칙 | Android 문서는 `shipmate-doc/shipmate-android/` 아래에 둔다. |

## 범위

현재 우선순위는 **통화녹음 업로드 기능**이다.

보류 범위:

- 출고표 화면
- 전체 OMS/ERP workflow
- 자동 통화녹음 감지

## Android 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Architecture | MVVM |
| 상태 관리 | ViewModel + StateFlow |
| 비동기 처리 | Kotlin Coroutines |
| Background jobs | WorkManager |
| Network | Retrofit + OkHttp |
| Local settings | Jetpack DataStore |
| Local database | Room |
| Dependency injection | Hilt |
| Navigation | Navigation Compose |
| Logging | Timber |
| Build system | Gradle Kotlin DSL |

## SDK 정책

| 항목 | 값 |
|---|---:|
| minSdk | 26 |
| targetSdk | 36 |
| compileSdk | 36 |

선정 이유:

- Android 8.0 이상이면 소상공인 사용자를 위한 실사용 단말 범위를 충분히 넓게 유지할 수 있다.
- API 36은 현재 앱에서 선택한 Android SDK target이다.
- 로컬 SDK 사용 가능 여부를 확인한 뒤 compile/target SDK를 상향할 수 있다.

## 업로드 전략

기본 전략은 **MinIO Presigned URL 직접 업로드**이다.

처리 흐름:

1. Android가 API 서버에 업로드 세션 생성을 요청한다.
2. API 서버가 사용자 인증, 업로드 동의, 파일 크기, content type을 검증한다.
3. API 서버가 metadata를 생성하고 MinIO presigned PUT URL을 반환한다.
4. Android가 음성 파일을 MinIO로 직접 업로드한다.
5. Android가 API 서버의 업로드 완료 endpoint를 호출한다.
6. API 서버가 MinIO object를 검증하고 분석 job을 등록한다.

향후 확장:

- 대용량 파일용 S3-compatible multipart upload.
- 모바일 네트워크 불안정성 대응을 위한 WorkManager retry.

## 초기 화면

| 항목 | 값 |
|---|---|
| Route | `/call-upload` |
| Component | `CallUploadScreen` |

필수 요소:

- 업로드 및 AI 분석 동의 checkbox
- 오디오 파일 picker
- 업로드 button
- 업로드 progress
- 최근 업로드 목록
- 분석 상태 표시

## API 계약

| 기능 | Method | Path |
|---|---|---|
| 업로드 세션 생성 | POST | `/api/call-recordings/upload-sessions` |
| 업로드 완료 | POST | `/api/call-recordings/{recordingId}/complete` |
| 최근 업로드 목록 | GET | `/api/call-recordings/recent` |
