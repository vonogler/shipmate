#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

ENV_FILE="${ENV_FILE:-shipmate-infra/.env.github-runner}"
REPORT_DIR="${REPORT_DIR:-shipmate-infra/reports/github-runner}"
REPORT_FILE="${REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-report.json}"
STAGE_REPORT_FILE="${STAGE_REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-stop-report.json}"

compose_args=(-f shipmate-infra/docker-compose.github-runner.yml)
if [[ -f "${ENV_FILE}" ]]; then
  compose_args=(--env-file "${ENV_FILE}" "${compose_args[@]}")
fi

docker compose "${compose_args[@]}" down

mkdir -p "${REPORT_DIR}"
cat > "${REPORT_FILE}" <<REPORT
{
  "status": "stopped",
  "stage": "stop",
  "reason": "-",
  "containerName": "shipmate-github-runner",
  "containerStatus": "stopped",
  "envFile": "${ENV_FILE}",
  "runnerLabel": "-",
  "finishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
REPORT

cp "${REPORT_FILE}" "${STAGE_REPORT_FILE}"

echo "report=${REPORT_FILE}"
