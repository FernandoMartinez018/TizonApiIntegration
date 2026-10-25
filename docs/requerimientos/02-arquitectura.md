# 02 · Arquitectura objetivo

```
 Android (APK)                         EC2 (Ubuntu 24.04, docker compose)
 ┌───────────────┐   HTTPS 443   ┌──────────────────────────────────────────────────┐
 │ TizonMovile   │──────────────►│ caddy (TLS Let's Encrypt, <ip>.sslip.io)         │
 │ CameraX, GPS  │               │   └─► reverse_proxy → api:8080                    │
 │ (AOSP), OSM   │               │ api  (Spring Boot, /api)  ──► db:5432 (Postgres)  │
 └───────────────┘               │   └─► http://ia:8000 + X-Internal-Token           │
                                 │ ia   (FastAPI + TF, modelo en volumen /models)    │
                                 │ db   (PostgreSQL 16, volumen pgdata)              │
                                 └──────────────────────────────────────────────────┘
       Puertos expuestos a Internet: solo 80/443 (caddy). 22 restringido a la IP del administrador.
```

## Componentes
| Servicio | Imagen | Red | Puertos | Volúmenes |
|---|---|---|---|---|
| `caddy` | `caddy:2` | `public`, `internal` | 80, 443 publicados | `caddy_data`, `Caddyfile` |
| `api` | Construida desde `TizonApiIntegration/Dockerfile` | `internal` | 8080 (solo interno) | — |
| `ia` | Construida desde `TizonDetectionModel/Dockerfile` | `internal` | 8000 (solo interno) | `./models:/models:ro` |
| `db` | `postgres:16-alpine` | `internal` | 5432 (solo interno) | `pgdata` |

Todo vive en `deploy/` dentro de la carpeta raíz (no en un repo de servicio). Opcionalmente se convierte en un 4.º repositorio `TizonDeploy` (ver 10-pendientes).

## Configuración (variables de entorno)
| Variable | Servicio | Obligatoria | Ejemplo o nota |
|---|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASS` | api | Sí (sin valor por defecto para `DB_PASS`) | `jdbc:postgresql://db:5432/tizon` |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | db | Sí | Los mismos valores que arriba |
| `JWT_SECRET` | api | Sí, al menos 32 bytes, sin valor por defecto | `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | api | No | `86400000` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | api | Sí, en el primer arranque | Crea el admin inicial si no existe |
| `IA_BASE_URL` | api | Sí | `http://ia:8000` |
| `IA_TIMEOUT_SECONDS` | api | No | `10` |
| `INTERNAL_API_TOKEN` | api, ia | Sí | Secreto compartido. La IA rechaza peticiones sin él (401). |
| `MODEL_PATH`, `MODEL_META_PATH` | ia | Sí | `/models/modelo_tizon_4c.keras`, `/models/model_meta.json` |
| `ALERT_THRESHOLD_PCT` | api | No | `30` |
| `CORS_ALLOWED_ORIGINS` | api | No | Vacío (la app nativa no lo necesita) |
| `PUBLIC_HOST` | caddy | Sí | `3-91-10-20.sslip.io` |

**App:** `API_BASE_URL` se inyecta por `BuildConfig` desde `local.properties` o con `-Ptizon.apiBaseUrl=...`. Hay variantes `debug` (puede apuntar a `http://10.0.2.2:8080/api/`) y `release` (solo `https://`).

## Decisiones técnicas y su justificación
- **Contenedor permanente para la IA:** elimina el arranque en frío (*cold start*) de 10–15 s de Lambda y permite cumplir el RNF de ≤ 5 s. El modelo pesa 9,5 MB, así que la carga en memoria es rápida.
- **Token interno entre la API y la IA:** defensa en profundidad aunque la IA no esté publicada.
- **Caddy:** TLS automático con poca configuración, open source.
- **Flyway:** el esquema queda versionado y reproducible. Reemplaza `ddl-auto: update`.
- **Una sola fuente de umbrales y clases:** la IA publica `classes` y `model_version` en `/health`. La API publica los umbrales en `GET /api/config`. La app no tiene valores fijos.
