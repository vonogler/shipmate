#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:18080}"
STAMP="$(date +%s)"
MERCHANT_ID="${MERCHANT_ID:-merchant_smoke_${STAMP}}"
LOGIN_ID="${LOGIN_ID:-smoke-${STAMP}}"
PASSWORD="${PASSWORD:-smoke-pass-1234}"
CUSTOMER_ID="${CUSTOMER_ID:-customer_smoke_${STAMP}}"
SMOKE_REPORT_FILE="${SMOKE_REPORT_FILE:-shipmate-infra/smoke-report.json}"
SMOKE_JUNIT_FILE="${SMOKE_JUNIT_FILE:-shipmate-infra/smoke-report.xml}"
SMOKE_REPORT_DIR="${SMOKE_REPORT_DIR:-shipmate-infra/reports/smoke}"
SMOKE_DIAGNOSTIC_DIR="${SMOKE_DIAGNOSTIC_DIR:-/tmp/shipmate-smoke-${STAMP}}"
current_step="준비"
smoke_status="failed"

if [[ "${SMOKE_SEED_DEFAULT_ACCOUNT:-false}" == "true" ]]; then
  echo "[0/12] 기본 계정 확인"
  API_BASE_URL="${API_BASE_URL}" bash "${SCRIPT_DIR}/seed-staging-default-account.sh"
fi

json_escape() {
  printf "%s" "$1" | tr '\r\n\t' '   ' | sed 's/\\/\\\\/g; s/"/\\"/g'
}

xml_escape() {
  printf "%s" "$1" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g; s/"/\&quot;/g; s/'"'"'/\&apos;/g'
}

write_report() {
  local finished_at
  local failures
  local last_http_status
  local last_curl_error
  local last_response_excerpt
  local failure_detail
  finished_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  failures="1"
  if [[ "${smoke_status}" == "ok" ]]; then
    failures="0"
  fi
  last_http_status=""
  last_curl_error=""
  last_response_excerpt=""
  failure_detail=""
  if [[ -f "${SMOKE_DIAGNOSTIC_DIR}/last-http-status.txt" ]]; then
    last_http_status="$(cat "${SMOKE_DIAGNOSTIC_DIR}/last-http-status.txt")"
  fi
  if [[ -f "${SMOKE_DIAGNOSTIC_DIR}/last-curl-error.txt" ]]; then
    last_curl_error="$(cat "${SMOKE_DIAGNOSTIC_DIR}/last-curl-error.txt")"
  fi
  if [[ -f "${SMOKE_DIAGNOSTIC_DIR}/last-response.txt" ]]; then
    last_response_excerpt="$(head -c 2000 "${SMOKE_DIAGNOSTIC_DIR}/last-response.txt")"
  fi
  if [[ "${smoke_status}" != "ok" ]]; then
    failure_detail="httpStatus=${last_http_status:-unknown}; curlError=${last_curl_error:-none}; response=${last_response_excerpt:-none}"
  fi
  mkdir -p "$(dirname "${SMOKE_REPORT_FILE}")" "$(dirname "${SMOKE_JUNIT_FILE}")" "${SMOKE_REPORT_DIR}"
  cat > "${SMOKE_REPORT_FILE}" <<REPORT
{
  "status": "${smoke_status}",
  "apiBaseUrl": "$(json_escape "${API_BASE_URL}")",
  "merchantId": "$(json_escape "${MERCHANT_ID}")",
  "inboundMessageId": "$(json_escape "${inbound_message_id:-}")",
  "orderDraftId": "$(json_escape "${order_draft_id:-}")",
  "lastStep": "$(json_escape "${current_step}")",
  "lastHttpStatus": "$(json_escape "${last_http_status}")",
  "failureDetail": "$(json_escape "${failure_detail}")",
  "finishedAt": "${finished_at}"
}
REPORT
  cat > "${SMOKE_JUNIT_FILE}" <<REPORT
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="shipmate-staging-smoke" tests="1" failures="${failures}" errors="0" skipped="0" timestamp="${finished_at}">
  <testcase classname="shipmate.staging" name="통합 시나리오">
REPORT
  if [[ "${smoke_status}" != "ok" ]]; then
    cat >> "${SMOKE_JUNIT_FILE}" <<REPORT
    <failure message="$(xml_escape "${current_step}")">마지막 단계: $(xml_escape "${current_step}")
$(xml_escape "${failure_detail}")</failure>
REPORT
  fi
  cat >> "${SMOKE_JUNIT_FILE}" <<REPORT
    <system-out>apiBaseUrl=$(xml_escape "${API_BASE_URL}")
merchantId=$(xml_escape "${MERCHANT_ID}")
inboundMessageId=$(xml_escape "${inbound_message_id:-}")
orderDraftId=$(xml_escape "${order_draft_id:-}")</system-out>
  </testcase>
</testsuite>
REPORT
  cp "${SMOKE_REPORT_FILE}" "${SMOKE_REPORT_DIR}/smoke-report-${STAMP}.json"
  cp "${SMOKE_JUNIT_FILE}" "${SMOKE_REPORT_DIR}/smoke-report-${STAMP}.xml"
}

