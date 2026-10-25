# Previews de UX/UI — TizonMovile (F3-MOB-00)

Capturas generadas automáticamente de todos los `@Preview` con Roborazzi + Robolectric
(`scripts/gradlew-docker.sh :composeApp:recordRoborazziDebug`). Cada estado en **claro**, **oscuro** y
**fuente al 200 %**, a 360×780 dp. Aprobadas por el usuario el 23-oct (hito de F3-MOB-00).
Total: 108 capturas.

Decisiones de diseño:
- Paleta verde y tierra (decisión del usuario; tokens en 06-ux-ui): verde hoja como color principal; arena, ocre,
  musgo, terracota y óxido para acentos y severidad. Todo texto ≥ 4,5:1 (WCAG AA) en claro y oscuro, verificado por cálculo.
- Severidad siempre con icono + texto + color. Certeza en palabras (Alta/Media/Baja), sin porcentajes en bruto.
- Escanear: botón principal de 72 dp siempre visible abajo; la tarjeta de resultado se pliega para ver la cámara.
- La vista de la cámara aparece como un fondo oscuro (en los previews no hay cámara real).
- Las recomendaciones muestran solo el texto genérico permitido hasta resolver P-05.
- Historial: TECNICO y ADMIN ven el usuario de cada sesión y un filtro por usuario (RF-09 CA4).
- Tizón temprano en ocre con icono de advertencia (el tardío sigue en óxido). Si la hoja es de otra planta,
  el chip dice "No es una hoja de papa" y no hay diagnóstico; el resumen separa tizón temprano y tardío.

## 1. Bienvenida

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| WelcomeCapturas | [ver](welcome/WelcomeCapturas__claro.png) | [ver](welcome/WelcomeCapturas__oscuro.png) | [ver](welcome/WelcomeCapturas__fuente-200.png) |
| WelcomePrivacidad | [ver](welcome/WelcomePrivacidad__claro.png) | [ver](welcome/WelcomePrivacidad__oscuro.png) | [ver](welcome/WelcomePrivacidad__fuente-200.png) |
| WelcomeQueHace | [ver](welcome/WelcomeQueHace__claro.png) | [ver](welcome/WelcomeQueHace__oscuro.png) | [ver](welcome/WelcomeQueHace__fuente-200.png) |

## 2. Permisos

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| PermisosCamaraConcedida | [ver](permissions/PermisosCamaraConcedida__claro.png) | [ver](permissions/PermisosCamaraConcedida__oscuro.png) | [ver](permissions/PermisosCamaraConcedida__fuente-200.png) |
| PermisosCamaraRechazada | [ver](permissions/PermisosCamaraRechazada__claro.png) | [ver](permissions/PermisosCamaraRechazada__oscuro.png) | [ver](permissions/PermisosCamaraRechazada__fuente-200.png) |
| PermisosInicial | [ver](permissions/PermisosInicial__claro.png) | [ver](permissions/PermisosInicial__oscuro.png) | [ver](permissions/PermisosInicial__fuente-200.png) |

## 3. Login y registro

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| LoginCargando | [ver](auth/LoginCargando__claro.png) | [ver](auth/LoginCargando__oscuro.png) | [ver](auth/LoginCargando__fuente-200.png) |
| LoginCredencialesIncorrectas | [ver](auth/LoginCredencialesIncorrectas__claro.png) | [ver](auth/LoginCredencialesIncorrectas__oscuro.png) | [ver](auth/LoginCredencialesIncorrectas__fuente-200.png) |
| LoginSinConexion | [ver](auth/LoginSinConexion__claro.png) | [ver](auth/LoginSinConexion__oscuro.png) | [ver](auth/LoginSinConexion__fuente-200.png) |
| LoginVacio | [ver](auth/LoginVacio__claro.png) | [ver](auth/LoginVacio__oscuro.png) | [ver](auth/LoginVacio__fuente-200.png) |
| RegistroConErrores | [ver](auth/RegistroConErrores__claro.png) | [ver](auth/RegistroConErrores__oscuro.png) | [ver](auth/RegistroConErrores__fuente-200.png) |

