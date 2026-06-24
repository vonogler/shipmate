# ShipMate 문서

ShipMate AI 관련 문서를 보관합니다.

## 문서 정책

- 설명형 개발 문서의 원본은 Markdown 파일입니다.
- 브라우저에서 볼 문서는 HTML 파일로 관리합니다.
- 문서 산출물은 Markdown과 HTML 두 종류만 사용합니다.
- Markdown과 HTML 내용이 다르면 Markdown을 기준으로 합니다.
- HTML 링크는 로컬 파일과 GitHub Pages에서 모두 동작하도록 상대 경로를 사용합니다.
- 새 문서를 YAML 파일로 생성하지 않습니다.

## 도메인 폴더 규칙

`shipmate-doc` 아래 프로젝트별 문서 폴더명은 실제 프로젝트 디렉터리명과 동일하게 둡니다.

```text
shipmate-doc/shipmate-admin/
shipmate-doc/shipmate-android/
shipmate-doc/shipmate-api/
shipmate-doc/shipmate-common/
shipmate-doc/shipmate-infra/
shipmate-doc/shipmate-merchant/
shipmate-doc/shipmate-ai/
```

`shipmate-doc/html/`은 전체 문서 포털 진입점이므로 프로젝트 폴더명 규칙의 예외입니다.

`shipmate-android` 문서는 다음 위치에 둡니다.

```text
shipmate-doc/shipmate-android/
  html/
```

`shipmate-admin` 관리자 문서는 다음 위치에 둡니다.

```text
shipmate-doc/shipmate-admin/
  html/
```

`shipmate-merchant` 가맹점/이용자 플랫폼 문서는 다음 위치에 둡니다.

```text
shipmate-doc/shipmate-merchant/
  html/
```

공통 문서는 다음 위치에 둡니다.

```text
shipmate-doc/shipmate-common/
  html/
```

ShipMate AI 문서는 다음 위치에 둡니다.

```text
shipmate-doc/shipmate-ai/
```

ASR/STT, 의사문 검토, 출고정보 추출, 학습 데이터, 모델 평가, 개인정보 보관 전략처럼 AI 전용 프로젝트 설계 문서는 이 폴더에 작성합니다.

## 공통 데이터 자산

공통 데이터 자산은 Markdown 원본과 HTML 보기 파일로 관리합니다.

| 자산 | Markdown 원본 | HTML 보기 파일 |
|---|---|---|
| 공통 필드명 | `shipmate-common/shared-field-names.md` | `shipmate-common/html/shared-field-names.html` |
| 공통 마스터 코드 | `shipmate-common/master-codes.md` | `shipmate-common/html/master-codes.html` |
| 공통 오류 코드 | `shipmate-common/error-codes.md` | `shipmate-common/html/error-codes.html` |

## 금지 규칙

Android 전용 문서를 `shipmate-doc/shipmate-admin/`에 두지 않습니다.
Admin 전용 문서를 `shipmate-doc/shipmate-android/`에 두지 않습니다.
가맹점/이용자 플랫폼 문서를 `shipmate-doc/shipmate-common/`이나 `shipmate-doc/shipmate-admin/`에 두지 않습니다.
공통 설계 문서를 `yaml/` 폴더에 두지 않습니다.
ShipMate AI 설계 문서를 `shipmate-doc/shipmate-android/`, `shipmate-doc/shipmate-admin/`, `shipmate-doc/shipmate-api/`에 두지 않습니다.
필드명, 마스터 코드, 오류 코드 번호 파일을 `shipmate-doc/data/`에 두지 않습니다.

## HTML 진입점

브라우저에서 다음 파일을 엽니다.

```text
html/index.html
```
