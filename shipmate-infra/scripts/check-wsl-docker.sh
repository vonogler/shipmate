#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.local/bin:$PATH"
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
      echo "ERROR: 지원하지 않는 DOCKER_MODE입니다: ${DOCKER_MODE}"
      exit 2
      ;;
  esac
}

echo "[1/4] Docker CLI"
docker_cmd --version
docker_cmd compose version

echo
echo "[2/4] Docker socket"
if [ -S /var/run/docker.sock ]; then
  ls -l /var/run/docker.sock
elif [[ "${DOCKER_MODE}" == "windows" ]] || { [[ "${DOCKER_MODE}" == "auto" ]] && command -v cmd.exe >/dev/null 2>&1 && windows_docker version >/dev/null 2>&1; }; then
  echo "WSL /var/run/docker.sock은 없지만 Windows Docker Desktop CLI로 Docker daemon에 연결됩니다."
else
  echo "ERROR: /var/run/docker.sock 이 없습니다."
  echo "Docker Desktop > Settings > Resources > WSL Integration 에서 현재 WSL 배포판을 켠 뒤 Docker Desktop을 재시작하세요."
  exit 2
fi

echo
echo "[3/4] Docker daemon"
docker_cmd version

echo
echo "[4/4] Containers"
docker_cmd ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
