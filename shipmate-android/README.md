# ShipMate Android

ShipMate AI Android MVP입니다.

## 현재 범위

- 카카오톡과 문자 앱에서 공유한 주문 원문을 확인 후 API로 전송합니다.
- 기존 통화 녹음 업로드 화면은 유지하지만 신규 개발 우선순위에서는 제외합니다.
- 출고표 검수 화면은 가맹점 웹 화면에서 먼저 다룹니다.

## 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- ViewModel + StateFlow
- Coroutines
- Retrofit + OkHttp
- Gradle Kotlin DSL
- compileSdk 36
- targetSdk 36
- minSdk 26

## 첫 화면

`CallUploadScreen`에 포함된 기능:

- 업로드 및 AI 분석 동의 체크박스
- 오디오 파일 선택기
- 업로드 버튼
- 업로드 진행률
- 최근 업로드 목록
- 분석 상태 표시

Android 공유 intent로 텍스트를 받으면 주문 원문 확인 dialog가 열립니다.

- 공유 원문 수정
- 출고비서 API 전송
- 전송 실패 시 앱 내부 임시 저장 후 재시도

## API 흐름

```text
카카오톡/문자 -> Android 공유: 주문 원문 텍스트 전달
Android -> API 서버: 주문 원문 생성

Android -> API 서버: 업로드 세션 생성
Android -> MinIO: presigned URL로 오디오 파일 PUT
Android -> API 서버: 업로드 완료 처리
API 서버 -> 분석 작업 큐
```

현재 저장소 구현은 공유 주문 원문 전송과 기존 업로드 세션 생성, MinIO presigned PUT 업로드, 업로드 완료 처리, 최근 업로드 새로고침을 `shipmate-api`와 연결합니다.

## 로컬 API URL

기본 API URL:

```text
http://10.0.2.2:8080/
```

이 기본값은 Android 에뮬레이터 테스트용입니다.

같은 Wi-Fi의 실제 휴대폰에서 테스트할 때는 PC Wi-Fi IP로 빌드합니다.

```powershell
.\gradlew.bat assembleDebug -PSHIPMATE_API_BASE_URL=http://192.168.0.25:8080/
```

환경 변수로도 설정할 수 있습니다.

```powershell
$env:SHIPMATE_API_BASE_URL='http://192.168.0.25:8080/'
.\gradlew.bat assembleDebug
```