## 4. Escanear

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| EscanearAnalizandoConCajas | [ver](scan/EscanearAnalizandoConCajas__claro.png) | [ver](scan/EscanearAnalizandoConCajas__oscuro.png) | [ver](scan/EscanearAnalizandoConCajas__fuente-200.png) |
| EscanearDialogoSalida | [ver](scan/EscanearDialogoSalida__claro.png) | [ver](scan/EscanearDialogoSalida__oscuro.png) | [ver](scan/EscanearDialogoSalida__fuente-200.png) |
| EscanearListo | [ver](scan/EscanearListo__claro.png) | [ver](scan/EscanearListo__oscuro.png) | [ver](scan/EscanearListo__fuente-200.png) |
| EscanearNoEsPapa | [ver](scan/EscanearNoEsPapa__claro.png) | [ver](scan/EscanearNoEsPapa__oscuro.png) | [ver](scan/EscanearNoEsPapa__fuente-200.png) |
| EscanearServicioNoDisponible | [ver](scan/EscanearServicioNoDisponible__claro.png) | [ver](scan/EscanearServicioNoDisponible__oscuro.png) | [ver](scan/EscanearServicioNoDisponible__fuente-200.png) |
| EscanearSinConexion | [ver](scan/EscanearSinConexion__claro.png) | [ver](scan/EscanearSinConexion__oscuro.png) | [ver](scan/EscanearSinConexion__fuente-200.png) |
| EscanearSinHoja | [ver](scan/EscanearSinHoja__claro.png) | [ver](scan/EscanearSinHoja__oscuro.png) | [ver](scan/EscanearSinHoja__fuente-200.png) |
| EscanearTarjetaPlegada | [ver](scan/EscanearTarjetaPlegada__claro.png) | [ver](scan/EscanearTarjetaPlegada__oscuro.png) | [ver](scan/EscanearTarjetaPlegada__fuente-200.png) |
| EscanearTizonTemprano | [ver](scan/EscanearTizonTemprano__claro.png) | [ver](scan/EscanearTizonTemprano__oscuro.png) | [ver](scan/EscanearTizonTemprano__fuente-200.png) |

## 5. Resumen y detalle

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| DetalleSesion | [ver](summary/DetalleSesion__claro.png) | [ver](summary/DetalleSesion__oscuro.png) | [ver](summary/DetalleSesion__fuente-200.png) |
| ResumenCriticoConAlerta | [ver](summary/ResumenCriticoConAlerta__claro.png) | [ver](summary/ResumenCriticoConAlerta__oscuro.png) | [ver](summary/ResumenCriticoConAlerta__fuente-200.png) |
| ResumenSano | [ver](summary/ResumenSano__claro.png) | [ver](summary/ResumenSano__oscuro.png) | [ver](summary/ResumenSano__fuente-200.png) |

## 6. Historial

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| HistorialCargando | [ver](history/HistorialCargando__claro.png) | [ver](history/HistorialCargando__oscuro.png) | [ver](history/HistorialCargando__fuente-200.png) |
| HistorialConDatos | [ver](history/HistorialConDatos__claro.png) | [ver](history/HistorialConDatos__oscuro.png) | [ver](history/HistorialConDatos__fuente-200.png) |
| HistorialError | [ver](history/HistorialError__claro.png) | [ver](history/HistorialError__oscuro.png) | [ver](history/HistorialError__fuente-200.png) |
| HistorialFiltroSinResultados | [ver](history/HistorialFiltroSinResultados__claro.png) | [ver](history/HistorialFiltroSinResultados__oscuro.png) | [ver](history/HistorialFiltroSinResultados__fuente-200.png) |
| HistorialTecnico | [ver](history/HistorialTecnico__claro.png) | [ver](history/HistorialTecnico__oscuro.png) | [ver](history/HistorialTecnico__fuente-200.png) |
| HistorialVacio | [ver](history/HistorialVacio__claro.png) | [ver](history/HistorialVacio__oscuro.png) | [ver](history/HistorialVacio__fuente-200.png) |

## 7. Alertas

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| AlertasAgricultor | [ver](alerts/AlertasAgricultor__claro.png) | [ver](alerts/AlertasAgricultor__oscuro.png) | [ver](alerts/AlertasAgricultor__fuente-200.png) |
| AlertasTecnico | [ver](alerts/AlertasTecnico__claro.png) | [ver](alerts/AlertasTecnico__oscuro.png) | [ver](alerts/AlertasTecnico__fuente-200.png) |
| AlertasVacias | [ver](alerts/AlertasVacias__claro.png) | [ver](alerts/AlertasVacias__oscuro.png) | [ver](alerts/AlertasVacias__fuente-200.png) |

## 8–9. Perfil y usuarios

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| PerfilAdminConfirmarSalida | [ver](profile/PerfilAdminConfirmarSalida__claro.png) | [ver](profile/PerfilAdminConfirmarSalida__oscuro.png) | [ver](profile/PerfilAdminConfirmarSalida__fuente-200.png) |
| PerfilAgricultor | [ver](profile/PerfilAgricultor__claro.png) | [ver](profile/PerfilAgricultor__oscuro.png) | [ver](profile/PerfilAgricultor__fuente-200.png) |
| UsuariosAdmin | [ver](profile/UsuariosAdmin__claro.png) | [ver](profile/UsuariosAdmin__oscuro.png) | [ver](profile/UsuariosAdmin__fuente-200.png) |

## Navegación

| Estado | Claro | Oscuro | Fuente 200 % |
|---|---|---|---|
| BarraInferiorConAlertas | [ver](navigation/BarraInferiorConAlertas__claro.png) | [ver](navigation/BarraInferiorConAlertas__oscuro.png) | [ver](navigation/BarraInferiorConAlertas__fuente-200.png) |

