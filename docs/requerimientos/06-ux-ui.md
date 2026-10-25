# 06 · UX/UI — App Android

## Contexto de uso (lo que guía cada decisión)
- Exteriores con sol directo: se necesita alto contraste.
- Una mano ocupada (sostiene la hoja): los botones van grandes y en la zona del pulgar.
- Conectividad intermitente: los estados de red deben ser claros.
- Usuarios con distinta familiaridad digital: lenguaje simple, sin tecnicismos ni porcentajes de confianza en bruto.

## Sistema visual
**Base:** Material 3 con tema propio en `ui/theme/` (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`). **No se usan colores fijos fuera del tema.**

**Tokens** (deben pasar WCAG AA de 4.5:1 para texto normal). **Paleta verde y tierra** (decisión del usuario
del 23-oct): verde hoja, arena, ocre, musgo, terracota y óxido; sin rojos puros, rosados ni azules. Los valores
vigentes están en `TizonMovile/.../ui/theme/Color.kt` (claro y oscuro).

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `primary` | `#2E6B30` (verde hoja) | `#8CD48A` | Acciones principales |
| `onPrimary` | `#FFFFFF` | `#0A3A0E` | |
| `secondary` | `#7A5C2E` (tierra) | `#E2C08D` (arena) | Acentos y botón "Finalizar" |
| `tertiary` | `#5B6B2E` (musgo) | `#C2CF94` | Acentos secundarios |
| `error` | `#9C3B1F` (terracota oscura) | `#F2A68A` | Errores y "Descartar" |
| `background` / `surface` | `#FAFAF5` / `#FFFFFF` | `#121411` / `#1C1F1A` | |
| `severityNone` | `#2D7231` (verde) | `#7FCB83` | Sano o sin afectación |
| `severityLow` | `#7A6500` (ocre) | `#D6BE52` | Baja |
| `severityWarning` | `#A04A16` (terracota) | `#EBA26F` | Advertencia |
| `severityCritical` | `#8E2A1C` (óxido) | `#E58A72` | Crítica |
| `outline` | `#73796E` | `#8D9387` | Bordes |

- **Tipografía:** la del sistema (Roboto). Cuerpo de al menos 16 sp, títulos de 22 a 28 sp, y el número de % afectado en 40 sp o más en la tarjeta de resultado.
- **Espaciado:** escala de 4 dp (4, 8, 12, 16, 24, 32).
- **Táctil:** objetivos de al menos 48 dp. El botón principal de escaneo mide 72 dp o más.
- **Severidad:** siempre se muestra con **icono + texto + color** (nunca solo color). Iconos: ✔ sano, ⚠ advertencia, ⛔ crítico, usando Material Icons.
- **Modo oscuro:** soportado con los tokens equivalentes. Por defecto se sigue la configuración del sistema.

## Navegación
Navigation Compose. Si no hay sesión iniciada: Bienvenida → Permisos → Login/Registro. Con sesión iniciada, una barra inferior con 4 destinos:

| Pestaña | Pantalla | Rol |
|---|---|---|
| Escanear | Cámara | Todos |
| Historial | Lista y detalle de sesiones | Todos |
| Alertas | Lista de alertas, con contador en la pestaña | Todos (TECNICO puede marcar como atendidas) |
| Perfil | Datos, cerrar sesión, "Acerca de" (versión de app y modelo) | Todos. ADMIN ve además "Usuarios". |

El botón "atrás" se comporta de forma estándar. Durante un escaneo activo aplica el diálogo de RF-04 CA6.

## Pantallas y estados

### 1. Bienvenida (solo la primera vez)
- 3 tarjetas deslizables: qué hace la app, cómo tomar buenas capturas (luz, distancia de 20 a 30 cm, una hoja a la vez) y la privacidad (las imágenes no se guardan).
- Botones "Siguiente" y "Comenzar".

### 2. Permisos (antes de pedirlos al sistema)
- Explica por qué se necesita la cámara (obligatoria), la ubicación (opcional) y las notificaciones (opcional).
- Si se rechaza la cámara, aparece una pantalla con el botón "Abrir ajustes". **La app no se cierra.**

### 3. Login / Registro
- Campos grandes con mostrar/ocultar contraseña y validación en línea.
- Mensajes según el error:
  - 401: "Usuario o contraseña incorrectos".
  - Sin red: "Sin conexión. Revisa tu internet".
  - 5xx: "El servicio no está disponible, intenta más tarde".
- Enlace "Crear cuenta" ↔ "Ya tengo cuenta".

### 4. Escanear (cámara)
- Vista previa a pantalla completa, con un **marco guía** central y el texto "Ubica una hoja dentro del marco".
- **Barra superior**: chip de estado (Listo / Analizando… / No veo una hoja / No es una hoja de papa / Servicio no disponible / Sin conexión), finca o lote, e indicador de GPS (con o sin ubicación).
- **Cajas:** borde de 3 dp del color de la clase (sano: verde; tizón temprano: ocre; tizón tardío: óxido), con la etiqueta "Sano", "Tizón temprano" o "Tizón tardío" en un fondo opaco para que sea legible sobre la imagen.
- **Tarjeta inferior** (*bottom sheet* plegable):
  - Diagnóstico del último frame en texto grande.
  - Certeza en lenguaje simple: ≥ 0,85 "Alta", ≥ 0,6 "Media", el resto "Baja".
  - % afectado de la sesión, barra de severidad y "N hojas analizadas".
- **Botón principal**:
  - Sin escaneo activo: "Iniciar escaneo".
  - Con escaneo activo: "Finalizar".
  - Debajo, la acción secundaria "Pausar".
- Opcional, solo si el dispositivo lo soporta: vibración corta al detectar tizón por primera vez en la sesión.
- **Estados vacíos y de error:** ver RF-07 CA3. Los reintentos se muestran sin bloquear la vista previa.

### 5. Resumen de sesión (al finalizar)
- Encabezado con la severidad (icono + texto + color) y el % afectado grande.
- Datos: hojas analizadas, afectadas, con tizón temprano, con tizón tardío, hojas que no eran de papa (si hubo), duración, ubicación y finca.
- Recomendaciones (del backend) con su fuente, bajo el título "Recomendaciones para tizón temprano" o "… tardío", según la enfermedad que predominó.
- Si se generó una alerta: banner "Se generó una alerta para tu técnico".
- Botones "Nuevo escaneo" y "Ver historial".

### 6. Historial
- Lista de tarjetas: fecha (formato "lun 3 nov, 10:15"), finca, chip de severidad y %.
- *Pull-to-refresh*, paginación infinita y estado vacío.
- **Detalle:** el mismo contenido del Resumen y un mini-mapa (si RF-12 está implementado).

### 7. Alertas
- Lista con la severidad, el usuario (para TECNICO), la finca y la fecha.
- Botón "Marcar como atendida" (TECNICO o ADMIN).

### 8. Perfil
- Nombre, usuario y rol.
- Cerrar sesión (con confirmación).
- Versión de la app y `model_version`.
- Enlace a "Cómo tomar buenas capturas".

### 9. Usuarios (ADMIN)
- Lista de usuarios con interruptor de habilitado y selector de rol.

## Textos (microcopy)
- Español neutro de Colombia, tuteo, frases cortas y verbos en imperativo. Todos los textos van en `strings.xml` o Compose Resources, **nunca escritos en el código**.
- Etiquetas: "Sano", "Tizón temprano", "Tizón tardío", "No veo una hoja", "No es una hoja de papa", "Analizando…", "Servicio no disponible", "Sin conexión", "Iniciar escaneo", "Finalizar", "Pausar".
- No se usan emojis como única señal (el ReportScreen actual usa 📋 ⏹ ▶). Se reemplazan por Material Icons con `contentDescription`.

## Accesibilidad (criterios de aceptación)
- Todo elemento interactivo tiene `contentDescription` o una etiqueta semántica.
- Los textos escalan con el tamaño de fuente del sistema hasta 200 % sin cortes (se verifica con *preview* a `fontScale = 2f`).
- El contraste cumple AA (se verifica con Accessibility Scanner en la prueba del dispositivo).
- La severidad nunca depende solo del color.

## Entregables de diseño
1. **Antes de implementar:** *previews* de Compose (`@Preview`) por pantalla y estado: claro y oscuro, `fontScale` 1 y 2, y estados vacío, cargando, error y con datos. Se capturan en `docs/ux/` como PNG (por ejemplo con Paparazzi o Roborazzi, o captura manual) para que el usuario los apruebe (hito 27-oct).
2. **Implementación:** con los *previews* aprobados.
