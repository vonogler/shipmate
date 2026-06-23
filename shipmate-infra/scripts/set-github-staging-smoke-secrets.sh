#!/usr/bin/env bash
set -euo pipefail

STAGING_API_BASE_URL="${STAGING_API_BASE_URL:-}"
STAGING_HTTPS_URL="${STAGING_HTTPS_URL:-}"
STAGING_API_HEALTH_URL="${STAGING_API_HEALTH_URL:-}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-}"
STAGING_ALLOW_INSECURE_TLS="${STAGING_ALLOW_INSECURE_TLS:-}"
ALLOW_LOCALHOST_SECRET="${ALLOW_LOCALHOST_SECRET:-false}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI가 설치되어 있지 않습니다."
  exit 127
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh auth status 확인은 실패했습니다. 이미 인증했다면 secret 설정을 계속 시도합니다." >&2
fi

is_local_url() {
  case "$1" in
    http://localhost*|https://localhost*|http://127.*|https://127.*|http://0.0.0.0*|https://0.0.0.0*|http://shipmate-*|https://shipmate-*|http://*.docker.internal*|https://*.docker.internal*)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

set_secret_if_present() {
  local name="$1"
  local value="$2"
  if [[ -z "${value}" ]]; then
    return
  fi
  gh secret set "${name}" --body "${value}"
  echo "secret=${name} 설정 완료"
}

if [[ -z "${STAGING_API_BASE_URL}${STAGING_HTTPS_URL}${STAGING_API_HEALTH_URL}${STAGING_RESOLVE_IP}${STAGING_ALLOW_INSECURE_TLS}" ]]; then
  cat >&2 <<'USAGE'
설정할 secret 값이 없습니다.

예:
STAGING_API_BASE_URL=https://temporary-host.example \
bash shipmate-infra/scripts/set-github-staging-smoke-secrets.sh

선택:
STAGING_HTTPS_URL=https://temporary-host.example/merchant
STAGING_API_HEALTH_URL=https://temporary-host.example/actuator/health
STAGING_RESOLVE_IP=203.0.113.10
STAGING_ALLOW_INSECURE_TLS=true
USAGE
  exit 1
fi

if [[ "${ALLOW_LOCALHOST_SECRET}" != "true" ]]; then
  for value in "${STAGING_API_BASE_URL}" "${STAGING_HTTPS_URL}" "${STAGING_API_HEALTH_URL}"; do
    if [[ -n "${value}" ]] && is_local_url "${value}"; then
      echo "GitHub hosted runner는 localhost/127.0.0.1/Docker 내부 URL에 접근할 수 없습니다: ${value}" >&2
      echo "터널 URL을 쓰거나 self-hosted runner를 사용할 때만 ALLOW_LOCALHOST_SECRET=true로 우회하세요." >&2
      exit 1
    fi
  done
fi

set_secret_if_present STAGING_API_BASE_URL "${STAGING_API_BASE_URL}"
set_secret_if_present STAGING_HTTPS_URL "${STAGING_HTTPS_URL}"
set_secret_if_present STAGING_API_HEALTH_URL "${STAGING_API_HEALTH_URL}"
set_secret_if_present STAGING_RESOLVE_IP "${STAGING_RESOLVE_IP}"
set_secret_if_present STAGING_ALLOW_INSECURE_TLS "${STAGING_ALLOW_INSECURE_TLS}"

echo "GitHub staging smoke secret 설정이 끝났습니다."
