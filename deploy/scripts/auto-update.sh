#!/usr/bin/env bash
# Despliegue continuo por *pull* (lo ejecuta tizon-auto-update.timer cada 5 min en la EC2).
#
# Para ia y api: si GHCR tiene una imagen nueva para IMAGE_TAG, recrea solo ese servicio y espera
# a que quede healthy. Si no lo logra, vuelve a la imagen anterior (rollback) y termina con error.
# GitHub nunca entra a la EC2: no se abre SSH ni se guardan llaves en GitHub (RNF-SEG-07).
set -euo pipefail

deploy_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$deploy_dir"
env_get() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | sed -E 's/[[:space:]]+#.*$//; s/[[:space:]]+$//'; }

registry="$(env_get IMAGE_REGISTRY)"; registry="${registry:-ghcr.io/fernandomartinez018}"
tag="$(env_get IMAGE_TAG)"; tag="${tag:-main}"
compose=(docker compose --env-file .env)
timeout_s="${HEALTH_TIMEOUT_SECONDS:-240}"

log() { echo "$(date -Iseconds) auto-update: $*"; }

wait_healthy() {
  local svc="$1" deadline=$((SECONDS + timeout_s)) cid state
  while [ "$SECONDS" -lt "$deadline" ]; do
    cid="$("${compose[@]}" ps -q "$svc")"
    state="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo missing)"
    [ "$state" = healthy ] && return 0
    sleep 5
  done
  return 1
}

updated=0
for svc in ia api; do   # primero la IA: la salud de la API depende de ella
  image="$registry/tizon-$svc:$tag"
  old_id="$(docker image inspect -f '{{.Id}}' "$image" 2>/dev/null || echo none)"
  if ! docker pull -q "$image" >/dev/null; then
    log "no se pudo descargar $image (¿docker login ghcr.io?)"; exit 1
  fi
  new_id="$(docker image inspect -f '{{.Id}}' "$image")"
  [ "$old_id" = "$new_id" ] && continue

  log "nueva imagen para $svc: ${new_id:7:12} (antes ${old_id:7:12})"
  [ "$old_id" != none ] && docker tag "$old_id" "$registry/tizon-$svc:rollback"
  "${compose[@]}" up -d --no-deps --no-build "$svc"

  if wait_healthy "$svc"; then
    log "$svc healthy"
    updated=1
  else
    log "$svc no quedó healthy en ${timeout_s}s"
    if [ "$old_id" != none ]; then
      log "rollback de $svc a ${old_id:7:12}"
      docker tag "$registry/tizon-$svc:rollback" "$image"
      "${compose[@]}" up -d --no-deps --no-build "$svc"
      if wait_healthy "$svc"; then log "$svc restaurado"; else log "$svc sigue sin health tras el rollback"; fi
    fi
    exit 1
  fi
done

[ "$updated" = 1 ] && docker image prune -f >/dev/null && log "despliegue completado"
exit 0
