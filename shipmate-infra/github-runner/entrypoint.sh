#!/usr/bin/env bash
set -euo pipefail

: "${GITHUB_REPOSITORY_URL:?GITHUB_REPOSITORY_URL이 필요합니다. 예: https://github.com/vonogler/shipmate}"
: "${GITHUB_RUNNER_TOKEN:?GITHUB_RUNNER_TOKEN이 필요합니다. GitHub Actions self-hosted runner 등록 token을 넣으세요.}"

RUNNER_NAME="${GITHUB_RUNNER_NAME:-shipmate-staging-runner}"
RUNNER_LABELS="${GITHUB_RUNNER_LABELS:-shipmate-staging,self-hosted,linux,x64}"
RUNNER_WORKDIR="${GITHUB_RUNNER_WORKDIR:-_work}"

cleanup() {
  ./config.sh remove --unattended --token "${GITHUB_RUNNER_TOKEN}" || true
}

trap cleanup EXIT INT TERM

./config.sh \
  --url "${GITHUB_REPOSITORY_URL}" \
  --token "${GITHUB_RUNNER_TOKEN}" \
  --name "${RUNNER_NAME}" \
  --labels "${RUNNER_LABELS}" \
  --work "${RUNNER_WORKDIR}" \
  --unattended \
  --replace

./run.sh
