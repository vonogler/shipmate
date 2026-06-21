#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_FILE="${WORKFLOW_FILE:-staging-smoke.yml}"
REF_NAME="${REF_NAME:-$(git branch --show-current 2>/dev/null || true)}"
POLL_SECONDS="${POLL_SECONDS:-10}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-600}"
RUNNER_LABEL="${RUNNER_LABEL:-${runner_label:-}}"
API_BASE_URL_INPUT="${API_BASE_URL:-${api_base_url:-}}"
RUN_STACK_CHECK="${RUN_STACK_CHECK:-${run_stack_check:-}}"
STAGING_HTTPS_URL_INPUT="${STAGING_HTTPS_URL:-${staging_https_url:-}}"
STAGING_API_HEALTH_URL_INPUT="${STAGING_API_HEALTH_URL:-${staging_api_health_url:-${API_HEALTH_URL:-}}}"
STAGING_RESOLVE_IP_INPUT="${STAGING_RESOLVE_IP:-${staging_resolve_ip:-}}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 설치되어 있지 않습니다. WSL에서 gh 설치 후 다시 실행하세요."
  echo "Ubuntu 예: sudo apt-get update && sudo apt-get install -y gh"
  echo "Oracle Linux 예: sudo dnf config-manager --add-repo https://cli.github.com/packages/rpm/gh-cli.repo && sudo dnf install -y gh"
  exit 127
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh auth status 확인은 실패했습니다. 이미 인증했다면 실제 workflow 실행을 계속 시도합니다." >&2
fi

if [[ -z "${REF_NAME}" ]]; then
  REF_NAME="main"
fi

echo "workflow=${WORKFLOW_FILE}"
echo "ref=${REF_NAME}"
dispatch_started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
workflow_args=(workflow run "${WORKFLOW_FILE}" --ref "${REF_NAME}")
if [[ -n "${RUNNER_LABEL}" ]]; then
  workflow_args+=(-f "runner_label=${RUNNER_LABEL}")
fi
if [[ -n "${API_BASE_URL_INPUT}" ]]; then
  workflow_args+=(-f "api_base_url=${API_BASE_URL_INPUT}")
fi
if [[ -n "${RUN_STACK_CHECK}" ]]; then
  workflow_args+=(-f "run_stack_check=${RUN_STACK_CHECK}")
fi
if [[ -n "${STAGING_HTTPS_URL_INPUT}" ]]; then
  workflow_args+=(-f "staging_https_url=${STAGING_HTTPS_URL_INPUT}")
fi
if [[ -n "${STAGING_API_HEALTH_URL_INPUT}" ]]; then
  workflow_args+=(-f "staging_api_health_url=${STAGING_API_HEALTH_URL_INPUT}")
fi
if [[ -n "${STAGING_RESOLVE_IP_INPUT}" ]]; then
  workflow_args+=(-f "staging_resolve_ip=${STAGING_RESOLVE_IP_INPUT}")
fi
run_output="$(gh "${workflow_args[@]}")"
echo "${run_output}"
run_id="$(printf "%s" "${run_output}" | sed -n 's#.*/actions/runs/\([0-9][0-9]*\).*#\1#p' | head -n 1)"

echo "원격 workflow 실행이 등록되었습니다. 최근 실행 상태를 확인합니다."
elapsed=0
while (( elapsed <= MAX_WAIT_SECONDS )); do
  if [[ -n "${run_id}" ]]; then
    run_row="$(gh run view "${run_id}" --json databaseId,status,conclusion,url,createdAt)"
  else
    run_row="$(gh run list --workflow "${WORKFLOW_FILE}" --branch "${REF_NAME}" --limit 10 --json databaseId,status,conclusion,url,createdAt --jq "[.[] | select(.createdAt >= \"${dispatch_started_at}\")] | .[0]")"
    run_id="$(printf "%s" "${run_row}" | sed -n 's/.*"databaseId":\([0-9][0-9]*\).*/\1/p')"
  fi
  status="$(printf "%s" "${run_row}" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
  conclusion="$(printf "%s" "${run_row}" | sed -n 's/.*"conclusion":"\([^"]*\)".*/\1/p')"
  url="$(printf "%s" "${run_row}" | sed -n 's/.*"url":"\([^"]*\)".*/\1/p')"
  echo "runId=${run_id:-unknown} status=${status:-unknown} conclusion=${conclusion:-} url=${url:-}"
  if [[ "${status}" == "completed" ]]; then
    [[ "${conclusion}" == "success" ]]
    exit $?
  fi
  sleep "${POLL_SECONDS}"
  elapsed=$((elapsed + POLL_SECONDS))
done

echo "제한 시간 안에 workflow가 끝나지 않았습니다."
exit 124
