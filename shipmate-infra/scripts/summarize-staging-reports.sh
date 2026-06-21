#!/usr/bin/env bash
set -euo pipefail

SMOKE_REPORT_FILE="${SMOKE_REPORT_FILE:-shipmate-infra/smoke-report.json}"
HTTPS_REPORT_FILE="${HTTPS_REPORT_FILE:-shipmate-infra/staging-https-report.json}"
SUMMARY_FILE="${SUMMARY_FILE:-shipmate-infra/reports/staging-check-summary.md}"
SUMMARY_HTML_FILE="${SUMMARY_HTML_FILE:-shipmate-infra/reports/staging-check-summary.html}"
GITHUB_REPORT_ROOT="${GITHUB_REPORT_ROOT:-shipmate-infra/reports/github-actions}"
GITHUB_RUNNER_REPORT_FILE="${GITHUB_RUNNER_REPORT_FILE:-shipmate-infra/reports/github-runner/self-hosted-runner-report.json}"
SMOKE_CLEANUP_REPORT_FILE="${SMOKE_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json}"

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
smoke_reason="$(json_value "${SMOKE_REPORT_FILE}" reason)"
smoke_api_base_url="$(json_value "${SMOKE_REPORT_FILE}" apiBaseUrl)"
smoke_merchant_id="$(json_value "${SMOKE_REPORT_FILE}" merchantId)"
smoke_order_draft_id="$(json_value "${SMOKE_REPORT_FILE}" orderDraftId)"
smoke_last_step="$(json_value "${SMOKE_REPORT_FILE}" lastStep)"
smoke_last_http_status="$(json_value "${SMOKE_REPORT_FILE}" lastHttpStatus)"
smoke_failure_detail="$(json_value "${SMOKE_REPORT_FILE}" failureDetail)"
smoke_finished_at="$(json_value "${SMOKE_REPORT_FILE}" finishedAt)"

https_status="$(json_value "${HTTPS_REPORT_FILE}" status)"
https_reason="$(json_value "${HTTPS_REPORT_FILE}" reason)"
https_merchant_url="$(json_value "${HTTPS_REPORT_FILE}" merchantUrl)"
https_api_health_url="$(json_value "${HTTPS_REPORT_FILE}" apiHealthUrl)"
https_resolve_ip="$(json_value "${HTTPS_REPORT_FILE}" resolveIp)"
https_check_mode="$(json_value "${HTTPS_REPORT_FILE}" httpsCheckMode)"
https_insecure="$(json_bool_or_text "${HTTPS_REPORT_FILE}" allowInsecureTls)"
https_last_step="$(json_value "${HTTPS_REPORT_FILE}" lastStep)"
https_finished_at="$(json_value "${HTTPS_REPORT_FILE}" finishedAt)"

runner_status="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" status)"
runner_stage="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" stage)"
runner_reason="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" reason)"
runner_container_name="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" containerName)"
runner_container_status="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" containerStatus)"
runner_label="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" runnerLabel)"
runner_finished_at="$(json_value "${GITHUB_RUNNER_REPORT_FILE}" finishedAt)"

cleanup_status="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" status)"
cleanup_mode="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" mode)"
cleanup_reason="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" reason)"
cleanup_merchant_pattern="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" merchantPattern)"
cleanup_total_rows="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" totalRows)"
cleanup_remaining_rows="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" remainingRows)"
cleanup_finished_at="$(json_value "${SMOKE_CLEANUP_REPORT_FILE}" finishedAt)"

artifact_cleanup_report_file="${ARTIFACT_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/github-smoke-artifact-cleanup-report.json}"
artifact_cleanup_status="$(json_value "${artifact_cleanup_report_file}" status)"
artifact_cleanup_mode="$(json_value "${artifact_cleanup_report_file}" mode)"
artifact_cleanup_keep_latest="$(json_value "${artifact_cleanup_report_file}" keepLatest)"
artifact_cleanup_protect_run_ids="$(json_value "${artifact_cleanup_report_file}" protectRunIds)"
artifact_cleanup_total_dirs="$(json_value "${artifact_cleanup_report_file}" totalRunDirs)"
artifact_cleanup_delete_candidates="$(json_value "${artifact_cleanup_report_file}" deleteCandidates)"
artifact_cleanup_finished_at="$(json_value "${artifact_cleanup_report_file}" finishedAt)"

