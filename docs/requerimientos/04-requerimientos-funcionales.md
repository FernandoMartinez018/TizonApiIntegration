# 04 · Requerimientos funcionales

Formato: **ID · Prioridad · Cierra (falencias)**, seguido de la descripción y los criterios de aceptación (CA) en estilo Dado / Cuando / Entonces.

---

## Autenticación y usuarios

### RF-01 · M · API-6, MOB-8 — Registro de usuario
El usuario crea una cuenta con usuario, nombre completo y contraseña. Queda con el rol `AGRICULTOR`.
- CA1: Dado un usuario nuevo válido, cuando se registra, entonces responde 201 y puede iniciar sesión de inmediato.
- CA2: Dado un usuario ya existente, entonces responde 409 `USERNAME_TAKEN`.
- CA3: Las contraseñas se guardan con BCrypt (coste 12) y nunca aparecen en logs ni en respuestas.
- CA4: Una contraseña de menos de 8 caracteres responde 400 con el error en `fields.password`.

### RF-02 · M · API-5, MOB-8 — Inicio y cierre de sesión
- CA1: Con credenciales válidas, la respuesta incluye `token`, `expires_at` (calculado desde `JWT_EXPIRATION_MS`) y `user`.
- CA2: Con credenciales inválidas o un usuario deshabilitado, responde 401 `BAD_CREDENTIALS`, sin indicar cuál de los dos datos falló.
- CA3: La app guarda el token cifrado (EncryptedSharedPreferences) y, al reabrirse antes de `expires_at`, entra directo a la pantalla principal.
- CA4: Al cerrar sesión desde el Perfil, se borra el token y se vuelve al login.
- CA5: Cualquier 401 recibido con un token guardado provoca el cierre de sesión y el mensaje "Tu sesión expiró, inicia sesión de nuevo".

### RF-03 · M · API-6 — Administrador inicial y gestión de roles
- CA1: En el primer arranque, si no existe ningún ADMIN, se crea uno con `ADMIN_USERNAME` y `ADMIN_PASSWORD`. Si esas variables faltan, la API **no arranca** y registra un error claro.
- CA2: Un ADMIN puede listar usuarios y cambiar su rol o habilitación. Otros roles reciben 403.
- CA3: Desaparece el `InMemoryUserDetailsManager` y el usuario `admin/admin`.

---

## Escaneo y análisis

### RF-04 · M · MOB-1, MOB-2, API-1 — Ciclo de vida de la sesión de escaneo
- CA1: La sesión se crea **al pulsar "Iniciar escaneo"**, no al abrir la pantalla.
- CA2: Todos los frames se envían a `POST /sessions/{id}/frames` de la sesión activa. El backend **nunca** crea sesiones de forma implícita.
- CA3: Un frame enviado a una sesión que no está ACTIVE responde 409 `SESSION_NOT_ACTIVE`. La app ofrece iniciar una sesión nueva.
- CA4: Al finalizar, la app limpia `sessionId` y los contadores, y muestra el resumen.
- CA5: El usuario de la sesión es siempre el del JWT. No queda ningún usuario escrito en el código.
- CA6: Salir de la pantalla de escaneo con una sesión activa pregunta "¿Finalizar escaneo?" (Finalizar / Seguir escaneando / Descartar). "Descartar" la deja en `CANCELLED`.

### RF-05 · M · MOB-4, MOB-5, MOB-7 — Captura y envío de frames
- CA1: Antes de codificar a JPEG, el frame se convierte correctamente a Bitmap (respetando *strides*, por ejemplo con `ImageProxy.toBitmap()`) y **se rota según `rotationDegrees`**.
- CA2: El lado mayor del frame se escala a 640 px y se codifica como JPEG con calidad 80. El tamaño final debe ser menor a 300 KB.
- CA3: Solo hay un envío en curso a la vez. El siguiente se envía `frame_interval_ms` después de recibir la respuesta. No se usa `delay` fijo.
- CA4: Las cajas se dibujan alineadas con la vista previa, teniendo en cuenta el escalado y el recorte de `FILL_CENTER`. Hay una prueba unitaria de la función de mapeo de coordenadas.

### RF-06 · M · IA-1, IA-2, IA-3, IA-5 — Análisis de IA
- CA1: La IA aplica el preprocesamiento declarado en `model_meta.json` (orden de colores y escala), verificado en F4 (ver 08-modelo-ia).
- CA2: El filtro de vegetación **no descarta las hojas con necrosis**: una imagen de prueba de hoja con tizón (set de verificación) debe producir `DETECTED`.
- CA3: El resultado principal sigue la regla de severidad del documento 03 (`TIZON_TARDIO` > `TIZON_TEMPRANO` > `SANO`).
- CA6 (D-08, 24-oct): si la hoja es de otra planta, la IA responde `NOT_POTATO` y no analiza el tizón; la app muestra "No es una hoja de papa".
- CA4: `/health` informa `classes` en el orden de salida del modelo, `model_version` e `input_spec`.
- CA5: La inferencia no bloquea el *event loop* (`run_in_threadpool`). El p95 de `inference_ms` en la EC2 debe ser menor a 1.500 ms.

