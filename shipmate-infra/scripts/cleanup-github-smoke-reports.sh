#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

REPORT_ROOT="${REPORT_ROOT:-shipmate-infra/reports/github-actions}"
KEEP_LATEST="${KEEP_LATEST:-10}"
REPORT_CLEANUP_APPLY="${REPORT_CLEANUP_APPLY:-false}"
PROTECT_RUN_IDS="${PROTECT_RUN_IDS:-}"
ARTIFACT_CLEANUP_REPORT_FILE="${ARTIFACT_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/github-smoke-artifact-cleanup-report.json}"

json_escape() {
  printf "%s" "$1" | tr '\r\n\t' '   ' | sed 's/\\/\\\\/g; s/"/\\"/g'
}

write_report() {
  local status="$1"
  local mode="$2"
  local total="$3"
  local delete_count="$4"
  local reason="$5"
  mkdir -p "$(dirname "${ARTIFACT_CLEANUP_REPORT_FILE}")"
  cat > "${ARTIFACT_CLEANUP_REPORT_FILE}" <<REPORT
{
  "status": "$(json_escape "${status}")",
  "mode": "$(json_escape "${mode}")",
  "reason": "$(json_escape "${reason}")",
  "reportRoot": "$(json_escape "${REPORT_ROOT}")",
  "keepLatest": "$(json_escape "${KEEP_LATEST}")",
  "protectRunIds": "$(json_escape "${PROTECT_RUN_IDS:-"-"}")",
  "totalRunDirs": "$(json_escape "${total}")",
  "deleteCandidates": "$(json_escape "${delete_count}")",
  "finishedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
REPORT
}

if [[ ! -d "${REPORT_ROOT}" ]]; then
  echo "reportRoot=${REPORT_ROOT}"
  echo "status=skipped"
  echo "reason=report-root-missing"
  write_report "skipped" "dry-run" "0" "0" "report-root-missing"
  exit 0
fi

mapfile -t report_dirs < <(find "${REPORT_ROOT}" -mindepth 1 -maxdepth 1 -type d -name 'run-*' -printf '%T@ %p\n' | sort -rn | awk '{print $2}')

echo "reportRoot=${REPORT_ROOT}"
echo "keepLatest=${KEEP_LATEST}"
echo "protectRunIds=${PROTECT_RUN_IDS:-"-"}"
echo "total=${#report_dirs[@]}"

if (( ${#report_dirs[@]} <= KEEP_LATEST )); then
  echo "deleteCandidates=0"
  write_report "ok" "dry-run" "${#report_dirs[@]}" "0" "-"
  echo "report=${ARTIFACT_CLEANUP_REPORT_FILE}"
  exit 0
fi

is_protected_run_dir() {
  local report_dir="$1"
  local run_id
  IFS=',' read -r -a protected_ids <<< "${PROTECT_RUN_IDS}"
  run_id="$(basename "${report_dir}" | sed -n 's/^run-\([0-9][0-9]*\)-.*/\1/p')"
  for protected_id in "${protected_ids[@]}"; do
    protected_id="$(printf '%s' "${protected_id}" | tr -d '[:space:]')"
    if [[ -n "${protected_id}" && "${run_id}" == "${protected_id}" ]]; then
      return 0
    fi
  done
  return 1
}

delete_candidates=()
for report_dir in "${report_dirs[@]:KEEP_LATEST}"; do
  if is_protected_run_dir "${report_dir}"; then
    continue
  fi
  delete_candidates+=("${report_dir}")
done
echo "deleteCandidates=${#delete_candidates[@]}"
if (( ${#delete_candidates[@]} > 0 )); then
  printf '%s\n' "${delete_candidates[@]}"
fi

if [[ "${REPORT_CLEANUP_APPLY}" != "true" ]]; then
  echo "mode=dry-run"
  write_report "ok" "dry-run" "${#report_dirs[@]}" "${#delete_candidates[@]}" "-"
  echo "실제 삭제는 REPORT_CLEANUP_APPLY=true bash shipmate-infra/scripts/cleanup-github-smoke-reports.sh 로 실행합니다."
  echo "report=${ARTIFACT_CLEANUP_REPORT_FILE}"
  exit 0
fi

echo "mode=apply"
for report_dir in "${delete_candidates[@]}"; do
  rm -rf "${report_dir}"
done
write_report "ok" "apply" "${#report_dirs[@]}" "${#delete_candidates[@]}" "-"
echo "report=${ARTIFACT_CLEANUP_REPORT_FILE}"
