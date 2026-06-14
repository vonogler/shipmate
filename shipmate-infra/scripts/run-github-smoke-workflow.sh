#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_FILE="${WORKFLOW_FILE:-staging-smoke.yml}"
REF_NAME="${REF_NAME:-$(git branch --show-current 2>/dev/null || true)}"
POLL_SECONDS="${POLL_SECONDS:-10}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-600}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 설치되어 있지 않습니다. WSL에서 gh 설치 후 다시 실행하세요."
  echo "Ubuntu 예: sudo apt-get update && sudo apt-get install -y gh"
  exit 127
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh 인증이 필요합니다. 먼저 gh auth login을 실행하세요."
  exit 1
fi

if [[ -z "${REF_NAME}" ]]; then
  REF_NAME="main"
fi

echo "workflow=${WORKFLOW_FILE}"
echo "ref=${REF_NAME}"
gh workflow run "${WORKFLOW_FILE}" --ref "${REF_NAME}"

echo "원격 workflow 실행이 등록되었습니다. 최근 실행 상태를 확인합니다."
elapsed=0
while (( elapsed <= MAX_WAIT_SECONDS )); do
  run_row="$(gh run list --workflow "${WORKFLOW_FILE}" --branch "${REF_NAME}" --limit 1 --json databaseId,status,conclusion,url --jq '.[0]')"
  run_id="$(printf "%s" "${run_row}" | sed -n 's/.*"databaseId":\([0-9][0-9]*\).*/\1/p')"
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
