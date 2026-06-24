#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

MYSQL_CONTAINER="${MYSQL_CONTAINER:-shipmate-staging-mysql}"
MYSQL_DATABASE="${SHIPMATE_MYSQL_DATABASE:-shipmate}"
MYSQL_USER="${SHIPMATE_MYSQL_USER:-root}"
MYSQL_PASSWORD="${SHIPMATE_MYSQL_ROOT_PASSWORD:-shipmate}"
SMOKE_MERCHANT_PATTERN="${SMOKE_MERCHANT_PATTERN:-merchant_smoke_%}"
SMOKE_CLEANUP_APPLY="${SMOKE_CLEANUP_APPLY:-false}"
SMOKE_CLEANUP_CONFIRM="${SMOKE_CLEANUP_CONFIRM:-}"
SMOKE_CLEANUP_REPORT_FILE="${SMOKE_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json}"
SMOKE_CLEANUP_JUNIT_FILE="${SMOKE_CLEANUP_JUNIT_FILE:-shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.xml}"
ARCHIVE_CLEANUP_REPORTS_SCRIPT="${ARCHIVE_CLEANUP_REPORTS_SCRIPT:-shipmate-infra/scripts/archive-cleanup-reports.sh}"
DOCKER_MODE="${DOCKER_MODE:-auto}"

windows_docker() {
  cmd.exe /c docker "$@"
}

docker_cmd() {
  case "${DOCKER_MODE}" in
    linux)
      docker "$@"
      ;;
    windows)
      windows_docker "$@"
      ;;
    auto)
      if docker version >/dev/null 2>&1; then
        docker "$@"
      elif command -v cmd.exe >/dev/null 2>&1 && windows_docker version >/dev/null 2>&1; then
        windows_docker "$@"
      else
        docker "$@"
      fi
      ;;
    *)
      echo "지원하지 않는 DOCKER_MODE입니다: ${DOCKER_MODE}" >&2
      exit 2
      ;;
  esac
}

json_escape() {
  printf "%s" "$1" | tr '\r\n\t' '   ' | sed 's/\\/\\\\/g; s/"/\\"/g'
}

