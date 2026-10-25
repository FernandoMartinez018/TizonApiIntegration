# Guion de prueba en dispositivo (RNF-PRU-05, F3-ALL-01)

Prueba manual de punta a punta en un **Android físico**. Se ejecuta dos veces, en las fechas que indique el usuario
(estaban planeadas para el 30-oct y el 5-nov):

| Prueba | Fecha | Servidor | APK | Cierra |
|---|---|---|---|---|
| **P1** | A definir | `docker compose` en el PC | *debug*, con `adb reverse` | F1-MOB-03, F3-MOB-01, F3-MOB-05, F6-MOB-01, F6-MOB-02 (y verifica F6-MOB-04) |
| **P2** | A definir | EC2 (F5-INF-04) | *release* firmada (F5-MOB-01) | F5-ALL-02 |

La ejecuta el usuario con el teléfono en la mano. Los hallazgos se registran como se indica en el §4.
Los resultados esperados citan los textos exactos de la app (`strings.xml`) y las reglas de los documentos 03 y 04.
El modelo se entrenó con fotos de laboratorio (PlantVillage; 08 §7): en campo puede fallar más. En esta prueba se valida **la app y el flujo**, no la exactitud del
diagnóstico: si el modelo se equivoca, se anota como insumo para F4, no como falla de la app.

---

## 1. Preparación (una vez, antes de P1)

### 1.1 Qué se necesita
- Teléfono con Android 7.0 (API 24) o superior y cámara trasera (P-06). Con Android 13 o superior también se
  prueba el permiso de notificaciones.
- Cable USB, o depuración inalámbrica en Android 11 o superior.
- En el PC: Docker Desktop y `adb` (Android SDK Platform-Tools, descarga oficial en developer.android.com; no hace
  falta Android Studio).
- Hojas de papa: al menos 3 sanas y 3 con tizón tardío. Si no hay hojas reales, se usan fotos impresas o en una
  pantalla; anota qué material usaste, porque influye en el resultado.
- Opcional: la app Accessibility Scanner en el teléfono, para revisar el contraste (06-ux-ui §Accesibilidad).
  Es una herramienta de prueba: no es dependencia de la app.

### 1.2 Activar la depuración USB
1. En el teléfono: *Ajustes → Acerca del teléfono →* toca 7 veces *Número de compilación*.
2. *Ajustes → Sistema → Opciones de desarrollador →* activa *Depuración USB*.
3. Conecta el cable y acepta la huella del PC. `adb devices` debe listar el teléfono como `device`.

### 1.3 Levantar el sistema local (P1)
Desde la raíz del proyecto, en Git Bash:

```bash
alias dc='docker compose -f deploy/docker-compose.yml --env-file deploy/.env'
```

```bash
dc up -d --build
```

```bash
deploy/scripts/smoke-test.sh
```

La prueba de humo debe terminar sin errores. Las credenciales del ADMIN son las de `deploy/.env`
(`ADMIN_USERNAME` / `ADMIN_PASSWORD`). **No las escribas en este documento ni en las evidencias.**

### 1.4 Compilar e instalar la APK *debug* (P1)
La APK *debug* solo permite HTTP hacia `localhost` y `10.0.2.2` (RNF-SEG-06). Con `adb reverse`, el
`localhost:8080` del teléfono llega al `localhost:8080` del PC: no hay que cambiar código ni abrir la API a la red.

```bash
TizonMovile/scripts/gradlew-docker.sh :composeApp:assembleDebug -Ptizon.apiBaseUrl=http://localhost:8080/api/
```

```bash
adb install -r TizonMovile/composeApp/build/outputs/apk/debug/TizonMovile-1.1.0-debug.apk
```

```bash
adb reverse tcp:8080 tcp:8080
```

`adb reverse` se pierde al desconectar el cable o reiniciar el teléfono: vuelve a ejecutarlo.
Con depuración inalámbrica (`adb pair` + `adb connect`) también funciona y el teléfono queda libre del cable.

> Alternativa sin `adb` (plan B de P-03, Wi-Fi): agrega la IP del PC en
> `TizonMovile/composeApp/src/debug/res/xml/network_security_config.xml` (sin commitearla), usa `API_BIND=0.0.0.0`
> en `deploy/.env`, abre el puerto 8080 en el firewall de Windows solo para redes privadas y compila con
> `-Ptizon.apiBaseUrl=http://<IP-del-PC>:8080/api/`. Requiere la decisión pendiente de P-03.

