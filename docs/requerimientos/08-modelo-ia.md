# 08 · Modelo de IA: verificación, preprocesamiento y evaluación

## 1. Lo que se sabe del modelo (inspección del 23-oct del `.h5` en la raíz)
| Atributo | Valor |
|---|---|
| Archivo | `modelo_tizon_tardio.h5` (9,5 MB). El README antiguo decía 87 MB: **puede ser otra versión**. |
| Formato | HDF5 legado, `keras_version=2.14.0`, backend TensorFlow |
| Arquitectura | **MobileNetV2** completa (transfer learning) → `GlobalAveragePooling2D` → `Dropout(0.3)` → `Dense(2, softmax)` |
| Entrada | `(None, 224, 224, 3)` |
| Capa de preprocesamiento interna | **Ninguna** (no tiene `Rescaling` ni `Lambda` de `preprocess_input`) |
| Pérdida y métrica | `categorical_crossentropy`, `accuracy` |

**Consecuencias:**
1. El preprocesamiento se aplicó **fuera** del modelo durante el entrenamiento y no quedó registrado. MobileNetV2 con `preprocess_input` espera valores en **[-1, 1]**. Si se entrenó con `ImageDataGenerator(rescale=1/255)`, espera **[0, 1]**. El código actual usa [0, 1] y BGR.
2. **El orden de las clases es desconocido.** Si se entrenó con `flow_from_directory` o `image_dataset_from_directory` usando las carpetas de PlantVillage (`Potato___Late_blight`, `Potato___healthy`), el orden alfabético deja **índice 0 = Late_blight** e índice 1 = healthy. Eso es **lo contrario** de `LABELS = ["Sano", "Tizon Tardio"]` en el código. Hay que verificarlo empíricamente.
3. **Compatibilidad:** TensorFlow 2.16 usa Keras 3 por defecto. Para cargar un `.h5` de Keras 2.14 de forma confiable, se fija `tensorflow-cpu==2.15.1` o se usa `tf_keras` con `TF_USE_LEGACY_KERAS=1`. Se decide en F4-IA-01 según la prueba de carga.

## 2. Verificación del preprocesamiento y del orden de clases (F4-IA-02)
**Procedimiento** (script `evaluation/calibrate.py`):
1. Tomar un **set de calibración** etiquetado: 30 o más imágenes por clase, **separado** del set de prueba final.
2. Evaluar la accuracy de las 8 combinaciones: {RGB, BGR} × {unit [0,1], mobilenet_v2 [-1,1]} × {orden [SANO, TIZON], orden [TIZON, SANO]}. Se clasifica la imagen completa, sin el filtro verde.
3. Elegir la combinación con mayor accuracy. Si la diferencia con la segunda es menor a 5 puntos, ampliar el set de calibración.
4. Guardar el resultado en `models/model_meta.json`:

```json
{
  "model_version": "mobilenetv2-2c-<fecha-o-hash>",
  "file": "modelo_tizon_tardio.h5",
  "sha256": "<hash del .h5>",
  "classes": ["TIZON_TARDIO", "SANO"],
  "input_spec": { "width": 224, "height": 224, "color_order": "RGB", "scaling": "mobilenet_v2" },
  "min_confidence": 0.30,
  "calibration": { "date": "2026-10-xx", "n_images": 60, "accuracy": 0.0, "notes": "" }
}
```
*(Los valores son de ejemplo; se completan con lo medido.)*

5. La IA **solo** usa `model_meta.json` para el preprocesamiento y las clases. Si el archivo falta o el `sha256` no coincide, `/health` informa `status=degraded`, `model_loaded=false`.

## 3. Rediseño del filtro de vegetación (F4-IA-03)
**Problema:** el umbral exige ≥ 40 % de verde en HSV `[35..75]` y descarta hojas necrosadas.

**Requisitos:**
- La máscara de "hoja" incluye el verde **y** los tonos marrones u oliva de la lesión que estén conectados a una región verde. Por ejemplo: máscara verde dilatada ∪ máscara marrón (H 5–30, S ≥ 40, V 30–200) dentro de la envolvente convexa de la región verde.
- El umbral de verde baja a ≥ 15 % dentro de la región. Se mantienen los filtros de textura y color uniforme contra fondos artificiales.
- Si el frame completo es una hoja en primer plano (región ≥ 60 % del área), se clasifica el frame completo.
- **Validación obligatoria:** en el set de prueba, el porcentaje de imágenes TIZON_TARDIO que terminan en NO_VEGETATION debe ser **≤ 5 %**. Se reporta antes y después del cambio.
- Todos los umbrales se definen como constantes en `app/vegetation.py`, con pruebas unitarias que incluyan una imagen sintética de hoja verde con manchas marrones.

