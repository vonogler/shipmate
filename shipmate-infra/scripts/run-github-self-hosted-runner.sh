#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

ENV_FILE="${ENV_FILE:-shipmate-infra/.env.github-runner}"
REPORT_DIR="${REPORT_DIR:-shipmate-infra/reports/github-runner}"
REPORT_FILE="${REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-report.json}"
STAGE_REPORT_FILE="${STAGE_REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-run-report.json}"

write_report() {
  local status="$1"
  local reason="$2"
  local container_status="${3:--}"
  mkdir -p "${REPORT_DIR}"
  cat > "${REPORT_FILE}" <<REPORT
{
  "status": "${status}",
  "stage": "run",
  "reason": "${reason}",
  "containerName": "shipmate-github-runner",
  "containerStatus": "${container_status}",
  "envFile": "${ENV_FILE}",
  "runnerLabel": "${GITHUB_RUNNER_LABELS:-shipmate-staging,self-hosted,linux,x64}",
  "finishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
REPORT
  cp "${REPORT_FILE}" "${STAGE_REPORT_FILE}"
}

if [[ ! -f "${ENV_FILE}" ]]; then
  write_report "skipped" "env-file-missing"
  cat >&2 <<'MESSAGE'
GitHub self-hosted runner 환경 파일이 없습니다.

1. GitHub 저장소 > Settings > Actions > Runners > New self-hosted runner에서 등록 token을 발급합니다.
2. shipmate-infra/.env.github-runner 파일을 만들고 아래 값을 입력합니다.

GITHUB_REPOSITORY_URL=https://github.com/vonogler/shipmate
GITHUB_RUNNER_TOKEN=발급받은_등록_token
GITHUB_RUNNER_NAME=shipmate-staging-runner
GITHUB_RUNNER_LABELS=shipmate-staging,self-hosted,linux,x64

3. 다시 실행합니다.

bash shipmate-infra/scripts/run-github-self-hosted-runner.sh
MESSAGE
  exit 1
fi

mkdir -p "${REPORT_DIR}"
docker compose \
  --env-file "${ENV_FILE}" \
  -f shipmate-infra/docker-compose.github-runner.yml \
  up -d --build github-runner

container_status="$(docker inspect -f '{{.State.Status}}' shipmate-github-runner 2>/dev/null || printf '%s' unknown)"
write_report "started" "-" "${container_status}"
echo "report=${REPORT_FILE}"
docker logs -f shipmate-github-runner