latest_artifact_summary=""
latest_artifact_summary_link="-"
latest_artifact_run_url="-"
latest_artifact_run_id="-"
latest_artifact_smoke_status="-"
latest_artifact_smoke_api_base_url="-"
latest_artifact_https_status="-"
latest_artifact_https_check_mode="-"
if [[ -d "${GITHUB_REPORT_ROOT}" ]]; then
  latest_artifact_summary="$(find "${GITHUB_REPORT_ROOT}" -path '*/artifact-summary.md' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-)"
  if [[ -n "${latest_artifact_summary}" ]]; then
    latest_artifact_summary_link="${latest_artifact_summary#shipmate-infra/reports/}"
    latest_artifact_run_id="$(sed -n 's/.*| runId | \([^|]*\) |.*/\1/p' "${latest_artifact_summary}" | head -n 1)"
    latest_artifact_run_url="$(sed -n 's/.*| run URL | \([^|]*\) |.*/\1/p' "${latest_artifact_summary}" | head -n 1)"
    latest_artifact_dir="$(dirname "${latest_artifact_summary}")"
    latest_artifact_smoke_status="$(json_value "${latest_artifact_dir}/smoke-report.json" status)"
    latest_artifact_smoke_api_base_url="$(json_value "${latest_artifact_dir}/smoke-report.json" apiBaseUrl)"
    latest_artifact_https_status="$(json_value "${latest_artifact_dir}/staging-https-report.json" status)"
    latest_artifact_https_check_mode="$(json_value "${latest_artifact_dir}/staging-https-report.json" httpsCheckMode)"
  fi
fi

cat > "${SUMMARY_FILE}" <<REPORT
# 스테이징 점검 요약

생성 시각: $(date -u +%Y-%m-%dT%H:%M:%SZ)

## Smoke

| 항목 | 값 |
|---|---|
| 상태 | ${smoke_status:-"-"} |
| 사유 | ${smoke_reason:-"-"} |
| API 기준 URL | ${smoke_api_base_url:-"-"} |
| 이용자 ID | ${smoke_merchant_id:-"-"} |
| 출고표 ID | ${smoke_order_draft_id:-"-"} |
| 마지막 단계 | ${smoke_last_step:-"-"} |
| 마지막 HTTP 상태 | ${smoke_last_http_status:-"-"} |
| 실패 상세 | ${smoke_failure_detail:-"-"} |
| 완료 시각 | ${smoke_finished_at:-"-"} |

## DNS 없는 HTTPS

| 항목 | 값 |
|---|---|
| 상태 | ${https_status:-"-"} |
| 사유 | ${https_reason:-"-"} |
| Merchant URL | ${https_merchant_url:-"-"} |
| API health URL | ${https_api_health_url:-"-"} |
| resolve IP | ${https_resolve_ip:-"-"} |
| 실행 유형 | ${https_check_mode:-"-"} |
| 자체 서명 TLS 허용 | ${https_insecure:-"-"} |
| 마지막 단계 | ${https_last_step:-"-"} |
| 완료 시각 | ${https_finished_at:-"-"} |

## 최근 GitHub Actions artifact

| 항목 | 값 |
|---|---|
| 최근 artifact 요약 | ${latest_artifact_summary_link:-"-"} |
| 최근 run ID | ${latest_artifact_run_id:-"-"} |
| 최근 run URL | ${latest_artifact_run_url:-"-"} |
| 최근 smoke 상태 | ${latest_artifact_smoke_status:-"-"} |
| 최근 smoke API 기준 URL | ${latest_artifact_smoke_api_base_url:-"-"} |
| 최근 HTTPS 상태 | ${latest_artifact_https_status:-"-"} |
| 최근 HTTPS 실행 유형 | ${latest_artifact_https_check_mode:-"-"} |

## Self-hosted runner

| 항목 | 값 |
|---|---|
| 상태 | ${runner_status:-"-"} |
| 단계 | ${runner_stage:-"-"} |
| 사유 | ${runner_reason:-"-"} |
| 컨테이너 | ${runner_container_name:-"-"} |
| 컨테이너 상태 | ${runner_container_status:-"-"} |
| runner label | ${runner_label:-"-"} |
| 완료 시각 | ${runner_finished_at:-"-"} |

## Smoke 테스트 데이터 정리

