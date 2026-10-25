#!/usr/bin/env bash
# Despliegue manual en la EC2.
#   deploy.sh          → descarga las imágenes de GHCR (IMAGE_TAG) y levanta todo.
#   deploy.sh --build  → git pull de los 3 repos en /opt/tizon y construye las imágenes en la EC2
#                        (alternativa si GHCR no está disponible; tarda más en una t3.medium).
set -euo pipefail

deploy_dir="$(cd "$(dirname "$0")/.." && pwd)"
root="$(dirname "$deploy_dir")"
cd "$deploy_dir"
[ -f .env ] || { echo "Falta deploy/.env (copiar desde .env.example)"; exit 1; }
[ -f models/model_meta.json ] || echo "Aviso: falta models/model_meta.json; la IA arrancará degradada"

compose=(docker compose --env-file .env)

if [ "${1:-}" = "--build" ]; then
  for repo in TizonDetectionModel TizonApiIntegration; do
    git -C "$root/$repo" pull --ff-only
  done
  "${compose[@]}" up -d --build
else
  "${compose[@]}" pull
  "${compose[@]}" up -d --no-build
fi

"${compose[@]}" ps
