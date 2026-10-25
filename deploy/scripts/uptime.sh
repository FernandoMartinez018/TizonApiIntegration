#!/usr/bin/env bash
# Disponibilidad (RNF-DIS-01): consulta el health compuesto (db + ia) y lo registra.
#   * * * * * /opt/tizon/deploy/scripts/uptime.sh
#   uptime.sh --summary [YYYY-MM-DD]   → % UP del día (por defecto hoy)
set -euo pipefail

log_file="${UPTIME_LOG:-/var/log/tizon/uptime.log}"
url="${HEALTH_URL:-http://127.0.0.1:8080/api/actuator/health}"

if [ "${1:-}" = "--summary" ]; then
  day="${2:-$(date +%F)}"
  grep "^$day" "$log_file" | awk -v d="$day" '
    { total++; if ($2 == "UP") up++ }
    END { if (total == 0) { print d ": sin datos"; exit 1 }
          printf "%s: %.2f%% UP (%d de %d minutos)\n", d, 100 * up / total, up, total }'
  exit 0
fi

code="$(curl -s -o /dev/null -m 10 -w '%{http_code}' "$url" || true)"
[ "$code" = 200 ] && state=UP || state=DOWN
echo "$(date -Iseconds) $state $code" >> "$log_file"
