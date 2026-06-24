#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

API_BASE_URL="${API_BASE_URL:-http://localhost:${SHIPMATE_STAGING_API_HOST_PORT:-18080}}"
STAGING_HTTPS_URL="${STAGING_HTTPS_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/merchant/}"
API_HEALTH_URL="${API_HEALTH_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/actuator/health}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-127.0.0.1}"
ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-true}"
SMOKE_CLEANUP_DRY_RUN="${SMOKE_CLEANUP_DRY_RUN:-true}"
SMOKE_CLEANUP_APPLY_AFTER="${SMOKE_CLEANUP_APPLY_AFTER:-false}"
RUN_UI_CHECK="${RUN_UI_CHECK:-false}"
SUITE_SUMMARY_FILE="${SUITE_SUMMARY_FILE:-shipmate-infra/reports/staging-smoke-suite-summary.json}"
ui_check_status="skipped"
ui_check_detail="-"
current_step="준비"
suite_finalized="false"

for arg in "$@"; do
  case "${arg}" in
    --cleanup-apply)
      SMOKE_CLEANUP_DRY_RUN=false
      SMOKE_CLEANUP_APPLY_AFTER=true
      ;;
    --check-ui)
      RUN_UI_CHECK=true
      ;;
    --skip-cleanup-dry-run)
      SMOKE_CLEANUP_DRY_RUN=false
      ;;
    *)
      echo "알 수 없는 옵션: ${arg}" >&2
      echo "사용법: bash shipmate-infra/scripts/run-staging-smoke-suite.sh [--cleanup-apply] [--check-ui] [--skip-cleanup-dry-run]" >&2
      exit 2
      ;;
  esac
done

export API_BASE_URL
export STAGING_HTTPS_URL
export API_HEALTH_URL
export STAGING_RESOLVE_IP
export ALLOW_INSECURE_TLS

json_escape() {
  printf "%s" "$1" | tr '\r\n\t' '   ' | sed 's/\\/\\\\/g; s/"/\\"/g'
}

json_value() {
  local file_path="$1"
  local key="$2"
  if [[ ! -f "${file_path}" ]]; then
    printf "%s" ""
    return
  fi
  sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "${file_path}" | head -n 1
}

write_suite_summary() {
  local suite_status="$1"
  local suite_reason="$2"
  local smoke_status
  local smoke_finished_at
  local cleanup_status
  local cleanup_mode
  local cleanup_remaining_rows
  local cleanup_finished_at
  local previous_success_at
  local previous_failure_at
  local latest_success_at
  local latest_failure_at
  smoke_status="$(json_value shipmate-infra/smoke-report.json status)"
  smoke_finished_at="$(json_value shipmate-infra/smoke-report.json finishedAt)"
  cleanup_status="$(json_value shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json status)"
  cleanup_mode="$(json_value shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json mode)"
  cleanup_remaining_rows="$(json_value shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json remainingRows)"
  cleanup_finished_at="$(json_value shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json finishedAt)"
  previous_success_at="$(json_value "${SUITE_SUMMARY_FILE}" latestSmokeSuccessAt)"
  previous_failure_at="$(json_value "${SUITE_SUMMARY_FILE}" latestSmokeFailureAt)"
  latest_success_at="${previous_success_at}"
  latest_failure_at="${previous_failure_at}"
  if [[ "${smoke_status}" == "ok" ]]; then
    latest_success_at="${smoke_finished_at}"
  elif [[ -n "${smoke_status}" ]]; then
    latest_failure_at="${smoke_finished_at}"
  fi
  mkdir -p "$(dirname "${SUITE_SUMMARY_FILE}")"
  printf '{"status":"%s","reason":"%s","smokeStatus":"%s","latestSmokeSuccessAt":"%s","latestSmokeFailureAt":"%s","cleanupStatus":"%s","cleanupMode":"%s","cleanupRemainingRows":"%s","cleanupFinishedAt":"%s","uiCheckStatus":"%s","uiCheckDetail":"%s","finishedAt":"%s"}\n' \
    "$(json_escape "${suite_status}")" \
    "$(json_escape "${suite_reason}")" \
    "$(json_escape "${smoke_status}")" \
    "$(json_escape "${latest_success_at}")" \
    "$(json_escape "${latest_failure_at}")" \
    "$(json_escape "${cleanup_status}")" \
    "$(json_escape "${cleanup_mode}")" \
    "$(json_escape "${cleanup_remaining_rows}")" \
    "$(json_escape "${cleanup_finished_at}")" \
    "$(json_escape "${ui_check_status}")" \
    "$(json_escape "${ui_check_detail}")" \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" > "${SUITE_SUMMARY_FILE}"
}

