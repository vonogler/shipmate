#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_FILE="${WORKFLOW_FILE:-staging-smoke.yml}"
ARTIFACT_NAME="${ARTIFACT_NAME:-shipmate-smoke-reports}"
REF_NAME="${REF_NAME:-$(git branch --show-current 2>/dev/null || true)}"
RUN_ID="${RUN_ID:-}"
DOWNLOAD_ROOT="${DOWNLOAD_ROOT:-shipmate-infra/reports/github-actions}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 설치되어 있지 않습니다."
  echo "Oracle Linux 예: sudo dnf config-manager --add-repo https://cli.github.com/packages/rpm/gh-cli.repo && sudo dnf install -y gh"
  echo "Ubuntu 예: sudo apt-get update && sudo apt-get install -y gh"
  exit 127
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh auth status 확인은 실패했습니다. 이미 인증했다면 실제 run 조회/다운로드를 계속 시도합니다." >&2
fi

if [[ -z "${REF_NAME}" ]]; then
  REF_NAME="main"
fi

if [[ -z "${RUN_ID}" ]]; then
  RUN_ID="$(gh run list \
    --workflow "${WORKFLOW_FILE}" \
    --branch "${REF_NAME}" \
    --limit 20 \
    --json databaseId,status,conclusion \
    --jq '.[] | select(.status == "completed" and .conclusion == "success") | .databaseId' \
    | head -n 1)"
fi

if [[ -z "${RUN_ID}" ]]; then
  echo "다운로드할 성공한 workflow run을 찾지 못했습니다."
  echo "WORKFLOW_FILE=${WORKFLOW_FILE} REF_NAME=${REF_NAME}"
  exit 1
fi

run_url="$(gh run view "${RUN_ID}" --json url --jq '.url')"
download_dir="${DOWNLOAD_ROOT}/run-${RUN_ID}-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "${download_dir}"

gh run download "${RUN_ID}" -n "${ARTIFACT_NAME}" -D "${download_dir}"

summary_md="$(find "${download_dir}" -name staging-check-summary.md -print -quit)"
summary_html="$(find "${download_dir}" -name staging-check-summary.html -print -quit)"
smoke_json="$(find "${download_dir}" -name smoke-report.json -print -quit)"
https_json="$(find "${download_dir}" -name staging-https-report.json -print -quit)"

cat > "${download_dir}/artifact-summary.md" <<REPORT
# GitHub Actions smoke artifact

| 항목 | 값 |
|---|---|
| workflow | ${WORKFLOW_FILE} |
| runId | ${RUN_ID} |
| run URL | ${run_url} |
| artifact | ${ARTIFACT_NAME} |
| 다운로드 경로 | ${download_dir} |
| Markdown 요약 | ${summary_md:-"-"} |
| HTML 요약 | ${summary_html:-"-"} |
| smoke JSON | ${smoke_json:-"-"} |
| HTTPS JSON | ${https_json:-"-"} |
REPORT

if [[ -x shipmate-infra/scripts/summarize-staging-reports.sh ]]; then
  GITHUB_REPORT_ROOT="${DOWNLOAD_ROOT}" bash shipmate-infra/scripts/summarize-staging-reports.sh >/dev/null
fi

echo "runId=${RUN_ID}"
echo "url=${run_url}"
echo "downloadDir=${download_dir}"
echo "summary=${download_dir}/artifact-summary.md"
