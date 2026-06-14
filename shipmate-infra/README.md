# ShipMate Infra

ShipMate 로컬 개발 인프라입니다.

## 서비스

- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`
- MySQL: `localhost:3306`
- shipmate-api: `http://localhost:8080`
- shipmate-merchant: `http://localhost:3001`

로컬 개발용 MySQL 계정, 비밀번호, 스키마명은 임시로 모두 `shipmate`를 사용합니다. 실제 운영에서는 반드시 다른 값으로 변경합니다.

같은 Wi-Fi의 휴대폰에서 테스트할 때는 `localhost` 대신 PC Wi-Fi IP를 사용합니다.

```text
MinIO API: http://{PC_WIFI_IP}:9000
shipmate-api: http://{PC_WIFI_IP}:8080
shipmate-admin: http://{PC_WIFI_IP}:3000
shipmate-merchant: http://{PC_WIFI_IP}:3001
```

## 시작

```powershell
Copy-Item .env.example .env
docker compose -f docker-compose.local.yml --env-file .env up -d
```

로컬 Compose project 이름은 `shipmate-local`로 고정한다. 같은 디렉터리에서 스테이징 Compose를 실행해도 로컬 서비스가 스테이징 서비스로 재생성되지 않도록 분리한다.

PowerShell 창에서 서버 로그를 계속 보려면 워크스페이스 루트의 `start_shipmate_local_stack_logs.bat`를 실행한다. 이 스크립트는 Docker Compose를 foreground로 실행하므로 API와 Merchant 로그가 같은 PowerShell 창에 표시된다. 화면에서 버튼을 누르거나 데이터를 조회하면 API 요청 로그가 `request method=... path=... status=... durationMs=...` 형태로 찍힌다.

## WSL 기준 개발 도구

개발과 검증은 가능하면 WSL 터미널에서 실행한다. Node, npm, JDK, Docker CLI, Docker Compose plugin은 WSL 사용자 환경에 설치해 사용하고, Docker daemon은 Docker Desktop WSL integration으로 연결한다.

WSL 확인 명령:

```bash
node -v
npm -v
java -version
bash shipmate-infra/scripts/check-wsl-docker.sh
curl -sS http://localhost:8080/actuator/health
curl -I -L http://localhost:3001/
```

`/var/run/docker.sock`이 없거나 권한 오류가 나면 Docker Desktop에서 다음 설정을 켠다.

```text
Docker Desktop > Settings > Resources > WSL Integration > Ubuntu 활성화
```

적용 후 Docker Desktop을 재시작하고 WSL 터미널도 새로 열어 다시 확인한다.

Codex sandbox 안에서만 `localhost:8080` 접근이 거부될 수 있다. 이 경우 실제 WSL 터미널 또는 권한 밖 실행에서 다음 순서로 확인한다.

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS http://127.0.0.1:8080/actuator/health
curl -sS "http://$(awk '/nameserver/ {print $2; exit}' /etc/resolv.conf):8080/actuator/health"
docker exec shipmate-api curl -fsS http://localhost:8080/actuator/health
```

현재 로컬 통합 테스트의 기본 API 주소는 WSL과 Windows 모두 `http://localhost:8080`으로 둔다. sandbox 때문에 실패할 때만 컨테이너 내부 health 또는 Windows localhost 확인을 보조로 사용한다.

## 로컬 스테이징

로컬 개발 환경과 동시에 띄울 수 있도록 스테이징용 Compose는 별도 Compose project 이름, 컨테이너명, 포트를 사용합니다.

```powershell
Copy-Item .env.staging.example .env.staging
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

스테이징 접속 주소:

```text
shipmate-merchant: http://localhost:13001
shipmate-api: http://localhost:18080
Nginx HTTPS front: https://staging-shipmate.com/merchant
MySQL: localhost:13306
MinIO API: http://localhost:19000
MinIO Console: http://localhost:19001
```

스테이징 통합 smoke script:

```bash
bash shipmate-infra/scripts/smoke-staging.sh
```

기본 대상은 `http://localhost:18080`이다. 회원가입, 로그인, 고객 저장, 주문 원문 접수, 초안 생성, 검수 저장, 제출, 조회, 재검수 전환, 재제출, CSV 검증, 변경 이력 metadata 확인까지 실행한다. 다른 hosts 또는 터널 주소로 검증할 때는 `API_BASE_URL=https://temporary-domain.example/api-root bash shipmate-infra/scripts/smoke-staging.sh`처럼 실행한다.