write_cleanup_report() {
  local status="$1"
  local mode="$2"
  local total_rows="$3"
  local remaining_rows="$4"
  local reason="$5"
  mkdir -p "$(dirname "${SMOKE_CLEANUP_REPORT_FILE}")"
  cat > "${SMOKE_CLEANUP_REPORT_FILE}" <<REPORT
{
  "status": "$(json_escape "${status}")",
  "mode": "$(json_escape "${mode}")",
  "reason": "$(json_escape "${reason}")",
  "merchantPattern": "$(json_escape "${SMOKE_MERCHANT_PATTERN}")",
  "totalRows": "$(json_escape "${total_rows}")",
  "remainingRows": "$(json_escape "${remaining_rows}")",
  "finishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
REPORT
}

write_cleanup_junit() {
  local status="$1"
  local mode="$2"
  local total_rows="$3"
  local remaining_rows="$4"
  local reason="$5"
  local failures="0"
  if [[ "${status}" != "ok" && "${status}" != "started" ]]; then
    failures="1"
  fi
  mkdir -p "$(dirname "${SMOKE_CLEANUP_JUNIT_FILE}")"
  cat > "${SMOKE_CLEANUP_JUNIT_FILE}" <<REPORT
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="shipmate-staging-smoke-cleanup" tests="1" failures="${failures}" errors="0" skipped="0">
  <testcase classname="shipmate.staging.cleanup" name="smoke cleanup ${mode}">
REPORT
  if [[ "${failures}" == "1" ]]; then
    cat >> "${SMOKE_CLEANUP_JUNIT_FILE}" <<REPORT
    <failure message="$(json_escape "${reason}")">status=${status} mode=${mode}</failure>
REPORT
  fi
  cat >> "${SMOKE_CLEANUP_JUNIT_FILE}" <<REPORT
    <system-out>totalRows=${total_rows}
remainingRows=${remaining_rows}
merchantPattern=${SMOKE_MERCHANT_PATTERN}</system-out>
  </testcase>
</testsuite>
REPORT
}

write_cleanup_outputs() {
  local status="$1"
  local mode="$2"
  local total_rows="$3"
  local remaining_rows="$4"
  local reason="$5"
  write_cleanup_report "${status}" "${mode}" "${total_rows}" "${remaining_rows}" "${reason}"
  write_cleanup_junit "${status}" "${mode}" "${total_rows}" "${remaining_rows}" "${reason}"
}

archive_cleanup_reports() {
  if [[ -x "${ARCHIVE_CLEANUP_REPORTS_SCRIPT}" ]]; then
    bash "${ARCHIVE_CLEANUP_REPORTS_SCRIPT}"
  fi
}

mysql_exec() {
  docker_cmd exec -i "${MYSQL_CONTAINER}" mysql \
    -u"${MYSQL_USER}" \
    -p"${MYSQL_PASSWORD}" \
    --database="${MYSQL_DATABASE}" \
    --batch \
    --raw
}

cleanup_count_rows() {
  mysql_exec <<SQL
set @smoke_pattern = '${SMOKE_MERCHANT_PATTERN}';
select 'tb_merchant_users' as table_name, count(*) as rows_to_delete from tb_merchant_users where merchant_id like @smoke_pattern
union all select 'tb_merchant_admin_profiles', count(*) from tb_merchant_admin_profiles where merchant_id like @smoke_pattern
union all select 'tb_customers', count(*) from tb_customers where merchant_id like @smoke_pattern
union all select 'tb_customer_price_items', count(*) from tb_customer_price_items where customer_id in (select customer_id from tb_customers where merchant_id like @smoke_pattern)
union all select 'tb_inbound_messages', count(*) from tb_inbound_messages where merchant_id like @smoke_pattern
union all select 'tb_order_drafts', count(*) from tb_order_drafts where merchant_id like @smoke_pattern
union all select 'tb_order_draft_item_candidates', count(*) from tb_order_draft_item_candidates where order_draft_id in (select order_draft_id from tb_order_drafts where merchant_id like @smoke_pattern)
union all select 'tb_audit_logs', count(*) from tb_audit_logs where merchant_id like @smoke_pattern
union all select 'tb_email_mailbox_configs', count(*) from tb_email_mailbox_configs where merchant_id like @smoke_pattern
union all select 'tb_email_attachments', count(*) from tb_email_attachments where merchant_id like @smoke_pattern
union all select 'tb_google_gmail_collected_messages', count(*) from tb_google_gmail_collected_messages where merchant_id like @smoke_pattern
union all select 'tb_google_oauth_states', count(*) from tb_google_oauth_states where merchant_id like @smoke_pattern
union all select 'tb_google_oauth_tokens', count(*) from tb_google_oauth_tokens where merchant_id like @smoke_pattern
union all select 'tb_gmail_polling_runs', count(*) from tb_gmail_polling_runs where merchant_id like @smoke_pattern
union all select 'tb_merchant_operation_settings', count(*) from tb_merchant_operation_settings where merchant_id like @smoke_pattern
union all select 'tb_error_logs', count(*) from tb_error_logs
where coalesce(metadata_json, '') like concat('%', replace(@smoke_pattern, '%', ''), '%')
   or coalesce(metadata_json, '') like '%customer_smoke_%'
   or coalesce(metadata_json, '') like '%smoke-%';
SQL
}

sum_count_rows() {
  awk 'NR > 1 { sum += $2 } END { print sum + 0 }'
}

count_rows_json_array() {
  awk '
    NR == 1 { next }
    {
      gsub(/\\/,"\\\\",$1)
      gsub(/"/,"\\\"",$1)
      rows[++count] = "    {\"tableName\":\"" $1 "\",\"rowsToDelete\":" $2 "}"
    }
    END {
      print "  \"tables\": ["
      for (i = 1; i <= count; i++) {
        suffix = i < count ? "," : ""
        print rows[i] suffix
      }
      print "  ],"
    }
  '
}

if [[ "${SMOKE_CLEANUP_APPLY}" != "true" ]]; then
  echo "mode=dry-run"
  dry_run_output="$(cleanup_count_rows)"
  printf "%s\n" "${dry_run_output}"
  total_rows="$(printf "%s\n" "${dry_run_output}" | sum_count_rows)"
  tables_json="$(printf "%s\n" "${dry_run_output}" | count_rows_json_array)"
  write_cleanup_outputs "ok" "dry-run" "${total_rows}" "${total_rows}" "-"
  tmp_report="${SMOKE_CLEANUP_REPORT_FILE}.tmp"
  awk -v tables_json="${tables_json}" '{
    if ($0 ~ /"finishedAt"/) {
      print tables_json
    }
    print
  }' "${SMOKE_CLEANUP_REPORT_FILE}" > "${tmp_report}"
  mv "${tmp_report}" "${SMOKE_CLEANUP_REPORT_FILE}"
  archive_cleanup_reports
  echo "실제 삭제는 SMOKE_CLEANUP_APPLY=true SMOKE_CLEANUP_CONFIRM='${SMOKE_MERCHANT_PATTERN}' bash shipmate-infra/scripts/cleanup-staging-smoke-data.sh 로 실행합니다."
  echo "report=${SMOKE_CLEANUP_REPORT_FILE}"
  exit 0