### 1.5 Usuarios de prueba
| Usuario | Rol | Cómo se crea |
|---|---|---|
| ADMIN de `deploy/.env` | Administrador | Lo crea la API en el primer arranque (RF-03 CA1) |
| `agricultor-prueba` | Agricultor | Registro en la app (DISP-10) |
| `tecnico-prueba` | Técnico agrónomo | Registro en la app y luego el ADMIN le cambia el rol (DISP-70) |

Las contraseñas las eliges tú (8 caracteres o más) y no se anotan aquí.

### 1.6 Ver los logs
```bash
dc logs --since 10m api ia
```

---

## 2. Casos de prueba

Marca cada caso en la columna de la prueba con ✔ (pasa), ✘ (falla) o N/A. Si falla, toma una captura
(`docs/requerimientos/evidencias/prueba-1/DISP-xx.png`) y anota el `request_id` de los logs cuando aplique.

### A. Instalación y primer uso
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-01 | F1-MOB-03 | Instala la APK y ábrela. | La app abre sin cerrarse y muestra la Bienvenida. | ☐ | ☐ |
| DISP-02 | 06 §1, RNF-SEG-12 | Recorre las 3 tarjetas con "Siguiente" y pulsa "Comenzar". | Tarjetas "Detecta el tizón tardío", "Toma buenas capturas" y "Tu privacidad" (las imágenes no se guardan). La Bienvenida no vuelve a salir al reabrir la app. | ☐ | ☐ |
| DISP-03 | F3-MOB-05, 06 §2 | Lee la pantalla de permisos antes de pulsar "Permitir". | "Antes de empezar": cámara (obligatoria), ubicación (opcional) y notificaciones (opcional), cada una con su motivo, **antes** del diálogo del sistema. | ☐ | ☐ |
| DISP-04 | F3-MOB-05, TX-7 | Pulsa "Permitir" en la cámara y **rechaza** en el diálogo del sistema (dos veces si lo vuelve a pedir). | Aparece "Necesitamos la cámara" con "Abrir ajustes". **La app no se cierra.** | ☐ | ☐ |
| DISP-05 | F3-MOB-05 | Desde "Abrir ajustes", concede la cámara y vuelve a la app. | La app detecta el permiso al volver y continúa sin reiniciarse. | ☐ | ☐ |
| DISP-06 | RF-10 CA2 | En ubicación, pulsa "Ahora no". | La app sigue al login. (La ubicación se concede luego en DISP-40.) | ☐ | ☐ |
| DISP-07 | RF-11 CA4 | En Android 13+: en notificaciones pulsa "Permitir" y acepta. | Diálogo del sistema después de la explicación. En Android 12 o inferior: N/A. | ☐ | ☐ |

### B. Cuenta y sesión
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-10 | RF-01 CA1, CA4 | "Crear cuenta" con `agricultor-prueba` y una contraseña de 5 caracteres; luego con una de 8 o más. | Primero: "Usa al menos 8 caracteres" en el campo. Después: entra directo a Escanear. | ☐ | ☐ |
| DISP-11 | RF-01 CA2 | Cierra sesión e intenta registrar otra vez `agricultor-prueba`. | "Ese usuario ya existe. Elige otro". | ☐ | ☐ |
| DISP-12 | RF-02 CA2 | Inicia sesión con una contraseña incorrecta. | "Usuario o contraseña incorrectos" (no dice cuál dato falló). | ☐ | ☐ |
| DISP-13 | F1 (bloqueo) | Equivoca la contraseña 5 veces seguidas en menos de 5 min y haz un 6.º intento con la contraseña **correcta**. | Los 5 primeros: "Usuario o contraseña incorrectos". El 6.º: "Demasiados intentos. Espera 5 minutos e intenta de nuevo". Pasados 5 min, la contraseña correcta funciona. | ☐ | ☐ |
| DISP-14 | RF-02 CA3 | Inicia sesión, cierra la app desde Recientes y ábrela de nuevo. | Entra directo a la pantalla principal, sin pedir login. | ☐ | ☐ |
| DISP-15 | RF-02 CA4 | Perfil → "Cerrar sesión" → confirma en "¿Cerrar sesión?". Reabre la app. | Vuelve al login. Al reabrir sigue en el login (el token se borró). | ☐ | ☐ |
| DISP-16 | RF-02 CA5 | En `deploy/.env` pon `JWT_EXPIRATION_MS=120000` y ejecuta `dc up -d api`. Inicia sesión, espera 3 min y desliza para refrescar el Historial. Al terminar, restaura el valor y repite `dc up -d api`. | "Tu sesión expiró, inicia sesión de nuevo" y la app vuelve al login. | ☐ | ☐ |
| DISP-17 | 06 §3 | P1: `adb reverse --remove tcp:8080` e intenta iniciar sesión. P2: modo avión. Restaura la conexión al terminar. | "Sin conexión. Revisa tu internet". | ☐ | ☐ |

