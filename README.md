# TizonApiIntegration

API de negocio del sistema Tizón Tardío (Spring Boot 3.5, Java 21). Autentica usuarios con JWT,
gestiona las sesiones de escaneo y reenvía cada frame al servicio de IA (`TizonDetectionModel`)
por la red interna. El contrato de la API está en `../contracts/tizon-api.yaml`.

## Requisitos
- JDK 21
- PostgreSQL 16 (en local se puede usar el `docker compose` de `../deploy/`)
- Servicio de IA accesible en `IA_BASE_URL`

## Configuración (variables de entorno)
Los secretos **no tienen valor por defecto**: si faltan, la API no arranca y el error indica cuál falta.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | No | JDBC de PostgreSQL. Por defecto `jdbc:postgresql://localhost:5432/tizon_db` |
| `DB_USER` | No | Por defecto `tizon_user` |
| `DB_PASS` | **Sí** | Contraseña de la base de datos |
| `JWT_SECRET` | **Sí** | Al menos 32 bytes. Generar con `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | No | Vigencia del token. Por defecto `86400000` (24 h) |
| `IA_BASE_URL` | **Sí** | URL interna del servicio de IA, p. ej. `http://ia:8000` |
| `IA_TIMEOUT_SECONDS` | No | Por defecto `10` |
| `INTERNAL_API_TOKEN` | **Sí** | Secreto compartido con la IA (header `X-Internal-Token`) |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes separados por comas. Vacío = CORS deshabilitado |
| `PORT` | No | Por defecto `8080`. Todas las rutas cuelgan de `/api` |

## Ejecución local
```bash
export DB_PASS=... JWT_SECRET="$(openssl rand -base64 48)" IA_BASE_URL=http://localhost:8000 INTERNAL_API_TOKEN=...
./mvnw spring-boot:run
```

## Pruebas
```bash
./mvnw -B verify      # compila y ejecuta las pruebas con el perfil "test" (H2 en memoria)
```
Las pruebas usan `src/test/resources/application-test.yml`; no necesitan PostgreSQL ni la IA.

## Docker
```bash
docker build -t tizon-api .
docker run --env-file ../deploy/.env -p 8080:8080 tizon-api
```
La imagen no contiene secretos ni URLs de ambiente: todo se inyecta al ejecutar el contenedor.