fi

if [[ "${SMOKE_CLEANUP_CONFIRM}" != "${SMOKE_MERCHANT_PATTERN}" ]]; then
  write_cleanup_outputs "blocked" "apply" "0" "0" "cleanup-confirm-mismatch"
  echo "status=blocked"
  echo "reason=cleanup-confirm-mismatch"
  echo "expected=SMOKE_CLEANUP_CONFIRM='${SMOKE_MERCHANT_PATTERN}'"
  echo "artifact의 merchantId와 삭제 pattern을 확인한 뒤 다시 실행합니다."
  exit 1
fi

echo "mode=apply"
before_output="$(cleanup_count_rows)"
before_rows="$(printf "%s\n" "${before_output}" | sum_count_rows)"
write_cleanup_outputs "started" "apply" "${before_rows}" "-" "-"
mysql_exec <<SQL
set @smoke_pattern = '${SMOKE_MERCHANT_PATTERN}';

delete from tb_email_attachments where merchant_id like @smoke_pattern;
delete from tb_google_gmail_collected_messages where merchant_id like @smoke_pattern;
delete from tb_google_oauth_states where merchant_id like @smoke_pattern;
delete from tb_google_oauth_tokens where merchant_id like @smoke_pattern;
delete from tb_gmail_polling_runs where merchant_id like @smoke_pattern;
delete from tb_customer_price_items
where customer_id in (
  select customer_id from tb_customers where merchant_id like @smoke_pattern
);
delete from tb_order_draft_item_candidates
where order_draft_id in (
  select order_draft_id from tb_order_drafts where merchant_id like @smoke_pattern
);
delete from tb_order_drafts where merchant_id like @smoke_pattern;
delete from tb_inbound_messages where merchant_id like @smoke_pattern;
delete from tb_customers where merchant_id like @smoke_pattern;
delete from tb_audit_logs where merchant_id like @smoke_pattern;
delete from tb_email_mailbox_configs where merchant_id like @smoke_pattern;
delete from tb_merchant_operation_settings where merchant_id like @smoke_pattern;
delete from tb_error_logs
where coalesce(metadata_json, '') like concat('%', replace(@smoke_pattern, '%', ''), '%')
   or coalesce(metadata_json, '') like '%customer_smoke_%'
   or coalesce(metadata_json, '') like '%smoke-%';
delete from tb_merchant_admin_profiles where merchant_id like @smoke_pattern;
delete from tb_merchant_users where merchant_id like @smoke_pattern;

select 'remaining_smoke_merchants' as metric, count(distinct merchant_id) as value
from tb_merchant_users
where merchant_id like @smoke_pattern;
SQL
after_output="$(cleanup_count_rows)"
after_rows="$(printf "%s\n" "${after_output}" | sum_count_rows)"
tables_json="$(printf "%s\n" "${after_output}" | count_rows_json_array)"
write_cleanup_outputs "ok" "apply" "${before_rows}" "${after_rows}" "-"
tmp_report="${SMOKE_CLEANUP_REPORT_FILE}.tmp"
awk -v tables_json="${tables_json}" '{
  if ($0 ~ /"finishedAt"/) {
    print tables_json
  }
  print
}' "${SMOKE_CLEANUP_REPORT_FILE}" > "${tmp_report}"
mv "${tmp_report}" "${SMOKE_CLEANUP_REPORT_FILE}"
archive_cleanup_reports
