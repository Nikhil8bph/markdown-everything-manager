#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
backend_dir="$repo_dir/backend"
temp_root="$(mktemp -d "${TMPDIR:-/tmp}/markcraft-mcp-acceptance.XXXXXX")"
mcp_token="$(openssl rand -hex 32)"
server_pid=""
server_log=""
server_url=""

stop_server() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  server_pid=""
}

cleanup() {
  stop_server
  rm -rf "$temp_root"
}
trap cleanup EXIT

free_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()'
}

start_server() {
  local mode="$1"
  local deletion_setting="$2"
  local port
  port="$(free_port)"
  local vault_root="$temp_root/vault-$mode"
  server_log="$temp_root/server-$mode.log"
  MARKCRAFT_MCP_ENABLED=true \
  MARKCRAFT_MCP_TOKEN="$mcp_token" \
  MARKCRAFT_MCP_AGENT_DELETION_ENABLED="$deletion_setting" \
  MARKCRAFT_VAULT_ROOT="$vault_root" \
    java -jar "$backend_dir/target/markcraft-0.1.0-SNAPSHOT.jar" --server.port="$port" >"$server_log" 2>&1 &
  server_pid=$!
  server_url="http://127.0.0.1:$port"

  for attempt in $(seq 1 60); do
    if curl --silent --fail "$server_url/api/v1/vault/tree" >/dev/null; then
      return
    fi
    if ! kill -0 "$server_pid" 2>/dev/null; then
      cat "$server_log"
      return 1
    fi
    sleep 1
  done
  cat "$server_log"
  return 1
}

verify_http_boundaries() {
  local no_token_status wrong_token_status hostile_host_status hostile_origin_status
  no_token_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --request POST --header 'Content-Type: application/json' --data '{}' "$server_url/mcp")"
  wrong_token_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --request POST --header 'Content-Type: application/json' --header 'Authorization: Bearer wrong-token' \
    --data '{}' "$server_url/mcp")"
  hostile_host_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --header "Authorization: Bearer $mcp_token" --header 'Host: attacker.example' "$server_url/mcp")"
  hostile_origin_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
    --request POST --header 'Content-Type: application/json' --header "Authorization: Bearer $mcp_token" \
    --header 'Origin: https://attacker.example' --data '{}' "$server_url/mcp")"
  [[ "$no_token_status" == 401 && "$wrong_token_status" == 401 \
    && "$hostile_host_status" == 400 && "$hostile_origin_status" == 400 ]]
  printf 'PASS: unauthorized=%s/%s hostile-host=%s hostile-origin=%s\n' \
    "$no_token_status" "$wrong_token_status" "$hostile_host_status" "$hostile_origin_status"
}

cd "$repo_dir"
./scripts/package-frontend.sh
cd "$backend_dir"
./mvnw -q clean package -DskipTests
./mvnw -q test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/mcp-test-classpath.txt -Dmdep.includeScope=test

client_classpath="$backend_dir/target/test-classes:$backend_dir/target/classes:$(cat "$backend_dir/target/mcp-test-classpath.txt")"

start_server disabled false
verify_http_boundaries
MARKCRAFT_MCP_TOKEN="$mcp_token" java -cp "$client_classpath" \
  io.github.nikhil8bph.markcraft.service.McpPackagedAcceptanceClient "$server_url" disabled
stop_server

start_server enabled true
verify_http_boundaries
curl --silent --fail "$server_url/" >/dev/null
MARKCRAFT_MCP_TOKEN="$mcp_token" java -cp "$client_classpath" \
  io.github.nikhil8bph.markcraft.service.McpPackagedAcceptanceClient "$server_url" enabled
printf 'PASS: packaged UI and vault API are served on the same loopback origin at %s\n' "$server_url"
printf 'PASS: packaged MCP acceptance completed with a temporary external vault\n'

if [[ "${KEEP_MCP_ACCEPTANCE_SERVER:-false}" == "true" ]]; then
  printf 'Keeping packaged server pid %s alive for manual browser inspection at %s\n' "$server_pid" "$server_url"
  printf 'Vault: %s/vault-enabled\n' "$temp_root"
  printf 'MCP token is available only in this script process; set MARKCRAFT_MCP_TOKEN in your browser client separately.\n'
  wait "$server_pid"
fi
