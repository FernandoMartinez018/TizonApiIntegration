# 07 · Backlog ejecutable por fases

**Cómo usarlo (Claude Code):**
- Ejecuta las fases en orden. Dentro de cada fase, sigue el orden de las tareas, salvo que la columna *Depende* permita paralelizar.
- Marca cada tarea con `[x]` en este archivo al terminarla (cumpliendo la definición de terminado de `CLAUDE.md`).
- Si una tarea queda bloqueada, anótala en `10-pendientes.md`.

**Repos:** `IA` = TizonDetectionModel · `API` = TizonApiIntegration · `MOB` = TizonMovile · `RAÍZ` = carpeta raíz

| Fase | Fechas objetivo | Rama |
|---|---|---|
| F1 Seguridad y limpieza | 23–25 oct | `dev` (todas las fases; ver CLAUDE.md §2.3) |
| F2 Contrato v2 | 24–26 oct | `fase/f2-contrato` |
| F5a Infraestructura local | 25–27 oct | `fase/f5-infra` |
| F3 Flujo + UX | 26–30 oct | `fase/f3-flujo-ux` |
| F4 Modelo | 28 oct–1 nov | `fase/f4-modelo` |
| F6 Usuarios, GPS y alertas | 1–4 nov | `fase/f6-usuarios-gps-alertas` |
| F5b Despliegue, CI y APK | 4–5 nov | `fase/f5-infra` (continúa) |
| F7 Cierre | 5–7 nov | — |

---

## F1 — Seguridad y limpieza

- [x] **F1-ALL-01** · RAÍZ/3 repos · M · RNF-SEG-01
  - Agregar a los `.gitignore`: `*.pem`, `*.jks`, `*.keystore`, `.env`, `*.h5`, `*.keras`, `local.properties` y `models/`.
  - Ejecutar `gitleaks detect` (instalarlo si falta) en los 3 repos y guardar el resultado en `docs/requerimientos/evidencias/gitleaks-f1.txt`.
  - **CA:** 0 hallazgos. Si aparece algo, documentarlo y removerlo.

- [x] **F1-API-01** · API · M · RNF-SEG-02 · cierra API-13, API-14
  - `application.yaml`: `jwt.secret: ${JWT_SECRET}` y `spring.datasource.password: ${DB_PASS}`, sin valores por defecto. `python.service.*` pasa a `ia.base-url: ${IA_BASE_URL}`, `ia.timeout-seconds: ${IA_TIMEOUT_SECONDS:10}` e `ia.internal-token: ${INTERNAL_API_TOKEN}`.
  - Validar al arrancar (`@ConfigurationProperties` + `@Validated`): secreto de al menos 32 bytes y token no vacío.
  - Quitar los `ENV` con secretos y la URL de AWS del `Dockerfile`.
  - Mover `application-test.yml` a `src/test/resources/` y anotar los tests con `@ActiveProfiles("test")`.
  - Eliminar `@EnableAsync` y la dependencia MockWebServer si no se usa todavía (se reintroduce en F3).
  - **CA:** sin `JWT_SECRET` la aplicación falla con un mensaje claro (hay prueba). `./mvnw verify` pasa.

- [x] **F1-API-02** · API · M · RNF-SEG-03, RNF-SEG-05 · cierra API-2, API-4, API-12
  - `SecurityConfig`: `permitAll` solo para `/auth/login`, `/auth/register` y `/actuator/health`. El resto exige autenticación.
  - Registrar `exceptionHandling` con `JwtAuthenticationEntryPoint` (401) y un nuevo `JsonAccessDeniedHandler` (403), ambos con el cuerpo `Error` del contrato.
  - CORS configurable por `CORS_ALLOWED_ORIGINS` (vacío = sin CORS).
  - **CA:** prueba parametrizada sobre cada ruta: sin token responde 401 JSON; con token, la respuesta deja de ser 401.

- [x] **F1-API-03** · API · M · cierra API-1 (parcial)
  - Quitar el usuario fijo `"Ferxxo"`: `analyzeFrame` usa `@AuthenticationPrincipal`. (La reestructuración completa llega en F2/F3.)
  - **CA:** no queda ninguna aparición de `Ferxxo` en el repo (`grep`).

