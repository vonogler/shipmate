#!/usr/bin/env bash
set -euo pipefail

SMOKE_REPORT_FILE="${SMOKE_REPORT_FILE:-shipmate-infra/smoke-report.json}"
HTTPS_REPORT_FILE="${HTTPS_REPORT_FILE:-shipmate-infra/staging-https-report.json}"
SUMMARY_FILE="${SUMMARY_FILE:-shipmate-infra/reports/staging-check-summary.md}"
SUMMARY_HTML_FILE="${SUMMARY_HTML_FILE:-shipmate-infra/reports/staging-check-summary.html}"

json_value() {
  local file_path="$1"
  local key="$2"
  if [[ ! -f "${file_path}" ]]; then
    printf "%s" "-"
    return
  fi
  sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "${file_path}" | head -n 1
}

json_bool_or_text() {
  local file_path="$1"
  local key="$2"
  if [[ ! -f "${file_path}" ]]; then
    printf "%s" "-"
    return
  fi
  sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\\([^,}]*\\).*/\\1/p" "${file_path}" | head -n 1 | tr -d '"'
}

mkdir -p "$(dirname "${SUMMARY_FILE}")"
mkdir -p "$(dirname "${SUMMARY_HTML_FILE}")"

smoke_status="$(json_value "${SMOKE_REPORT_FILE}" status)"
smoke_api_base_url="$(json_value "${SMOKE_REPORT_FILE}" apiBaseUrl)"
smoke_merchant_id="$(json_value "${SMOKE_REPORT_FILE}" merchantId)"
smoke_order_draft_id="$(json_value "${SMOKE_REPORT_FILE}" orderDraftId)"
smoke_last_step="$(json_value "${SMOKE_REPORT_FILE}" lastStep)"
smoke_finished_at="$(json_value "${SMOKE_REPORT_FILE}" finishedAt)"

https_status="$(json_value "${HTTPS_REPORT_FILE}" status)"
https_merchant_url="$(json_value "${HTTPS_REPORT_FILE}" merchantUrl)"
https_api_health_url="$(json_value "${HTTPS_REPORT_FILE}" apiHealthUrl)"
https_resolve_ip="$(json_value "${HTTPS_REPORT_FILE}" resolveIp)"
https_insecure="$(json_bool_or_text "${HTTPS_REPORT_FILE}" allowInsecureTls)"
https_last_step="$(json_value "${HTTPS_REPORT_FILE}" lastStep)"
https_finished_at="$(json_value "${HTTPS_REPORT_FILE}" finishedAt)"

cat > "${SUMMARY_FILE}" <<REPORT
# 스테이징 점검 요약

생성 시각: $(date -u +%Y-%m-%dT%H:%M:%SZ)

## Smoke

| 항목 | 값 |
|---|---|
| 상태 | ${smoke_status:-"-"} |
| API 기준 URL | ${smoke_api_base_url:-"-"} |
| 이용자 ID | ${smoke_merchant_id:-"-"} |
| 출고표 ID | ${smoke_order_draft_id:-"-"} |
| 마지막 단계 | ${smoke_last_step:-"-"} |
| 완료 시각 | ${smoke_finished_at:-"-"} |

## DNS 없는 HTTPS

| 항목 | 값 |
|---|---|
| 상태 | ${https_status:-"-"} |
| Merchant URL | ${https_merchant_url:-"-"} |
| API health URL | ${https_api_health_url:-"-"} |
| resolve IP | ${https_resolve_ip:-"-"} |
| 자체 서명 TLS 허용 | ${https_insecure:-"-"} |
| 마지막 단계 | ${https_last_step:-"-"} |
| 완료 시각 | ${https_finished_at:-"-"} |
REPORT

cat > "${SUMMARY_HTML_FILE}" <<REPORT
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>스테이징 점검 요약</title>
  <style>
    body { font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; margin: 32px; color: #1f2937; }
    main { max-width: 960px; margin: 0 auto; }
    table { width: 100%; border-collapse: collapse; margin: 12px 0 28px; }
    th, td { border: 1px solid #d1d5db; padding: 10px 12px; text-align: left; }
    th { background: #f3f4f6; }
    code { background: #f3f4f6; padding: 2px 4px; border-radius: 4px; }
  </style>
</head>
<body>
  <main>
    <h1>스테이징 점검 요약</h1>
    <p>생성 시각: <code>$(date -u +%Y-%m-%dT%H:%M:%SZ)</code></p>
    <h2>Smoke</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${smoke_status:-"-"}</td></tr>
      <tr><td>API 기준 URL</td><td>${smoke_api_base_url:-"-"}</td></tr>
      <tr><td>이용자 ID</td><td>${smoke_merchant_id:-"-"}</td></tr>
      <tr><td>출고표 ID</td><td>${smoke_order_draft_id:-"-"}</td></tr>
      <tr><td>마지막 단계</td><td>${smoke_last_step:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${smoke_finished_at:-"-"}</td></tr>
    </table>
    <h2>DNS 없는 HTTPS</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${https_status:-"-"}</td></tr>
      <tr><td>Merchant URL</td><td>${https_merchant_url:-"-"}</td></tr>
      <tr><td>API health URL</td><td>${https_api_health_url:-"-"}</td></tr>
      <tr><td>resolve IP</td><td>${https_resolve_ip:-"-"}</td></tr>
      <tr><td>자체 서명 TLS 허용</td><td>${https_insecure:-"-"}</td></tr>
      <tr><td>마지막 단계</td><td>${https_last_step:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${https_finished_at:-"-"}</td></tr>
    </table>
    <p><a href="staging-check-summary.md">Markdown 원본</a></p>
  </main>
</body>
</html>
REPORT

echo "summary=${SUMMARY_FILE}"
echo "html=${SUMMARY_HTML_FILE}"
