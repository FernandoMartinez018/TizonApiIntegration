# 10 · Pendientes, supuestos y riesgos

Claude Code: actualiza este archivo cuando una tarea quede bloqueada. Indica la fecha, el ID de la tarea y qué se necesita.

## Insumos pendientes del usuario
| # | Pendiente | Bloquea | Fecha límite | Plan B |
|---|---|---|---|---|
| P-01 | ~~Ubicación del **dataset de entrenamiento** y del notebook, si existe~~. **Resuelto el 24-oct.** Respuesta del usuario (24-oct):** se entrenó con PlantVillage de Kaggle; ya no tiene las imágenes ni el notebook. Se descargó PlantVillage de forma temporal (ya borrado) y se reentrenó con una partición propia (08 §7) | F4-IA-02, F4-IA-04, F4-IA-05 | 27-oct | — |
| P-02 | **Fotos de campo** propias para el set de prueba independiente (30 o más por clase) | F4-IA-04 (calidad del reporte) | 1-nov | Reportar solo con el set disponible y declararlo como limitación |
| P-03 | Región de AWS y creación de la EC2 + Elastic IP | F5-INF-04, F7 | 2-nov | Demo local con `docker compose` en el PC y la APK *debug* apuntando a la IP de la LAN |
| P-04 | **Resuelto el 24-oct.** El usuario aprobó el *force push*: los `main` remotos ya no tienen llaves y el CI pasa. Visibilidad: los tres repos son públicos, sin el documento académico ni los análisis que lo comentan (decisión del usuario). Detalle original: *force push* de `main` en `TizonDetectionModel` y `TizonApiIntegration` (historial ya purgado de `.pem` en local) y visibilidad de los repos. **24-oct:** gitleaks sobre un clon del remoto confirma una llave privada en cada `main` remoto (`tizon-key-new.pem`, `tizon-spring-key.pem`; ya invalidadas). Hasta el *force push*, el job de gitleaks del CI falla en esos dos repos y GitHub no permite el PR `qa → main` (historias sin relación). `dev` y `qa` ya están en GitHub con el historial purgado | F1 (cierre de RNF-SEG-01 en el remoto), promoción a `main` | 25-oct | — |
| P-05 | ~~Textos de recomendación agronómica con fuente~~. **Resuelto el 24-oct** (F4-ALL-07): recomendaciones por la enfermedad que predominó, resumidas del ICA (2011), la FHIA (2014) y la Universidad de Wisconsin ([fuentes-recomendaciones.md](fuentes-recomendaciones.md)). Queda recomendada la revisión del resumen por un ingeniero agrónomo antes de usar la app en un cultivo real | RF-08 CA3 | 3-nov | — |
| P-06 | Dispositivo Android físico para las pruebas en dispositivo. **24-oct:** el usuario define cuándo se hacen (estaban planeadas para el 30-oct y el 5-nov) | RNF-PRU-05 | A definir por el usuario | Emulador (sin validar la cámara ni el GPS reales) |
| P-07 | ~~¿Crear un 4.º repositorio para `deploy/`, `contracts/` y `docs/`?~~ **Resuelto el 24-oct:** la carpeta raíz se versiona en la rama huérfana `sistema` de TizonApiIntegration (decisión del usuario). El `.gitignore` excluye los repos, los modelos, los secretos, los respaldos, el PDF y los análisis | Versionado de la documentación | 25-oct | — |

## Bloqueos y verificaciones pendientes (registrados por Claude Code)
| Fecha | Tarea | Qué falta | Qué se necesita |
|---|---|---|---|
| 23-oct | F1-MOB-03 | CA "la app arranca en el emulador": compila en Docker, pero no hay Android SDK ni emulador en el equipo de desarrollo. | Instalar la APK debug en un emulador o dispositivo (se cubre en la prueba 1 del dispositivo, F3-ALL-01). |
| 23-oct | P-03 (plan B) | La APK *debug* solo permite HTTP hacia `10.0.2.2` y `localhost` (F1-MOB-01). Una demo en la LAN con HTTP necesitaría agregar esa IP en `src/debug/res/xml/network_security_config.xml`. | Decidir la IP de la LAN si se usa el plan B. |

