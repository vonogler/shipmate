#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

API_BASE_URL="${API_BASE_URL:-http://localhost:${SHIPMATE_STAGING_API_HOST_PORT:-18080}}"

until curl -fsS "${API_BASE_URL}/actuator/health" >/dev/null; do
  echo "스테이징 API 준비 대기: ${API_BASE_URL}"
  sleep 2
done

API_BASE_URL="${API_BASE_URL}" bash shipmate-infra/scripts/seed-staging-default-account.sh
