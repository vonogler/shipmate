#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:18080}"
LOGIN_ID="${SHIPMATE_DEFAULT_LOGIN_ID:-shipmate}"
PASSWORD="${SHIPMATE_DEFAULT_PASSWORD:-shipmate}"
DISPLAY_NAME="${SHIPMATE_DEFAULT_DISPLAY_NAME:-ShipMate 관리자}"
ROLE="${SHIPMATE_DEFAULT_ROLE:-ADMIN}"

login() {
  curl -sS -i -X POST "${API_BASE_URL}/api/admin/auth/login" \
    -H 'Content-Type: application/json' \
    --data "{\"loginId\":\"${LOGIN_ID}\",\"password\":\"${PASSWORD}\"}"
}

register() {
  curl -sS -i -X POST "${API_BASE_URL}/api/admin/auth/register" \
    -H 'Content-Type: application/json' \
    --data "{\"loginId\":\"${LOGIN_ID}\",\"password\":\"${PASSWORD}\",\"displayName\":\"${DISPLAY_NAME}\",\"role\":\"${ROLE}\"}"
}

login_response="$(login)"
if printf '%s' "$login_response" | grep -q '^HTTP/.* 200'; then
  echo "기본 계정 로그인 확인: ${LOGIN_ID}"
  exit 0
fi

register_response="$(register)"
if printf '%s' "$register_response" | grep -q '^HTTP/.* 200'; then
  echo "기본 계정 생성 완료: ${LOGIN_ID}"
  exit 0
fi

if printf '%s' "$register_response" | grep -q 'SM-AUT-8001'; then
  echo "기본 계정은 존재하지만 로그인에 실패했습니다. 비밀번호를 확인하세요: ${LOGIN_ID}" >&2
  exit 1
fi

echo "$register_response" >&2
exit 1