## 4. Evaluación (F4-IA-04), que cumple las métricas del PDF
Script `evaluation/evaluate.py --data <dir> --mode {classifier,pipeline}`:
- **Estructura de datos:** `<dir>/SANO/*.jpg`, `<dir>/TIZON_TARDIO/*.jpg`.
- **Modos:**
  - `classifier`: imagen completa al modelo. Mide la calidad pura del modelo.
  - `pipeline`: flujo real (`/v1/analyze`, con filtro). Mide lo que vive el usuario. Los resultados NO_VEGETATION se cuentan como **predicción SANO** (falso negativo si la imagen tenía tizón) y además se reportan aparte.
- **Salida:** `evaluation/reports/<fecha>_<modo>.md` y `.json` con:
  - Accuracy, Precision, Recall, F1 y Especificidad (positivo = TIZON_TARDIO).
  - Tasa de error y matriz de confusión.
  - Latencia de inferencia media y p95.
  - Número de imágenes por clase.
  - Una tabla con la meta del PDF y el valor medido (✔/✘).
- **Integridad del set de prueba:**
  - Sin solapamiento con el set de entrenamiento: se comprueba con hash perceptual (pHash) contra el dataset de entrenamiento, si está disponible, y se reportan los duplicados.
  - Se documenta el origen de cada subconjunto.
  - Idealmente incluye **fotos de campo propias**: al menos 30 por clase, tomadas con el celular de prueba. Así se mide la brecha entre condiciones de laboratorio y de campo.
- **Calidad del dataset de entrenamiento** (si se entrega): conteo por clase, desbalance, duplicados exactos y perceptuales, resolución, ejemplos mal etiquetados (revisión de una muestra de 20 por clase). Va en `evaluation/reports/dataset_audit.md`.

## 5. Reentrenamiento (F4-IA-05) — hecho el 24-oct, ver §7
- Se usa `training/train.py`, reproducible (semilla fija), con MobileNetV2 + `preprocess_input` **dentro** del modelo (capa `Rescaling` o `Lambda`), *data augmentation* (rotación, brillo, recorte) y balanceo por pesos de clase.
- Partición estratificada 70/15/15 sin duplicados entre particiones.
- Exporta `.keras` (y `.h5` por compatibilidad) más `model_meta.json`. Se compara contra el modelo actual con el mismo set de prueba. Se adopta **solo si mejora el Recall sin bajar la Especificidad por debajo de 85 %**.

## 6. Pendiente del usuario
- ~~Ubicación del dataset de entrenamiento y, si existe, el notebook original.~~ Respuesta del 24-oct: se usó
  PlantVillage de Kaggle; ya no se tienen las imágenes ni el notebook (ver §7).
- Fotos de campo para el set de prueba independiente (P-02).

Si no llega el dataset antes del **27-oct**: se usa como respaldo el subconjunto de papa de PlantVillage (público) para calibración y prueba, **advirtiendo en el reporte** el posible solapamiento con el entrenamiento y la diferencia frente a condiciones de campo. Antes de redistribuir las imágenes, hay que verificar la licencia del dataset en la fuente.

## 7. Modelo de 4 salidas (24-oct, D-08)
**Por qué se reentrenó:** el usuario amplió el alcance (tizón temprano y saber si la hoja es de papa) y el modelo
original no se podía evaluar con honestidad: se entrenó con PlantVillage, pero ya no existen sus imágenes, su notebook
ni su partición, así que cualquier prueba con PlantVillage podía incluir imágenes de entrenamiento.

**Datos** (bajados de forma temporal de `github.com/spMohanty/PlantVillage-Dataset`, `raw/color`, y borrados al
terminar; el repositorio no declara licencia, así que no se redistribuyen):

| Clase | Imágenes | Origen |
|---|---|---|
| `SANO` | 152 | `Potato___healthy` |
| `TIZON_TEMPRANO` | 1.000 | `Potato___Early_blight` |
| `TIZON_TARDIO` | 1.000 | `Potato___Late_blight` |
| `NO_PAPA` | 1.050 | 30 hojas por cada uno de los 35 tipos de otros 13 cultivos (incluye tomate con tizón temprano y tardío) |

