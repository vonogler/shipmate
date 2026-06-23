#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

GITHUB_STEP_SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-}"
SMOKE_CLEANUP_REPORT_FILE="${SMOKE_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.json}"
SMOKE_CLEANUP_JUNIT_FILE="${SMOKE_CLEANUP_JUNIT_FILE:-shipmate-infra/reports/cleanup/staging-smoke-cleanup-report.xml}"
ARTIFACT_CLEANUP_REPORT_FILE="${ARTIFACT_CLEANUP_REPORT_FILE:-shipmate-infra/reports/cleanup/github-smoke-artifact-cleanup-report.json}"

json_value() {
  local file_path="$1"
  local key="$2"
  if [[ ! -f "${file_path}" ]]; then
    printf "%s" "-"
    return
  fi
  local value
  value="$(sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "${file_path}" | head -n 1)"
  if [[ -z "${value}" ]]; then
    value="$(sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\\([^,}]*\\).*/\\1/p" "${file_path}" | head -n 1 | tr -d '\"')"
  fi
  printf "%s" "${value:-"-"}"
}

xml_attr() {
  local file_path="$1"
  local element="$2"
  local attr="$3"
  if [[ ! -f "${file_path}" ]]; then
    printf "%s" "-"
    return
  fi
  sed -n "s/.*<${element}[^>]* ${attr}=\"\\([^\"]*\\)\".*/\\1/p" "${file_path}" | head -n 1
}

append_cleanup_table() {
  local title="$1"
  local file_path="$2"
  if [[ ! -f "${file_path}" ]]; then
    {
      echo "## ${title}"
      echo
      echo "리포트 파일이 없어 표시할 내용이 없습니다."
      echo
    } >> "${GITHUB_STEP_SUMMARY_FILE}"
    return
  fi
  {
    echo "## ${title}"
    echo
    echo "| 항목 | 값 |"
    echo "|---|---|"
    echo "| 상태 | $(json_value "${file_path}" status) |"
    echo "| 실행 유형 | $(json_value "${file_path}" mode) |"
    echo "| 사유 | $(json_value "${file_path}" reason) |"
    echo "| 대상 row 수 | $(json_value "${file_path}" totalRows) |"
    echo "| 잔여 row 수 | $(json_value "${file_path}" remainingRows) |"
    echo "| 보호 run ID | $(json_value "${file_path}" protectRunIds) |"
    echo "| 삭제 후보 | $(json_value "${file_path}" deleteCandidates) |"
    echo "| 완료 시각 | $(json_value "${file_path}" finishedAt) |"
    echo
  } >> "${GITHUB_STEP_SUMMARY_FILE}"
}

append_junit_table() {
  local file_path="$1"
  if [[ ! -f "${file_path}" ]]; then
    return
  fi
  {
    echo "## Cleanup JUnit"
    echo
    echo "| 항목 | 값 |"
    echo "|---|---|"
    echo "| testsuite | $(xml_attr "${file_path}" testsuite name) |"
    echo "| tests | $(xml_attr "${file_path}" testsuite tests) |"
    echo "| failures | $(xml_attr "${file_path}" testsuite failures) |"
    echo "| errors | $(xml_attr "${file_path}" testsuite errors) |"
    echo "| skipped | $(xml_attr "${file_path}" testsuite skipped) |"
    echo "| testcase | $(xml_attr "${file_path}" testcase name) |"
    echo "| classname | $(xml_attr "${file_path}" testcase classname) |"
    echo "| 파일 | \`${file_path}\` |"
    echo
  } >> "${GITHUB_STEP_SUMMARY_FILE}"
}

if [[ -z "${GITHUB_STEP_SUMMARY_FILE}" ]]; then
  echo "GITHUB_STEP_SUMMARY가 없어 GitHub step summary 생성을 건너뜁니다."
  exit 0
fi

append_cleanup_table "Smoke 테스트 데이터 정리" "${SMOKE_CLEANUP_REPORT_FILE}"
append_cleanup_table "GitHub Actions artifact 정리" "${ARTIFACT_CLEANUP_REPORT_FILE}"
append_junit_table "${SMOKE_CLEANUP_JUNIT_FILE}"

echo "summary=${GITHUB_STEP_SUMMARY_FILE}"
