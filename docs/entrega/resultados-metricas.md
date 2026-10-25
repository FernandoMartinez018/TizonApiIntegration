# Resultados frente a las metas (F7-02)

Estado al 24-oct-2026, rama `dev` de los tres repositorios.

**Reglas** (05-requerimientos-no-funcionales):
- Se reporta lo medido aunque no alcance la meta.
- No se ajustan los sets de prueba para mejorar los números.

Los reportes están en `TizonDetectionModel/evaluation/reports/` y cada uno trae la versión del modelo, su sha256, el
origen de las imágenes, la máquina y la lista de errores.

## 1. Tabla final: meta frente a medido
Modelo en uso: `mobilenetv2-4c-02b1cb27-20261024` (`modelo_tizon_4c.keras`). Para la calidad del modelo,
"laboratorio" es la partición de prueba de PlantVillage (479 imágenes) y "campo" es el set de PlantDoc (274 fotos).
Las dos se miden en modo `pipeline`, el flujo real de `/v1/analyze`.

| Meta del PDF | Meta | Medido | Cumple | Evidencia |
|---|---|---|---|---|
| Accuracy | ≥ 90 % | Laboratorio 94,4 % · campo 73,7 % | ✔ laboratorio · ✘ campo | `2026-10-24_pipeline.md`, `2026-10-24_campo-plantdoc_pipeline.md` |
| Precision (tizón tardío) | ≥ 90 % | 94,8 % · 11,8 % | ✔ · ✘ | ídem |
| Recall (tizón tardío) | ≥ 90 % | 97,3 % · 6,2 % | ✔ · ✘ | ídem |
| F1 (tizón tardío) | ≥ 90 % | 96,1 % · 8,2 % | ✔ · ✘ | ídem |
| Especificidad (tizón tardío) | ≥ 85 % | 97,6 % · 93,8 % | ✔ · ✔ | ídem |
| Tasa de error | ≤ 10 % | 5,6 % · 26,3 % | ✔ · ✘ | ídem |
| Tiempo de respuesta por imagen | ≤ 5 s (p95, de punta a punta) | Stack local sin red móvil: p95 de 390 ms medido por el cliente y de 348 ms según el `latency_ms` de la API (30 frames). En el dispositivo: pendiente | ⏳ dispositivo (DISP-90, DISP-91) | §6 |
| Inferencia de la IA (RNF-REN-02) | p95 < 1,5 s en la EC2 | PC de desarrollo: p95 de 0,3 s | ⏳ EC2 (P-03) | §6 |
| Disponibilidad | ≥ 95 % | Sin medir: `deploy/scripts/uptime.sh` corre en la EC2, que aún no existe | ⏳ EC2 (P-03) | — |
| Usabilidad (SUS) | ≥ 85 | Sin medir: el instrumento está listo (`encuesta-sus.md`) | ⏳ F7-03 | — |
| Funcionalidades del PDF verificadas | ≥ 90 % | 100 % (8 de 8 con prueba automatizada que pasa; 4 esperan confirmación en el dispositivo) | ✔ | `matriz-pruebas.md` |
| Cobertura de líneas de la API (RNF-PRU-02) | ≥ 70 % en `service` y `security` | 96,7 % y 88,8 % (93,5 % en total) | ✔ | JaCoCo (`./mvnw verify`) |

**Lectura corta:**
- En fotos de laboratorio, el modelo cumple todas las metas de calidad del PDF.
- En fotos de campo no cumple: rechaza como "no es una hoja de papa" el 72,9 % de las hojas de papa.
- El candidato v2, entrenado también con fotos de campo, reduce ese rechazo al 15,7 % y sube el recall de tizón tardío
  de 6 % a 59 %. Aun así no llega a las metas en campo, y su adopción espera la decisión del usuario (§4).

## 2. Laboratorio: PlantVillage
Son 479 imágenes que el modelo no vio: 22 sanas, 150 de tizón temprano, 150 de tardío y 157 de otras plantas. Las del
v2 son exactamente las mismas, porque `--keep-split` conserva la partición. Entre corchetes va el intervalo de Wilson
al 95 %.

