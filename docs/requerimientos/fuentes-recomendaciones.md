# Fuentes de las recomendaciones agronómicas (P-05)

Resuelve P-05 y RF-08 CA3 (24-oct). Las recomendaciones están en la tabla `recommendations` de la API, sembrada por
`V6__recommendations_by_disease.sql`.

## Regla de selección
- Se cuentan los frames DETECTED de la sesión con tizón temprano y con tizón tardío.
- El resumen trae las recomendaciones de la enfermedad con más frames. En empate gana el tizón tardío, porque es la
  enfermedad más severa y la misma regla de desempate usa `dominant_label`.
- Si no hubo frames afectados, la lista va vacía. Las hojas sanas, sin vegetación o de otras plantas no cuentan.
- El código está en `SessionStatsCalculator.predominantDisease` (API) y el contrato está en el esquema `Recommendation.disease`.

## Criterios de redacción
- Cada texto resume lo que dice una guía técnica y lleva su fuente con la página. No se agregó nada que no esté en la fuente.
- No se nombran productos comerciales ni dosis. El control químico siempre remite a un ingeniero agrónomo, como indican
  el ICA y la FHIA. Eso también cubre los errores del modelo, porque el primer paso es confirmar el diagnóstico.
- Se usa el tuteo, como en el resto de la app.
- **Pendiente:** que un ingeniero agrónomo revise el resumen antes de usar la app en un cultivo real. Las fuentes son
  oficiales, pero el resumen lo hizo el equipo.

## Referencias
| Clave en la app | Referencia completa | Enlace |
|---|---|---|
| ICA (2011) | Instituto Colombiano Agropecuario. *Manejo fitosanitario del cultivo de la papa (Solanum tuberosum subsp. andigena y S. phureja): medidas para la temporada invernal*. Bogotá, 2011. Código 00.09.31.11. Secciones "Gota o añublo de la papa" (p. 18-19) y "Tizón temprano" (p. 19-20). | [ica.gov.co](https://www.ica.gov.co/getattachment/b2645c33-d4b4-4d9d-84ac-197c55e7d3d0/Manejo-fitosanitario-del-cultiva-de-la-papa-nbsp;-.aspx) |
| FHIA (2014) | Rivera C., J. M.; Brown, J. K.; Weller, S.; Melgar, J. C. *Consideraciones técnicas para el efectivo manejo integrado del tizón tardío en papa*. Fundación Hondureña de Investigación Agrícola, La Lima, 2014. 17 p. Reproducción autorizada sin fines comerciales, citando la fuente. | [fhia.org.hn](https://fhia.org.hn/wp-content/uploads/consideraciones_tecnicas_para_manejo_de_tizon_tardio_en_papa.pdf) |
| Universidad de Wisconsin-Madison (2026) | Gevens, A.; Abbrescia, A.; Groves, R.; Bradford, B. *Potato Early Blight*. UW Vegetable Pathology, University of Wisconsin-Madison. Última actualización: 24 de agosto de 2026. | [vegpath.plantpath.wisc.edu](https://vegpath.plantpath.wisc.edu/diseases/potato-early-blight) |

## Trazabilidad de cada texto
| id | Enfermedad | Idea | Qué dice la fuente |
|---|---|---|---|
| 1 | Tizón tardío | Agrónomo, diagnóstico y plan de fungicidas | ICA p. 19: el plan de rotación de fungicidas se establece con un ingeniero agrónomo y combina productos preventivos y sistémicos con mecanismos de acción distintos. FHIA p. 14: lo ideal es iniciar las aplicaciones cuando el monitoreo detecta síntomas. |
| 2 | Tizón tardío | Retirar hojas con lesiones | FHIA p. 7 (sanitización): en parcelas pequeñas, eliminar a mano las hojas infectadas apenas se detectan baja el inóculo secundario. |
| 3 | Tizón tardío | Riego | FHIA p. 7: evitar el riego aéreo y, si es la única opción, no regar por la tarde, para que las hojas no pasen la noche húmedas. |
| 4 | Tizón tardío | Monitoreo y clima | FHIA p. 5: los ataques más severos ocurren con noches de 10-15 °C, días de 15-22 °C y humedad libre sobre la hoja por 6 horas o más. ICA p. 19: en épocas lluviosas el ciclo de la enfermedad dura de 4 a 6 días, y se recomienda consultar la información meteorológica y vigilar el cultivo de forma permanente. |
| 5 | Tizón tardío | Próxima siembra | ICA p. 19: semilla certificada, siembra oportuna antes de las lluvias, distancias que disminuyan la humedad, drenajes, aporques altos, variedades tolerantes y fertilización según el análisis de suelo. FHIA p. 6-7: eliminar las plantas voluntarias y evitar dosis altas de nitrógeno. |
| 6 | Tizón temprano | Agrónomo y control químico | ICA p. 20: el control químico es preventivo, debe seguir la recomendación de un ingeniero agrónomo y es diferente al de la gota. Wisconsin: los fungicidas controlan bien la enfermedad si se aplican temprano en la infección. |
| 7 | Tizón temprano | Nutrición y vigor | ICA p. 19-20: la enfermedad ataca sobre todo plantas desnutridas, afectadas por otras plagas o enfermedades o con menos vigor, y el manejo incluye la fertilización. Wisconsin: la deficiencia de nitrógeno favorece la enfermedad. |
| 8 | Tizón temprano | Dónde mirar | ICA p. 19: manchas cafés con anillos concéntricos que empiezan en las hojas más viejas. Wisconsin: los síntomas aparecen cerca de la base de la planta y suben, y conviene monitorear con regularidad. |
| 9 | Tizón temprano | Residuos y rotación | ICA p. 20: el hongo sobrevive en residuos de cosecha, toyas y malezas, y el manejo incluye destruirlos y rotar cultivos. Wisconsin: rotar con cultivos que no sean solanáceas durante 3 a 4 años. |
| 10 | Tizón temprano | Humedad del follaje | Wisconsin: evitar el exceso de riego y mantener un espaciamiento que permita la circulación del aire. ICA p. 20: usar barreras rompevientos, porque la lluvia y el viento diseminan el hongo. |