실행 결과는 기본적으로 `shipmate-infra/smoke-report.json`, `shipmate-infra/smoke-report.xml`에 남기고, CI 수집용 복사본은 `shipmate-infra/reports/smoke/smoke-report-{timestamp}.json`, `shipmate-infra/reports/smoke/smoke-report-{timestamp}.xml`에 남긴다. CI에서 별도 산출물 경로가 필요하면 `SMOKE_REPORT_DIR`을 지정한다.

스모크와 HTTPS 확인 리포트를 운영 점검용 Markdown으로 요약하려면 다음을 실행한다.

```bash
bash shipmate-infra/scripts/summarize-staging-reports.sh
```

기본 출력은 `shipmate-infra/reports/staging-check-summary.md`이며, GitHub Actions artifact에도 함께 업로드된다.

GitHub Actions에서는 `.github/workflows/staging-smoke.yml`이 `workflow_dispatch` 수동 실행으로 smoke script를 실행하고, JSON/JUnit XML 리포트를 `shipmate-smoke-reports` artifact로 업로드한다. 실제 원격 CI에서 쓰려면 repository secret `STAGING_API_BASE_URL`에 runner가 접근 가능한 스테이징 API 주소를 넣는다. 값이 없으면 로컬 기본값 `http://localhost:18080`을 사용한다.

WSL에 `gh` CLI와 GitHub 인증이 준비되어 있으면 다음 스크립트로 원격 workflow를 직접 실행하고 완료 상태를 확인한다.

```bash
bash shipmate-infra/scripts/run-github-smoke-workflow.sh
```

기본 workflow는 `staging-smoke.yml`, 기본 branch는 현재 git branch다. 필요하면 `WORKFLOW_FILE`, `REF_NAME`, `MAX_WAIT_SECONDS` 환경 변수로 바꾼다. `gh`가 없으면 WSL에서 `sudo apt-get update && sudo apt-get install -y gh`로 설치한 뒤 `gh auth login`을 먼저 실행한다.

스테이징 서버 로그를 PowerShell 창에서 계속 보려면 워크스페이스 루트의 `start_shipmate_staging_stack_logs.bat`를 실행한다.

스테이징 기본 계정, 비밀번호, 스키마명은 임시로 모두 `shipmate`입니다. 실제 운영 또는 외부 접근 가능한 스테이징에서는 반드시 변경합니다.

### 스테이징 도메인과 TLS

앞단 웹서버는 Nginx를 사용합니다.

```text
https://staging-shipmate.com/merchant
```

Nginx는 `/merchant`를 `shipmate-staging-merchant`로, `/api`와 `/actuator`를 `shipmate-staging-api`로 프록시합니다. TLS는 로컬 검증과 Windows 클라이언트 호환성을 위해 `ssl_protocols TLSv1.2 TLSv1.3`으로 둡니다.

공인 DNS 구매와 A 레코드 등록은 마지막 단계로 미룬다. DNS 등록 전에는 hosts 파일, 로컬 Nginx, 터널, 또는 `API_BASE_URL`로 지정한 임시 주소 기준으로 기능 검증을 진행한다. 실제 브라우저에서 공인 HTTPS로 접속하려면 다음 조건이 필요합니다.

- `staging-shipmate.com` DNS A 레코드가 Windows Docker Desktop이 실행되는 PC 또는 라우터의 공인 IP를 가리켜야 한다.
- 외부 접속을 허용하려면 공유기/방화벽에서 80, 443 포트를 해당 PC로 전달해야 한다.
- `shipmate-infra/certs/staging/fullchain.pem`, `privkey.pem`에 공인 인증서를 넣어야 한다.
- 현재 로컬 검증용 자체 서명 인증서는 브라우저에서 신뢰되지 않는다.

공인 접속 준비 체크:

```powershell
Resolve-DnsName staging-shipmate.com
Test-NetConnection staging-shipmate.com -Port 80
Test-NetConnection staging-shipmate.com -Port 443
curl.exe -I https://staging-shipmate.com/merchant/
```

인증서 파일은 저장소에 커밋하지 않는다. `certs/staging/fullchain.pem`, `certs/staging/privkey.pem`은 서버 또는 로컬 스테이징 장비에만 둔다.

