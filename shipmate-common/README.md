# ShipMate Common

ShipMate 프로젝트 전반에서 공유하는 공통 기준의 안내 파일입니다.

## 소유 범위

필드명, 마스터 코드, 상태 코드, 오류 메시지 코드 번호는 Markdown 원본과 HTML 보기 파일로 관리합니다.

```text
shipmate-doc/shipmate-common/
  shared-field-names.md
  master-codes.md
  error-codes.md
  html/
    shared-field-names.html
    master-codes.html
    error-codes.html
```

## 파일 역할

- `shipmate-doc/shipmate-common/shared-field-names.md`: Android, Admin, API, 이후 서비스가 공유하는 표준 필드명입니다.
- `shipmate-doc/shipmate-common/master-codes.md`: 통제된 업무 코드 목록입니다. 이후 RDB 마스터 코드 테이블과 seed data의 기준으로 사용합니다.
- `shipmate-doc/shipmate-common/error-codes.md`: 오류 코드 번호, 메시지, 클라이언트 표시 기준을 관리합니다.

공통 데이터 자산은 YAML 파일로 유지하지 않습니다.