**Entrenamiento** (`training/train.py`, semilla 42): partición estratificada 70/15/15 en `training/split.csv`, con
los 44 pares de casi duplicados (pHash) dentro de la misma partición; MobileNetV2 (ImageNet) con `Rescaling` a
[-1, 1] **dentro** del modelo (la IA usa `scaling: none`, RGB); aumento de datos y pesos por clase; base congelada y
luego ajuste fino de los bloques 13–16. Keras 3.15.1 (fijado en `requirements.txt`).

**Resultados** sobre la partición de prueba (479 imágenes que el modelo no vio; reportes en
`TizonDetectionModel/evaluation/reports/2026-10-24_*`):

| Métrica | Meta | `classifier` | `pipeline` (filtro + modelo) |
|---|---|---|---|
| Accuracy (global, 4 clases) | ≥ 90 % | 95,6 % | 94,4 % |
| Precision TIZON_TARDIO | ≥ 90 % | 94,2 % | 94,8 % |
| Recall TIZON_TARDIO | ≥ 90 % | 97,3 % | 97,3 % |
| F1 TIZON_TARDIO | ≥ 90 % | 95,7 % | 96,1 % |
| Especificidad TIZON_TARDIO | ≥ 85 % | 97,3 % | 97,6 % |
| Tasa de error | ≤ 10 % | 4,4 % | 5,6 % |

- Tizón temprano: F1 98,0 % (`classifier`) y 95,6 % (`pipeline`).
- ¿Es papa? (`pipeline`): el 1,6 % de las hojas de papa se rechaza y el 7,6 % de las de otras plantas se acepta.
- F4-IA-03: TIZON_TARDIO en `NO_VEGETATION` 0 %; TIZON_TEMPRANO 4,0 % después de aceptar el amarillo de la clorosis
  conectado al verde (antes 14 %). El suelo sintético sigue sin analizarse.
- Línea base sin `NO_PAPA` (3 clases, misma partición de papa): accuracy 98,1 %. Distinguir otras plantas cuesta
  unos 2 puntos.
- Integridad: 0 duplicados exactos entre prueba y entrenamiento; 3 casi duplicados en el umbral (distancia 6),
  todos entre clases distintas; revisados a mano: son hojas distintas fotografiadas con la misma composición.

**Regla de adopción (§5):** el modelo anterior no se puede comparar con justicia (preprocesamiento sin verificar,
posible solapamiento con su entrenamiento y sin tizón temprano ni `NO_PAPA`). El nuevo cumple todas las metas del PDF
en la prueba independiente y queda adoptado; el anterior sigue en la carpeta raíz para volver atrás.

**Limitaciones:** fotos de laboratorio (una hoja sobre fondo uniforme): el desempeño en campo no está medido (P-02).
`NO_PAPA` solo conoce 13 cultivos de PlantVillage; malezas u otras plantas de campo pueden aceptarse como papa. Hay
solo 152 hojas sanas (22 en la prueba), así que las métricas de `SANO` tienen poca precisión estadística.

## 8. Prueba con fotos de campo y candidato v2 (24-oct, P-02 y propuesta 5)
**Set de campo** (no hay fotos propias todavía): PlantDoc (`github.com/pratikkayal/PlantDoc-Dataset`, CC BY 4.0),
fotos de internet tomadas en condiciones reales. Se armó con toda su partición test (papa con tizón y otras especies)
y el 30 % de las hojas de papa de su partición train, apartado por grupos de casi duplicados. Se retiraron 6 fotos
que PlantDoc repite en su entrenamiento: 5 aparecen con otra etiqueta (la misma imagen como tizón tardío y como
temprano, o como tomate), lo que muestra ruido en sus etiquetas. Quedan **274 fotos: 38 de tizón temprano, 32 de
tardío y 204 de otras especies. PlantDoc no tiene papa sana.** El origen de cada foto está en
`TizonDetectionModel/training/sources.csv` (`uso` = `campo`).

**Modelo en uso (4c), modo `pipeline`:** accuracy 73,7 %, recall de tizón tardío 6,2 % y **el 72,9 % de las hojas de
papa se rechaza como `NO_PAPA`**. Aprendió que "papa" es una hoja de PlantVillage sobre fondo uniforme: cualquier
foto de campo le parece otra planta. Su accuracy es la de responder siempre "no es papa" (204 / 274 = 74,5 %).

