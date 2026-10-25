# Matriz de pruebas (RNF-PRU-01, F7-01)

**Meta del PDF (§4.11):** ≥ 90 % de las funcionalidades principales verificadas. Según 05-RNF, cuenta cada fila con una
prueba que pasa. Las metas de calidad del modelo y del producto están en la §C.

**Estado al 24-oct-2026** (después del modelo de 4 salidas, D-08) (rama `dev` de los 3 repos):

| Repositorio | Pruebas automatizadas | Resultado |
|---|---|---|
| TizonDetectionModel | 80 unitarias (pytest, incluye la prueba de contrato) + 2 de integración con el modelo real | ✔ todas pasan |
| TizonApiIntegration | 89 (JUnit, MockWebServer, H2 en modo PostgreSQL y Testcontainers con PostgreSQL 16). Cobertura JaCoCo de líneas: 92,8 % en total, `service` 95,5 % y `security` 88,8 % (mínimo exigido 70 %, RNF-PRU-02) | ✔ todas pasan |
| TizonMovile | 163 (55 unitarias de ViewModels, cliente y mapeo + 108 capturas Roborazzi de todos los `@Preview`) | ✔ todas pasan |
| Sistema completo | `deploy/scripts/smoke-test.sh`, una verificación extra de 11 llamadas (alertas, usuarios, filtro del historial, cancelación, 409, 404 y 401) y una prueba con imágenes reales (sana, tizón temprano, tizón tardío y 2 de otras plantas) contra `docker compose` | ✔ pasan |
| Dispositivo | Guion `guion-prueba-dispositivo.md` (DISP-01 a DISP-99) | ⏳ P1 el 30-oct, P2 el 5-nov |

**Leyenda:** ✔ verificada (todos sus criterios tienen prueba que pasa) · ◐ con prueba que pasa, pero un criterio solo
se confirma en el dispositivo o con datos reales · ⏳ sin prueba que pase todavía · N/A fuera del alcance.

---

## A. Funcionalidades del PDF (§4.11)

| # | Funcionalidad | Pruebas automatizadas (repo: prueba) | Manual | Estado |
|---|---|---|---|---|
| 1 | Carga de imágenes | App: `ScanViewModelTest` (iniciarCreaLaSesion…YPideUnFrame, unSoloFrameEnCursoYElSiguienteTrasElIntervalo), `PreviewTransformTest.elFrameSeEscalaA640PorElLadoMayor`. API: `SessionFlowTest.elDuenoEnviaFramesYRecibeEstadisticas`. Sistema: *smoke test* (frame analizado) | DISP-21, DISP-22 (cámara real, rotación del frame) | ◐ |
| 2 | Procesamiento de imágenes | IA: `test_predictor` (decodificación, escala `unit` y `mobilenet_v2`, RGB/BGR), `test_vegetation` (8: hoja sana, necrosis, suelo, fondos artificiales, primer plano), `test_model_meta` (5) | — | ✔ |
| 3 | Clasificación de la imagen | IA: `test_predictor.test_load_model` y `test_predict_imagen_verde_cumple_el_contrato` (integración con el `.h5`), `test_api.test_frame_valido_devuelve_resultado_principal`, `test_contract` (7) | — | ✔ |
| 4 | Identificación de tizón tardío | IA: `test_gana_la_clase_mas_severa_aunque_tenga_menor_confianza`, `test_hoja_con_necrosis_se_detecta_completa_y_pasa_el_filtro`. API: `SessionStatsCalculatorTest` (6), `AlertFlowTest.reglaDeAlerta`. Exactitud medida en la partición de prueba de PlantVillage (08 §7): accuracy 95,6 % / 94,4 %, F1 de tizón tardío 95,7 % / 96,1 % y de temprano 98,0 % / 95,6 % | DISP-24, DISP-24A, DISP-25A | ◐ (falta campo, P-02) |
| 5 | Presentación del resultado | App: 102 capturas Roborazzi (escaneo, resumen, historial y alertas en claro, oscuro y fuente al 200 %), `ScanViewModelTest.finalizarEmiteElResumenYLimpiaLaSesion`, `ModelsDeserializationTest` (6) | DISP-22, DISP-23 (cajas alineadas sobre la cámara), DISP-32 | ◐ |
| 6 | Manejo de imágenes no válidas | IA: `test_base64_invalido_responde_400_invalid_image`, `test_imagen_corrupta_responde_400_invalid_image`, `test_frame_corto_responde_422_con_campo`, `test_sin_hoja_es_resultado_valido_con_label_null`. API: `IaClientIntegrationTest.ia400EsInvalidImage`, `SessionFlowTest.noVegetationEsUnResultadoQueSePersiste`. App: `ScanViewModelTest.frameInvalidoSeDescartaSinCambiarElEstado`, `sinHojaMuestraElEstadoYConservaElUltimoDiagnostico` | DISP-25 | ✔ |
| 7 | Registro de resultados | API: `SessionFlowTest` (persistencia, historial, `errorDeLaIaNoPersisteNada`), `PostgresSchemaTest` (Flyway + Hibernate en PostgreSQL real), `AlertFlowTest` (6). App: `HistoryViewModelTest` (7). Sistema: verificación extra (historial filtrado, cancelación) | DISP-51, DISP-52 | ✔ |
| 8 | Funcionamiento general de la interfaz | App: 102 capturas Roborazzi, `AuthViewModelTest` (9), `SessionManagerTest` (5), `HistoryViewModelTest` (7), `ScanViewModelTest` (12) | DISP-01 a DISP-84 | ◐ |

