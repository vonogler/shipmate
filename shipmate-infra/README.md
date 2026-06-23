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

다른 DB나 API가 이미 `3306`, `8080`, `3001`을 사용 중이면 `.env`를 바꾸지 않고 실행 시점에 host 포트만 바꾼다.

```bash
SHIPMATE_MYSQL_HOST_PORT=13307 \
SHIPMATE_API_HOST_PORT=18081 \
SHIPMATE_MERCHANT_HOST_PORT=13002 \
docker compose -f docker-compose.local.yml --env-file .env up -d --build
```

이 경우 컨테이너 내부 연결은 그대로 `mysql:3306`, `shipmate-api:8080`을 사용하므로 API의 Flyway migration은 정상 실행된다. 외부에서 MySQL에 직접 접속할 때만 `localhost:13307`처럼 바뀐 host 포트를 사용한다.

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
bash shipmate-infra/scripts/check-local-ports.sh
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
shipmate-admin: http://localhost:13000
shipmate-merchant: http://localhost:13001
shipmate-api: http://localhost:18080
Nginx HTTPS admin: https://staging-shipmate.com/admin
Nginx HTTPS front: https://staging-shipmate.com/merchant
MySQL: localhost:13306
MinIO API: http://localhost:19000
MinIO Console: http://localhost:19001
```

포트 충돌 여부는 Compose 실행 전에 다음으로 확인한다.

```bash
bash shipmate-infra/scripts/check-local-ports.sh
```

충돌이 있으면 `.env.staging`을 고치기 전에 실행 시점에 host 포트만 바꿀 수 있다.

```bash
SHIPMATE_STAGING_ADMIN_HOST_PORT=13010 \
SHIPMATE_STAGING_MERCHANT_HOST_PORT=13011 \
SHIPMATE_STAGING_API_HOST_PORT=18090 \
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build
```

스테이징 통합 smoke script:

```bash
bash shipmate-infra/scripts/smoke-staging.sh
```

기본 대상은 `http://localhost:18080`이다. 회원가입, 로그인, 고객 저장, 주문 원문 접수, 초안 생성, 검수 저장, 제출, 조회, 재검수 전환, 재제출, CSV 검증, 변경 이력 metadata 확인까지 실행한다. 다른 hosts 또는 터널 주소로 검증할 때는 `API_BASE_URL=https://temporary-domain.example/api-root bash shipmate-infra/scripts/smoke-staging.sh`처럼 실행한다.

스모크 시작 전에 기본 내부관리자 계정 `shipmate / shipmate` 존재 여부까지 확인하려면 다음처럼 실행한다.

```bash
SMOKE_SEED_DEFAULT_ACCOUNT=true bash shipmate-infra/scripts/smoke-staging.sh
```

seed와 cleanup은 분리해서 운영한다. `SMOKE_SEED_DEFAULT_ACCOUNT=true`는 로그인 검증용 기본 계정만 준비하고, smoke 시나리오가 생성한 `merchant_smoke_%` 테스트 데이터는 삭제하지 않는다. 테스트 데이터 정리는 `cleanup-staging-smoke-data.sh` dry-run 확인 뒤 별도 apply로 실행한다.

실행 결과는 기본적으로 `shipmate-infra/smoke-report.json`, `shipmate-infra/smoke-report.xml`에 남기고, CI 수집용 복사본은 `shipmate-infra/reports/smoke/smoke-report-{timestamp}.json`, `shipmate-infra/reports/smoke/smoke-report-{timestamp}.xml`에 남긴다. 실패 시에는 마지막 단계, 마지막 HTTP 상태, curl 오류, 응답 본문 일부를 리포트에 포함한다. CI에서 별도 산출물 경로가 필요하면 `SMOKE_REPORT_DIR`을 지정한다.

스테이징 smoke는 `merchant_smoke_{timestamp}`, `customer_smoke_{timestamp}`, `smoke-{timestamp}` 패턴의 테스트 데이터를 생성한다. 공용 스테이징 DB에서는 smoke 통과 후 바로 삭제하지 않고, 실패 재현과 artifact 추적을 위해 우선 dry-run으로 삭제 대상을 확인한다.

```bash
bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
```

실제 삭제는 명시적으로 apply를 켠 경우에만 수행한다. 이 스크립트는 스테이징 전용이며 운영 DB에 사용하지 않는다.

