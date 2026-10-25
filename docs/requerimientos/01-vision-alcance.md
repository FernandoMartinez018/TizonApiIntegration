# 01 · Visión y alcance

## Objetivo
Entregar el 8-nov-2026 una versión **segura, consistente y medible** del sistema de detección de tizón tardío. Para eso se corrigen las falencias detectadas y se agregan gestión de usuarios, ubicación GPS y alertas. Además, se demuestran con números las métricas comprometidas en el documento académico.

## Actores
| Actor | Descripción | Rol en el sistema |
|---|---|---|
| Agricultor | Escanea su cultivo con el celular en campo | `AGRICULTOR` |
| Técnico agrónomo | Revisa sesiones y alertas de varios agricultores | `TECNICO` |
| Administrador | Gestiona usuarios y opera el sistema | `ADMIN` |

## Flujo principal (objetivo)
1. El usuario se registra o inicia sesión.
2. Inicia un escaneo: la app pide la ubicación (opcional) y crea una sesión.
3. Apunta a las hojas. La app envía frames periódicamente.
4. La IA clasifica cada región de hoja como `Sano` o `Tizon Tardio`.
5. La app muestra el resultado en vivo: cajas, diagnóstico y severidad.
6. El usuario finaliza el escaneo y recibe un resumen con su severidad y recomendaciones.
7. Si la severidad supera el umbral, se genera una **alerta**, que ven el propio usuario y los técnicos.
8. El historial muestra las sesiones con fecha, ubicación y severidad, y opcionalmente un mapa OSM.

## Alcance
| Incluido (M/S) | Condicional (C) | Fuera del alcance (W) |
|---|---|---|
| Correcciones de seguridad, flujo, modelo e infraestructura | Mapa OpenStreetMap de sesiones | Identificación de malezas por especie |
| Contrato API único | Grad-CAM (localizar la lesión) | Otras enfermedades de la papa |
| Tizón temprano y aviso "No es una hoja de papa" (D-08) | | |
| Gestión de usuarios con roles | Pruebas de UI instrumentadas | Drones y cámaras fijas en campo |
| GPS open source (AOSP) por sesión | | Carga de fotos desde la galería |
| Alertas (backend + notificación local) | | iOS y escritorio |
| Rediseño UX/UI de la app | | Publicación en Play Store |
| Evaluación del modelo con métricas del PDF | | Guardado de imágenes para reentrenar |
| Despliegue en una EC2 nueva con docker-compose | | Push con FCM |
| APK *release* firmada para instalación manual | | |

## Decisiones vigentes
| ID | Decisión |
|---|---|
| D-01 | Solo Android. Se retiran iOS, JVM, `:server` y el código de plantilla de `:shared`. |
| D-02 | La IA corre como contenedor permanente junto a Spring en la misma EC2, en una red Docker interna. Se retiran Lambda, API Gateway y S3. |
| D-03 | GPS, mapas y notificaciones solo con componentes open source u on-premise. Si alguno no funciona en el dispositivo de prueba, se descarta sin bloquear la entrega. |
| D-04 | Clases del modelo: `Sano` y `Tizon Tardio`. **Reemplazada por D-08 el 24-oct.** |
| D-05 | La APK se distribuye manualmente y se firma con un *keystore* propio fuera de git. |
| D-06 | Las llaves `.pem` antiguas se purgaron del historial local de `TizonDetectionModel` y `TizonApiIntegration` (hecho el 23-oct). El usuario hará *force push*. |
| D-07 | HTTPS en la EC2 con Caddy y un certificado de Let's Encrypt sobre un nombre gratuito `<ip-con-guiones>.sslip.io`, salvo que aparezca un dominio propio. |
| D-08 | (Usuario, 24-oct) El sistema detecta **tizón temprano** además del tardío y **discrimina si la hoja es de papa**: si no lo es, no analiza el tizón. Meta de acierto ≥ 90 %. Modelo reentrenado con PlantVillage (el original se entrenó con PlantVillage de Kaggle y ya no se tienen sus imágenes ni su notebook). |