**Candidato v2** (`modelo_tizon_4c_v2.keras`, `mobilenetv2-4c-a7c205d2-20261024`): mismo entrenamiento de §7, con
`--keep-split` (la prueba de laboratorio es la misma) y más datos solo en train y val:
- 137 fotos de campo de papa con tizón de PlantDoc train, repetidas 3 veces (sobremuestreo: son pocas frente a 2.000
  de laboratorio). Las restantes de PlantDoc no se usaron para entrenar.
- 789 hojas de otras plantas como `NO_PAPA`: 349 de PlantDoc train y 440 sanas de PlantVillage.

| `pipeline` | Meta | 4c laboratorio | v2 laboratorio | 4c campo | v2 campo |
|---|---|---|---|---|---|
| Accuracy (global) | ≥ 90 % | 94,4 % | 93,7 % | 73,7 % | 73,4 % |
| Precision tizón tardío | ≥ 90 % | 94,8 % | 96,6 % | 11,8 % | 47,5 % |
| Recall tizón tardío | ≥ 90 % | 97,3 % | 94,7 % | 6,2 % | 59,4 % |
| F1 tizón tardío | ≥ 90 % | 96,1 % | 95,6 % | 8,2 % | 52,8 % |
| Especificidad tizón tardío | ≥ 85 % | 97,6 % | 98,5 % | 93,8 % | 91,3 % |
| F1 tizón temprano | — | 95,6 % | 95,3 % | 21,7 % | 38,3 % |
| Hojas de papa rechazadas (`NO_PAPA`) | — | 1,6 % | 1,6 % | 72,9 % | 15,7 % |
| Otras plantas aceptadas como papa | — | 7,6 % | 8,3 % | 4,4 % | 19,6 % |

- En `classifier` (modelo solo), el v2 da en laboratorio 96,0 % de accuracy y 96,7 % de F1 de tizón tardío. En campo,
  `classifier` y `pipeline` dan casi lo mismo: accuracy 73,7 % y 73,4 %, y rechazo de papa 18,6 % y 15,7 %.
- Intervalos de Wilson al 95 % en campo: recall de tizón tardío de 1,7 % a 20,1 % (4c) frente a 42,3 % a 74,5 % (v2).
  No se solapan. La accuracy global se solapa por completo (68 % a 79 % en ambos).
- Latencia: los dos modelos tardan lo mismo (misma arquitectura). Medidos en el mismo proceso y con la misma imagen,
  la media es de unos 238 ms y el p95 de unos 300 ms en el PC de desarrollo. La meta (p95 < 1,5 s) se mide en la EC2.
- Integridad: sin duplicados entre el set de campo y el entrenamiento del v2 después de retirar las 6 fotos. En
  laboratorio aparecen 2 casi duplicados más (papa con cerezo o soya sanos, en el umbral de pHash), que por ser de
  especies distintas no son la misma hoja.

**Decisión: el modelo en uso sigue siendo el 4c, y la adopción del v2 queda en manos del usuario.** La regla
fijada antes de ver los resultados pedía dos cosas al v2 frente al 4c sobre el mismo set de campo: mejorar la accuracy
y bajar el rechazo de papa. El rechazo bajó del 73 % al 16 %, pero la accuracy quedó igual. En este set la accuracy
global no sirve para compararlos, porque el 74 % de las fotos son de otras plantas.

**Recomendación: adoptar el v2.** Es el único que detecta tizón en fotos de campo y conserva las metas de laboratorio.
Su costo es que analiza el 19,6 % de las hojas de otras plantas como si fueran papa, frente al 4,4 % del 4c.

Para adoptarlo:
1. En `deploy/models/`, reemplazar `model_meta.json` por `model_meta_v2.json` y poner
   `MODEL_FILE=modelo_tizon_4c_v2.keras` en `deploy/.env`. Luego reiniciar `ia`.
2. Copiar ese meta a `TizonDetectionModel/models/model_meta.json` y actualizar las referencias al archivo del modelo.

**Limitaciones que siguen:**
- No hay papa sana de campo, así que no se mide cuántas hojas sanas se marcarían como enfermas.
- PlantDoc tiene etiquetas con ruido y fotos de internet, que no son las del celular en el cultivo.
- Ninguno de los dos llega al 90 % en campo.
- El siguiente paso es el set propio (`protocolo-fotos-campo.md`). Además, hay datasets abiertos de campo con papa sana
  que valdría la pena evaluar, con su licencia verificada:
  - Shabrina et al. (2023), *Potato Leaf Disease Dataset in Uncontrolled Environment*, Mendeley Data;
  - *Potato Leaf (Healthy and Late Blight)*, Mendeley Data.
