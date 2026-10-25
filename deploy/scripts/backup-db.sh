#!/usr/bin/env bash
# Respaldo diario de PostgreSQL (cron 02:30). Conserva 7 días.
#   30 2 * * * /opt/tizon/deploy/scripts/backup-db.sh >> /var/log/tizon/backup.log 2>&1
set -euo pipefail

deploy_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$deploy_dir"
env_get() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | sed -E 's/[[:space:]]+#.*$//; s/[[:space:]]+$//'; }

backup_dir="${BACKUP_DIR:-/var/backups/tizon}"
mkdir -p "$backup_dir"
file="$backup_dir/tizon-$(date +%F_%H%M).sql.gz"

docker compose --env-file .env exec -T db \
  pg_dump -U "$(env_get POSTGRES_USER)" -d "$(env_get POSTGRES_DB)" --no-owner | gzip > "$file"
echo "$(date -Iseconds) respaldo $file ($(du -h "$file" | cut -f1))"

find "$backup_dir" -name 'tizon-*.sql.gz' -mtime +7 -delete
