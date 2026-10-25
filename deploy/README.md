# Despliegue — Tizón Tardío

`docker compose` con 4 servicios: **db** (PostgreSQL 16), **ia** (FastAPI + TensorFlow), **api**
(Spring Boot) y **caddy** (HTTPS con Let's Encrypt, perfil `tls`). Solo caddy publica puertos a Internet;
la IA y la base de datos están en una red interna sin salida (RNF-SEG-07).

> **Estado (23-oct):** todo está preparado, pero **no se ha creado nada en AWS** (decisión del usuario).
> Los pasos de la sección 3 los ejecuta el usuario cuando decida desplegar (pendiente P-03).

## 1. Flujo de CI/CD

```
 push/PR a dev, qa o main ──► GitHub Actions: pruebas + linter + gitleaks (los 3 repos)
 push a qa o main         ──► publica ghcr.io/fernandomartinez018/tizon-{ia,api}:{qa|main} y :sha-<commit>
                              y la APK release como artefacto (TizonMovile, solo main)
 EC2 (cada 5 min)         ──► tizon-auto-update.timer → scripts/auto-update.sh
                              docker pull → si hay imagen nueva: recrea ia y luego api → espera healthy
                              → si no queda healthy en 4 min: vuelve a la imagen anterior (rollback)
```

- **GitHub nunca entra a la EC2**: no se abre el puerto 22 a GitHub ni se guardan llaves SSH en GitHub.
- `IMAGE_TAG` en `.env` decide qué rama sigue la EC2 (`main` en producción, `qa` en un ambiente de QA).
  Para fijar una versión exacta o volver atrás: `IMAGE_TAG=sha-<commit>` y `scripts/deploy.sh`.
- Si los paquetes de GHCR son privados: en la EC2, una vez, `docker login ghcr.io -u <usuario>` con un
  *token* de GitHub de solo lectura (`read:packages`).
- Registro del auto-update: `journalctl -u tizon-auto-update -n 50`.

## 2. Ejecución local
```bash
cp deploy/.env.example deploy/.env      # completar POSTGRES_PASSWORD, JWT_SECRET e INTERNAL_API_TOKEN
cp modelo_tizon_4c.keras deploy/models/            # modelo en uso (D-08); no va a git
cp TizonDetectionModel/models/model_meta.json deploy/models/
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build
docker compose -f deploy/docker-compose.yml --env-file deploy/.env ps      # db, ia y api "healthy"
SMOKE_USER=... SMOKE_PASS=... deploy/scripts/smoke-test.sh                 # http://localhost:8080/api
```
- La API queda en `http://localhost:8080/api` (para el emulador: `http://10.0.2.2:8080/api/`).
- Para probar con un celular en la LAN (plan B de P-03): `API_BIND=0.0.0.0` y agregar la IP del PC en
  `TizonMovile/composeApp/src/debug/res/xml/network_security_config.xml`.
- HTTPS local opcional: `COMPOSE_PROFILES=tls` y `PUBLIC_HOST=localhost` (Caddy usa su CA interna).

## 3. Runbook en la EC2 (manual, una vez)
1. **AWS** (usuario): EC2 Ubuntu Server 24.04, `t3.medium` (2 vCPU, 4 GB) o mayor, disco gp3 de 30 GB,
   Elastic IP y Security Group con 80 y 443 abiertos y 22 **solo a la IP del administrador**. Par de llaves
   nuevo, guardado fuera de cualquier repositorio. Región: `sa-east-1` o `us-east-1` (decide el usuario).
   Costo mensual: consultar la calculadora de precios de AWS vigente antes de crear la instancia.
2. Copiar y ejecutar el bootstrap: `scp deploy/scripts/bootstrap-ec2.sh ubuntu@<IP>:` y
   `sudo bash bootstrap-ec2.sh` (Docker, compose, swap, zona horaria). Volver a entrar por SSH.
3. Clonar los repos y copiar `deploy/`:
   ```bash
   cd /opt/tizon
   git clone https://github.com/FernandoMartinez018/TizonDetectionModel.git
   git clone https://github.com/FernandoMartinez018/TizonApiIntegration.git
   # desde el PC: scp -r deploy ubuntu@<IP>:/opt/tizon/
   ```
   (Los repos solo se necesitan para `deploy.sh --build`; con GHCR basta `deploy/`).
4. Copiar el modelo: `scp modelo_tizon_4c.keras TizonDetectionModel/models/model_meta.json ubuntu@<IP>:/opt/tizon/deploy/models/`.
5. Crear `/opt/tizon/deploy/.env` desde `.env.example`: secretos con `openssl rand -base64 48`,
   `COMPOSE_PROFILES=tls`, `PUBLIC_HOST=<ip-con-guiones>.sslip.io`, `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
   `chmod 600 .env`.
6. `docker login ghcr.io` (si los paquetes son privados) y `scripts/deploy.sh`. Esperar a que los 4
   servicios estén `healthy` (`docker compose --env-file .env ps`).
7. Verificar: `curl https://<PUBLIC_HOST>/api/actuator/health` → `UP` y
   `scripts/smoke-test.sh https://<PUBLIC_HOST>/api`.
8. Activar el CD y las tareas programadas:
   ```bash
   sudo cp scripts/systemd/tizon-auto-update.* /etc/systemd/system/
   sudo systemctl daemon-reload && sudo systemctl enable --now tizon-auto-update.timer
   crontab -e
   # * * * * * /opt/tizon/deploy/scripts/uptime.sh
   # 30 2 * * * /opt/tizon/deploy/scripts/backup-db.sh >> /var/log/tizon/backup.log 2>&1
   ```
9. **Apagado:** `docker compose --env-file .env down` conserva los volúmenes (`pgdata`). Detener la
   instancia cuando no se use, para controlar el costo.

## 4. Operación
| Tarea | Comando (en `/opt/tizon/deploy`) |
|---|---|
| Estado | `docker compose --env-file .env ps` |
| Logs | `docker compose --env-file .env logs -f api ia` |
| Disponibilidad del día | `scripts/uptime.sh --summary` |
| Respaldo manual | `scripts/backup-db.sh` |
| Restaurar respaldo | `gunzip -c /var/backups/tizon/<archivo>.sql.gz \| docker compose --env-file .env exec -T db psql -U tizon -d tizon` |
| Cambiar el modelo | reemplazar `models/*.h5` y `models/model_meta.json` → `docker compose --env-file .env restart ia` |
| Prueba de recuperación (RNF-DIS-02) | `docker kill tizon-ia-1` → vuelve sola en menos de 60 s (`restart: unless-stopped`) |

## 5. Archivos
| Archivo | Uso |
|---|---|
| `docker-compose.yml` | Servicios, redes `public`/`internal`, healthchecks, reinicio automático y límites de memoria |
| `Caddyfile` | `{$PUBLIC_HOST}` → `api:8080` solo para `/api/*`, cuerpo máximo de 3 MB |
| `.env.example` | Todas las variables, sin valores reales |
| `models/` | `.h5` + `model_meta.json` (no se versionan) |
| `scripts/auto-update.sh` | CD por *pull* con rollback (lo ejecuta el *timer* de systemd) |
| `scripts/deploy.sh` | Despliegue manual (`--build` para construir en la EC2) |
| `scripts/smoke-test.sh` | Health, login, config, sesión, frame y fin |
| `scripts/bootstrap-ec2.sh` | Preparación única de la EC2 |
| `scripts/backup-db.sh` / `uptime.sh` | Respaldo diario (7 días) y registro de disponibilidad |
