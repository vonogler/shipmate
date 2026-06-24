#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

API_BASE_URL="${API_BASE_URL:-http://localhost:${SHIPMATE_STAGING_API_HOST_PORT:-18080}}"
STAGING_HTTPS_URL="${STAGING_HTTPS_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/merchant/}"
API_HEALTH_URL="${API_HEALTH_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/actuator/health}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-127.0.0.1}"
ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-true}"
SMOKE_CLEANUP_DRY_RUN="${SMOKE_CLEANUP_DRY_RUN:-true}"

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
else
  echo "[5/6] smoke 테스트 데이터 cleanup dry-run 건너뜀"
fi

echo "[6/6] 점검 요약 생성"
bash shipmate-infra/scripts/summarize-staging-reports.sh

echo "staging-smoke-suite=ok"