| Métrica | Meta | 4c `classifier` | 4c `pipeline` | v2 `classifier` | v2 `pipeline` |
|---|---|---|---|---|---|
| Accuracy | ≥ 90 % | 95,6 % [93,4–97,1] | 94,4 % [91,9–96,1] | 96,0 % [93,9–97,4] | 93,7 % [91,2–95,6] |
| Precision tizón tardío | ≥ 90 % | 94,2 % | 94,8 % | 96,7 % | 96,6 % |
| Recall tizón tardío | ≥ 90 % | 97,3 % [93,3–99,0] | 97,3 % [93,3–99,0] | 96,7 % [92,4–98,6] | 94,7 % [89,8–97,3] |
| F1 tizón tardío | ≥ 90 % | 95,7 % | 96,1 % | 96,7 % | 95,6 % |
| Especificidad tizón tardío | ≥ 85 % | 97,3 % | 97,6 % | 98,5 % | 98,5 % |
| Tasa de error | ≤ 10 % | 4,4 % | 5,6 % | 4,0 % | 6,3 % |
| F1 tizón temprano | — | 98,0 % | 95,6 % | 98,0 % | 95,3 % |

- Accuracy global sobre las 4 clases. Precision, recall, F1 y especificidad se calculan con el tizón tardío contra el
  resto, como en el PDF.
- Filtro de vegetación (F4-IA-03, meta ≤ 5 %): ninguna hoja de tizón tardío termina en `NO_VEGETATION`; en tizón
  temprano termina el 4,0 % con el 4c.
- Sin duplicados exactos entre prueba y entrenamiento. Los casi duplicados (3 con el 4c y 5 con el v2) son siempre de
  clases o especies distintas, así que no son la misma hoja.
- Línea base sin la clase "no es papa" (3 clases): accuracy 98,1 %. Distinguir otras plantas cuesta unos 2 puntos.