- [x] **F1-IA-01** · IA · M · RNF-SEG-07 · cierra IA-10
  - Middleware o dependencia FastAPI que exige `X-Internal-Token == INTERNAL_API_TOKEN` en `/analyze` (que se renombrará a `/v1/analyze` en F2). Se compara con `hmac.compare_digest`.
  - Si la variable falta en `APP_ENV != local`, el servicio no arranca.
  - CORS: se elimina (no hay navegadores).
  - **CA:** pruebas: sin token 401, con token incorrecto 401, con token correcto 200.

- [x] **F1-MOB-01** · MOB · M · RNF-SEG-06, RNF-SEG-08 · cierra MOB-9, MOB-10
  - Eliminar los `println` del token y la URL.
  - Ktor `Logging` solo en `BuildConfig.DEBUG`, con `LogLevel.INFO` y `sanitizeHeader { it == HttpHeaders.Authorization }`.
  - `API_BASE_URL` desde `BuildConfig` (propiedad Gradle `tizon.apiBaseUrl`; en *debug*, por defecto `http://10.0.2.2:8080/api/`). Eliminar las 3 apariciones de `56.125.227.216`.
  - `network_security_config`: en *release* sin *cleartext*; en *debug* se permite *cleartext* solo para `10.0.2.2` y `localhost`, mediante un recurso por *build type*.
  - **CA:** `grep -r "56.125" .` no devuelve nada. La APK *release* rechaza `http`.

- [x] **F1-MOB-02** · MOB · M · decisión D-01 · cierra MOB-11, MOB-16
  - Retirar los *targets* iOS y JVM, `iosApp/`, los módulos `:server` y `:shared` (mover lo útil a `composeApp`), `Greeting` y el README de plantilla.
  - Simplificar el proyecto a Android con Compose (puede seguir usando el *plugin* KMP con un solo *target* o migrar a Android puro; documentar la elección).
  - **CA:** `./gradlew :composeApp:assembleDebug` compila.

- [x] **F1-MOB-03** · MOB · M · RNF-MAN-06 · cierra MOB-12
  - Centralizar todas las dependencias en `libs.versions.toml`.
  - Actualizar Ktor a 3.x, Koin a 4.x y kotlinx-serialization a una versión compatible con Kotlin 2.3, y agregar las de navegación, CameraX y WorkManager.
  - **CA:** compila y la app arranca en el emulador.

---

## F2 — Contrato v2 (fuente: `contracts/tizon-api.yaml`)

- [x] **F2-IA-01** · IA · M · RF-06 CA4 · cierra IA-5, IA-13
  - Renombrar a `POST /v1/analyze`. Esquemas Pydantic idénticos al contrato (`IaAnalyzeRequest`, `IaAnalyzeResponse`, `Region`, `IaHealth`).
  - Crear `app/model_meta.py`, que carga `MODEL_META_PATH`. Las clases y el `input_spec` salen de ahí; desaparece `LABELS` escrito en el código.
  - `/health` con `classes`, `model_version` e `input_spec`.
  - Se usa `status=NO_VEGETATION` y `label=null` en lugar de "Sin deteccion".
  - **CA:** prueba de contrato que valida las respuestas contra el OpenAPI (`openapi-core` o `schemathesis`). Tests actualizados sin "Tizon Temprano".

- [x] **F2-API-01** · API · M · cierra IA-5, TX-1
  - DTOs nuevos según el contrato (records con `@JsonProperty` en snake_case o `PropertyNamingStrategies.SNAKE_CASE` global).
  - Nuevo `ApiError` y `GlobalExceptionHandler` con los `code` del enum.
  - Endpoints con la forma v2: `/sessions`, `/sessions/{id}`, `/sessions/{id}/frames`, `/sessions/{id}/end`, `/config` y `/auth/me`. La lógica se completa en F3.
  - Eliminar `/analysis/**`.
  - Generar la documentación con springdoc y verificar que coincide con el contrato (prueba que compare los *paths*).
  - **CA:** `./mvnw verify` pasa. `GET /v3/api-docs` expone los *paths* del contrato (solo en el perfil *dev*).

