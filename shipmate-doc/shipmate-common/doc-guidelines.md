# ShipMate 문서 작성 가이드라인

## 문서 정보

| 항목 | 값 |
|---|---|
| 서비스 | ShipMate AI |
| 대상 프로젝트 | `shipmate-doc` |
| Root path | `C:/Users/vonog/OneDrive/dev/workspace_list/shipmate_workspace/shipmate-doc` |
| 갱신일 | 2026-06-03 |
| 공통 문서 위치 | 공통 설계 문서는 `shipmate-doc/shipmate-common/` 아래에 둔다. |
| 문서 산출물 형식 | Markdown 원본과 HTML 보기 파일만 사용한다. |

## 문서 원칙

| 문서 유형 | 기준 형식 |
|---|---|
| 설명형 개발 문서 | Markdown |
| 브라우저 보기 문서 | HTML |
| API 설명 문서 | Markdown + HTML |
| 표준값/코드표 설명 문서 | Markdown + HTML |

작성 규칙:

- 모든 문서 원본은 `.md` 파일로 작성한다.
- 브라우저에서 볼 문서는 `.html` 파일로 생성하거나 유지한다.
- Markdown과 HTML 내용이 다르면 Markdown을 기준으로 한다.
- HTML은 Markdown 원본을 기준으로 갱신한다.
- `shipmate-doc` 안에서는 YAML 문서를 새로 만들지 않는다.
- YAML은 문서 산출물이 아니며, 문서 생성 대상에서도 제외한다.

## 도메인별 문서 폴더

### Common

| 항목 | 값 |
|---|---|
| Root | `common` |
| 규칙 | 모든 공통 설계 문서는 `shipmate-doc/shipmate-common/` 아래에 둔다. |
| Markdown 원본 | `common/` |
| HTML 보기 파일 | `shipmate-common/html/` |

예시:

- `common/doc-guidelines.md`
- `shipmate-common/html/doc-guidelines.html`
- `common/project-structure.md`
- `shipmate-common/html/project-structure.html`
- `common/product-overview.md`
- `shipmate-common/html/product-overview.html`

### Android

| 항목 | 값 |
|---|---|
| Root | `android` |
| 규칙 | 모든 Android 앱 문서는 `shipmate-doc/shipmate-android/` 아래에 둔다. |
| Markdown 원본 | `android/` |
| HTML 보기 파일 | `shipmate-android/html/` |
| 대상 repository | `shipmate-android` |

예시:

- `android/android-tech-stack.md`
- `shipmate-android/html/android-tech-stack.html`
- `android/call-upload-screen.md`
- `shipmate-android/html/call-upload-screen.html`

### Admin

| 항목 | 값 |
|---|---|
| Root | `admin` |
| 규칙 | 모든 `shipmate-admin` 문서는 `shipmate-doc/shipmate-admin/` 아래에 둔다. |
| Markdown 원본 | `admin/` |
| HTML 보기 파일 | `shipmate-admin/html/` |
| 대상 repository | `shipmate-admin` |

예시:

- `admin/admin-tech-stack.md`
- `shipmate-admin/html/admin-tech-stack.html`
- `admin/admin-menu-structure.md`
- `shipmate-admin/html/admin-menu-structure.html`

## Root 폴더

| Path | 목적 |
|---|---|
| `common` | 공통 설계 문서 |
| `android` | `shipmate-android` 문서 |
| `admin` | `shipmate-admin` 문서 |
| `api` | API 설명 문서 |
| `policies` | 동의, 개인정보, 통화녹음 보관, 약관 |
| `html` | 최상위 문서 포털 |
| `scripts` | Markdown에서 HTML을 생성하거나 검증하는 스크립트 |

## 금지 규칙

