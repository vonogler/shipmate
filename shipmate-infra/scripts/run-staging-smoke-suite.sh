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

echo "[1/6] 스테이징 컨테이너 상태 확인"
docker ps --filter "name=shipmate-staging" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
curl -fsS "${API_BASE_URL}/actuator/health" >/dev/null

echo "[2/6] 기본 계정 확인"
bash shipmate-infra/scripts/check-staging-default-account.sh

echo "[3/6] DNS 없는 HTTPS 확인"
bash shipmate-infra/scripts/check-staging-https.sh

echo "[4/6] 스테이징 smoke"
SMOKE_SEED_DEFAULT_ACCOUNT=true bash shipmate-infra/scripts/smoke-staging.sh
if docker ps --format "{{.Names}}" | grep -qx "shipmate-staging-api"; then
  docker exec shipmate-staging-api mkdir -p /workspace/shipmate-infra
  docker cp shipmate-infra/smoke-report.json shipmate-staging-api:/workspace/shipmate-infra/smoke-report.json
fi

if [[ "${SMOKE_CLEANUP_DRY_RUN}" == "true" ]]; then
  echo "[5/6] smoke 테스트 데이터 cleanup dry-run"
  bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
elif [[ "${SMOKE_CLEANUP_APPLY_AFTER}" == "true" ]]; then
  echo "[5/6] smoke 테스트 데이터 cleanup apply"
  SMOKE_CLEANUP_APPLY=true \
  SMOKE_CLEANUP_CONFIRM='merchant_smoke_%' \
  bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh
else
  echo "[5/6] smoke 테스트 데이터 cleanup 건너뜀"
fi

echo "[6/6] 점검 요약 생성"
bash shipmate-infra/scripts/summarize-staging-reports.sh
if docker ps --format "{{.Names}}" | grep -qx "shipmate-staging-api"; then
  docker exec shipmate-staging-api mkdir -p /workspace/shipmate-infra/reports
  docker cp shipmate-infra/reports/staging-check-summary.html shipmate-staging-api:/workspace/shipmate-infra/reports/staging-check-summary.html
fi

if [[ "${RUN_UI_CHECK}" == "true" ]]; then
  echo "[옵션] Admin/Merchant Playwright UI 확인"
  (
    cd shipmate-infra
    ADMIN_BASE_URL="${ADMIN_BASE_URL:-https://127.0.0.1/admin/}" \
    MERCHANT_BASE_URL="${MERCHANT_BASE_URL:-https://127.0.0.1/merchant/}" \
    npm run check:ui
  )
fi

echo "staging-smoke-suite=ok"
