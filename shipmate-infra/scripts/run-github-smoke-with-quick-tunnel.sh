#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_FILE="${WORKFLOW_FILE:-staging-smoke.yml}"
REF_NAME="${REF_NAME:-$(git branch --show-current 2>/dev/null || true)}"
TUNNEL_CONTAINER="${TUNNEL_CONTAINER:-shipmate-staging-api-tunnel}"
TUNNEL_IMAGE="${TUNNEL_IMAGE:-cloudflare/cloudflared:latest}"
TUNNEL_NETWORK="${TUNNEL_NETWORK:-shipmate-staging_default}"
TUNNEL_TARGET="${TUNNEL_TARGET:-http://shipmate-api:8080}"
TUNNEL_WAIT_SECONDS="${TUNNEL_WAIT_SECONDS:-90}"
KEEP_TUNNEL="${KEEP_TUNNEL:-false}"
CLEANUP_SECRET="${CLEANUP_SECRET:-true}"
DOCKER_MODE="${DOCKER_MODE:-auto}"
ALLOW_OVERWRITE_SECRET="${ALLOW_OVERWRITE_SECRET:-false}"
QUICK_TUNNEL_REPORT_DIR="${QUICK_TUNNEL_REPORT_DIR:-shipmate-infra/reports/quick-tunnel}"
QUICK_TUNNEL_FORCE_WORKFLOW_FAILURE="${QUICK_TUNNEL_FORCE_WORKFLOW_FAILURE:-false}"
secret_created_by_script="false"
failed_log_file=""

if [[ -z "${REF_NAME}" ]]; then
  REF_NAME="main"
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 설치되어 있지 않습니다."
  exit 127
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh auth status 확인은 실패했습니다. 이미 인증했다면 실제 명령을 계속 시도합니다." >&2
fi

powershell_docker() {
  local script="& 'C:\Program Files\Docker\Docker\resources\bin\docker.exe'"
  local arg escaped
  for arg in "$@"; do
    escaped="${arg//\'/\'\'}"
    script+=" '${escaped}'"
  done
  powershell.exe -NoProfile -Command "${script}"
}

docker_cmd() {
  case "${DOCKER_MODE}" in
    linux)
      docker "$@"
      ;;
    windows)
      powershell_docker "$@"
      ;;
    auto)
      if docker version >/dev/null 2>&1; then
        docker "$@"
      else
        powershell_docker "$@"
      fi
      ;;
    *)
      echo "지원하지 않는 DOCKER_MODE입니다: ${DOCKER_MODE}" >&2
      exit 2
      ;;
  esac
}

print_tunnel_diagnostics() {
  local reason="$1"
  echo "quickTunnelDiagnosticReason=${reason}" >&2
  echo "dockerMode=${DOCKER_MODE}" >&2
  echo "tunnelContainer=${TUNNEL_CONTAINER}" >&2
  echo "tunnelNetwork=${TUNNEL_NETWORK}" >&2
  echo "tunnelTarget=${TUNNEL_TARGET}" >&2
  echo "[diagnostic] docker container 상태" >&2
  docker_cmd ps -a --filter "name=${TUNNEL_CONTAINER}" >&2 || true
  echo "[diagnostic] docker network 확인" >&2
  docker_cmd network inspect "${TUNNEL_NETWORK}" --format '{{.Name}} {{range $name, $_ := .Containers}}{{$name}} {{end}}' >&2 || true
  echo "[diagnostic] tunnel 로그 tail" >&2
  docker_cmd logs --tail 120 "${TUNNEL_CONTAINER}" >&2 || true
  cat >&2 <<MESSAGE
조치 기준:
- tunnelNetwork에 shipmate-api와 tunnel 컨테이너가 함께 있어야 합니다.
- tunnelTarget은 self-hosted runner 내부 주소가 아니라 tunnel 컨테이너가 접근할 Docker network 주소여야 합니다.
- GitHub hosted runner 검증은 trycloudflare/ngrok/공인 HTTPS URL만 사용할 수 있습니다.
- Docker Desktop WSL integration이 끊겼으면 bash shipmate-infra/scripts/check-wsl-docker.sh로 먼저 확인합니다.
MESSAGE
}

