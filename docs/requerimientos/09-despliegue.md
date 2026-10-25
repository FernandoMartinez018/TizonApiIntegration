# 09 · Despliegue en EC2 y entrega de la APK

> Claude Code prepara los archivos y scripts. **El usuario ejecuta los pasos en AWS** (crear la instancia, el Security Group y el acceso SSH).

## 1. Infraestructura (manual, una vez)
| Ítem | Valor recomendado | Justificación |
|---|---|---|
| Región | `sa-east-1` (São Paulo) o `us-east-1` | São Paulo tiene menor latencia desde Colombia. us-east-1 es más barata. Decide el usuario. |
| Instancia | `t3.medium` (2 vCPU, 4 GB) como mínimo | TensorFlow (~1–1,5 GB) + Spring (~0,5 GB) + PostgreSQL + Caddy. `t3.small` (2 GB) no alcanza con holgura. Medir en F4. |
| SO | Ubuntu Server 24.04 LTS | |
| Disco | gp3 de 30 GB | Imágenes Docker de TensorFlow, unos 2–3 GB |
| IP | Elastic IP | Necesaria para que el nombre `sslip.io` no cambie |
| Security Group | 80 y 443 abiertos a 0.0.0.0/0. 22 **solo a la IP del administrador** | RNF-SEG-07 |
| Par de llaves | Nuevo (las `.pem` anteriores quedan retiradas). **Se guarda fuera de cualquier repositorio.** | RNF-SEG-01 |
| Costo | Indicar el costo mensual estimado en el README de despliegue después de consultar los precios vigentes de AWS | No asumir precios |

## 2. Archivos que crea Claude Code (`deploy/` en la raíz)
```
deploy/
├── docker-compose.yml       # caddy, api, ia, db (redes public/internal, healthchecks, restart)
├── Caddyfile                # {$PUBLIC_HOST} → reverse_proxy api:8080; request_body max_size 3MB
├── .env.example             # todas las variables de 02-arquitectura, sin valores reales
├── models/.gitkeep          # aquí van el .h5 y model_meta.json en el servidor
├── scripts/
│   ├── bootstrap-ec2.sh     # instala Docker Engine + plugin compose, crea usuario, ufw opcional
│   ├── deploy.sh            # git pull de los 3 repos + docker compose up -d --build
│   ├── backup-db.sh         # pg_dump diario a /var/backups/tizon (retención 7 días)
│   └── uptime.sh            # curl /api/actuator/health cada minuto → log + resumen (RNF-DIS-01)
└── README.md                # runbook paso a paso (ver §3)
```

## 3. Runbook (contenido mínimo del `deploy/README.md`)
1. Crear la EC2 y la Elastic IP según §1, y conectarse por SSH.
2. Ejecutar `bootstrap-ec2.sh`.
3. Clonar los 3 repos en `/opt/tizon/` y copiar `deploy/` (o clonar el repo `TizonDeploy`, si se crea).
4. Copiar `modelo_tizon_4c.keras` y `model_meta.json` a `deploy/models/` (por `scp`). El `.h5` anterior solo hace falta para volver atrás.
5. Crear `deploy/.env` desde `.env.example`:
   - Generar los secretos con `openssl rand -base64 48`.
   - `PUBLIC_HOST=<ip-con-guiones>.sslip.io`.
6. Ejecutar `docker compose up -d --build` y esperar a que los servicios estén `healthy`.
7. Verificar:
   - `curl https://<PUBLIC_HOST>/api/actuator/health` responde UP.
   - Un login con el ADMIN inicial funciona.
8. Instalar el *cron* de `backup-db.sh` y `uptime.sh`.
9. **Apagado:** `docker compose down` conserva los volúmenes. Detener la instancia cuando no se use, para controlar el costo.

## 4. APK *release* para instalación manual
- **Firma:** *keystore* generado una sola vez con `keytool`, guardado fuera de git. Las variables `TIZON_KEYSTORE_PATH`, `TIZON_KEYSTORE_PASSWORD`, `TIZON_KEY_ALIAS` y `TIZON_KEY_PASSWORD` se leen en `composeApp/build.gradle.kts`.
- **Configuración:** `-Ptizon.apiBaseUrl=https://<PUBLIC_HOST>/api/`.
- `versionCode` y `versionName` se incrementan en cada entrega (`1.1.0` para el 8-nov).
- **Activar R8:** `isMinifyEnabled = true`, con reglas para kotlinx-serialization, Ktor y Koin. Si da problemas antes del 8-nov, se deja desactivado y se documenta.
- **Entregable:** `TizonMovile-1.1.0-release.apk` con su SHA-256 y las instrucciones de instalación (habilitar "Instalar apps desconocidas").
