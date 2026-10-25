# Protocolo de fotos de campo (P-02)

Sirve para armar un set de prueba propio, tomado en condiciones reales, y medir con él el modelo, igual que con
PlantVillage (laboratorio) y PlantDoc (fotos de internet). Es el insumo que falta para cerrar P-02 y para saber cuánto
se aleja el desempeño en campo del de laboratorio (08-modelo-ia §4).

## 1. Qué fotografiar
| Carpeta | Qué va | Mínimo | Ideal |
|---|---|---|---|
| `SANO` | Hojas de papa sin lesiones | 30 | 50 |
| `TIZON_TEMPRANO` | Hojas de papa con manchas cafés de anillos concéntricos, casi siempre en las hojas de abajo | 30 | 50 |
| `TIZON_TARDIO` | Hojas de papa con lesiones de borde irregular, café grisáceo, a veces con el moho blanco en el envés | 30 | 50 |
| `NO_PAPA` | Hojas de otras plantas del lote o de los bordes: malezas, cultivos vecinos y otras solanáceas como tomate o uchuva | 30 | 50 |

La carpeta `NO_PAPA` es la más importante para medir el aviso "No es una hoja de papa". El modelo solo conoce los
13 cultivos de PlantVillage, así que las malezas del lote son su punto más débil.

## 2. Cómo tomarlas
- Usa el celular de la prueba en el dispositivo, sin zoom digital, a 20-40 cm de la hoja, con una hoja o un foliolo
  centrado. Así es como la app ve la hoja al escanear.
- Varía las condiciones: plantas, lotes y días distintos, con sol y con nublado, sin flash.
- Toma como máximo 2 fotos de la misma hoja. Fotos casi iguales inflan el resultado (el reporte las detecta con pHash).
- No fotografíes personas ni placas de vehículos.

## 3. Cómo etiquetarlas
- Etiqueta solo lo que sea claro. Si dudas entre un tizón y otra enfermedad o un daño por insectos, **no uses la foto**.
  Una etiqueta adivinada daña más la medición que una foto menos.
- Lo ideal es que un ingeniero agrónomo confirme las etiquetas, sobre todo las de tizón temprano frente a tardío.
- Anota cada foto en un `etiquetas.csv` con las columnas `archivo,clase,fecha,lote,etiqueto,observaciones`.

## 4. Dónde guardarlas
- Guárdalas en una carpeta **fuera de los repositorios**, con una subcarpeta por clase (`SANO/`, `TIZON_TEMPRANO/`,
  `TIZON_TARDIO/`, `NO_PAPA/`).
- **No las subas a git:** los tres repos son públicos y las fotos guardan en el EXIF la ubicación GPS de la finca. Si
  hay que compartirlas, borra antes los metadatos.
- No las uses para entrenar mientras sean el set de prueba. Si después se quiere entrenar con ellas, se necesita otro
  set de prueba.

## 5. Cómo evaluar
Desde la raíz de `TizonDetectionModel`, con el entorno de desarrollo instalado (README):

```bash
python -m evaluation.evaluate --data <carpeta> --mode classifier --model ../modelo_tizon_4c.keras --tag campo-propio --source "Fotos propias: <lugar>, <fechas>, <celular>"
python -m evaluation.evaluate --data <carpeta> --mode pipeline --model ../modelo_tizon_4c.keras --tag campo-propio --source "Fotos propias: <lugar>, <fechas>, <celular>"
```

- `classifier` mide el modelo solo. `pipeline` mide lo que vive el usuario (filtro de vegetación + modelo + aviso de
  hoja que no es de papa).
- Los reportes quedan en `evaluation/reports/<fecha>_campo-propio_<modo>.md` y `.json`, con la tabla de metas del PDF
  y la matriz de confusión. Se versionan junto con el modelo evaluado. Las fotos no se versionan.
- Puede faltar cualquier carpeta salvo `TIZON_TARDIO`. El reporte indica cuántas imágenes tuvo cada clase.

## 6. Cómo leer el resultado
- Compara con los reportes de laboratorio (`*_classifier.md`, `*_pipeline.md`) y de PlantDoc (`*_campo-plantdoc_*.md`).
- Una caída grande en campo indica que hacen falta fotos de campo en el entrenamiento. Eso es trabajo futuro y se
  declara como limitación en `docs/entrega/resultados-metricas.md`.
- Los errores de cada imagen aparecen al final del reporte: revísalos antes de concluir. A veces el error está en la
  etiqueta y no en el modelo.