- [x] **F2-API-02** · API · M · RNF-MAN-01 · cierra API-9
  - Agregar Flyway con `V1__baseline.sql`:
    - `scan_sessions` con `location_lat`, `location_lon`, `location_accuracy_m`, `field_name`, `analyzed_frames`, `no_vegetation_frames`, `severity`, `last_frame_at`.
    - `scan_results` con `status`, `label` nullable, `prob_sano`, `prob_tizon_tardio`, `regions_json`, `model_version`, `latency_ms`, `inference_ms`, `request_id`.
  - Se elimina la columna `prob_tizon_temprano`. Se usa `ddl-auto: validate`.
  - **CA:** la aplicación arranca contra PostgreSQL vacío (Testcontainers o el compose de F5a) y contra H2 en las pruebas.

- [x] **F2-MOB-01** · MOB · M
  - Modelos `@Serializable` según el contrato. `ApiClient` con los endpoints v2 y mapeo de `Error` a una `sealed class AppError` (`Network`, `Unauthorized`, `SessionNotActive`, `IaUnavailable`, `IaTimeout`, `InvalidImage`, `Validation(fields)`, `Unknown`).
  - Desaparece `addAuth()` y el header duplicado.
  - **CA:** pruebas unitarias de deserialización con *fixtures* JSON tomados del contrato y del mapeo de errores con MockEngine de Ktor.

---

## F5a — Infraestructura local (en paralelo con F2 y F3)

- [x] **F5-IA-01** · IA · M · cierra IA-6, IA-7, IA-8, IA-9, IA-11, IA-12
  - Dockerfile de una sola etapa (`python:3.12-slim`, usuario no root, `HEALTHCHECK`), sin dependencias de Lambda.
  - Eliminar `mangum`, `boto3`, `config/template*.yml`, `trust-policy.json`, `docker/` (duplicado) y el `docker-compose.yml` roto, o reemplazar este último por uno de desarrollo que funcione.
  - Inferencia con `run_in_threadpool`.
  - `.env.example` y README reescrito.
  - **CA:** `docker build` funciona y `/health` responde con el modelo montado.

- [x] **F5-RAÍZ-01** · RAÍZ · M · cierra TX-3
  - Crear `deploy/docker-compose.yml` (caddy opcional en local con el perfil `tls`), `deploy/.env.example` y `deploy/models/.gitkeep`, según 02 y 09.
  - **CA:** con `docker compose --env-file deploy/.env up --build` los 4 servicios quedan `healthy` (caddy, si el perfil está activo). Un login y un frame de prueba funcionan con `curl` (script `deploy/scripts/smoke-test.sh`).

---

## F3 — Integridad del flujo + UX (las tareas MOB tocan cada pantalla una sola vez)

- [x] **F3-API-01** · API · M · RF-04 · cierra API-1
  - `SessionService.start(user, req)`.
  - `FrameService.analyze(user, sessionId, frame)`: valida la propiedad (404) y el estado ACTIVE (409). **No crea sesiones.**
  - `end(user, sessionId)`.
  - Tarea `@Scheduled` que cancela las sesiones inactivas por más de 30 min.
  - **CA:** pruebas de cada rama (dueño o ajeno, ACTIVE o COMPLETED, inexistente).

- [x] **F3-API-02** · API · M · RF-07 · cierra API-3, API-11, TX-2
  - `IaClient` (WebClient): *timeout* de 10 s, un reintento solo ante `ConnectException`, propagación de `X-Request-Id` y `X-Internal-Token`.
  - Mapeo de errores:
    - IA 400 → 400 `INVALID_IMAGE`.
    - IA 503 o conexión rechazada → 502 `IA_UNAVAILABLE`.
    - *Timeout* → 504 `IA_TIMEOUT`.
  - **Sin *fallback* a vacío. No se persiste nada en caso de error.**
  - **CA:** pruebas con MockWebServer para 200, 400, 503, *timeout* y conexión rechazada, verificando que `scan_results` no cambia en los errores.

- [x] **F3-API-03** · API · M · RF-08 · cierra API-7
  - `SessionStatsCalculator`: función pura con las reglas de `SessionStats` y la severidad según `AppConfig`.
  - Recomendaciones desde la tabla `recommendations` (Flyway `V2__recommendations.sql`, con el texto genérico mientras se resuelve P-05). _(24-oct: reemplazadas por las recomendaciones por enfermedad de F4-ALL-07)_
  - **CA:** pruebas unitarias con los casos borde de RF-08 CA4.