### C. Escaneo
Inicia sesión como `agricultor-prueba` para este bloque.

| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-20 | RF-04 CA1 | Abre Escanear, ve a Historial sin pulsar nada y vuelve. | No aparece ninguna sesión nueva "En curso". La sesión solo se crea al pulsar "Iniciar escaneo". | ☐ | ☐ |
| DISP-21 | RF-10 CA4 | Pulsa "Iniciar escaneo" y escribe un nombre en "Finca o lote" (opcional). | La barra superior muestra el chip de estado, la finca y "Sin ubicación" (se rechazó en DISP-06). | ☐ | ☐ |
| DISP-22 | RF-05, RF-06 | Apunta a una hoja **sana** a 20–30 cm. | Caja alrededor de la hoja con la etiqueta "Sano". La tarjeta inferior muestra el diagnóstico, "Certeza: Alta/Media/Baja" y las hojas analizadas aumentan. | ☐ | ☐ |
| DISP-23 | RF-05 CA4 | Mueve la hoja hacia los bordes de la pantalla. | La caja sigue alineada con la hoja también cerca de los bordes (recorte FILL_CENTER). | ☐ | ☐ |
| DISP-24 | RF-06 CA2 | Apunta a una hoja **con tizón tardío**. | Caja con la etiqueta "Tizón tardío" en el color de severidad; el % afectado de la sesión sube. Si el modelo dice "Sano", anótalo para F4. | ☐ | ☐ |
| DISP-24A | RF-06, D-08 | Apunta a una hoja con **tizón temprano** (manchas oscuras con anillos y halo amarillo). | Caja "Tizón temprano" en ocre con icono de advertencia; el % afectado sube. Si el modelo dice otra cosa, anótalo para F4. | ☐ | ☐ |
| DISP-25 | RF-07 CA3 | Apunta al piso o a una pared. | "No veo una hoja. Acerca la cámara". Ese frame no se cuenta como "Sano". | ☐ | ☐ |
| DISP-25A | D-08 | Apunta a una hoja de **otra planta** (tomate, frijol, una maleza). | Chip "No es una hoja de papa" y el aviso "Esta hoja no parece de papa. Apunta a una hoja de papa"; no hay diagnóstico ni aumentan las hojas analizadas. Anota las plantas que se aceptaron como papa. | ☐ | ☐ |
| DISP-26 | RF-07 CA1, cero fallas silenciosas | Con el escaneo activo: `dc stop ia`. Espera 30 s y ejecuta `dc start ia`. | "Servicio no disponible. Reintentando…" sin bloquear la vista previa; las hojas analizadas **no** aumentan y nunca aparece un diagnóstico por el error. Al volver la IA (puede tardar hasta 1 min en cargar el modelo), el escaneo sigue solo en la misma sesión. | ☐ | ☐ |
| DISP-27 | RF-07 CA3 | P1: `adb reverse --remove tcp:8080` con el escaneo activo y luego `adb reverse tcp:8080 tcp:8080`. P2: modo avión y luego quitarlo. | "Sin conexión" mientras dura el corte; al volver la red sigue la misma sesión con sus contadores. | ☐ | ☐ |
| DISP-28 | F3-MOB-01 | Con el escaneo activo, gira el teléfono a horizontal y de vuelta, 3 veces. | Se conservan la sesión, los contadores y el último resultado. En Historial hay **una sola** sesión "En curso". | ☐ | ☐ |
| DISP-29 | 06 §4 | Pulsa "Pausar" y luego "Continuar". | "En pausa": no se envían frames (las hojas analizadas no cambian). Al continuar, sigue la misma sesión. | ☐ | ☐ |
| DISP-30 | RF-04 CA6, F3-MOB-01 | Con el escaneo activo, pulsa "atrás". Elige "Seguir escaneando"; repite y elige "Descartar". | Diálogo "¿Finalizar escaneo?" con Finalizar / Seguir escaneando / Descartar. "Seguir escaneando" continúa. "Descartar" deja la sesión como "Descartado" en Historial. | ☐ | ☐ |
| DISP-31 | RF-04 CA3 | Inicia un escaneo, pulsa "Pausar" y cierra la sesión desde el servidor (comando abajo). Pulsa "Continuar". | "Ese escaneo ya había terminado. Inicia uno nuevo." y la app queda lista para un escaneo nuevo. | ☐ | ☐ |
| DISP-32 | RF-04 CA4, RF-08 | Escanea al menos 5 hojas y pulsa "Finalizar". | "Resumen del escaneo": severidad con icono + texto + color, % afectado grande, hojas analizadas, hojas con tizón, con tizón temprano, con tizón tardío, hojas que no eran de papa (si hubo), duración, ubicación, finca y recomendaciones con "Fuente: …". Botones "Nuevo escaneo" y "Ver historial". | ☐ | ☐ |
| DISP-33 | RF-04 CA4, RF-10 CA4 | Pulsa "Nuevo escaneo" e "Iniciar escaneo". | Los contadores empiezan en cero y "Finca o lote" propone el último nombre usado. | ☐ | ☐ |