trap write_report EXIT

extract_json_string() {
  local key="$1"
  sed -n "s/.*\"${key}\":\"\\([^\"]*\\)\".*/\\1/p"
}

request_json() {
  local http_status
  local curl_exit
  mkdir -p "${SMOKE_DIAGNOSTIC_DIR}"
  set +e
  http_status="$(curl -sS -o "${SMOKE_DIAGNOSTIC_DIR}/last-response.txt" -w "%{http_code}" "$@" 2>"${SMOKE_DIAGNOSTIC_DIR}/last-curl-error.txt")"
  curl_exit=$?
  set -e
  printf "%s" "${http_status}" > "${SMOKE_DIAGNOSTIC_DIR}/last-http-status.txt"
  if (( curl_exit != 0 )); then
    echo "HTTP 요청 실패: ${current_step}" >&2
    cat "${SMOKE_DIAGNOSTIC_DIR}/last-curl-error.txt" >&2
    exit "${curl_exit}"
  fi
  if [[ ! "${http_status}" =~ ^2[0-9][0-9]$ ]]; then
    echo "API가 HTTP ${http_status}를 반환했습니다: ${current_step}" >&2
    head -c 2000 "${SMOKE_DIAGNOSTIC_DIR}/last-response.txt" >&2
    echo >&2
    exit 1
  fi
  cat "${SMOKE_DIAGNOSTIC_DIR}/last-response.txt"
}

current_step="[1/12] 회원가입"
echo "${current_step}"
register_response="$(
  request_json -X POST "${API_BASE_URL}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{
      \"merchantId\":\"${MERCHANT_ID}\",
      \"loginId\":\"${LOGIN_ID}\",
      \"password\":\"${PASSWORD}\",
      \"displayName\":\"스모크 검증\",
      \"role\":\"ADMIN\"
    }"
)"
access_token="$(printf "%s" "${register_response}" | extract_json_string accessToken)"
if [[ -z "${access_token}" ]]; then
  echo "accessToken을 찾지 못했습니다."
  echo "${register_response}"
  exit 1
fi

current_step="[2/12] 로그인"
echo "${current_step}"
request_json -X POST "${API_BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"loginId\":\"${LOGIN_ID}\",\"password\":\"${PASSWORD}\"}" >/dev/null