| 항목 | 값 |
|---|---|
| 상태 | ${cleanup_status:-"-"} |
| 실행 유형 | ${cleanup_mode:-"-"} |
| 사유 | ${cleanup_reason:-"-"} |
| 이용자 ID pattern | ${cleanup_merchant_pattern:-"-"} |
| 대상 row 수 | ${cleanup_total_rows:-"-"} |
| 잔여 row 수 | ${cleanup_remaining_rows:-"-"} |
| 완료 시각 | ${cleanup_finished_at:-"-"} |

## GitHub Actions artifact 정리

| 항목 | 값 |
|---|---|
| 상태 | ${artifact_cleanup_status:-"-"} |
| 실행 유형 | ${artifact_cleanup_mode:-"-"} |
| 최신 보관 개수 | ${artifact_cleanup_keep_latest:-"-"} |
| 보호 run ID | ${artifact_cleanup_protect_run_ids:-"-"} |
| 전체 run 디렉터리 | ${artifact_cleanup_total_dirs:-"-"} |
| 삭제 후보 | ${artifact_cleanup_delete_candidates:-"-"} |
| 완료 시각 | ${artifact_cleanup_finished_at:-"-"} |

## 실행 명령

| 용도 | 명령 |
|---|---|
| 로컬 스테이징 smoke | \`bash shipmate-infra/scripts/smoke-staging.sh\` |
| 로컬 스테이징 전체 점검 | \`bash shipmate-infra/scripts/check-staging-stack.sh\` |
| HTTPS 점검 | \`ALLOW_INSECURE_TLS=true STAGING_RESOLVE_IP=127.0.0.1 bash shipmate-infra/scripts/check-staging-https.sh\` |
| DNS 없는 HTTPS 간편 점검 | \`bash shipmate-infra/scripts/check-staging-dnsless-https.sh\` |
| 원격 smoke workflow | \`bash shipmate-infra/scripts/run-github-smoke-workflow.sh\` |
| quick tunnel 원격 smoke | \`bash shipmate-infra/scripts/run-github-smoke-with-quick-tunnel.sh\` |
| self-hosted runner env 생성 | \`bash shipmate-infra/scripts/create-github-runner-env.sh\` |
| self-hosted runner 등록 | \`bash shipmate-infra/scripts/run-github-self-hosted-runner.sh\` |
| self-hosted runner 정지 | \`bash shipmate-infra/scripts/stop-github-self-hosted-runner.sh\` |
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
      <tr><td>사유</td><td>${smoke_reason:-"-"}</td></tr>
      <tr><td>API 기준 URL</td><td>${smoke_api_base_url:-"-"}</td></tr>
      <tr><td>이용자 ID</td><td>${smoke_merchant_id:-"-"}</td></tr>
      <tr><td>출고표 ID</td><td>${smoke_order_draft_id:-"-"}</td></tr>
      <tr><td>마지막 단계</td><td>${smoke_last_step:-"-"}</td></tr>
      <tr><td>마지막 HTTP 상태</td><td>${smoke_last_http_status:-"-"}</td></tr>
      <tr><td>실패 상세</td><td>${smoke_failure_detail:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${smoke_finished_at:-"-"}</td></tr>
    </table>
    <h2>DNS 없는 HTTPS</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${https_status:-"-"}</td></tr>
      <tr><td>사유</td><td>${https_reason:-"-"}</td></tr>
      <tr><td>Merchant URL</td><td>${https_merchant_url:-"-"}</td></tr>
      <tr><td>API health URL</td><td>${https_api_health_url:-"-"}</td></tr>
      <tr><td>resolve IP</td><td>${https_resolve_ip:-"-"}</td></tr>
      <tr><td>실행 유형</td><td>${https_check_mode:-"-"}</td></tr>
      <tr><td>자체 서명 TLS 허용</td><td>${https_insecure:-"-"}</td></tr>
      <tr><td>마지막 단계</td><td>${https_last_step:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${https_finished_at:-"-"}</td></tr>
    </table>
    <h2>최근 GitHub Actions artifact</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>최근 artifact 요약</td><td><a href="${latest_artifact_summary_link:-"#"}">${latest_artifact_summary_link:-"-"}</a></td></tr>
      <tr><td>최근 run ID</td><td>${latest_artifact_run_id:-"-"}</td></tr>
      <tr><td>최근 run URL</td><td>${latest_artifact_run_url:-"-"}</td></tr>
      <tr><td>최근 smoke 상태</td><td>${latest_artifact_smoke_status:-"-"}</td></tr>
      <tr><td>최근 smoke API 기준 URL</td><td>${latest_artifact_smoke_api_base_url:-"-"}</td></tr>
      <tr><td>최근 HTTPS 상태</td><td>${latest_artifact_https_status:-"-"}</td></tr>
      <tr><td>최근 HTTPS 실행 유형</td><td>${latest_artifact_https_check_mode:-"-"}</td></tr>
    </table>
    <h2>Self-hosted runner</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${runner_status:-"-"}</td></tr>
      <tr><td>단계</td><td>${runner_stage:-"-"}</td></tr>
      <tr><td>사유</td><td>${runner_reason:-"-"}</td></tr>
      <tr><td>컨테이너</td><td>${runner_container_name:-"-"}</td></tr>
      <tr><td>컨테이너 상태</td><td>${runner_container_status:-"-"}</td></tr>
      <tr><td>runner label</td><td>${runner_label:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${runner_finished_at:-"-"}</td></tr>
    </table>
    <h2>Smoke 테스트 데이터 정리</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${cleanup_status:-"-"}</td></tr>
      <tr><td>실행 유형</td><td>${cleanup_mode:-"-"}</td></tr>
      <tr><td>사유</td><td>${cleanup_reason:-"-"}</td></tr>
      <tr><td>이용자 ID pattern</td><td>${cleanup_merchant_pattern:-"-"}</td></tr>
      <tr><td>대상 row 수</td><td>${cleanup_total_rows:-"-"}</td></tr>
      <tr><td>잔여 row 수</td><td>${cleanup_remaining_rows:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${cleanup_finished_at:-"-"}</td></tr>
    </table>
    <h2>GitHub Actions artifact 정리</h2>
    <table>
      <tr><th>항목</th><th>값</th></tr>
      <tr><td>상태</td><td>${artifact_cleanup_status:-"-"}</td></tr>
      <tr><td>실행 유형</td><td>${artifact_cleanup_mode:-"-"}</td></tr>
      <tr><td>최신 보관 개수</td><td>${artifact_cleanup_keep_latest:-"-"}</td></tr>
      <tr><td>보호 run ID</td><td>${artifact_cleanup_protect_run_ids:-"-"}</td></tr>
      <tr><td>전체 run 디렉터리</td><td>${artifact_cleanup_total_dirs:-"-"}</td></tr>
      <tr><td>삭제 후보</td><td>${artifact_cleanup_delete_candidates:-"-"}</td></tr>
      <tr><td>완료 시각</td><td>${artifact_cleanup_finished_at:-"-"}</td></tr>
    </table>
    <h2>실행 명령</h2>
    <table>
      <tr><th>용도</th><th>명령</th></tr>
      <tr><td>로컬 스테이징 smoke</td><td><code>bash shipmate-infra/scripts/smoke-staging.sh</code></td></tr>
      <tr><td>로컬 스테이징 전체 점검</td><td><code>bash shipmate-infra/scripts/check-staging-stack.sh</code></td></tr>
      <tr><td>HTTPS 점검</td><td><code>ALLOW_INSECURE_TLS=true STAGING_RESOLVE_IP=127.0.0.1 bash shipmate-infra/scripts/check-staging-https.sh</code></td></tr>
      <tr><td>DNS 없는 HTTPS 간편 점검</td><td><code>bash shipmate-infra/scripts/check-staging-dnsless-https.sh</code></td></tr>
      <tr><td>원격 smoke workflow</td><td><code>bash shipmate-infra/scripts/run-github-smoke-workflow.sh</code></td></tr>
      <tr><td>quick tunnel 원격 smoke</td><td><code>bash shipmate-infra/scripts/run-github-smoke-with-quick-tunnel.sh</code></td></tr>
      <tr><td>self-hosted runner env 생성</td><td><code>bash shipmate-infra/scripts/create-github-runner-env.sh</code></td></tr>
      <tr><td>self-hosted runner 등록</td><td><code>bash shipmate-infra/scripts/run-github-self-hosted-runner.sh</code></td></tr>
      <tr><td>self-hosted runner 정지</td><td><code>bash shipmate-infra/scripts/stop-github-self-hosted-runner.sh</code></td></tr>
    </table>
    <p><a href="staging-check-summary.md">Markdown 원본</a></p>
  </main>
</body>
</html>
REPORT

echo "summary=${SUMMARY_FILE}"
echo "html=${SUMMARY_HTML_FILE}"