- [x] **F3-API-04** · API · M · RNF-OBS-01, RNF-OBS-02, RNF-OBS-03 · cierra API-10, TX-5
  - Filtro de `X-Request-Id` (lo genera si falta, MDC).
  - Logs JSON (logback + logstash-encoder).
  - `HealthIndicator` de la IA (usa `/health` y `model_loaded`).
  - Métricas `tizon.ia.latency` y `tizon.ia.errors`.
  - **CA:** `/actuator/health` responde DOWN si la IA está caída (prueba).

- [x] **F3-MOB-00** · MOB · M · 06-ux-ui §Sistema visual y §Entregables
  - Tema (tokens), componentes base (`SeverityChip`, `StatusChip`, `PrimaryScanButton`, `ResultSheet`, `EmptyState`, `ErrorBanner`) y `@Preview` de todas las pantallas en todos sus estados.
  - Exportar los PNG a `docs/ux/`.
  - **🔒 Hito:** detenerse y pedir al usuario la aprobación de los *previews* (27-oct) antes de F3-MOB-02 en adelante.

- [ ] **F3-MOB-01** _(implementada el 23-oct; falta validar en dispositivo)_ · MOB · M · 06-ux-ui §Navegación · cierra MOB-15
  - Navigation Compose con el grafo de autenticación y el grafo principal (barra inferior).
  - ViewModels de AndroidX (`lifecycle-viewmodel-compose`) con `koinViewModel()`. Se elimina el `dispose()` manual.
  - **CA:** rotar la pantalla no pierde el estado ni crea sesiones. El botón "atrás" se comporta como el estándar.

- [x] **F3-MOB-02** · MOB · M · RF-02 · cierra MOB-8
  - Login y registro rediseñados.
  - `TokenStore` con EncryptedSharedPreferences e inicio automático.
  - Logout.
  - Interceptor de 401 que cierra la sesión y navega al login.
  - **CA:** pruebas del ViewModel (éxito, 401, sin red, validación).

- [x] **F3-MOB-03** · MOB · M · RF-04, RF-05, RF-07 · cierra MOB-1, MOB-2, MOB-3, MOB-4, MOB-5, MOB-6, MOB-7, MOB-13 (parcial: estado sin conexión)
  - Pantalla Escanear rediseñada:
    - `ImageProxy.toBitmap()` + rotación + escalado a 640 px + JPEG 80.
    - Bucle de envío controlado por la respuesta (no por `delay` fijo).
    - Estados de RF-07.
    - Cajas mapeadas con `PreviewTransform` (función pura con prueba).
    - Diálogo de salida.
  - **CA:** pruebas del `ScanViewModel` con un repositorio falso (iniciar, frames, NO_VEGETATION, 502 con reintento, 409, finalizar y limpiar). Prueba del mapeo de coordenadas.

- [x] **F3-MOB-04** · MOB · M · RF-08, RF-09 · cierra MOB-14 _(24-oct: se agregó el filtro por usuario de RF-09 CA4)_
  - Resumen de sesión, Historial (paginado, *pull-to-refresh*) y Detalle.
  - Severidad y umbrales solo desde `/config` y la respuesta del backend.
  - **CA:** no quedan umbrales numéricos en el código de la UI (`grep` de `30f`, `10f`, `15f`).

- [ ] **F3-MOB-05** _(implementada el 23-oct; falta validar en dispositivo)_ · MOB · M · 06-ux-ui §Bienvenida, §Permisos · RNF-SEG-12 · cierra TX-7
  - Bienvenida y permisos con explicación previa. La cámara rechazada **no cierra la app**.
  - **CA:** prueba manual en el guion del dispositivo.

- [ ] **F3-ALL-01** · RAÍZ · M · RNF-PRU-05 _(guion listo el 24-oct; falta la prueba 1 del 30-oct con el usuario)_
  - Escribir `guion-prueba-dispositivo.md`.
  - **🔒 Hito: prueba 1 en el dispositivo (30-oct)** con el usuario. Registrar los hallazgos en `10-pendientes.md`.

---

## F4 — Modelo (ver 08-modelo-ia)

