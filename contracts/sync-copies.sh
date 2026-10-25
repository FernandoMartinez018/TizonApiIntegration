#!/usr/bin/env bash
# Copia el contrato (fuente de verdad) a los repos que lo usan en sus pruebas de contrato.
# Ejecutar después de cada cambio en contracts/tizon-api.yaml y commitear la copia en cada repo.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
for repo in TizonDetectionModel TizonApiIntegration TizonMovile; do
  if [ -d "$root/$repo/contracts" ]; then
    cp "$root/contracts/tizon-api.yaml" "$root/$repo/contracts/tizon-api.yaml"
    echo "copiado → $repo/contracts/tizon-api.yaml"
  fi
done
