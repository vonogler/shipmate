#!/usr/bin/env bash
set -euo pipefail

HOST="${HOST:-127.0.0.1}"

check_port() {
  local label="$1"
  local port="$2"
  if timeout 1 bash -c ":</dev/tcp/${HOST}/${port}" >/dev/null 2>&1; then
    printf '사용 중: %-28s %s:%s\n' "$label" "$HOST" "$port"
  else
    printf '비어 있음: %-28s %s:%s\n' "$label" "$HOST" "$port"
  fi
}

echo "[로컬 기본 포트]"
check_port "local mysql" "${SHIPMATE_MYSQL_HOST_PORT:-3306}"
check_port "local api" "${SHIPMATE_API_HOST_PORT:-8080}"
check_port "local merchant" "${SHIPMATE_MERCHANT_HOST_PORT:-3001}"

echo
echo "[스테이징 기본 포트]"
check_port "staging mysql" "${SHIPMATE_STAGING_MYSQL_HOST_PORT:-13306}"
check_port "staging api" "${SHIPMATE_STAGING_API_HOST_PORT:-18080}"
check_port "staging admin" "${SHIPMATE_STAGING_ADMIN_HOST_PORT:-13000}"
check_port "staging merchant" "${SHIPMATE_STAGING_MERCHANT_HOST_PORT:-13001}"
check_port "staging nginx http" "${SHIPMATE_STAGING_HTTP_HOST_PORT:-80}"
check_port "staging nginx https" "${SHIPMATE_STAGING_HTTPS_HOST_PORT:-443}"

echo
echo "포트가 충돌하면 .env를 바꾸기 전에 실행 시점 환경 변수로 host 포트만 조정한다."