Comando de DISP-31. Cancela en la BD **todas** las sesiones activas, como lo hace la limpieza por inactividad; en P2,
úsalo solo si nadie más está escaneando:

```bash
dc exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
UPDATE scan_sessions SET status = 'CANCELLED', ended_at = now() WHERE status = 'ACTIVE';
SQL
```

### D. Ubicación (F6-MOB-01)
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-40 | RF-10 CA1–CA3 | Concede la ubicación en *Ajustes → Apps → TizonMovile → Permisos*. Con el GPS encendido, cerca de una ventana o al aire libre, inicia y finaliza un escaneo. | La barra muestra "Con ubicación" en menos de 10 s. El resumen muestra las coordenadas con su precisión ("±N m"), cerca de tu posición real. | ☐ | ☐ |
| DISP-41 | RF-10 CA2 | Apaga el GPS del teléfono (o quita el permiso) e inicia un escaneo. | El escaneo funciona igual. Barra y resumen: "Sin ubicación". | ☐ | ☐ |

### E. Historial
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-50 | RF-09 CA3 | Con un usuario nuevo sin escaneos, abre Historial. | "Aún no tienes escaneos" con el botón "Iniciar primer escaneo", que lleva a Escanear. | ☐ | ☐ |
| DISP-51 | RF-09 CA1 | Con `agricultor-prueba`, abre Historial y desliza hacia abajo. | Tarjetas con fecha y hora local (formato "lun 3 nov, 10:15"), finca, chip de severidad (icono + texto), % y estado. El gesto refresca la lista. | ☐ | ☐ |
| DISP-52 | RF-09 CA2 | Toca una sesión. | "Detalle del escaneo" con las estadísticas, la ubicación (o "Sin ubicación") y las recomendaciones. | ☐ | ☐ |
| DISP-53 | 03 §Propiedad | Inicia sesión con otro agricultor. | No ve las sesiones de `agricultor-prueba`. | ☐ | ☐ |
| DISP-54 | RF-09 CA4 | Inicia sesión como `tecnico-prueba` (después de DISP-70). Escribe `agricultor-prueba` en "Filtrar por usuario" y busca; luego "Quitar filtro". | Ve las sesiones de todos con "Usuario: …" en cada tarjeta; el filtro deja solo las de ese usuario. Un usuario sin sesiones muestra "No hay escaneos de …". | ☐ | ☐ |

### F. Alertas y notificaciones (F6-MOB-02)
Regla (03 §Alerta): se crea una alerta al finalizar una sesión con **5 o más hojas analizadas** y un % afectado
igual o mayor a `ALERT_THRESHOLD_PCT` (30 por defecto). Si con el material disponible el modelo no llega al 30 %,
pon `ALERT_THRESHOLD_PCT=0` en `deploy/.env`, ejecuta `dc up -d api`, haz la prueba y restaura el valor. Anota que
usaste el umbral 0.

| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-60 | RF-11 CA1 | Como `agricultor-prueba`, finaliza una sesión que cumpla la regla. | El resumen muestra "Se generó una alerta para tu técnico". | ☐ | ☐ |
| DISP-61 | RF-11 CA2 | Con el permiso de notificaciones concedido y la sesión iniciada, deja la app en segundo plano después de DISP-60. Mide el tiempo. | En 15 min o menos llega "Alertas de tizón tardío — Hay 1 alerta abierta. Toca para revisarla.", que abre la app. En *Ajustes → Notificaciones* existe el canal "Alertas de cultivo". El ahorro de batería puede retrasarla: anota el tiempo real. | ☐ | ☐ |
| DISP-62 | RF-11 CA3 | Como `tecnico-prueba`, abre Alertas y pulsa "Marcar como atendida". | La alerta muestra la severidad, "Agricultor: agricultor-prueba", la finca y la fecha; después pasa a "Atendida por tecnico-prueba". El contador de la pestaña baja. | ☐ | ☐ |
| DISP-63 | RF-11 CA3 | Como `agricultor-prueba`, abre Alertas. | Ve su alerta, sin el botón "Marcar como atendida". | ☐ | ☐ |

### G. Perfil y administración (F6-MOB-04)
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-70 | RF-03 CA2 | Registra `tecnico-prueba`. Como ADMIN: Perfil → "Usuarios" → cambia su rol a "Técnico agrónomo". | El cambio se guarda; al iniciar sesión como `tecnico-prueba`, el Perfil muestra "Técnico agrónomo". | ☐ | ☐ |
| DISP-71 | RF-02 CA2 | Como ADMIN, en "Usuarios" apaga "Habilitado" de `agricultor-prueba`. Cierra sesión e intenta entrar como `agricultor-prueba`. Vuelve a habilitarlo al terminar. | "Usuario o contraseña incorrectos". | ☐ | ☐ |
| DISP-72 | RF-02 CA5 | Con `agricultor-prueba` en el teléfono, deshabilítalo con el primer comando de abajo y refresca el Historial. Al terminar, ejecuta el segundo comando. | "Tu sesión expiró, inicia sesión de nuevo" y la app vuelve al login (el token sigue vigente, pero la API rechaza a un usuario deshabilitado). | ☐ | ☐ |
| DISP-73 | RF-03 CA2 | Como agricultor o técnico, abre el Perfil. | No aparece "Usuarios". | ☐ | ☐ |
| DISP-74 | 06 §8 | Abre el Perfil. | Nombre, usuario, rol, "Versión de la app: 1.1.0", "Modelo: …" (el `model_version` de `deploy/models/model_meta.json`) y "Cómo tomar buenas capturas". | ☐ | ☐ |

Comandos de DISP-72. Con un solo teléfono, el ADMIN no puede actuar mientras el agricultor tiene la sesión
abierta, así que el cambio se hace en la BD:

```bash
dc exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
UPDATE app_users SET enabled = false WHERE username = 'agricultor-prueba';
SQL
```

```bash
dc exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
UPDATE app_users SET enabled = true WHERE username = 'agricultor-prueba';
SQL
```

### H. Accesibilidad y uso en campo
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-80 | 06 §Accesibilidad | Sube el tamaño de fuente del sistema al máximo y recorre Login, Escanear, Resumen, Historial y Perfil. | Ningún texto se corta. "Finalizar" sigue visible en Escanear. | ☐ | ☐ |
| DISP-81 | 06 §Accesibilidad | Activa TalkBack y recorre Escanear durante un escaneo. | Cada botón e icono se anuncia con su nombre; los cambios del chip de estado se anuncian solos. | ☐ | ☐ |
| DISP-82 | 06 §Accesibilidad | (Opcional) Accessibility Scanner en Login, Escanear, Resumen e Historial. | Sin avisos de contraste de texto. | ☐ | ☐ |
| DISP-83 | 06 §Sistema visual | Activa el tema oscuro del sistema. | Todo legible, con la paleta verde y tierra. | ☐ | ☐ |
| DISP-84 | 06 §Contexto de uso | P2: escanea al aire libre con sol directo, sosteniendo el teléfono con una mano. | Chip y tarjeta legibles; "Iniciar escaneo" y "Finalizar" se alcanzan con el pulgar. | N/A | ☐ |