```bash
SMOKE_CLEANUP_APPLY=true \
SMOKE_CLEANUP_CONFIRM='merchant_smoke_%' \
bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
```

삭제 대상은 `merchant_id like 'merchant_smoke_%'` 업무 데이터로 제한한다. 통합 실패 분석을 위해 최근 artifact에 기록된 `merchantId`, `inboundMessageId`, `orderDraftId`를 확인한 뒤 실행한다. 실제 삭제 시에는 `SMOKE_CLEANUP_CONFIRM` 값이 삭제 pattern과 일치해야 한다.

운영 타이밍은 다음 기준으로 둔다.

| 상황 | 기준 |
|---|---|
| 기능 개발 중 반복 smoke | 삭제하지 않고 누적 보관한다. 실패 재현과 비교를 우선한다. |
| 하루 개발 종료 또는 20건 이상 누적 | dry-run으로 대상 row 수를 확인한 뒤 필요하면 apply를 1회 실행한다. |
| GitHub Actions artifact 분석 중 | 해당 run의 `merchantId`, `orderDraftId` 확인이 끝날 때까지 삭제하지 않는다. |
| 공용 스테이징 DB 용량/목록 노이즈 증가 | dry-run 리포트를 staging summary에 남긴 뒤 apply를 실행한다. |

cleanup 실행 결과는 `shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json`과 JUnit XML에 남기며, staging summary에도 표시된다.

GitHub Actions workflow는 cleanup JSON/JUnit 리포트를 artifact로 업로드하고, `GITHUB_STEP_SUMMARY`에도 Smoke 테스트 데이터 정리, artifact 정리, cleanup JUnit testcase 요약 표를 표시한다. GitHub Actions의 기본 테스트 탭에 JUnit을 별도 테스트로 노출하는 외부 reporter는 아직 추가하지 않았으므로, 현재 기준의 표준 확인 위치는 workflow summary와 `shipmate-smoke-reports` artifact다.

스모크와 HTTPS 확인 리포트를 운영 점검용 Markdown으로 요약하려면 다음을 실행한다.

```bash
bash shipmate-infra/scripts/summarize-staging-reports.sh
```

내부관리자와 가맹점관리자 브라우저 흐름은 Playwright 스크립트로 확인한다. 기본 대상은 로컬 스테이징 Nginx의 `/admin/`, `/merchant/` 경로다.

처음 실행하는 WSL 환경에서는 Node 의존성과 Chromium, OS 라이브러리를 준비한다.

```bash
cd shipmate-infra
npm install
npx playwright install chromium
sudo apt-get update
sudo apt-get install -y libnspr4 libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libxkbcommon0 libxdamage1 libxrandr2 libgbm1 libgtk-3-0 libasound2t64
npm run check:ui
```

다른 주소를 확인할 때는 다음처럼 지정한다.

```bash
ADMIN_BASE_URL=https://staging-shipmate.com/admin/ \
MERCHANT_BASE_URL=https://staging-shipmate.com/merchant/ \
npm run check:ui
```

기본 출력은 `shipmate-infra/reports/staging-check-summary.md`이며, GitHub Actions artifact에도 함께 업로드된다.

GitHub Actions에서는 `.github/workflows/staging-smoke.yml`이 `workflow_dispatch` 수동 실행으로 smoke script를 실행하고, JSON/JUnit XML 리포트를 `shipmate-smoke-reports` artifact로 업로드한다. 실제 원격 CI에서 쓰려면 repository secret `STAGING_API_BASE_URL`에 runner가 접근 가능한 스테이징 API 주소를 넣는다. 값이 없으면 GitHub hosted runner에서 로컬 PC의 `localhost`에 접근할 수 없으므로 smoke 실행을 건너뛰고 skipped 리포트를 남긴다.

WSL에 `gh` CLI와 GitHub 인증이 준비되어 있으면 다음 스크립트로 원격 workflow를 직접 실행하고 완료 상태를 확인한다.

```bash
bash shipmate-infra/scripts/run-github-smoke-workflow.sh
```