## 3. Campo: PlantDoc
PlantDoc ([github.com/pratikkayal/PlantDoc-Dataset](https://github.com/pratikkayal/PlantDoc-Dataset), CC BY 4.0) son
fotos de internet tomadas en condiciones reales.

- **Composición:** 274 fotos, de las cuales 38 son de tizón temprano, 32 de tardío y 204 de otras especies.
- **Sin papa sana:** PlantDoc no la incluye.
- **Origen:** toda la partición test de PlantDoc y el 30 % de las hojas de papa de su partición train, que se apartó
  antes de entrenar el v2.
- **Fotos retiradas:** 6 que PlantDoc repite en su entrenamiento. Cinco de ellas tienen otra etiqueta: la misma imagen
  aparece como tizón tardío y como temprano, o como tomate.

| `pipeline` | 4c (en uso) | v2 (candidato) |
|---|---|---|
| Accuracy | 73,7 % [68,2–78,6] | 73,4 % [67,8–78,2] |
| Precision tizón tardío | 11,8 % | 47,5 % |
| Recall tizón tardío | 6,2 % [1,7–20,1] | 59,4 % [42,3–74,5] |
| F1 tizón tardío | 8,2 % | 52,8 % |
| Especificidad tizón tardío | 93,8 % | 91,3 % |
| F1 / recall tizón temprano | 21,7 % / 13,2 % | 38,3 % / 47,4 % |
| Hojas de papa rechazadas como "no es papa" | 72,9 % | 15,7 % |
| Hojas de otras plantas aceptadas como papa | 4,4 % | 19,6 % |

- Responder siempre "no es papa" daría 74,5 % de accuracy en este set (204 de 274). Por eso la accuracy global no sirve
  para comparar los dos modelos en campo: el 4c logra la suya rechazando casi todo.
- En modo `classifier` (el modelo sin el filtro) los resultados son casi iguales: 4c 74,5 % y v2 73,7 % de accuracy.

## 4. Candidato v2 y decisión pendiente
- **Qué cambia el v2:** son las mismas imágenes y la misma partición de laboratorio, más fotos de campo solo en train y
  val. En concreto, 137 fotos de papa con tizón de PlantDoc (repetidas 3 veces) y 789 hojas de otras plantas.
- **Laboratorio:** el v2 sigue cumpliendo todas las metas.
- **Campo:** es el único de los dos que detecta tizón.
- **Por qué no se adoptó:** la regla fijada antes de ver los resultados pedía mejorar la accuracy y el rechazo de papa en
  campo. La accuracy quedó igual (§3), así que no se adoptó de forma automática.
- **Recomendación: adoptarlo.** El costo es analizar como papa el 19,6 % de las hojas de otras plantas, frente al 4,4 %
  del 4c. Los pasos para adoptarlo están en `docs/requerimientos/08-modelo-ia.md` §8.

## 5. ¿Es papa? (discriminación papa / otras plantas)
| `pipeline` | Papa rechazada | Otras plantas aceptadas como papa |
|---|---|---|
| 4c laboratorio | 1,6 % | 7,6 % |
| v2 laboratorio | 1,6 % | 8,3 % |
| 4c campo | 72,9 % | 4,4 % |
| v2 campo | 15,7 % | 19,6 % |

La clase interna "no es papa" solo conoce 13 cultivos de PlantVillage y algunas especies de PlantDoc. Las malezas del
lote no están representadas, así que pueden pasar como papa.

## 6. Tiempo de respuesta y latencia
- **Inferencia (los dos modelos tienen la misma arquitectura):** medidos en el mismo proceso, con la misma imagen y
  alternados, dan una media de unos 238 ms y un p95 de unos 300 ms en el PC de desarrollo (Windows 11, 14 CPU). En
  los reportes, el p95 varía entre 0,11 s y 0,34 s según la carga de la máquina en cada momento. Esa cifra está
  anotada en cada reporte.
- **De punta a punta en local:** son 30 frames por `docker compose` (API → IA → PostgreSQL) desde el mismo PC, sin red
  móvil. El cliente midió una media de 279 ms y un p95 de 390 ms. Según el `latency_ms` de la API, la media fue de 245
  ms y el p95 de 348 ms.
- **Falta medir en condiciones reales:**
  - el tiempo en el dispositivo con red móvil (DISP-90, DISP-91);
  - el p95 de inferencia en la EC2 elegida.

## 7. Pruebas automatizadas
| Repositorio | Pruebas | Resultado |
|---|---|---|
| TizonDetectionModel | 82 unitarias + 2 de integración con el modelo real | ✔ |
| TizonApiIntegration | 90, incluida la migración Flyway sobre PostgreSQL 16 real (Testcontainers). Cobertura: 93,5 % de líneas | ✔ |
| TizonMovile | 163: 55 unitarias + 108 capturas de todas las pantallas | ✔ |
| Sistema completo | Prueba de humo, recomendaciones por enfermedad (3 sesiones) e imágenes reales por `docker compose` | ✔ |

La matriz de trazabilidad (`docs/requerimientos/matriz-pruebas.md`) cubre las 8 funcionalidades del PDF y los 11 RF
dentro del alcance, todos con una prueba que pasa.

## 8. Análisis de causas: la brecha entre laboratorio y campo
1. **Cambio de dominio.** PlantVillage muestra una hoja sobre fondo uniforme y con luz controlada. En campo hay varias
   hojas, fondos complejos, sombras y cámaras distintas. Es la limitación conocida de los modelos entrenados solo con
   PlantVillage.
2. **La clase "no es papa" se volvió "no es de PlantVillage".** Con solo fotos de laboratorio, el 4c aprendió que
   cualquier foto de campo es de otra planta. El v2 lo corrige en buena parte con fotos de campo de papa en el
   entrenamiento.
3. **No hay papa sana de campo** en ningún dataset usado. No se puede medir cuántas hojas sanas se marcarían como
   enfermas en el cultivo, y el v2 nunca vio una hoja sana fuera del laboratorio.
4. **Etiquetas con ruido:** PlantDoc tiene la misma imagen con etiquetas distintas (6 casos en este set).
5. **Sets pequeños:** con 32 hojas de tizón tardío en campo, los intervalos de confianza son amplios (§3).

## 9. Pendiente de medir
| Qué | Depende de | Plan |
|---|---|---|
| Desempeño con fotos propias del cultivo | P-02 | `docs/requerimientos/protocolo-fotos-campo.md` |
| Tiempo de respuesta en el dispositivo, cámara, GPS y alertas | P-06 (pruebas en dispositivo) | `guion-prueba-dispositivo.md` |
| Inferencia y disponibilidad en la EC2 | P-03 | `deploy/` listo; `uptime.sh` |
| Usabilidad | F7-03 | `encuesta-sus.md` con 5 personas o más |
| Revisión agronómica de las recomendaciones | Ingeniero agrónomo | `fuentes-recomendaciones.md` |

## 10. Reportes
| Reporte | Qué mide |
|---|---|
| `2026-10-24_entrenamiento.md` | Entrenamiento del 4c y su prueba (`classifier`) |
| `2026-10-24_classifier.md`, `2026-10-24_pipeline.md` | 4c en laboratorio |
| `2026-10-24_campo-plantdoc_classifier.md`, `..._pipeline.md` | 4c en campo |
| `2026-10-24_entrenamiento_v2.md` | Entrenamiento del candidato v2 |
| `2026-10-24_v2_classifier.md`, `2026-10-24_v2_pipeline.md` | v2 en laboratorio |
| `2026-10-24_v2-campo-plantdoc_classifier.md`, `..._pipeline.md` | v2 en campo |
| `2026-10-24_entrenamiento_3clases.md` | Línea base sin la clase "no es papa" |
| `dataset_audit.md` | Auditoría del dataset de laboratorio |