current_step="[3/12] 고객 저장"
echo "${current_step}"
request_json -X POST "${API_BASE_URL}/api/customers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${access_token}" \
  -d "{
    \"customerId\":\"${CUSTOMER_ID}\",
    \"merchantId\":\"${MERCHANT_ID}\",
    \"customerName\":\"스모크 고객\",
    \"customerPhone\":\"02-1000-2000\",
    \"defaultRecipientName\":\"스모크 담당자\",
    \"defaultRecipientPhone\":\"010-1000-2000\",
    \"defaultPostalCode\":\"04524\",
    \"defaultAddress\":\"서울특별시 중구 세종대로 110\",
    \"defaultAddressDetail\":\"검증실\",
    \"favoriteItemNames\":[\"종이박스\"],
    \"priceItems\":[{
      \"itemNameKeyword\":\"종이박스\",
      \"itemSpecKeyword\":\"300x200x150\",
      \"itemUnit\":\"장\",
      \"unitPrice\":450
    }]
  }" >/dev/null

current_step="[4/12] 주문 원문 접수"
echo "${current_step}"
inbound_response="$(
  request_json -X POST "${API_BASE_URL}/api/inbound-messages" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${access_token}" \
    -d "{
      \"merchantId\":\"${MERCHANT_ID}\",
      \"merchantStaffId\":\"staff_smoke_${STAMP}\",
      \"customerId\":\"${CUSTOMER_ID}\",
      \"sourceChannel\":\"KAKAO_PASTE\",
      \"businessChannel\":\"PACKAGING_CUSTOM\",
      \"rawText\":\"종이박스 300x200x150 200장 스모크 검증 주문입니다.\"
    }"
)"
inbound_message_id="$(printf "%s" "${inbound_response}" | extract_json_string inboundMessageId)"

current_step="[5/12] 출고표 초안 생성"
echo "${current_step}"
draft_response="$(
  request_json -X POST "${API_BASE_URL}/api/order-drafts/from-inbound-message" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${access_token}" \
    -d "{\"sourceInboundMessageId\":\"${inbound_message_id}\"}"
)"
order_draft_id="$(printf "%s" "${draft_response}" | extract_json_string orderDraftId)"
if [[ -z "${order_draft_id}" ]]; then
  echo "orderDraftId를 찾지 못했습니다."
  echo "${draft_response}"
  exit 1
fi

current_step="[6/12] 출고표 초안 검수 저장"
echo "${current_step}"
request_json -X PUT "${API_BASE_URL}/api/order-drafts/${order_draft_id}/review" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${access_token}" \
  -d "{
    \"itemCandidates\":[{
      \"itemName\":\"종이박스\",
      \"itemSpec\":\"300x200x150\",
      \"itemQuantity\":\"200\",
      \"itemUnit\":\"장\",
      \"matchedUnitPrice\":450
    }],
    \"deliveryRecipientName\":\"스모크 담당자\",
    \"deliveryRecipientPhone\":\"010-1000-2000\",
    \"deliveryPostalCode\":\"04524\",
    \"deliveryAddress\":\"서울특별시 중구 세종대로 110\",
    \"deliveryAddressDetail\":\"검증실\",
    \"deliveryRequestMemo\":\"문 앞\",
    \"deliveryMemo\":\"스모크 검증\",
    \"courierMemo\":\"파손주의\"
  }" >/dev/null

current_step="[7/12] 출고표 제출"
echo "${current_step}"
submit_response="$(
  request_json -X POST "${API_BASE_URL}/api/order-drafts/${order_draft_id}/submit" \
    -H "Authorization: Bearer ${access_token}"
)"
submit_status="$(printf "%s" "${submit_response}" | extract_json_string orderDraftStatus)"
if [[ "${submit_status}" != "SUBMITTED" ]]; then
  echo "제출 상태가 아닙니다: ${submit_status}"
  echo "${submit_response}"
  exit 1
fi

current_step="[8/12] 제출 출고표 조회"
echo "${current_step}"
shipping_response="$(
  request_json "${API_BASE_URL}/api/shipping-orders/${order_draft_id}" \
    -H "Authorization: Bearer ${access_token}"
)"
shipping_status="$(printf "%s" "${shipping_response}" | extract_json_string orderDraftStatus)"
if [[ "${shipping_status}" != "SUBMITTED" ]]; then
  echo "조회 상태가 아닙니다: ${shipping_status}"
  echo "${shipping_response}"
  exit 1