Let's Encrypt를 사용할 경우 Windows 호스트에서 인증서를 발급한 뒤 위 경로에 복사하고 Nginx 컨테이너를 재시작한다.

### 공인 DNS 등록 전 임시 외부 접속

공인 DNS 등록 전에는 세 가지 방식 중 하나로 검증한다.

| 방식 | 용도 | 제한 |
|---|---|---|
| 로컬 hosts | 같은 PC 또는 통제 가능한 내부망 브라우저 검증 | 외부 OAuth callback 검증에는 부적합 |
| Cloudflare Tunnel | 임시 HTTPS 외부 URL과 OAuth redirect 검증 | Cloudflare 계정과 터널 실행 필요 |
| ngrok | 빠른 임시 HTTPS URL 검증 | 무료 플랜 URL 변경과 세션 제한 가능 |

로컬 hosts 방식:

```powershell
notepad C:\Windows\System32\drivers\etc\hosts
```

```text
127.0.0.1 staging-shipmate.com
```

Cloudflare Tunnel 또는 ngrok 방식:

```text
1. 임시 HTTPS URL을 발급한다.
2. Google Console 승인된 redirect URI에 https://{temporary-host}/api/oauth/google/callback 을 추가한다.
3. shipmate-infra/.env.staging 의 STAGING_GOOGLE_OAUTH_REDIRECT_URI를 같은 값으로 맞춘다.
4. docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build shipmate-api shipmate-merchant nginx
```

DNS 등록 전 HTTPS 확인 스크립트:

```bash
ALLOW_INSECURE_TLS=true STAGING_RESOLVE_IP=127.0.0.1 bash shipmate-infra/scripts/check-staging-https.sh
```

`STAGING_RESOLVE_IP`를 지정하면 로컬 hosts 파일을 수정하지 않고 `curl --resolve`로 `staging-shipmate.com`을 해당 IP에 임시 매핑한다. Cloudflare Tunnel 또는 ngrok URL을 확인할 때는 `STAGING_HTTPS_URL`, `API_HEALTH_URL`을 터널 URL로 지정한다.

확인 결과는 `shipmate-infra/staging-https-report.json`, `shipmate-infra/staging-https-report.xml`에 저장되고, CI 수집용 복사본은 `shipmate-infra/reports/https/` 아래에 남는다. GitHub Actions artifact에도 smoke 리포트와 함께 업로드된다.

브라우저에서 DNS 등록 없이 로컬 스테이징을 확인하는 최소 절차:

```text
1. docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build shipmate-api shipmate-merchant nginx
2. 로컬 hosts에 127.0.0.1 staging-shipmate.com을 추가하거나, check-staging-https.sh처럼 curl --resolve를 사용한다.
3. 자체 서명 인증서 경고를 허용하고 https://staging-shipmate.com/merchant/에 접속한다.
4. Google OAuth callback까지 검증해야 하면 hosts 방식 대신 Cloudflare Tunnel 또는 ngrok HTTPS URL을 Google Console redirect URI에 추가한다.
```

공인 DNS가 없는 상태에서 외부 사용자 브라우저까지 검증해야 하는 경우에는 hosts 방식으로는 부족하다. 이때는 임시 HTTPS 터널 URL을 스테이징 URL로 보고 `.env.staging`의 `STAGING_GOOGLE_OAUTH_REDIRECT_URI`와 Google Console redirect URI를 같은 값으로 맞춘다.

## MySQL 기준

첫 DB 마이그레이션은 공통 마스터 코드 테이블 `tb_code`를 생성합니다. 모든 업무 테이블명은 `tb_` prefix를 붙이는 것을 기준으로 합니다.

현재 MySQL 마이그레이션은 `tb_code`를 먼저 생성한 뒤 핵심 업무 테이블을 생성합니다. 업무 테이블명은 모두 `tb_` prefix를 사용합니다.

`minio-init` 서비스가 다음 버킷을 생성합니다.

```text
shipmate-call-recordings
shipmate-email-attachments
```

## 객체 키 규칙

```text
call-recordings/{tenantId}/{userId}/{recordingId}/original.{ext}
```

예시:

```text
call-recordings/local-tenant/local-user/rec_20260525_001/original.m4a
```
