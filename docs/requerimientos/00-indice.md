# Especificación de requerimientos — Tizón Tardío v2

Versión 1.0 · 23-oct-2026 · Responsable: Fernando Martínez · Entrega: **8-nov-2026**

| # | Documento | Contenido |
|---|---|---|
| 01 | [Visión y alcance](01-vision-alcance.md) | Objetivo, actores, alcance, fuera del alcance, decisiones |
| 02 | [Arquitectura objetivo](02-arquitectura.md) | Componentes, red, configuración por ambiente |
| 03 | [Contrato API](03-contrato-api.md) + [`/contracts/tizon-api.yaml`](../../contracts/tizon-api.yaml) | Endpoints, modelos, errores, umbrales |
| 04 | [Requerimientos funcionales](04-requerimientos-funcionales.md) | RF con criterios de aceptación |
| 05 | [Requerimientos no funcionales](05-requerimientos-no-funcionales.md) | Seguridad, rendimiento, calidad, métricas del PDF |
| 06 | [UX/UI](06-ux-ui.md) | Sistema visual, pantallas, estados, textos, accesibilidad |
| 07 | [Backlog por fases](07-backlog.md) | **Tareas ejecutables** para Claude Code, en orden |
| 08 | [Modelo de IA y evaluación](08-modelo-ia.md) | Verificación del modelo, preprocesamiento, evaluación |
| 09 | [Despliegue en EC2](09-despliegue.md) | Runbook de infraestructura y entrega de la APK |
| 10 | [Pendientes y supuestos](10-pendientes.md) | Insumos faltantes y decisiones abiertas |
| — | [Guion de prueba en dispositivo](guion-prueba-dispositivo.md) | Casos DISP de las pruebas 1 (30-oct) y 2 (5-nov) |
| — | [Matriz de pruebas](matriz-pruebas.md) | Funcionalidades del PDF y RF con sus pruebas y su estado |
| — | [Encuesta SUS](encuesta-sus.md) | Instrumento de usabilidad y cálculo del puntaje |

**Trazabilidad:** cada RF/RNF indica las falencias que cierra (códigos de `ANALISIS_BRECHAS_TIZON.md`), y cada tarea del backlog indica los RF/RNF que implementa.

**Prioridad (MoSCoW):**
- **M** = imprescindible para el 8-nov.
- **S** = debería estar.
- **C** = si sobra tiempo.
- **W** = no se hará ahora.
