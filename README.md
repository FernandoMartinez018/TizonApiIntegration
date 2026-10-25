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
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | **Sí, si no existe ningún ADMIN** | Crean el ADMIN inicial en el primer arranque (contraseña ≥ 8 caracteres). Si faltan y no hay ADMIN, la API no arranca |
| `IA_BASE_URL` | **Sí** | URL interna del servicio de IA, p. ej. `http://ia:8000` |
| `IA_TIMEOUT_SECONDS` | No | Por defecto `10` |
| `INTERNAL_API_TOKEN` | **Sí** | Secreto compartido con la IA (header `X-Internal-Token`) |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes separados por comas. Vacío = CORS deshabilitado |
| `ALERT_THRESHOLD_PCT` | No | % afectado que genera alerta al finalizar una sesión. Por defecto `30` |
| `PORT` | No | Por defecto `8080`. Todas las rutas cuelgan de `/api` |

Los umbrales de severidad (10 % y 30 %), el mínimo de frames para alertar y el ritmo de envío están en
`application.yaml` (`tizon.*`) y la app los obtiene de `GET /api/config`.

Clases (`ClassId`, en orden de severidad): `SANO`, `TIZON_TEMPRANO` y `TIZON_TARDIO`. Las dos de tizón cuentan como
afectadas para el % afectado, la severidad y las alertas; `label_counts` las separa. Los frames `NOT_POTATO` (hojas de
otras plantas, sin análisis de tizón) y `NO_VEGETATION` se guardan, pero no cuentan en el % afectado.

## Usuarios y roles
Los usuarios viven en la tabla `app_users` (contraseñas con BCrypt, coste 12). El registro crea cuentas
`AGRICULTOR`; un `ADMIN` cambia roles (`TECNICO`, `ADMIN`) y habilitación en `/admin/users`. Un AGRICULTOR solo
ve sus sesiones; TECNICO y ADMIN leen las de todos. Un usuario deshabilitado pierde el acceso aunque su token
siga vigente. Tras 5 intentos fallidos en 5 minutos, el login de ese usuario queda bloqueado 5 minutos (429).

## Endpoints (contrato v2)
`POST /auth/register`, `POST /auth/login`, `GET /auth/me`, `GET /admin/users`, `PATCH /admin/users/{id}`, `GET /config`, `POST|GET /sessions`, `GET /sessions/{id}`,
`POST /sessions/{id}/frames`, `POST /sessions/{id}/end` y `GET /actuator/health`. El contrato completo está en
`contracts/tizon-api.yaml` (copia de la fuente de verdad de la carpeta raíz; se actualiza con
`../contracts/sync-copies.sh`). Con el perfil `dev` (`SPRING_PROFILES_ACTIVE=dev`) se publica el OpenAPI
generado en `/api/v3/api-docs`, y `OpenApiContractTest` verifica que coincida con el contrato.

## Observabilidad
- `X-Request-Id`: se usa el de la app (o se genera), va en el MDC, en la respuesta, en el cuerpo `Error` y se
  reenvía a la IA, de modo que una petición se sigue en los logs de los tres servicios.
- Logs JSON con `LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash` (activado en `../deploy/docker-compose.yml`):
  `request_id`, `session_id`, `status`, `latency_ms` e `inference_ms`. Nunca se registran tokens ni frames.
- `/actuator/health` es compuesto (`db` + `ia`): DOWN (503) si la IA no responde o su modelo no cargó.
- Métricas en `/actuator/metrics` (requiere token): `tizon.ia.latency` (timer) y `tizon.ia.errors` (por `code`).

## Base de datos
El esquema lo crea **Flyway** (`src/main/resources/db/migration`) y Hibernate solo lo valida
(`ddl-auto: validate`). Los frames no se guardan, solo los resultados (RNF-SEG-12). La retención de las
sesiones es indefinida durante la fase académica.

Las recomendaciones del resumen salen de la tabla `recommendations` (`V6__recommendations_by_disease.sql`): son
las de la enfermedad que predominó entre los frames afectados (en empate, tizón tardío), cada una con su fuente.
Las referencias completas están en `docs/requerimientos/fuentes-recomendaciones.md` de la rama `sistema`.

## Ejecución local
```bash
export DB_PASS=... JWT_SECRET="$(openssl rand -base64 48)" IA_BASE_URL=http://localhost:8000 INTERNAL_API_TOKEN=...
./mvnw spring-boot:run
```

## Pruebas
```bash
./mvnw -B verify      # compila y ejecuta las pruebas con el perfil "test" (H2 en memoria)
```
Las pruebas usan `src/test/resources/application-test.yml` (H2 en modo PostgreSQL); no necesitan la IA.
`PostgresSchemaTest` levanta un PostgreSQL 16 con Testcontainers y se omite si no hay Docker.

Cobertura con JaCoCo (RNF-PRU-02): el reporte queda en `target/site/jacoco/index.html`, y `verify` falla si los
paquetes `service` o `security` bajan del 70 % de líneas cubiertas (24-oct: 95,5 % y 88,8 %).

## Docker
```bash
docker build -t tizon-api .
docker run --env-file ../deploy/.env -p 8080:8080 tizon-api
```
La imagen no contiene secretos ni URLs de ambiente: todo se inyecta al ejecutar el contenedor.

## CI/CD (GitHub Actions)
`.github/workflows/ci.yml` corre en cada push y PR a `dev`, `qa` y `main`: `./mvnw -B verify` y gitleaks.
En cada push a `qa` o `main` publica la imagen `ghcr.io/<owner>/tizon-api` con las etiquetas `:qa` o `:main`
y `:sha-<commit>`. La EC2 la descarga por *pull* (`deploy/scripts/auto-update.sh`): GitHub nunca entra a la EC2.
Si el paquete de GHCR es privado, la EC2 necesita un token de solo lectura (`read:packages`).