**Resultado de la §A:** 8 de 8 funcionalidades con prueba automatizada que pasa (**100 %**). 4 de 8 quedan
totalmente verificadas (✔) y 4 esperan la prueba en el dispositivo (◐).

---

## B. Requerimientos funcionales (04-requerimientos-funcionales)

| RF | Pruebas automatizadas | Manual | Pendiente | Estado |
|---|---|---|---|---|
| RF-01 Registro | API: `UserFlowTest` (registroCreaAgricultorQuePuedeIniciarSesion, usuarioDuplicadoResponde409, claveCortaRespondeErrorEnElCampo; BCrypt coste 12 en el hash). App: `AuthViewModelTest` (registro…) | DISP-10, DISP-11 | — | ✔ |
| RF-02 Inicio y cierre de sesión | API: `UserFlowTest` (credencialesInvalidasYUsuarioDeshabilitadoDanElMismo401, tokenDeUsuarioDeshabilitadoDejaDeFuncionar, cincoFallosBloqueanElUsuario), `LoginAttemptServiceTest` (3), `SecurityConfigTest` (5). App: `SessionManagerTest` (5), `TizonApiClientTest.un401ConSesionAvisaQueElTokenExpiro`, `AuthViewModelTest` | DISP-12 a DISP-17, DISP-71, DISP-72 | CA3: token cifrado y entrada directa al reabrir, en el dispositivo | ◐ |
| RF-03 Administrador y roles | API: `UserFlowTest` (elAdminInicialSeCreaDesdeLaConfiguracion, soloAdminGestionaUsuarios, adminNoPuedeModificarseASiMismo), `RequiredSecretsStartupTest` | DISP-70, DISP-73 | — | ✔ |
| RF-04 Ciclo de la sesión | API: `SessionFlowTest` (iniciarConUbicacionYFincaUsaElUsuarioDelJwt, sesionFinalizadaResponde409YNoCreaUnaNueva, descartarDejaLaSesionCancelada, sesionesAbandonadasPasanACancelled). App: `ScanViewModelTest` (iniciar…, sesionNoActiva…, finalizar…, descartarCancelaLaSesion) | DISP-20, DISP-30, DISP-31 | CA6: diálogo al salir, en el dispositivo | ◐ |
| RF-05 Captura y envío de frames | App: `PreviewTransformTest` (5), `ScanViewModelTest.unSoloFrameEnCursoYElSiguienteTrasElIntervalo` | DISP-21 a DISP-23, DISP-28 | CA1: rotación según `rotationDegrees` con la cámara real | ◐ |
| RF-06 Análisis de IA | IA: `test_predictor` (16, incluye 4 salidas y `NOT_POTATO`), `test_vegetation` (12, incluye clorosis y suelo), `test_api` (10), `test_contract` (11), integración (2). Entrenamiento, calibración y evaluación con pruebas (`test_evaluation`, 15) | — | CA5: p95 en la EC2. Desempeño en campo (P-02) | ◐ |
| RF-07 Errores visibles | API: `IaClientIntegrationTest` (5), `SessionFlowTest.errorDeLaIaNoPersisteNada`, `ObservabilityTest` (3). App: `ScanViewModelTest` (servicioNoDisponibleReintentaCon2Y4Segundos, sinConexionMuestraSinConexion, frameInvalido…). Sistema: el mismo `request_id` en los logs de la API y de la IA | DISP-26, DISP-27, DISP-95 | — | ✔ |
| RF-08 Resumen y recomendaciones | API: `SessionStatsCalculatorTest` (0 frames, todos NO_VEGETATION, 100 % afectado, empate, umbrales). App: `ScanViewModelTest.finalizarEmiteElResumenYLimpiaLaSesion` | DISP-32 | CA3: textos con fuente (P-05); hoy solo el texto genérico permitido | ✔ |
| RF-09 Historial | API: `SessionFlowTest.historialPropioYFiltroSoloParaTecnico`. App: `HistoryViewModelTest` (paginación, refresco, error, filtro del técnico, respuesta atrasada). Sistema: filtro con y sin resultados | DISP-50 a DISP-54 | — | ✔ |
| RF-10 Ubicación GPS | API: `SessionFlowTest.iniciarConUbicacionYFincaUsaElUsuarioDelJwt`. App: `ScanViewModelTest` (iniciar…ConUbicacion…, sinPermisoDeUbicacionElEscaneoSigueSinUbicacion, cambiarDeFincaSeGuarda) | DISP-40, DISP-41 | CA3: posición real en menos de 10 s | ◐ |
| RF-11 Alertas | API: `AlertFlowTest` (reglaDeAlerta, sesionQueSuperaElUmbral…, menosDeCincoFramesNoGeneraAlerta, unaSolaAlertaPorSesion, soloTecnicoOAdminMarcanComoAtendida, sinceFiltraLasAlertasNuevas). App: `HistoryViewModelTest.elSiguienteSinceEsLaAlertaMasReciente` | DISP-60 a DISP-63 | CA2: notificación local en 15 min o menos | ◐ |
| RF-12 Mapa (prioridad C) | — | — | No se implementa; RF-12 CA3 permite omitirlo | N/A |