| 23-oct | F3-MOB-00 (hito) | ✔ Resuelto el 23-oct: el usuario aprobó los previews (`docs/ux/`) sin ajustes. | — |
| 23-oct | F4-IA-03 | ~~Falta el reporte con imágenes reales.~~ **Resuelto el 24-oct** (08 §7). | — |
| 23-oct | F4-IA-02, F4-IA-04 | ~~Calibración y evaluación con datos.~~ **Resuelto el 24-oct** con el modelo de 4 salidas (08 §7). | — |
| 24-oct | F4-ALL-06 (D-08) | Métricas de laboratorio (PlantVillage). Falta medir en campo: fotos propias (P-02) y la prueba en el dispositivo. `NO_PAPA` solo conoce 13 cultivos de PlantVillage; malezas u otras plantas de campo pueden aceptarse como papa. Solo 152 hojas sanas. | Fotos de campo (P-02) y prueba 1 en el dispositivo (DISP-24A, DISP-25A). |
| 24-oct | F7-03 | El instrumento SUS está en `encuesta-sus.md`. Falta aplicarlo a 5 personas o más, después de las tareas del §1. | Participantes y fecha (P1 o P2). |
| 24-oct | F5-MOB-01 (entrega) | **Por ahora no se necesita** (decisión del usuario, 24-oct). Cuando haga falta, la APK de la entrega debe firmarse con el keystore propio del usuario (D-05) y la URL de la EC2. El SHA-256 del certificado y del archivo se registra al compilarla (`scripts/apk-sha256.sh`). | Keystore del usuario (README de TizonMovile §APK release firmada) y P-03. |
| 23-oct | F5-INF-04, F5-ALL-02 | Despliegue en la EC2 y prueba 2 en el dispositivo. El usuario decidió no crear nada en AWS por ahora; todo queda listo en `deploy/`. | Decisión del usuario (P-03). |
| 23-oct | P-05 | ~~Las recomendaciones usan solo el texto genérico.~~ **Resuelto el 24-oct** con F4-ALL-07. | — |

| 23-oct | F3-MOB-01, F3-MOB-05, F6-MOB-01, F6-MOB-02 | Implementadas con pruebas unitarias, pero sus criterios de aceptación son de dispositivo: rotación sin perder estado, botón atrás, cámara rechazada sin cierre, GPS real y notificación de alerta en ≤ 15 min. | Prueba 1 en dispositivo (F3-ALL-01, P-06). |
| 23-oct | F6-MOB-03 | Mapa osmdroid (prioridad C) no iniciado; se omite sin bloquear la entrega (RF-12 CA3). | Decisión del usuario si sobra tiempo. |

## Supuestos
- S-01: La carpeta raíz se versiona en la rama `sistema` de TizonApiIntegration (P-07); los tres repos siguen siendo independientes.
- S-02: La identidad git local de los 3 repos es `Luis Fernando Martinez Mongui <luis.martinez@sofka.local>` (la del historial existente). El usuario puede cambiarla.
- S-03: Durante la fase académica, la cantidad de usuarios es menor a 50 y los frames concurrentes menos de 5. No se diseña para escalar horizontalmente.

## Riesgos
| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| El modelo no alcanza las metas del PDF en campo | Media | Alto | En laboratorio se cumplen (08 §7). Medir con fotos propias (P-02) y en el dispositivo; reportar con honestidad y analizar las causas |
| El orden de clases está invertido o el preprocesamiento es incorrecto | Media | Crítico | F4-IA-02 antes de cualquier otra mejora del modelo |
| Retraso en el rediseño de la UX | Media | Medio | Hito de aprobación de *previews* el 27-oct. Recortes en el orden de `PLAN_TRABAJO_TIZON.md` §2.1 |
| Actualizar versiones (Ktor 3, Koin 4) rompe la compilación | Media | Medio | Hacerlo en F1-MOB-03, al inicio, con compilación verificada |
| La `t3.medium` se queda corta | Baja | Medio | Medir en F4. Subir a `t3.large` si el p95 supera la meta |

## Hecho antes de este backlog (23-oct)
- Historial de `TizonDetectionModel` y `TizonApiIntegration` reescrito localmente sin `*.pem`. Commits nuevos: `4ca5640` y `0707789`.
- Las llaves se movieron a `_llaves_retiradas/`. Hay respaldo completo previo en `_respaldo_git/*.bundle` (**contiene las llaves: no subir ni compartir**).
- Se configuró `core.autocrlf=true` en los 3 repos y la identidad git local (S-02).