기본 workflow는 `staging-smoke.yml`, 기본 branch는 현재 git branch다. 필요하면 `WORKFLOW_FILE`, `REF_NAME`, `MAX_WAIT_SECONDS` 환경 변수로 바꾼다. `gh`가 없으면 WSL 배포판에 맞게 설치한 뒤 `gh auth login`을 먼저 실행한다.

Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y gh
```

Oracle Linux:

```bash
sudo dnf install -y 'dnf-command(config-manager)'
sudo dnf config-manager --add-repo https://cli.github.com/packages/rpm/gh-cli.repo
sudo dnf install -y gh
```

GitHub Actions artifact를 로컬로 내려받으려면 다음을 실행한다.

```bash
bash shipmate-infra/scripts/download-github-smoke-reports.sh
```

기본적으로 현재 branch의 최신 성공 run에서 `shipmate-smoke-reports` artifact를 내려받아 `shipmate-infra/reports/github-actions/run-{runId}-{timestamp}/`에 저장하고, `artifact-summary.md`에 run URL과 주요 리포트 파일 경로를 정리한다. 특정 실행을 내려받으려면 `RUN_ID=27503312344 bash shipmate-infra/scripts/download-github-smoke-reports.sh`처럼 실행한다.

다운로드한 GitHub Actions artifact는 로컬 `reports/` 아래에만 보관하고 git에는 포함하지 않는다. 기본 보관 기준은 최신 10개 run 디렉터리다. 정리 전에는 dry-run으로 삭제 후보를 먼저 확인한다.

```bash
bash shipmate-infra/scripts/cleanup-github-smoke-reports.sh
```

실제 삭제는 명시적으로 apply를 켠 경우에만 수행한다.

```bash
REPORT_CLEANUP_APPLY=true bash shipmate-infra/scripts/cleanup-github-smoke-reports.sh
```

보관 개수를 바꿀 때는 `KEEP_LATEST=20`처럼 지정한다. 실패 분석 중인 run은 별도 위치에 복사하지 않고, 필요한 경우 `RUN_ID=... download-github-smoke-reports.sh`로 다시 내려받는다.

중요 run은 보관 개수 밖으로 밀려도 삭제 후보에서 제외할 수 있다.

```bash
PROTECT_RUN_IDS=27901147270,27900208928 \
bash shipmate-infra/scripts/cleanup-github-smoke-reports.sh
```

원격 runner에서 실제 smoke를 실행하려면 GitHub repository secret `STAGING_API_BASE_URL`에 runner가 접근 가능한 HTTPS API 주소를 넣는다. GitHub hosted runner는 개발 PC의 `localhost`, `127.0.0.1`, WSL 내부 주소에 접근할 수 없으므로 Cloudflare Tunnel, ngrok, 공인 staging 주소, 또는 self-hosted runner에서 접근 가능한 주소를 사용한다.

GitHub Actions smoke 대상 URL은 runner 위치에 따라 다르게 둔다.

| 실행 위치 | `runner_label` | `api_base_url` 또는 secret | 사용 기준 |
|---|---|---|---|
| GitHub hosted runner | 비움 또는 `ubuntu-latest` | Cloudflare Tunnel/ngrok/공인 HTTPS URL | 로컬 PC와 Docker network에 직접 접근할 수 없으므로 외부에서 접근 가능한 URL만 사용한다. |
| self-hosted runner 컨테이너 | `shipmate-staging` | `http://shipmate-api:8080` | runner가 `shipmate-staging_default` Docker network 안에서 실행될 때 사용한다. secret에 넣지 않고 workflow input으로 전달하는 것을 기본으로 한다. |
| self-hosted runner 호스트 | `shipmate-staging` | `http://host.docker.internal:18080` 또는 `http://localhost:18080` | runner가 Docker network 밖의 WSL/Windows 호스트에서 실행될 때만 사용한다. |
| 공인 스테이징 | 필요에 따라 선택 | `https://staging-shipmate.com` 계열 | DNS와 TLS가 준비된 마지막 단계에서 사용한다. |

`STAGING_API_BASE_URL` repository secret에는 외부 runner가 접근해야 하는 URL만 넣는다. Docker network 내부 URL인 `http://shipmate-api:8080`은 GitHub hosted runner에서 실패하므로 secret으로 상시 저장하지 않는다.