### RF-07 · M · API-3, MOB-6, MOB-13 (parcial) — Manejo de errores visible
- CA1: Si la IA está caída, responde con error o excede el tiempo, Spring responde 502 o 504 y **no inserta** ninguna fila en `scan_results`.
- CA2: Ante una imagen inválida, responde 400 `INVALID_IMAGE`. Si es una imagen corrupta generada por la propia app, no se muestra al usuario; se registra y se descarta el frame.
- CA3: La app distingue cuatro estados visibles:
  - "Analizando…"
  - "No veo una hoja. Acerca la cámara" (`NO_VEGETATION`)
  - "Servicio no disponible. Reintentando…" (502/504, con reintento automático y espera creciente de 2, 4 y 8 s)
  - "Sin conexión"
- CA4: Cada petición lleva un `X-Request-Id` (generado por la app) que se propaga a Spring y a la IA y aparece en los logs de los tres servicios.

### RF-08 · M · API-7 — Resumen de sesión y recomendaciones
- CA1: Las estadísticas siguen `SessionStats`: `avg_confidence` solo sobre los frames DETECTED y `dominant_label` es la clase más frecuente (en empate, la más severa). Afectado = tizón temprano o tardío; el resumen los muestra por separado (`label_counts`).
- CA2: La severidad se calcula con los umbrales de configuración: `warning_pct=10` y `critical_pct=30` por defecto.
- CA3: El backend entrega las recomendaciones por severidad, cada una con su `source`. Se guardan en una tabla `recommendations` sembrada por Flyway. **No se inventan textos agronómicos:** si no hay una fuente validada, el texto se limita a "Consulta a un técnico agrónomo" y queda anotado en 10-pendientes.
- CA4: Hay pruebas unitarias del cálculo con casos borde: 0 frames, todos NO_VEGETATION, 100 % afectados.

### RF-09 · M · MOB-14 — Historial
- CA1: Lista paginada de las sesiones propias, con fecha y hora local (America/Bogota), finca o lote, % afectado, severidad (icono + texto + color) y estado.
- CA2: El detalle de una sesión muestra las estadísticas, la ubicación (coordenadas y, si F6-C está hecho, mini-mapa) y las recomendaciones.
- CA3: Si no hay sesiones, se muestra un estado vacío con la acción "Iniciar primer escaneo".
- CA4: TECNICO y ADMIN pueden filtrar por usuario.

---

## Ubicación y alertas

### RF-10 · S · §3.2 del PDF — Ubicación GPS open source
- CA1: Se usa `android.location.LocationManager` (proveedores GPS y red). **No** se usa `play-services-location`.
- CA2: El permiso `ACCESS_FINE_LOCATION` se pide con una explicación previa. Si se rechaza, el escaneo sigue sin ubicación y la sesión se guarda con `location=null`.
- CA3: Al iniciar el escaneo se toma una posición (o la última conocida con menos de 2 min), con un tiempo de espera de 10 s. Se guarda `accuracy_m`.
- CA4: Es opcional ingresar el nombre de la finca o lote; se recuerda el último usado.

### RF-11 · S · §3.2 del PDF — Alertas
- CA1: Al finalizar una sesión que cumple la regla de alerta (documento 03), se crea una alerta `OPEN`.
- CA2: La app consulta `GET /alerts?status=OPEN&since=…` al abrir y cada 15 min con WorkManager mientras haya sesión iniciada. Si hay alertas nuevas, muestra una **notificación local** (canal "Alertas de cultivo"). No se usa Firebase.
- CA3: La pantalla de Alertas lista las alertas abiertas y atendidas. Un TECNICO puede marcarlas como atendidas (`ack`).
- CA4: En Android 13+ se pide el permiso `POST_NOTIFICATIONS` con una explicación previa.

### RF-12 · C — Mapa de sesiones (OSM)
- CA1: Mapa con osmdroid (licencia Apache-2.0) y teselas de OpenStreetMap. Se configura un `User-Agent` propio, según la política de uso de teselas de OSM.
- CA2: Muestra marcadores coloreados por severidad para las sesiones con ubicación. Al tocar uno, abre el detalle.
- CA3: Si no se completa a tiempo, se oculta la pestaña y la entrega no se bloquea.
