#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

MODE="${MODE:-resolve}"
STAGING_HOST="${STAGING_HOST:-staging-shipmate.com}"
STAGING_RESOLVE_IP="${STAGING_RESOLVE_IP:-127.0.0.1}"
ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-}"
TEMPORARY_HTTPS_URL="${TEMPORARY_HTTPS_URL:-${STAGING_TUNNEL_URL:-}}"

case "${MODE}" in
  resolve)
    export STAGING_HOST
    export STAGING_RESOLVE_IP
    export ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-true}"
    export HTTPS_CHECK_MODE="dnsless-resolve"
    bash shipmate-infra/scripts/check-staging-https.sh
    ;;
  tunnel)
    : "${TEMPORARY_HTTPS_URL:?MODE=tunnel 에서는 TEMPORARY_HTTPS_URL이 필요합니다. 예: https://temporary-host.trycloudflare.com}"
    export STAGING_HTTPS_URL="${TEMPORARY_HTTPS_URL%/}/merchant/"
    export API_HEALTH_URL="${TEMPORARY_HTTPS_URL%/}/actuator/health"
    export ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-false}"
    export HTTPS_CHECK_MODE="dnsless-tunnel"
    bash shipmate-infra/scripts/check-staging-https.sh
    ;;
  hosts)
    cat <<MESSAGE
hosts 방식은 관리자 권한으로 OS hosts 파일에 아래 줄을 추가한 뒤 실행합니다.

127.0.0.1 ${STAGING_HOST}

Windows:
  notepad C:\\Windows\\System32\\drivers\\etc\\hosts

추가 후:
  MODE=hosts bash shipmate-infra/scripts/check-staging-dnsless-https.sh
MESSAGE
    export STAGING_HOST
    export STAGING_RESOLVE_IP=""
    export ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-true}"
    export HTTPS_CHECK_MODE="dnsless-hosts"
    bash shipmate-infra/scripts/check-staging-https.sh
    ;;
  *)
    cat >&2 <<MESSAGE
지원하지 않는 MODE입니다: ${MODE}

사용 가능한 값:
- resolve: hosts 파일 수정 없이 curl --resolve로 127.0.0.1에 매핑
- hosts: OS hosts 파일에 staging host를 등록한 뒤 확인
- tunnel: Cloudflare/ngrok 임시 HTTPS URL 확인
MESSAGE
    exit 1
    ;;
esac
