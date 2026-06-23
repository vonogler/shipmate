#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

CLEANUP_ARCHIVE_ENABLED="${CLEANUP_ARCHIVE_ENABLED:-true}"
CLEANUP_ARCHIVE_DIR="${CLEANUP_ARCHIVE_DIR:-shipmate-infra/reports/cleanup/archive}"
CLEANUP_ARCHIVE_KEEP_LATEST="${CLEANUP_ARCHIVE_KEEP_LATEST:-20}"
CLEANUP_REPORT_DIR="${CLEANUP_REPORT_DIR:-shipmate-infra/reports/cleanup}"

if [[ "${CLEANUP_ARCHIVE_ENABLED}" != "true" ]]; then
  echo "cleanupArchive=disabled"
  exit 0
fi

if [[ ! -d "${CLEANUP_REPORT_DIR}" ]]; then
  echo "cleanupReportDir=${CLEANUP_REPORT_DIR}"
  echo "cleanupArchive=skipped"
  echo "reason=cleanup-report-dir-missing"
  exit 0
fi

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive_path="${CLEANUP_ARCHIVE_DIR}/${stamp}"
mkdir -p "${archive_path}"

copied=0
for report_file in \
  "${CLEANUP_REPORT_DIR}/staging-smoke-cleanup-report.json" \
  "${CLEANUP_REPORT_DIR}/staging-smoke-cleanup-report.xml" \
  "${CLEANUP_REPORT_DIR}/github-smoke-artifact-cleanup-report.json"; do
  if [[ -f "${report_file}" ]]; then
    cp "${report_file}" "${archive_path}/"
    copied=$((copied + 1))
  fi
done

cat > "${archive_path}/cleanup-archive-manifest.txt" <<REPORT
createdAt=${stamp}
sourceDir=${CLEANUP_REPORT_DIR}
copiedFiles=${copied}
keepLatest=${CLEANUP_ARCHIVE_KEEP_LATEST}
REPORT

mapfile -t archives < <(find "${CLEANUP_ARCHIVE_DIR}" -mindepth 1 -maxdepth 1 -type d -printf '%T@ %p\n' 2>/dev/null | sort -rn | awk '{print $2}')

deleted=0
if (( ${#archives[@]} > CLEANUP_ARCHIVE_KEEP_LATEST )); then
  for archive_dir in "${archives[@]:CLEANUP_ARCHIVE_KEEP_LATEST}"; do
    rm -rf "${archive_dir}"
    deleted=$((deleted + 1))
  done
fi

echo "cleanupArchive=${archive_path}"
echo "copiedFiles=${copied}"
echo "deletedArchives=${deleted}"