### I. Rendimiento (RNF-REN-01, RNF-REN-02)
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-90 | RNF-REN-01, 02 | Después de escanear 20 hojas o más, ejecuta la consulta de abajo. | Anota el p95 de `latency_ms` (Spring + IA) y de `inference_ms`. Meta en la EC2: `inference_ms` p95 < 1.500 ms. En P1 es informativo (PC local). | ☐ | ☐ |
| DISP-91 | RNF-REN-01 | Con un cronómetro, 10 veces: apunta a una hoja nueva y mide hasta que aparece su diagnóstico. | Anota el máximo. Meta: ≤ 5 s (objetivo interno ≤ 2 s). Incluye la pausa entre frames (`frame_interval_ms`). | ☐ | ☐ |

```bash
dc exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
SELECT count(*) AS frames,
       percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms)   AS p95_latency_ms,
       percentile_cont(0.95) WITHIN GROUP (ORDER BY inference_ms) AS p95_inference_ms
FROM scan_results
WHERE analyzed_at > now() - interval '3 hours';
SQL
```

### J. Trazabilidad y privacidad
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-95 | RF-07 CA4 | Toma un `request_id` de `dc logs --since 5m api` y búscalo con `dc logs --since 5m ia \| grep <request_id>`. | El mismo `request_id` aparece en la API y en la IA. | ☐ | ☐ |
| DISP-96 | RNF-SEG-12, DoD | `dc logs api ia \| grep -c -i -e "bearer" -e "frame_b64"` | `0`: los logs no tienen tokens ni imágenes. | ☐ | ☐ |

### K. APK *release* (F5-MOB-01)
| ID | Requisito | Pasos | Resultado esperado | P1 | P2 |
|---|---|---|---|---|---|
| DISP-99 | F5-MOB-01 | P1 (al final): desinstala la *debug* (la firma es otra) e instala la *release* de prueba (URL `https://ejemplo.invalid/api/`). Recorre Bienvenida → Permisos → Login e intenta entrar. P2: la *release* real contra la EC2 en todos los casos. | P1: abre y navega sin cerrarse (R8 no rompió el arranque ni la navegación) y muestra "Sin conexión. Revisa tu internet". P2: todos los casos pasan con la *release*. | ☐ | ☐ |

---

## 3. Prueba 2 (5-nov) contra la EC2
- APK: la *release* firmada con el keystore real (README de TizonMovile §Firma), compilada con
  `-Ptizon.apiBaseUrl=https://<PUBLIC_HOST>/api/`. Antes de instalarla, compara el SHA-256 del certificado con el
  documentado.
- Se repiten todos los casos con la columna P2. Los cortes de red se hacen con modo avión y con datos móviles; los
  comandos `dc` se ejecutan por SSH en la EC2, en `/opt/tizon` (runbook de `deploy/README.md`).
- Si la EC2 no está lista (P-03), P2 se repite en local como P1 y la limitación se anota en `10-pendientes.md`.

---

## 4. Registro de hallazgos
Cada ✘ se agrega a la tabla *Bloqueos y verificaciones pendientes* de `10-pendientes.md`:

| Fecha | Tarea | Qué falta | Qué se necesita |
|---|---|---|---|
| (fecha) | DISP-xx (y la tarea del backlog) | Qué se esperaba, qué pasó y la evidencia (captura o `request_id`) | Corrección en la app, la API o la IA, o un insumo pendiente |

Cuando todos los casos de una tarea pasan, esa tarea se marca como terminada en `07-backlog.md`:

| Tarea | Casos |
|---|---|
| F1-MOB-03 | DISP-01 |
| F3-MOB-01 | DISP-28, DISP-30 |
| F3-MOB-05 | DISP-02 a DISP-05 |
| F6-MOB-01 | DISP-40, DISP-41 |
| F6-MOB-02 | DISP-60 a DISP-63 |
| F4-ALL-06 (en campo) | DISP-24A, DISP-25A |
| F5-ALL-02 | Todos los casos en P2 |