**Resultado de la §B:** 11 de 11 RF dentro del alcance con prueba automatizada que pasa (**100 %**): 5 ✔ y 6 ◐.

---

## C. Metas de calidad del PDF (§4.1–§4.10)

| Métrica (PDF) | Meta | Cómo se mide | Estado |
|---|---|---|---|
| Accuracy, Precision, Recall, F1 | ≥ 90 % | `evaluation/evaluate.py` sobre la partición de prueba (479 imágenes): 95,6 / 94,2 / 97,3 / 95,7 % (`classifier`) y 94,4 / 94,8 / 97,3 / 96,1 % (`pipeline`) | ✔ en laboratorio; ⏳ en campo (P-02) |
| Especificidad | ≥ 85 % | 97,3 % (`classifier`) y 97,6 % (`pipeline`) | ✔ en laboratorio |
| Tasa de error | ≤ 10 % | 4,4 % (`classifier`) y 5,6 % (`pipeline`) | ✔ en laboratorio |
| Tiempo de respuesta | ≤ 5 s por imagen | DISP-90 (p95 de `latency_ms` en la BD) y DISP-91 (cronómetro); `inference_ms` p95 < 1.500 ms en la EC2 (RNF-REN-02) | ⏳ P1 (local) y P2 (EC2) |
| Disponibilidad | ≥ 95 % | `deploy/scripts/uptime.sh` cada minuto en la EC2 | ⏳ depende de la EC2 (P-03) |
| Usabilidad | ≥ 85 % | `encuesta-sus.md`: puntaje SUS promedio ≥ 85 con 5 personas o más | ⏳ falta aplicarla |
| Mantenibilidad | Actualizar el modelo sin reconstruir | Se reemplazan `deploy/models/*.h5` y `model_meta.json` y se reinicia `ia` (RNF-MAN-02); `test_model_meta.test_el_meta_del_repo_es_valido` y la carga con verificación de `sha256` | ✔ |
| Cobertura de pruebas | ≥ 90 % de las funcionalidades | Esta matriz | ✔ 100 % con prueba que pasa (§A) |

---

## Cómo se actualiza
1. Después de cada prueba en el dispositivo, el estado de cada ◐ pasa a ✔ si sus casos DISP pasaron, o a ✘ si
   alguno falló (el hallazgo va a `10-pendientes.md`).
2. Con el dataset (P-01, P-02), los reportes de `evaluation/reports/` completan la §C. Los valores se copian tal
   cual, aunque no alcancen la meta.
3. Los conteos de pruebas automatizadas se toman de la última ejecución del CI de cada repo.
