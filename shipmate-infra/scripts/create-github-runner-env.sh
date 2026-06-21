#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

REPOSITORY="${GITHUB_REPOSITORY:-vonogler/shipmate}"
REPOSITORY_URL="${GITHUB_REPOSITORY_URL:-https://github.com/${REPOSITORY}}"
RUNNER_NAME="${GITHUB_RUNNER_NAME:-shipmate-staging-runner}"
RUNNER_LABELS="${GITHUB_RUNNER_LABELS:-shipmate-staging,self-hosted,linux,x64}"
ENV_FILE="${ENV_FILE:-shipmate-infra/.env.github-runner}"
REPORT_DIR="${REPORT_DIR:-shipmate-infra/reports/github-runner}"
REPORT_FILE="${REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-report.json}"
STAGE_REPORT_FILE="${STAGE_REPORT_FILE:-${REPORT_DIR}/self-hosted-runner-create-env-report.json}"

write_report() {
  local status="$1"
  local reason="$2"
  mkdir -p "${REPORT_DIR}"
  cat > "${REPORT_FILE}" <<REPORT
{
  "status": "${status}",
  "stage": "create-env",
  "reason": "${reason}",
  "containerName": "shipmate-github-runner",
  "containerStatus": "-",
  "envFile": "${ENV_FILE}",
  "runnerLabel": "${RUNNER_LABELS}",
  "finishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
REPORT
  cp "${REPORT_FILE}" "${STAGE_REPORT_FILE}"
}

classify_gh_error() {
  local error_text="$1"
  if printf "%s" "${error_text}" | grep -Eiq 'connect|connection|network|Could not resolve|resolve|timeout|timed out|internet'; then
    printf "%s" "github-network-unavailable"
    return
  fi
  if printf "%s" "${error_text}" | grep -Eiq '401|403|authentication|auth|credential|permission|Resource not accessible|Bad credentials'; then
    printf "%s" "github-auth-failed"
    return
  fi
  if printf "%s" "${error_text}" | grep -Eiq '404|Not Found|repository'; then
    printf "%s" "github-repository-not-found"
    return
  fi
  printf "%s" "github-token-request-failed"
}

if ! command -v gh >/dev/null 2>&1; then
  write_report "failed" "gh-cli-missing"
  cat >&2 <<'MESSAGE'
gh CLI가 필요합니다.

설치 후 다음을 먼저 실행합니다.

gh auth login
MESSAGE
  exit 1
fi

gh_error_file="$(mktemp)"
if ! token="$(gh api --method POST "repos/${REPOSITORY}/actions/runners/registration-token" --jq .token 2>"${gh_error_file}")"; then
  gh_error="$(cat "${gh_error_file}")"
  rm -f "${gh_error_file}"
  reason="$(classify_gh_error "${gh_error}")"
  write_report "failed" "${reason}"
  printf "%s\n" "${gh_error}" >&2
  cat >&2 <<MESSAGE
GitHub runner 등록 token 발급 요청이 실패했습니다.

확인할 항목:
- gh auth status
- gh token에 repo 또는 admin:repo_hook 수준의 저장소 관리 권한이 있는지
- 저장소 경로: ${REPOSITORY}
MESSAGE
  exit 1
fi
rm -f "${gh_error_file}"

if [[ -z "${token}" || "${token}" == "null" ]]; then
  write_report "failed" "github-token-empty"
  echo "GitHub runner 등록 token을 발급하지 못했습니다." >&2
  exit 1
fi

cat > "${ENV_FILE}" <<ENV
GITHUB_REPOSITORY_URL=${REPOSITORY_URL}
GITHUB_RUNNER_TOKEN=${token}
GITHUB_RUNNER_NAME=${RUNNER_NAME}
GITHUB_RUNNER_LABELS=${RUNNER_LABELS}
ENV

chmod 600 "${ENV_FILE}"
write_report "env-created" "-"
echo "env=${ENV_FILE}"
echo "report=${REPORT_FILE}"
echo "runner_label=${RUNNER_LABELS}"
echo "다음 명령으로 runner를 실행합니다: bash shipmate-infra/scripts/run-github-self-hosted-runner.sh"