Docker Desktop 스테이징 network에 Cloudflare quick tunnel을 임시로 연결해 secret 설정, workflow 실행, artifact 다운로드, tunnel/secret 정리까지 한 번에 처리하려면 다음을 실행한다.

```bash
bash shipmate-infra/scripts/run-github-smoke-with-quick-tunnel.sh
```

기본값은 `shipmate-staging_default` network에서 `http://shipmate-api:8080`을 외부 임시 URL로 노출한다. WSL `docker`가 Docker Desktop integration에 연결되지 않은 환경에서는 스크립트가 Windows Docker Desktop CLI를 PowerShell 경유로 호출한다. 기존 `STAGING_API_BASE_URL` secret이 있으면 실수로 덮어쓰지 않도록 중단하며, 임시 tunnel URL로 덮어써도 되는 경우에만 `ALLOW_OVERWRITE_SECRET=true`를 지정한다. 검증 후 기본적으로 tunnel 컨테이너와 임시 `STAGING_API_BASE_URL` secret을 정리한다. 임시 URL을 유지해야 할 때만 `KEEP_TUNNEL=true CLEANUP_SECRET=false`를 사용한다.

quick tunnel workflow가 실패하거나 artifact 자동 다운로드가 실패하면 `gh run view --log-failed` 출력을 로컬 리포트로 저장한다.

```text
shipmate-infra/reports/quick-tunnel/quick-tunnel-workflow-report.txt
shipmate-infra/reports/quick-tunnel/quick-tunnel-workflow-{runId}-failed-log.txt
```

이 리포트에는 run ID, run URL, workflow exit code, artifact 다운로드 상태, 실패 로그 파일 경로를 기록한다. 성공한 실행도 `quick-tunnel-workflow-report.txt`에 마지막 성공 상태를 남긴다.

공인 DNS 없이 반복 검증을 더 안정화하려면 GitHub self-hosted runner를 스테이징 Docker network 또는 같은 WSL/Windows 호스트에서 실행하는 방식이 가능하다. 이 경우 GitHub hosted runner가 외부 URL로 접근하지 않아도 되므로 quick tunnel과 `STAGING_API_BASE_URL` 임시 secret을 생략할 수 있다. 다만 runner 등록 token은 GitHub 웹 UI 또는 `gh` 권한이 필요하다.

self-hosted runner 컨테이너 PoC는 다음 파일로 준비되어 있다.

```text
shipmate-infra/github-runner/Dockerfile
shipmate-infra/docker-compose.github-runner.yml
shipmate-infra/scripts/run-github-self-hosted-runner.sh
```

GitHub 저장소의 `Settings > Actions > Runners > New self-hosted runner`에서 등록 token을 발급한 뒤 `shipmate-infra/.env.github-runner`에 다음 값을 저장한다. 이 파일은 git에 포함하지 않는다.

GitHub 웹 UI 기준 발급 위치:

```text
vonogler/shipmate 저장소
> Settings
> Actions
> Runners
> New self-hosted runner
> Linux
> x64
```

화면에 표시되는 `./config.sh --url ... --token ...` 명령에서 `--token` 뒤 값을 `GITHUB_RUNNER_TOKEN`에 넣는다. token은 짧은 시간 뒤 만료되므로 발급 직후 실행한다.

```bash
GITHUB_REPOSITORY_URL=https://github.com/vonogler/shipmate
GITHUB_RUNNER_TOKEN=발급받은_등록_token
GITHUB_RUNNER_NAME=shipmate-staging-runner
GITHUB_RUNNER_LABELS=shipmate-staging,self-hosted,linux,x64
```

`gh` 인증이 되어 있으면 token 발급과 `.env.github-runner` 생성을 helper로 처리할 수 있다.

```bash
bash shipmate-infra/scripts/create-github-runner-env.sh
```

기본 저장소는 `vonogler/shipmate`다. 다른 저장소를 쓸 때는 `GITHUB_REPOSITORY=owner/repo`를 지정한다.

runner를 실행한다.

```bash
bash shipmate-infra/scripts/run-github-self-hosted-runner.sh
```

로컬 runner 컨테이너를 중지하고 최근 상태 리포트를 `stopped`로 남기려면 다음을 실행한다.

```bash
bash shipmate-infra/scripts/stop-github-self-hosted-runner.sh
```

운영 기준은 `짧은 검증용 일회성 등록`을 기본으로 둔다.