- 새 문서를 `.yaml` 또는 `.yml` 파일로 생성하지 않는다.
- 새 source 문서를 root `yaml/` 아래에 추가하지 않는다.
- 도메인 문서를 root `html/` 아래에 추가하지 않는다.
- root `html/`은 `index.html` 또는 가벼운 포털 페이지 용도로만 사용한다.
- 공통 설계 문서는 `common/`과 `shipmate-common/html/`에 둔다.
- Android 전용 문서는 `android/`와 `shipmate-android/html/`에 둔다.
- Admin 전용 문서는 `admin/`과 `shipmate-admin/html/`에 둔다.
- 필드명, master code, status code, error code number를 `shipmate-doc/data/`에 저장하지 않는다.

## 공통 데이터 자산

공통 데이터 자산은 Markdown 원본과 HTML 보기 파일로 관리한다.

기존 YAML 공통 데이터 자산은 제거하며, 새 기준 문서는 `shipmate-doc/shipmate-common` 아래에 둔다.

| 자산 | 위치 | 목적 |
|---|---|---|
| Field names | `shipmate-doc/shipmate-common/shared-field-names.md` / `shipmate-doc/shipmate-common/html/shared-field-names.html` | Android, Admin, API, 향후 service에서 공유하는 표준 field name |
| Master codes | `shipmate-doc/shipmate-common/master-codes.md` / `shipmate-doc/shipmate-common/html/master-codes.html` | RDB master code table과 seed data의 향후 기준 |
| Error codes | `shipmate-doc/shipmate-common/error-codes.md` / `shipmate-doc/shipmate-common/html/error-codes.html` | 표준 error message code와 code number |

## 필드와 DB 컬럼 생성 전 확인

- 새 API field, DTO field, Java/Kotlin/TypeScript field, 화면 데이터 field를 만들기 전에는 `shipmate-doc/shipmate-common/field-dictionary.md`를 먼저 확인한다.
- 새 field가 기존 사전에 없으면 구현 전 또는 구현과 동시에 필드 딕셔너리 Markdown/HTML에 의미, DB column 연결 여부, codeSet 여부를 기록한다.
- 새 DB table 또는 column을 만들기 전에는 `shipmate-doc/shipmate-common/database-column-dictionary.md`를 먼저 확인한다.
- 새 DB column이 기존 사전에 없으면 Flyway migration 작성 전 또는 작성과 동시에 DB 컬럼 딕셔너리 Markdown/HTML에 table, column, API field, type, nullable, index/FK, code_set 여부를 기록한다.
- API field와 DB column이 연결되는 업무 데이터는 필드 딕셔너리와 DB 컬럼 딕셔너리 양쪽에서 같은 table/column을 가리키도록 맞춘다.

## HTML 링크 가이드라인

Entry point는 `html/index.html`이다.

링크 규칙:

- HTML 파일 안에서는 상대 경로를 사용한다.
- `html/index.html`에서 Common HTML 문서는 `../shipmate-common/html/doc-guidelines.html` 형식으로 연결한다.
- `html/index.html`에서 Common Markdown 원본은 `../shipmate-common/doc-guidelines.md` 형식으로 연결한다.
- `html/index.html`에서 Android HTML 문서는 `../shipmate-android/html/android-tech-stack.html` 형식으로 연결한다.
- `html/index.html`에서 Android Markdown 원본은 `../shipmate-android/android-tech-stack.md` 형식으로 연결한다.
- `html/index.html`에서 Admin HTML 문서는 `../shipmate-admin/html/admin-tech-stack.html` 형식으로 연결한다.
- `html/index.html`에서 Admin Markdown 원본은 `../shipmate-admin/admin-tech-stack.md` 형식으로 연결한다.
- 모든 HTML 파일은 UTF-8 encoding을 사용한다.

## 권장 HTML 문서

| 파일 | 원본 | 목적 |
|---|---|---|
| `html/index.html` | - | 문서 포털 |
| `shipmate-common/html/doc-guidelines.html` | `common/doc-guidelines.md` | 공통 문서 작성 규칙 |
| `shipmate-android/html/android-tech-stack.html` | `android/android-tech-stack.md` | Android 앱 기술 스택 |
| `shipmate-admin/html/admin-tech-stack.html` | `admin/admin-tech-stack.md` | Admin 앱 기술 스택 |
