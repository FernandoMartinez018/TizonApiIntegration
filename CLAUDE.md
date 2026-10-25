# CLAUDE.md — Proyecto Tizón Tardío (workspace)

Instrucciones para Claude Code. Léelas completas antes de cualquier cambio.

## 1. Qué es esto
Es un sistema académico que detecta tizón tardío (*Phytophthora infestans*) y tizón temprano en hojas de papa con la cámara de un Android, y avisa cuando la hoja no es de papa. **La entrega es el 8 de noviembre de 2026.**

Esta carpeta raíz se versiona en la rama **`sistema`** de `TizonApiIntegration` (P-07, 24-oct): `docs/`, `deploy/`, `contracts/`, `CLAUDE.md` y `README.md`. Ese repositorio es **público**: el `.gitignore` de la raíz deja fuera los tres repos, los modelos, los `.env`, los respaldos, las llaves, `contextos/` y los tres análisis previos (decisión del usuario). Contiene tres repositorios independientes:

| Carpeta | Rol | Stack | Remoto |
|---|---|---|---|
| `TizonDetectionModel/` | Servicio de IA (inferencia) | Python 3.12, FastAPI, TensorFlow-CPU, OpenCV | `github.com/FernandoMartinez018/TizonDetectionModel` |
| `TizonApiIntegration/` | API de negocio | Java 21, Spring Boot 3.5, Spring Security + JWT, JPA, PostgreSQL | `github.com/FernandoMartinez018/TizonApiIntegration` |
| `TizonMovile/` | App Android | Kotlin 2.3, Compose Multiplatform (solo Android), Ktor, Koin, CameraX | `github.com/FernandoMartinez018/TizonMovile` |

**Otros elementos de la raíz:**
- `modelo_tizon_4c.keras`: modelo en uso (MobileNetV2, salidas SANO, TIZON_TEMPRANO, TIZON_TARDIO y NO_PAPA; entrenado con `TizonDetectionModel/training/train.py`). `modelo_tizon_4c_v2.keras`: candidato entrenado también con fotos de campo de PlantDoc; espera la decisión del usuario (P-08, `08-modelo-ia.md` §8). `modelo_tizon_tardio.h5`: modelo anterior (2 clases, Keras 2.14), solo para volver atrás. **Ninguno va a git.**
- `contextos/`: documento académico en PDF (fuente de los requerimientos de calidad).
- `docs/requerimientos/`: **especificación que debes implementar**. Empieza por `00-indice.md`.
- `contracts/tizon-api.yaml`: **contrato OpenAPI, fuente de verdad** de la API pública (app ↔ Spring) y de la interna (Spring ↔ IA).
- `BASE_CONOCIMIENTO_TIZON.md`, `ANALISIS_BRECHAS_TIZON.md`, `PLAN_TRABAJO_TIZON.md`: análisis previo. Los códigos de falencias (IA-x, API-x, MOB-x, TX-x) vienen de ahí.
- `_respaldo_git/`: *bundles* de respaldo previos a la purga de llaves. **No tocar ni subir.**
- `_llaves_retiradas/`: llaves `.pem` antiguas, ya invalidadas. **Nunca copiarlas a un repositorio.**

## 2. Cómo trabajar

1. **Sigue el backlog** `docs/requerimientos/07-backlog.md` en el orden de las fases. Cada tarea tiene un ID (p. ej. `F3-API-02`), los archivos a tocar y criterios de aceptación. Una tarea solo está terminada cuando sus criterios se cumplen **y** sus pruebas pasan.
2. **Contrato primero.** Si una tarea cambia la API, actualiza `contracts/tizon-api.yaml` en el mismo cambio y ajusta las tres capas.
3. **Ramas (decisión del usuario, 23-oct):** en cada repo, `dev` → `qa` → `main`. Todo el trabajo se hace y se commitea en `dev`. La promoción a `qa` y `main` la hace el usuario (merge/PR). Ya no se usan ramas por fase.
4. **Commits:** pequeños y en español, con el formato `tipo(ámbito): descripción [ID-TAREA]`. Ejemplo: `fix(sesiones): usar usuario del JWT en /analysis/frame [F3-API-01]`.
   - Tipos: feat, fix, refactor, test, docs, chore, build, ci.
   - No hagas *push*: lo hace el usuario.
5. **No inventes datos.** Si falta información (dataset, dominio, credenciales), detente en esa tarea, déjala anotada en `docs/requerimientos/10-pendientes.md` y continúa con la siguiente que no dependa de ella.
6. **Fin de línea:** los repos usan `core.autocrlf=true` (Windows). No reformatees archivos completos solo por el fin de línea.

## 3. Reglas que no se negocian

- **Cero secretos en git:**
  - Nada de `.pem`, `.jks`/`.keystore`, `.env` ni contraseñas o secretos JWT con valor por defecto en el código o los Dockerfiles.
  - Usa variables de entorno, con un `.env.example` sin valores reales.
- **Cero fallas silenciosas.** Un error técnico nunca se convierte en un resultado de diagnóstico ("Sin detección") ni se guarda como tal.
- **Clases (ampliadas por el usuario el 24-oct):** `Sano`, `Tizón temprano` y `Tizón tardío`. Si la hoja es de otra
  planta, la IA responde `NOT_POTATO` y **no** analiza el tizón (salida interna del modelo `NO_PAPA`, que nunca es una
  etiqueta). Identificar malezas por especie y los drones siguen **fuera del alcance** (trabajo futuro).
  - Los identificadores técnicos van sin tilde: `SANO`, `TIZON_TEMPRANO`, `TIZON_TARDIO`, `NOT_POTATO`.
  - Los textos de la UI van con tilde: "Sano", "Tizón temprano", "Tizón tardío", "Sin detección", "No es una hoja de papa".
- **Solo Android.** No reintroducir *targets* iOS ni JVM.
- **Solo open source u on-premise** para GPS, mapas y notificaciones: nada de Google Play Services, Firebase/FCM ni Google Maps. La APK se instala manualmente, sin Play Store.
- **El despliegue es una EC2 nueva con `docker-compose`** (sin Lambda, API Gateway ni S3). La IA no se expone a Internet.

## 4. Comandos

```bash
# IA (TizonDetectionModel)
python -m venv .venv && . .venv/bin/activate && pip install -r requirements-dev.txt
pytest test/ -m "not integration"        # unitarias
MODEL_PATH=../modelo_tizon_4c.keras pytest test/ -m integration
ruff check app test

# API (TizonApiIntegration)
./mvnw -B verify                          # compila + pruebas (perfil test con H2)

# App (TizonMovile)
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleRelease     # requiere keystore por variables de entorno

# Sistema completo (raíz, una vez completada la tarea F5-RAÍZ-01)
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up --build
```

## 5. Definición de terminado (para cada tarea)
- [ ] Se cumplen los criterios de aceptación de la tarea.
- [ ] Hay pruebas nuevas o actualizadas, y todas las pruebas del repo pasan.
- [ ] El contrato OpenAPI está actualizado si cambió la API.
- [ ] No se introdujeron secretos, logs con token o imagen, ni valores fijos de ambiente (IP o URL).
- [ ] El README del repo está actualizado si cambió la forma de ejecutar o configurar.
- [ ] Hay un commit con el ID de la tarea.
