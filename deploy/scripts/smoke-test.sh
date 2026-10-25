#!/usr/bin/env bash
# Prueba de humo de punta a punta (F5-RAÍZ-01): health, login, config, sesión, un frame y fin.
#
# Uso: deploy/scripts/smoke-test.sh [BASE_URL]
#   BASE_URL por defecto: http://localhost:8080/api   (en la EC2: https://<PUBLIC_HOST>/api)
#   Credenciales: SMOKE_USER / SMOKE_PASS, o ADMIN_USERNAME / ADMIN_PASSWORD de deploy/.env.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
env_file="$here/../.env"
env_get() { [ -f "$env_file" ] && grep -E "^$1=" "$env_file" | tail -1 | cut -d= -f2- | sed -E 's/[[:space:]]+#.*$//; s/[[:space:]]+$//'; }

base="${1:-${BASE_URL:-http://localhost:8080/api}}"
user="${SMOKE_USER:-$(env_get ADMIN_USERNAME)}"
pass="${SMOKE_PASS:-$(env_get ADMIN_PASSWORD)}"
if [ -z "$user" ] || [ -z "$pass" ]; then echo "Define SMOKE_USER y SMOKE_PASS (o ADMIN_* en deploy/.env)"; exit 2; fi

fail() { echo "✘ $1"; exit 1; }
json_field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
request_id="smoke-$(date +%s)"

echo "→ $base"

code=$(curl -s -o /dev/null -w '%{http_code}' "$base/actuator/health")
[ "$code" = 200 ] || fail "health respondió $code (¿IA o base de datos caídas?)"
echo "✔ health UP"

login=$(curl -sf -X POST "$base/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$user\",\"password\":\"$pass\"}") || fail "login rechazado"
token=$(echo "$login" | json_field token)
[ -n "$token" ] || fail "login sin token"
echo "✔ login"
auth=(-H "Authorization: Bearer $token" -H 'Content-Type: application/json' -H "X-Request-Id: $request_id")

curl -sf "$base/config" "${auth[@]}" | grep -q '"severity"' || fail "GET /config"
echo "✔ config"

session=$(curl -sf -X POST "$base/sessions" "${auth[@]}" -d '{"field_name":"smoke-test"}') || fail "POST /sessions"
session_id=$(echo "$session" | json_field session_id)
echo "✔ sesión $session_id"

frame=$(curl -s -w '\n%{http_code}' -X POST "$base/sessions/$session_id/frames" "${auth[@]}" \
  -d "{\"frame_b64\":\"$(tr -d '\r\n' < "$here/frame-prueba.b64")\"}")
frame_code=$(echo "$frame" | tail -1)
[ "$frame_code" = 200 ] || fail "frame respondió $frame_code: $(echo "$frame" | head -1)"
status=$(echo "$frame" | head -1 | json_field status)
echo "✔ frame analizado (status=$status, request_id=$request_id)"

curl -sf -X POST "$base/sessions/$session_id/end" "${auth[@]}" | grep -q '"COMPLETED"' || fail "fin de sesión"
echo "✔ sesión finalizada"
echo "Smoke test OK"