| 방식 | 기준 | 이유 |
|---|---|---|
| 일회성 등록 후 중지 | 로컬 PC/WSL에서 스테이징 smoke를 검증할 때 기본값 | runner token과 로컬 Docker socket 노출 시간을 줄이고, GitHub에 오래된 offline runner가 쌓이는 문제를 줄인다. |
| 컨테이너 재사용 | 같은 장비에서 짧은 시간 안에 여러 workflow를 반복 실행할 때만 사용 | 재등록 시간을 줄일 수 있지만, 검증 종료 후 반드시 stop script로 내린다. |
| 장기 상주 runner | 공인 스테이징 서버와 전용 runner 계정이 준비된 뒤 검토 | 개인 개발 PC에서는 보안과 상태 관리 부담이 크므로 기본으로 사용하지 않는다. |

일회성 검증 절차는 `create-github-runner-env.sh` 실행, `run-github-self-hosted-runner.sh` 실행, workflow 완료 확인, artifact 다운로드, `stop-github-self-hosted-runner.sh` 실행 순서다. 재사용할 때도 하루 작업이 끝나면 runner 컨테이너를 중지한다.

GitHub 저장소 화면에 남은 오래된 runner는 다음 위치에서 제거한다.

```text
vonogler/shipmate 저장소
> Settings
> Actions
> Runners
> 대상 runner 선택
> Remove
```

등록 token이 만료되었거나 runner가 오프라인으로 남아 있으면 GitHub 화면에서 기존 runner를 제거한 뒤 `create-github-runner-env.sh`를 다시 실행해 재등록한다.

실행 스크립트는 결과를 `shipmate-infra/reports/github-runner/self-hosted-runner-report.json`에 남긴다. 이후 `bash shipmate-infra/scripts/summarize-staging-reports.sh`를 실행하면 스테이징 점검 요약 Markdown/HTML의 `Self-hosted runner` 섹션에서도 최근 상태를 확인할 수 있다.

runner 리포트는 단계별 복사본도 남긴다.

```text
shipmate-infra/reports/github-runner/self-hosted-runner-create-env-report.json
shipmate-infra/reports/github-runner/self-hosted-runner-run-report.json
shipmate-infra/reports/github-runner/self-hosted-runner-stop-report.json
```

GitHub Actions artifact에는 workflow 실행 전에 이미 존재하는 `create-env`, `run` 리포트를 포함한다. `stop` 리포트는 workflow가 끝나고 artifact 업로드가 완료된 뒤 로컬에서 runner를 중지할 때 생성되므로 같은 run artifact에 포함하지 않는다. stop 상태는 로컬 `self-hosted-runner-stop-report.json`과 다음 실행의 artifact 또는 스테이징 점검 요약으로 확인한다.

GitHub Actions 수동 실행에서는 다음 값을 입력한다.

```text
runner_label=shipmate-staging
api_base_url=http://shipmate-api:8080
run_stack_check=true
```

이렇게 실행하면 runner 컨테이너가 스테이징 Docker network 내부 API로 직접 smoke와 전체 점검을 실행한다. 등록 token 없이 반복 검증할 때는 quick tunnel 자동화 스크립트를 기본 검증 방식으로 둔다.

secret 설정 helper:

```bash
STAGING_API_BASE_URL=https://temporary-host.example \
bash shipmate-infra/scripts/set-github-staging-smoke-secrets.sh
```

HTTPS 점검 secret까지 함께 넣는 예:

```bash
STAGING_API_BASE_URL=https://temporary-host.example \
STAGING_HTTPS_URL=https://temporary-host.example/merchant \
STAGING_API_HEALTH_URL=https://temporary-host.example/actuator/health \
bash shipmate-infra/scripts/set-github-staging-smoke-secrets.sh
```

`localhost` 계열 URL은 기본적으로 막는다. self-hosted runner처럼 같은 장비에서 실행하는 특수 상황에서는 `ALLOW_LOCALHOST_SECRET=true`를 명시해서 우회한다.

스테이징 서버 로그를 PowerShell 창에서 계속 보려면 워크스페이스 루트의 `start_shipmate_staging_stack_logs.bat`를 실행한다.

