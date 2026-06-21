#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

API_BASE_URL="${API_BASE_URL:-http://localhost:${SHIPMATE_STAGING_API_HOST_PORT:-18080}}"
STAGING_HTTPS_URL="${STAGING_HTTPS_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/merchant/}"
API_HEALTH_URL="${API_HEALTH_URL:-https://${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}/actuator/health}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-127.0.0.1}"
ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-true}"

export API_BASE_URL
export STAGING_HTTPS_URL
export API_HEALTH_URL
export STAGING_RESOLVE_IP
export ALLOW_INSECURE_TLS

echo "[1/4] 기본 계정 확인"
bash shipmate-infra/scripts/check-staging-default-account.sh

echo "[2/4] DNS 없는 HTTPS 확인"
bash shipmate-infra/scripts/check-staging-https.sh

echo "[3/4] 스테이징 smoke"
SMOKE_SEED_DEFAULT_ACCOUNT=true bash shipmate-infra/scripts/smoke-staging.sh

echo "[4/4] 점검 요약 생성"
bash shipmate-infra/scripts/summarize-staging-reports.sh

echo "staging-check=ok"
