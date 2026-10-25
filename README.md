# Tizón Tardío — carpeta del sistema

Sistema académico que detecta tizón tardío y tizón temprano en hojas de papa con la cámara de un Android y avisa
cuando la hoja no es de papa. Entrega: 8 de noviembre de 2026.

Esta rama (`sistema` de TizonApiIntegration) guarda lo que no pertenece a un solo repositorio:

| Carpeta | Contenido |
|---|---|
| `docs/requerimientos/` | Especificación (empieza por `00-indice.md`), backlog, pendientes, guion de prueba en dispositivo, matriz de pruebas, encuesta SUS y evidencias |
| `docs/ux/` | Capturas de todas las pantallas de la app (claro, oscuro y fuente al 200 %) |
| `docs/entrega/` | Resultados frente a las metas e insumos para el documento académico |
| `contracts/tizon-api.yaml` | Contrato OpenAPI, fuente de verdad (copias en la IA y la API con `contracts/sync-copies.sh`) |
| `deploy/` | `docker compose` del sistema completo, Caddy y scripts de despliegue, actualización y respaldo |
| `CLAUDE.md` | Instrucciones de trabajo |

El código está en tres repositorios (en la carpeta del proyecto quedan como subcarpetas, cada una con su git):

| Repositorio | Rol |
|---|---|
| [TizonDetectionModel](https://github.com/FernandoMartinez018/TizonDetectionModel) | Servicio de IA (FastAPI + TensorFlow) |
| [TizonApiIntegration](https://github.com/FernandoMartinez018/TizonApiIntegration) | API de negocio (Spring Boot) |
| [TizonMovile](https://github.com/FernandoMartinez018/TizonMovile) | App Android (Kotlin + Compose) |

No se versionan aquí: los modelos, los `.env`, el documento académico ni los respaldos (ver `.gitignore`).