- [x] **F4-IA-01** · IA · M — (adelantada el 23-oct: sin ella el modelo no cargaba) Fijar la versión de TensorFlow que carga el `.h5` sin advertencias de incompatibilidad (2.15.1 o `tf_keras`). **CA:** prueba de integración `load_model` + predicción sobre una imagen de prueba.
- [x] **F4-IA-02** · IA · M · cierra IA-2 — `evaluation/calibrate.py` y `models/model_meta.json` según 08 §2. **CA:** reporte de las 8 combinaciones en `evaluation/reports/`. El servicio usa la combinación elegida. _(24-oct: resuelta con F4-IA-05: el modelo nuevo declara su preprocesamiento y orden de clases por construcción; `calibrate.py` queda para modelos de origen desconocido)_
- [x] **F4-IA-03** · IA · M · cierra IA-1, IA-3, IA-4 — Filtro de vegetación rediseñado (08 §3) y regla del resultado principal por severidad. **CA:** NO_VEGETATION en las imágenes con tizón ≤ 5 %, con reporte antes y después. _(24-oct: con PlantVillage, TIZON_TARDIO en NO_VEGETATION 0 % y TIZON_TEMPRANO 4,0 % tras aceptar la clorosis conectada al verde; reporte `2026-10-24_pipeline.md`)_
- [x] **F4-IA-04** · IA · M · cierra IA-14 — `evaluation/evaluate.py` en los modos `classifier` y `pipeline`, la auditoría del dataset y la medición de latencia en la máquina objetivo. **CA:** reportes con la tabla meta frente a lo medido. _(24-oct: reportes `classifier`, `pipeline` y `dataset_audit`; todas las metas del PDF se cumplen en la prueba de laboratorio. La latencia en la EC2 se mide en F5-ALL-02 / DISP-90)_
- [x] **F4-IA-05** · IA · S — Reentrenamiento (08 §5) solo si F4-IA-04 queda por debajo de la meta y hay margen antes del 3-nov. _(24-oct: reentrenado con `training/train.py` por D-08 y porque el modelo original no se podía evaluar con honestidad; ver 08 §7)_
- [x] **F4-ALL-06** · 3 repos · M · D-08 — (agregada el 24-oct a pedido del usuario) Tizón temprano y discriminación papa / no papa: contrato (`TIZON_TEMPRANO`, `NOT_POTATO`, `label_counts`, `not_potato_frames`), IA (salida interna `NO_PAPA`), API (Flyway V5) y app (estado "No es una hoja de papa", resumen por tipo de tizón). **CA:** pruebas en las tres capas y prueba de punta a punta con imágenes reales en `docker compose`: sana, tizón temprano y tardío diagnosticados; hojas de otras plantas → `NOT_POTATO`.
- [x] **F4-ALL-07** · 3 repos · M · RF-08 CA3 · cierra P-05 — (agregada el 24-oct a pedido del usuario) Recomendaciones según la enfermedad que predominó entre los frames afectados de la sesión (en empate, tizón tardío), con textos resumidos del ICA, la FHIA y la Universidad de Wisconsin ([fuentes-recomendaciones.md](fuentes-recomendaciones.md)): contrato (`Recommendation.disease`), API (Flyway V6) y app (título "Recomendaciones para tizón temprano / tardío"). **CA:** pruebas de la regla (mayoría, empate y sin afectados) y del flujo para cada enfermedad.

---

## F6 — Usuarios, GPS y alertas

- [x] **F6-API-01** · API · M · RF-01, RF-03 · cierra API-6
  - Entidad `User` (Flyway `V3__users.sql`), `JpaUserDetailsService`, registro, `/auth/me`, *seeding* del ADMIN desde variables de entorno y `/admin/users`.
  - Se elimina `UserConfig` (en memoria).
  - Límite de intentos de login (RNF-SEG-09).
  - **CA:** pruebas de registro, duplicado, login, usuario deshabilitado y autorización por rol.

- [x] **F6-API-02** · API · S · RF-10, RF-11 · cierra API-8 (parcial: contexto de ubicación; las imágenes quedan fuera del alcance)
  - `location` y `field_name` en la sesión.
  - Entidad `Alert` (`V4__alerts.sql`), creación al finalizar la sesión según la regla, `GET /alerts` con `since` y `POST /alerts/{id}/ack` (TECNICO o ADMIN).
  - **CA:** pruebas de la regla (umbral, mínimo 5 frames, una alerta por sesión) y de visibilidad por rol.

