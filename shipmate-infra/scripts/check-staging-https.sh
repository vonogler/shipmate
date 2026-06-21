#!/usr/bin/env bash
set -euo pipefail

STAGING_HOST="${STAGING_HOST:-staging-shipmate.com}"
MERCHANT_PATH="${MERCHANT_PATH:-/merchant/}"
HTTPS_URL="${STAGING_HTTPS_URL:-https://${STAGING_HOST}${MERCHANT_PATH}}"
API_HEALTH_URL="${API_HEALTH_URL:-https://${STAGING_HOST}/actuator/health}"
RESOLVE_IP="${STAGING_RESOLVE_IP:-}"
ALLOW_INSECURE_TLS="${ALLOW_INSECURE_TLS:-false}"
HTTPS_CHECK_MODE="${HTTPS_CHECK_MODE:-direct}"
HTTPS_REPORT_FILE="${HTTPS_REPORT_FILE:-shipmate-infra/staging-https-report.json}"
HTTPS_JUNIT_FILE="${HTTPS_JUNIT_FILE:-shipmate-infra/staging-https-report.xml}"
HTTPS_REPORT_DIR="${HTTPS_REPORT_DIR:-shipmate-infra/reports/https}"
STAMP="$(date +%s)"
current_step="준비"
https_status="failed"

json_escape() {
  printf "%s" "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

xml_escape() {
  printf "%s" "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&apos;/g'
}

write_report() {
  local finished_at
  local failures
  finished_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  failures="1"
  if [[ "${https_status}" == "ok" ]]; then
    failures="0"
  fi
  mkdir -p "$(dirname "${HTTPS_REPORT_FILE}")" "$(dirname "${HTTPS_JUNIT_FILE}")" "${HTTPS_REPORT_DIR}"
  cat > "${HTTPS_REPORT_FILE}" <<REPORT
{
  "status": "${https_status}",
  "stagingHost": "$(json_escape "${STAGING_HOST}")",
  "merchantUrl": "$(json_escape "${HTTPS_URL}")",
  "apiHealthUrl": "$(json_escape "${API_HEALTH_URL}")",
  "resolveIp": "$(json_escape "${RESOLVE_IP}")",
  "httpsCheckMode": "$(json_escape "${HTTPS_CHECK_MODE}")",
  "allowInsecureTls": "${ALLOW_INSECURE_TLS}",
  "lastStep": "$(json_escape "${current_step}")",
  "finishedAt": "${finished_at}"
}
REPORT
  cat > "${HTTPS_JUNIT_FILE}" <<REPORT
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="shipmate-staging-https" tests="1" failures="${failures}" errors="0" skipped="0" timestamp="${finished_at}">
  <testcase classname="shipmate.staging" name="DNS 없는 HTTPS 확인">
REPORT
  if [[ "${https_status}" != "ok" ]]; then
    cat >> "${HTTPS_JUNIT_FILE}" <<REPORT
    <failure message="$(xml_escape "${current_step}")">마지막 단계: $(xml_escape "${current_step}")</failure>
REPORT
  fi
  cat >> "${HTTPS_JUNIT_FILE}" <<REPORT
    <system-out>stagingHost=$(xml_escape "${STAGING_HOST}")
merchantUrl=$(xml_escape "${HTTPS_URL}")
apiHealthUrl=$(xml_escape "${API_HEALTH_URL}")
resolveIp=$(xml_escape "${RESOLVE_IP}")
httpsCheckMode=$(xml_escape "${HTTPS_CHECK_MODE}")</system-out>
  </testcase>
</testsuite>
REPORT
  cp "${HTTPS_REPORT_FILE}" "${HTTPS_REPORT_DIR}/staging-https-report-${STAMP}.json"
  cp "${HTTPS_JUNIT_FILE}" "${HTTPS_REPORT_DIR}/staging-https-report-${STAMP}.xml"
}

trap write_report EXIT

curl_args=(-fsSI --max-time 15)
health_args=(-fsS --max-time 15)

if [[ "${ALLOW_INSECURE_TLS}" == "true" ]]; then
  curl_args+=(-k)
  health_args+=(-k)
fi

if [[ -n "${RESOLVE_IP}" ]]; then
  curl_args+=(--resolve "${STAGING_HOST}:443:${RESOLVE_IP}")
  health_args+=(--resolve "${STAGING_HOST}:443:${RESOLVE_IP}")
fi

current_step="[1/2] Merchant HTTPS"
echo "${current_step}"
curl "${curl_args[@]}" "${HTTPS_URL}"

current_step="[2/2] API health HTTPS"
echo "${current_step}"
curl "${health_args[@]}" "${API_HEALTH_URL}"
echo
https_status="ok"
echo "staging_https=ok"
echo "report=${HTTPS_REPORT_FILE}"
echo "junit=${HTTPS_JUNIT_FILE}"
