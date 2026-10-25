# 05 · Requerimientos no funcionales

## Seguridad
| ID | Pri | Requisito | Criterio de verificación | Cierra |
|---|---|---|---|---|
| RNF-SEG-01 | M | No hay secretos en los repositorios ni en su historial | `gitleaks detect` sin hallazgos en los 3 repos. `.gitignore` incluye `*.pem`, `*.jks`, `*.keystore`, `.env`, `*.h5` y `local.properties`. | TX-6 |
| RNF-SEG-02 | M | Los secretos no tienen valor por defecto | La API no arranca sin `JWT_SECRET`, `DB_PASS` o `INTERNAL_API_TOKEN`. Hay prueba de arranque fallido. | API-13 |
| RNF-SEG-03 | M | Todo endpoint de `/api` exige JWT, salvo `auth/register`, `auth/login` y `actuator/health` | Prueba de `SecurityConfig` sobre cada ruta (401 sin token). | API-2 |
| RNF-SEG-04 | M | La autorización se basa en la propiedad y el rol | Pruebas: un AGRICULTOR no ve sesiones ajenas (404). Un no-ADMIN recibe 403 en `/admin`. | API-1 |
| RNF-SEG-05 | M | Las respuestas 401 y 403 usan el formato JSON `Error` | `JwtAuthenticationEntryPoint` y `AccessDeniedHandler` registrados. | API-4 |
| RNF-SEG-06 | M | Transporte cifrado | La APK *release* solo usa `https` (sin *cleartext*). TLS válido en la EC2 (Caddy + Let's Encrypt). | MOB-9 |
| RNF-SEG-07 | M | La IA no está expuesta a Internet y exige token interno | Sin puerto publicado en compose. `/v1/analyze` sin `X-Internal-Token` responde 401. | IA-10 |
| RNF-SEG-08 | M | No hay datos sensibles en los logs | Sin `println` de tokens. Ktor `Logging` solo en *debug*, en nivel `INFO` y ocultando `Authorization`. Spring no registra cuerpos con Base64. | MOB-10 |
| RNF-SEG-09 | S | Límite de intentos de login | 5 intentos fallidos por usuario en 5 min bloquean 5 min (en memoria basta). | — |
| RNF-SEG-10 | S | Tamaño máximo de petición | Frame mayor a 2 MB responde 413 (`spring.servlet.multipart` / filtro). Caddy limita el cuerpo a 3 MB. | — |
| RNF-SEG-11 | M | APK firmada con *keystore* fuera de git | La firma se lee de las variables `TIZON_KEYSTORE_*`. | — |
| RNF-SEG-12 | M | Privacidad: los frames no se almacenan (solo resultados). La ubicación es opcional y requiere consentimiento. La pantalla de bienvenida informa qué datos se guardan. Retención de sesiones: indefinida durante la fase académica, documentada en el README de la API. | Revisión del esquema (sin columna de imagen) y de los textos de bienvenida. | TX-7 |

## Rendimiento y disponibilidad (metas del PDF)
| ID | Pri | Requisito | Meta | Cómo se mide |
|---|---|---|---|---|
| RNF-REN-01 | M | Tiempo de respuesta por imagen, de punta a punta (app → resultado) | **≤ 5 s (p95)**. Objetivo interno: ≤ 2 s | `latency_ms` en Spring + cronómetro en la app. Reporte en F4. |
| RNF-REN-02 | M | Inferencia de la IA | p95 menor a 1.500 ms en la EC2 elegida | Campo `inference_ms`. Script de carga con 100 imágenes. |
| RNF-DIS-01 | S | Disponibilidad | ≥ 95 % durante las jornadas de prueba | Health compuesto (`db` + `ia`) consultado cada minuto por un *cron* en la EC2, con log a archivo y resumen diario (`deploy/scripts/uptime.sh`). |
| RNF-DIS-02 | M | Reinicio automático | `restart: unless-stopped` y *healthchecks* en los 4 servicios | Prueba: `docker kill ia` y verificar que se recupera en menos de 60 s. |

## Calidad del modelo (metas del PDF; ver 08-modelo-ia)
| Métrica | Meta |
|---|---|
| Accuracy, Precision, Recall (TIZON_TARDIO) y F1 | ≥ 90 % |
| Especificidad | ≥ 85 % |
| Tasa de error | ≤ 10 % |

Con 4 salidas del modelo (D-08), la accuracy y la tasa de error son globales (todas las clases) y precision, recall,
F1 y especificidad son de `TIZON_TARDIO` contra el resto. El reporte agrega las métricas por clase y la
discriminación papa / no papa (`evaluation/evaluate.py`).

**Regla:** se reporta el valor medido **aunque no alcance la meta**, con un análisis de causas. No se ajusta el set de prueba para mejorar los números.

## Mantenibilidad
| ID | Pri | Requisito |
|---|---|---|
| RNF-MAN-01 | M | El esquema de base de datos está versionado con Flyway (`V1__baseline.sql`, `V2__users.sql`…). Se usa `ddl-auto: validate`. |
| RNF-MAN-02 | M | El modelo se actualiza reemplazando `/models/*.h5` y `model_meta.json` y reiniciando el contenedor `ia`, sin recompilar ningún servicio. |
| RNF-MAN-03 | M | Clases, umbrales y recomendaciones se definen en un solo lugar (IA: `model_meta.json`; API: configuración y base de datos). La app no tiene valores fijos. |
| RNF-MAN-04 | M | Cada repo tiene un README con requisitos, variables, ejecución local, pruebas y despliegue. |
| RNF-MAN-05 | S | Linters: ruff (Python), ktlint o detekt (opcional), y `-Werror` desactivado pero sin advertencias nuevas en Java. |
| RNF-MAN-06 | M | Versiones de dependencias unificadas en `gradle/libs.versions.toml`. Ninguna dependencia de Ktor, Koin o serialization se declara fuera del catálogo. |

## Pruebas (meta del PDF: ≥ 90 % de las funcionalidades probadas)
| ID | Pri | Requisito |
|---|---|---|
| RNF-PRU-01 | M | Matriz `docs/requerimientos/matriz-pruebas.md` que liste las 8 funcionalidades del PDF §4.11 y cada RF, con su prueba automatizada o manual. Meta: ≥ 90 % de las filas con prueba que pasa. |
| RNF-PRU-02 | M | API: pruebas de servicio con MockWebServer (IA simulada: OK, 400, 503, *timeout*), de seguridad y de repositorio (H2 o Testcontainers). Cobertura de líneas JaCoCo ≥ 70 % en `service` y `security`. |
| RNF-PRU-03 | M | IA: pruebas unitarias (pytest) del preprocesamiento, el filtro, la regla de resultado principal y la autenticación interna. Las de integración usan el `.h5` real. |
| RNF-PRU-04 | M | App: pruebas unitarias de ViewModels (repositorio falso), del mapeo de coordenadas de cajas, del cálculo de severidad para la UI y del manejo de errores. |
| RNF-PRU-05 | M | Prueba manual de punta a punta en un dispositivo físico, con un guion en `docs/requerimientos/guion-prueba-dispositivo.md`. Se hace dos veces: 30-oct y 5-nov. |
| RNF-PRU-06 | S | CI en GitHub Actions por repo, que ejecute las pruebas, el linter y gitleaks. |

## Usabilidad y accesibilidad
Detalle en 06-ux-ui. Meta del PDF: satisfacción ≥ 85 %, medida con una encuesta SUS de 10 preguntas (instrumento en `docs/requerimientos/encuesta-sus.md`) a al menos 5 usuarios.

## Observabilidad
| ID | Pri | Requisito |
|---|---|---|
| RNF-OBS-01 | M | `X-Request-Id` de punta a punta (ver RF-07 CA4). |
| RNF-OBS-02 | M | Logs en formato JSON en la API y la IA, con `request_id`, `session_id`, `latency_ms`, `inference_ms` y `status`. |
| RNF-OBS-03 | S | Métricas de Actuator: `tizon.ia.latency` (timer) y `tizon.ia.errors` (counter, por código). |