fi

current_step="[9/12] 재검수 전환"
echo "${current_step}"
reopen_reason="smoke-reopen-${STAMP}"
reopen_response="$(
  request_json -X POST "${API_BASE_URL}/api/shipping-orders/${order_draft_id}/reopen-review" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${access_token}" \
    -d "{\"reopenReason\":\"${reopen_reason}\"}"
)"
reopen_status="$(printf "%s" "${reopen_response}" | extract_json_string orderDraftStatus)"
if [[ "${reopen_status}" != "NEEDS_REVIEW" ]]; then
  echo "재검수 상태가 아닙니다: ${reopen_status}"
  echo "${reopen_response}"
  exit 1
fi

current_step="[10/12] 재검수 저장과 재제출"
echo "${current_step}"
request_json -X PUT "${API_BASE_URL}/api/order-drafts/${order_draft_id}/review" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${access_token}" \
  -d "{
    \"itemCandidates\":[{
      \"itemName\":\"종이박스\",
      \"itemSpec\":\"300x200x150\",
      \"itemQuantity\":\"220\",
      \"itemUnit\":\"장\",
      \"matchedUnitPrice\":450
    }],
    \"deliveryRecipientName\":\"스모크 담당자\",
    \"deliveryRecipientPhone\":\"010-1000-2000\",
    \"deliveryPostalCode\":\"04524\",
    \"deliveryAddress\":\"서울특별시 중구 세종대로 110\",
    \"deliveryAddressDetail\":\"검증실\",
    \"deliveryRequestMemo\":\"문 앞\",
    \"deliveryMemo\":\"스모크 재검수\",
    \"courierMemo\":\"파손주의\"
  }" >/dev/null
resubmit_response="$(
  request_json -X POST "${API_BASE_URL}/api/order-drafts/${order_draft_id}/submit" \
    -H "Authorization: Bearer ${access_token}"
)"
resubmit_status="$(printf "%s" "${resubmit_response}" | extract_json_string orderDraftStatus)"
if [[ "${resubmit_status}" != "SUBMITTED" ]]; then
  echo "재제출 상태가 아닙니다: ${resubmit_status}"
  echo "${resubmit_response}"
  exit 1
fi

current_step="[11/12] CSV 검증"
echo "${current_step}"
csv_validation_response="$(
  request_json -X POST "${API_BASE_URL}/api/shipping-orders/courier-csv/validate" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${access_token}" \
    -d "{\"orderDraftIds\":[\"${order_draft_id}\"],\"courierCsvTemplate\":\"GENERIC\"}"
)"
if ! printf "%s" "${csv_validation_response}" | grep -q '"valid":true'; then
  echo "CSV 검증이 통과하지 못했습니다."
  echo "${csv_validation_response}"
  exit 1
fi

current_step="[12/12] 변경 이력 metadata 확인"
echo "${current_step}"
audit_response="$(
  request_json "${API_BASE_URL}/api/operation/audit-logs?actionType=ORDER_DRAFT_REOPENED&targetType=ORDER_DRAFT&merchantId=${MERCHANT_ID}&searchText=${reopen_reason}&limit=5" \
    -H "Authorization: Bearer ${access_token}"
)"
if ! printf "%s" "${audit_response}" | grep -q "${reopen_reason}"; then
  echo "재검수 변경 이력 metadata를 찾지 못했습니다."
  echo "${audit_response}"
  exit 1
fi

echo "smoke=ok"
smoke_status="ok"
echo "merchantId=${MERCHANT_ID}"
echo "inboundMessageId=${inbound_message_id}"
echo "orderDraftId=${order_draft_id}"
echo "report=${SMOKE_REPORT_FILE}"
echo "junit=${SMOKE_JUNIT_FILE}"