sync_reports_to_api() {
  if docker ps --format "{{.Names}}" | grep -qx "shipmate-staging-api"; then
    docker exec shipmate-staging-api mkdir -p /workspace/shipmate-infra/reports/cleanup >/dev/null 2>&1 || true
    [[ -f shipmate-infra/smoke-report.json ]] && docker cp shipmate-infra/smoke-report.json shipmate-staging-api:/workspace/shipmate-infra/smoke-report.json >/dev/null 2>&1 || true
    [[ -f shipmate-infra/reports/staging-check-summary.html ]] && docker cp shipmate-infra/reports/staging-check-summary.html shipmate-staging-api:/workspace/shipmate-infra/reports/staging-check-summary.html >/dev/null 2>&1 || true
    [[ -f "${SUITE_SUMMARY_FILE}" ]] && docker cp "${SUITE_SUMMARY_FILE}" shipmate-staging-api:/workspace/shipmate-infra/reports/staging-smoke-suite-summary.json >/dev/null 2>&1 || true
    [[ -f shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json ]] && docker cp shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json shipmate-staging-api:/workspace/shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json >/dev/null 2>&1 || true
  fi
}

finalize_on_exit() {
  local exit_code=$?
  if [[ "${suite_finalized}" == "true" ]]; then
    return
  fi
  if (( exit_code == 0 )); then
    write_suite_summary "ok" "-"
  else
    write_suite_summary "failed" "${current_step}"
  fi
  bash shipmate-infra/scripts/summarize-staging-reports.sh >/dev/null 2>&1 || true
  sync_reports_to_api
}

trap finalize_on_exit EXIT

current_step="[1/6] 스테이징 컨테이너 상태 확인"
echo "[1/6] 스테이징 컨테이너 상태 확인"
docker ps --filter "name=shipmate-staging" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
curl -fsS "${API_BASE_URL}/actuator/health" >/dev/null

current_step="[2/6] 기본 계정 확인"
echo "[2/6] 기본 계정 확인"
bash shipmate-infra/scripts/check-staging-default-account.sh

current_step="[3/6] DNS 없는 HTTPS 확인"
echo "[3/6] DNS 없는 HTTPS 확인"
bash shipmate-infra/scripts/check-staging-https.sh

current_step="[4/6] 스테이징 smoke"
echo "[4/6] 스테이징 smoke"
SMOKE_SEED_DEFAULT_ACCOUNT=true bash shipmate-infra/scripts/smoke-staging.sh
sync_reports_to_api

if [[ "${SMOKE_CLEANUP_DRY_RUN}" == "true" ]]; then
  current_step="[5/6] smoke 테스트 데이터 cleanup dry-run"
  echo "[5/6] smoke 테스트 데이터 cleanup dry-run"
  bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
elif [[ "${SMOKE_CLEANUP_APPLY_AFTER}" == "true" ]]; then
  current_step="[5/6] smoke 테스트 데이터 cleanup apply"
  echo "[5/6] smoke 테스트 데이터 cleanup apply"
  SMOKE_CLEANUP_APPLY=true \
  SMOKE_CLEANUP_CONFIRM='merchant_smoke_%' \
  bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
else
  echo "[5/6] smoke 테스트 데이터 cleanup 건너뜀"
fi

current_step="[6/6] 점검 요약 생성"
echo "[6/6] 점검 요약 생성"
bash shipmate-infra/scripts/summarize-staging-reports.sh
sync_reports_to_api

if [[ "${RUN_UI_CHECK}" == "true" ]]; then
  current_step="[옵션] Admin/Merchant Playwright UI 확인"
  echo "[옵션] Admin/Merchant Playwright UI 확인"
  set +e
  ui_check_output="$(
    cd shipmate-infra
    ADMIN_BASE_URL="${ADMIN_BASE_URL:-https://127.0.0.1/admin/}" \
    MERCHANT_BASE_URL="${MERCHANT_BASE_URL:-https://127.0.0.1/merchant/}" \
    npm run check:ui
  2>&1)"
  ui_check_exit=$?
  set -e
  printf "%s\n" "${ui_check_output}"
  if (( ui_check_exit != 0 )); then
    ui_check_status="failed"
    ui_check_detail="$(printf "%s" "${ui_check_output}" | tail -n 20)"
    exit "${ui_check_exit}"
  fi
  ui_check_status="ok"
  ui_check_detail="-"
fi

current_step="완료"
write_suite_summary "ok" "-"
bash shipmate-infra/scripts/summarize-staging-reports.sh
sync_reports_to_api
suite_finalized="true"

echo "staging-smoke-suite=ok"