write_quick_tunnel_report() {
  local status="$1"
  local run_id_value="$2"
  local run_url_value="$3"
  local workflow_exit_value="$4"
  local artifact_download_status="$5"
  local reason="$6"
  mkdir -p "${QUICK_TUNNEL_REPORT_DIR}"
  {
    echo "status=${status}"
    echo "reason=${reason}"
    echo "workflowFile=${WORKFLOW_FILE}"
    echo "refName=${REF_NAME}"
    echo "runId=${run_id_value:-"-"}"
    echo "runUrl=${run_url_value:-"-"}"
    echo "workflowExit=${workflow_exit_value}"
    echo "artifactDownloadStatus=${artifact_download_status}"
    echo "failedLogFile=${failed_log_file:-"-"}"
    echo "finishedAt=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  } > "${QUICK_TUNNEL_REPORT_DIR}/quick-tunnel-workflow-report.txt"
}

capture_failed_workflow_log() {
  local run_id_value="$1"
  if [[ -z "${run_id_value}" || -n "${failed_log_file}" ]]; then
    return 0
  fi
  mkdir -p "${QUICK_TUNNEL_REPORT_DIR}"
  failed_log_file="${QUICK_TUNNEL_REPORT_DIR}/quick-tunnel-workflow-${run_id_value}-failed-log.txt"
  if ! gh run view "${run_id_value}" --log-failed > "${failed_log_file}" 2>&1; then
    echo "gh run view --log-failed 출력 저장에 실패했습니다: ${failed_log_file}" >&2
  else
    echo "failedLogFile=${failed_log_file}"
  fi
}

cleanup() {
  local exit_code=$?
  if [[ "${KEEP_TUNNEL}" != "true" ]]; then
    docker_cmd rm -f "${TUNNEL_CONTAINER}" >/dev/null 2>&1 || true
  fi
  if [[ "${CLEANUP_SECRET}" == "true" && "${secret_created_by_script}" == "true" ]]; then
    GH_PROMPT_DISABLED=1 gh secret delete STAGING_API_BASE_URL >/dev/null 2>&1 || true
  fi
  exit "${exit_code}"
}
trap cleanup EXIT

echo "tunnelContainer=${TUNNEL_CONTAINER}"
echo "tunnelNetwork=${TUNNEL_NETWORK}"
echo "tunnelTarget=${TUNNEL_TARGET}"
docker_cmd rm -f "${TUNNEL_CONTAINER}" >/dev/null 2>&1 || true
docker_cmd run -d \
  --name "${TUNNEL_CONTAINER}" \
  --network "${TUNNEL_NETWORK}" \
  "${TUNNEL_IMAGE}" \
  tunnel --url "${TUNNEL_TARGET}" --no-autoupdate >/dev/null

tunnel_url=""
elapsed=0
while (( elapsed <= TUNNEL_WAIT_SECONDS )); do
  tunnel_logs="$(docker_cmd logs "${TUNNEL_CONTAINER}" 2>&1 || true)"
  tunnel_url="$(printf "%s\n" "${tunnel_logs}" | sed -n 's#.*\(https://[a-zA-Z0-9.-]*\.trycloudflare\.com\).*#\1#p' | head -n 1)"
  if [[ -n "${tunnel_url}" ]]; then
    break
  fi
  sleep 2
  elapsed=$((elapsed + 2))
done

if [[ -z "${tunnel_url}" ]]; then
  echo "Cloudflare quick tunnel URL을 찾지 못했습니다." >&2
  print_tunnel_diagnostics "tunnel-url-not-found"
  exit 1
fi

echo "tunnelUrl=${tunnel_url}"
export TEMPORARY_HTTPS_URL="${tunnel_url}"
health_elapsed=0
until curl -fsS "${tunnel_url}/actuator/health" >/dev/null; do
  if (( health_elapsed >= TUNNEL_WAIT_SECONDS )); then
    echo "Cloudflare quick tunnel health 확인에 실패했습니다: ${tunnel_url}/actuator/health" >&2
    print_tunnel_diagnostics "tunnel-health-failed"
    exit 1
  fi
  sleep 2
  health_elapsed=$((health_elapsed + 2))
done

if gh secret list | awk '{print $1}' | grep -qx 'STAGING_API_BASE_URL'; then
  if [[ "${ALLOW_OVERWRITE_SECRET}" != "true" ]]; then
    echo "기존 STAGING_API_BASE_URL secret이 있습니다." >&2
    echo "임시 tunnel URL로 덮어쓴 뒤 정리하려면 ALLOW_OVERWRITE_SECRET=true를 지정하세요." >&2
    exit 1
  fi
fi
STAGING_API_BASE_URL="${tunnel_url}" bash shipmate-infra/scripts/set-github-staging-smoke-secrets.sh
secret_created_by_script="true"

set +e
if [[ "${QUICK_TUNNEL_FORCE_WORKFLOW_FAILURE}" == "true" ]]; then
  workflow_output="$(API_BASE_URL="https://127.0.0.1:1" WORKFLOW_FILE="${WORKFLOW_FILE}" REF_NAME="${REF_NAME}" bash shipmate-infra/scripts/run-github-smoke-workflow.sh)"
else
  workflow_output="$(WORKFLOW_FILE="${WORKFLOW_FILE}" REF_NAME="${REF_NAME}" bash shipmate-infra/scripts/run-github-smoke-workflow.sh)"
fi
workflow_exit=$?
set -e
printf "%s\n" "${workflow_output}"
run_id="$(printf "%s\n" "${workflow_output}" | sed -n 's/.*runId=\([0-9][0-9]*\).*/\1/p' | tail -n 1)"
run_url="$(printf "%s\n" "${workflow_output}" | sed -n 's/.*url=\(https:\/\/[^[:space:]]*\).*/\1/p' | tail -n 1)"

if [[ -z "${run_id}" ]]; then
  echo "workflow run ID를 찾지 못했습니다." >&2
  echo "GitHub Actions 화면에서 ${WORKFLOW_FILE} 최근 실행을 확인하세요." >&2
  exit 1
fi

artifact_download_status="ok"
if ! RUN_ID="${run_id}" bash shipmate-infra/scripts/download-github-smoke-reports.sh; then
  artifact_download_status="failed"
  capture_failed_workflow_log "${run_id}"
  write_quick_tunnel_report "failed" "${run_id}" "${run_url}" "${workflow_exit}" "${artifact_download_status}" "artifact-download-failed"
  cat >&2 <<MESSAGE
workflow artifact 자동 다운로드가 실패했습니다.

수동 확인:
${run_url:-gh run view ${run_id} --web}
gh run view ${run_id} --web
gh run view ${run_id} --log-failed
RUN_ID=${run_id} bash shipmate-infra/scripts/download-github-smoke-reports.sh
MESSAGE
fi

if (( workflow_exit != 0 )); then
  capture_failed_workflow_log "${run_id}"
  write_quick_tunnel_report "failed" "${run_id}" "${run_url}" "${workflow_exit}" "${artifact_download_status}" "workflow-failed"
  echo "workflow가 실패했습니다. runId=${run_id} runUrl=${run_url:-"-"}" >&2
  exit "${workflow_exit}"
fi

write_quick_tunnel_report "ok" "${run_id}" "${run_url}" "${workflow_exit}" "${artifact_download_status}" "workflow-succeeded"
