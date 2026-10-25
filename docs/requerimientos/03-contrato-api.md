# 03 · Contrato API

La **fuente de verdad** es [`/contracts/tizon-api.yaml`](../../contracts/tizon-api.yaml) (OpenAPI 3.1, validado). Este documento resume las reglas y la migración.

## Reglas
1. JSON en `snake_case` en todas las capas. Esto incluye el login: se reemplaza `expiresIn` por `expires_at`.
2. Identificadores de clase: `SANO` y `TIZON_TARDIO`. La app traduce a "Sano" y "Tizón tardío". Desaparecen los strings libres como "Tizon Tardio" y "Sin deteccion".
3. "No hay hoja en el cuadro" **no es un error**. Se responde 200 con `status=NO_VEGETATION` y `label=null`.
4. Los errores técnicos **siempre** responden un código HTTP de error con el cuerpo `Error` (`code`, `message` en español). **Nunca se persisten como resultado.**
5. Severidad y umbrales: se calculan en el backend. La app los obtiene de `GET /api/config` y no los define en el código.
6. Cajas (`regions`): coordenadas relativas (0–1) **sobre el frame tal como lo envió la app, después de corregir la rotación**. Representan regiones de hoja analizadas, no lesiones.
7. Los *timeouts* se escalonan:
   - IA: `inference` menor a 3 s esperado.
   - Spring hacia la IA: 10 s, con 1 reintento solo ante una falla de conexión.
   - App hacia Spring: 15 s.

## Migración de endpoints (v1 → v2)
| v1 (actual) | v2 (objetivo) | Nota |
|---|---|---|
| `POST /api/auth/login` | `POST /api/auth/login` | La respuesta cambia a `{token, expires_at, user}` |
| — | `POST /api/auth/register`, `GET /api/auth/me` | Nuevos |
| — | `GET /api/config` | Nuevo |
| `POST /api/analysis/sessions/start` | `POST /api/sessions` | Acepta `location` y `field_name` |
| `POST /api/analysis/frame` (sessionId en el cuerpo) | `POST /api/sessions/{id}/frames` | La sesión va en la ruta: elimina la creación implícita de sesiones |
| `POST /api/analysis/sessions/{id}/end` | `POST /api/sessions/{id}/end` | Devuelve severidad, recomendaciones y `alert_id` |
| — | `POST /api/sessions/{id}/cancel` | Nuevo (23-oct): "Descartar" de RF-04 CA6; deja la sesión en `CANCELLED` |
| `GET /api/analysis/sessions/history` | `GET /api/sessions?page&size` | Paginado |
| — | `GET /api/sessions/{id}` | Nuevo |
| — | `GET /api/alerts`, `POST /api/alerts/{id}/ack` | Nuevos |
| — | `GET/PATCH /api/admin/users…` | Nuevos (ADMIN) |
| IA `POST /analyze` (público vía API Gateway) | IA `POST /v1/analyze` (interno, `X-Internal-Token`) | |
| IA `GET /health` | IA `GET /health` con `classes`, `model_version` e `input_spec` | |

No se mantiene compatibilidad con v1: las tres capas se despliegan juntas.

## Reglas de negocio asociadas
- **Resultado principal de un frame:** entre las regiones con confianza ≥ `MIN_CONFIDENCE` (0,30), se toma la clase más severa (`TIZON_TARDIO` > `TIZON_TEMPRANO` > `SANO`) y, en empate, la de mayor confianza. Si no hay regiones, el estado es `NO_VEGETATION`. Si las regiones son de otras plantas (salida interna `NO_PAPA`), el estado es `NOT_POTATO` y no hay diagnóstico (D-08, 24-oct).
- **Estadísticas de la sesión:** ver el esquema `SessionStats`. El `affection_pct` usa solo los frames DETECTED como denominador; afectado = tizón temprano o tardío; `label_counts` separa las clases y `not_potato_frames` cuenta las hojas de otras plantas.
- **Alerta:** se crea al finalizar una sesión si `affection_pct ≥ alert_threshold_pct` **y** `analyzed_frames ≥ 5`, para evitar alertas con 1 frame. Hay una sola alerta por sesión.
- **Sesiones abandonadas:** una sesión ACTIVE sin frames durante 30 min pasa a `CANCELLED` (tarea programada cada 5 min).
- **Intentos de login (RNF-SEG-09):** 5 fallos en 5 min bloquean el usuario 5 min; el login responde 429 `TOO_MANY_ATTEMPTS` (agregado el 23-oct).
- **Propiedad:** un AGRICULTOR solo ve y opera sus propias sesiones y alertas. TECNICO y ADMIN pueden leer las de todos. Solo el dueño puede enviar frames o finalizar su sesión. Para un recurso ajeno se responde 404, no 403, para no revelar que existe.