- [ ] **F6-MOB-01** _(implementada el 23-oct; falta validar en dispositivo)_ · MOB · S · RF-10
  - `LocationProvider` con `LocationManager` (sin Play Services), con *timeout* de 10 s y última posición conocida.
  - Campo finca o lote.
  - Indicador de GPS en Escanear.
  - **CA:** funciona en el dispositivo de prueba. Si se rechaza el permiso, el escaneo sigue sin ubicación.

- [ ] **F6-MOB-02** _(implementada el 23-oct; falta validar en dispositivo)_ · MOB · S · RF-11
  - Pantalla Alertas.
  - `AlertsWorker` (WorkManager, cada 15 min) con notificación local y permiso `POST_NOTIFICATIONS`.
  - Contador en la pestaña.
  - **CA:** una alerta creada en el backend aparece como notificación en 15 min o menos y al abrir la app.

- [ ] **F6-MOB-03** · MOB · C · RF-12 — Mapa osmdroid (se recorta primero si hay retraso). _(no iniciada: prioridad C, RF-12 CA3 permite omitirla)_
- [x] **F6-MOB-04** · MOB · M — Perfil y pantalla de Usuarios (ADMIN).

---

## F5b — Despliegue, CI y APK

- [x] **F5-RAÍZ-02** · RAÍZ · M — Scripts de `deploy/scripts/` y `deploy/README.md` (runbook de 09 §3). **CA:** revisión con `shellcheck`.
- [x] **F5-ALL-01** · 3 repos · S · RNF-PRU-06 · cierra TX-4 — GitHub Actions por repo: pruebas, linter y gitleaks.
- [x] **F5-MOB-01** · MOB · M — Firma *release* por variables de entorno, R8 con reglas, `versionName 1.1.0`. **CA:** `assembleRelease` genera una APK firmada. Se documenta el SHA-256. _(verificado el 24-oct con un keystore de prueba: `evidencias/f5-mob-01-release.txt`)_
- [ ] **F5-INF-04** · Usuario + Claude · M — Despliegue en la EC2 nueva siguiendo el runbook (depende de P-03). **CA:** *smoke test* contra `https://<PUBLIC_HOST>`.
- [ ] **F5-ALL-02** · RAÍZ · M — **🔒 Hito: prueba 2 en el dispositivo (5-nov)** contra la EC2.

---

## F7 — Cierre (5–7 nov)

- [ ] **F7-01** · RAÍZ · M · RNF-PRU-01 — `matriz-pruebas.md` completa, con el % de funcionalidades probadas. _(armada el 24-oct con las pruebas automatizadas; se completa con P1, P2 y los reportes de F4)_
- [ ] **F7-02** · RAÍZ · M — `docs/entrega/resultados-metricas.md`: tabla final meta frente a medido (modelo, latencia, disponibilidad, cobertura) con enlaces a los reportes.
- [ ] **F7-03** · RAÍZ · S — `encuesta-sus.md` (instrumento SUS en español) y, si se aplicó, los resultados. _(instrumento listo el 24-oct; falta aplicarlo a 5 personas o más)_
- [ ] **F7-04** · RAÍZ · M — Insumos para el documento académico: arquitectura real (diagrama), alcance (tizón temprano y aviso de hoja que no es de papa incluidos por D-08; malezas por especie y drones como trabajo futuro) y resultados. **No se reescribe el PDF**: se entrega `docs/entrega/insumos-documento.md` para que los autores lo integren.
- [ ] **F7-05** · RAÍZ · M — Actualizar `BASE_CONOCIMIENTO_TIZON.md` con el estado final y cerrar `10-pendientes.md`.

---

## Prompts sugeridos para iniciar cada fase en Claude Code
```
Lee CLAUDE.md y docs/requerimientos/00-indice.md. Ejecuta la fase F1 del backlog
(docs/requerimientos/07-backlog.md) en orden, creando la rama fase/f1-seguridad en cada repo.
Al terminar cada tarea, corre las pruebas, haz commit con el ID y marca la casilla.
Si algo bloquea, regístralo en 10-pendientes.md y continúa con la siguiente tarea independiente.
```
Para las siguientes fases, cambia `F1` por `F2`, `F5a`, `F3`, etc. En F3, recuerda detenerte en el hito de aprobación de los *previews*.