스테이징 기본 계정, 비밀번호, 스키마명은 임시로 모두 `shipmate`입니다. 실제 운영 또는 외부 접근 가능한 스테이징에서는 반드시 변경합니다.

스테이징 기본 내부관리자 계정이 없으면 다음 스크립트로 생성 또는 로그인 확인을 수행한다.

```bash
bash shipmate-infra/scripts/seed-staging-default-account.sh
```

기본값은 `API_BASE_URL=http://localhost:18080`, 로그인 ID와 비밀번호는 `shipmate / shipmate`다. 다른 값을 사용할 때는 실행 시점 환경 변수로 지정한다.

```bash
API_BASE_URL=http://localhost:18080 \
SHIPMATE_DEFAULT_LOGIN_ID=shipmate \
SHIPMATE_DEFAULT_PASSWORD=shipmate \
bash shipmate-infra/scripts/seed-staging-default-account.sh
```

스테이징 API 기동 직후 health를 기다린 다음 기본 계정을 확인하려면 다음 스크립트를 사용한다.

```bash
bash shipmate-infra/scripts/check-staging-default-account.sh
```

스테이징 stack 기동 뒤 기본 계정, DNS 없는 HTTPS, 통합 smoke, 점검 요약 생성을 한 번에 실행하려면 다음을 사용한다.

```bash
bash shipmate-infra/scripts/check-staging-stack.sh
```

기본값은 `API_BASE_URL=http://localhost:18080`, `STAGING_HTTPS_URL=https://staging-shipmate.com/merchant/`, `API_HEALTH_URL=https://staging-shipmate.com/actuator/health`, `STAGING_RESOLVE_IP=127.0.0.1`, `ALLOW_INSECURE_TLS=true`다. 다른 도메인, 포트, tunnel URL을 쓸 때는 실행 시점 환경 변수로 바꾼다.

### 스테이징 도메인과 TLS

앞단 웹서버는 Nginx를 사용합니다.

```text
https://staging-shipmate.com/admin
https://staging-shipmate.com/merchant
```

Nginx는 `/admin`을 `shipmate-staging-admin`으로, `/merchant`를 `shipmate-staging-merchant`로, `/api`와 `/actuator`를 `shipmate-staging-api`로 프록시합니다. TLS는 로컬 검증과 Windows 클라이언트 호환성을 위해 `ssl_protocols TLSv1.2 TLSv1.3`으로 둡니다.

Admin 또는 Merchant 컨테이너를 재생성하면 Docker network IP가 바뀔 수 있다. Nginx는 Docker DNS `127.0.0.11`을 사용해 upstream을 짧은 주기로 다시 해석하도록 설정한다. 프론트 경로가 뒤바뀌어 보이면 우선 Nginx를 재시작해 DNS cache를 비운다.

```bash
docker compose --env-file .env.staging -f docker-compose.staging.yml restart nginx
```

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
curl.exe -I https://staging-shipmate.com/admin/
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

반복 확인은 간편 wrapper를 사용한다. 기본값은 `MODE=resolve`, `STAGING_RESOLVE_IP=127.0.0.1`, `ALLOW_INSECURE_TLS=true`다.

```bash
bash shipmate-infra/scripts/check-staging-dnsless-https.sh
MODE=hosts bash shipmate-infra/scripts/check-staging-dnsless-https.sh
MODE=tunnel STAGING_TUNNEL_URL=https://temporary-host.example bash shipmate-infra/scripts/check-staging-dnsless-https.sh
```

확인 결과는 `shipmate-infra/staging-https-report.json`, `shipmate-infra/staging-https-report.xml`에 저장되고, CI 수집용 복사본은 `shipmate-infra/reports/https/` 아래에 남는다. GitHub Actions artifact에도 smoke 리포트와 함께 업로드된다.

브라우저에서 DNS 등록 없이 로컬 스테이징을 확인하는 최소 절차:

```text
1. docker compose --env-file .env.staging -f docker-compose.staging.yml up -d --build shipmate-api shipmate-merchant nginx
2. 로컬 hosts에 127.0.0.1 staging-shipmate.com을 추가하거나, check-staging-https.sh처럼 curl --resolve를 사용한다.
3. 자체 서명 인증서 경고를 허용하고 https://staging-shipmate.com/admin/ 또는 https://staging-shipmate.com/merchant/에 접속한다.
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
