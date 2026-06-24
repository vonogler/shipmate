#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

API_BASE_URL="${API_BASE_URL:-http://localhost:${SHIPMATE_STAGING_API_HOST_PORT:-18080}}"
STAGING_DOMAIN="${SHIPMATE_STAGING_DOMAIN:-staging-shipmate.com}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-127.0.0.1}"

echo "[1/4] Docker 컨테이너"
docker ps --filter "name=shipmate-staging" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "[2/4] API health"
curl -fsS "${API_BASE_URL}/actuator/health"
echo

echo "[3/4] Admin HTTPS"
curl -k -fsS -I --resolve "${STAGING_DOMAIN}:443:${STAGING_RESOLVE_IP}" "https://${STAGING_DOMAIN}/admin/" | sed -n '1,6p'

echo "[4/4] Merchant HTTPS"
curl -k -fsS -I --resolve "${STAGING_DOMAIN}:443:${STAGING_RESOLVE_IP}" "https://${STAGING_DOMAIN}/merchant/" | sed -n '1,6p'

echo "dnsless-access=ok"
