# ═══════════════════════════════════════════════════════════════════════════
# Dockerfile — Tizón Tardío Spring Boot API
#
# Estrategia multistage:
#   Stage 1 "build"   → Maven + JDK 21 compila el código y genera el JAR
#   Stage 2 "runtime" → JRE 21 Alpine ejecuta el JAR (imagen final ~200 MB)
#
# Por qué multistage:
#   - El stage "build" pesa ~600 MB (Maven + JDK completo + dependencias)
#   - La imagen final solo lleva lo necesario para EJECUTAR (no compilar)
#   - El usuario descarga ~200 MB en lugar de ~600 MB
#   - El código fuente y Maven NO quedan en la imagen de producción
#
# Uso en desarrollo local:
#   docker build -t tizon-spring .
#   docker run -p 8080:8080 --env-file .env tizon-spring
#
# Uso con docker-compose:
#   docker-compose up (ya está configurado en docker-compose.yml)
#
# Uso en AWS EC2:
#   Ver fase 3 de la guía de despliegue en el README.md
# ═══════════════════════════════════════════════════════════════════════════


# ───────────────────────────────────────────────────────────────────────────
# STAGE 1 — BUILD
#
# Usa la imagen oficial de Maven con JDK 21 (Eclipse Temurin).
# Eclipse Temurin es la distribución OpenJDK más popular para contenedores.
# Esta imagen NO llega a producción — solo sirve para compilar.
# ───────────────────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21 AS build

WORKDIR /workspace

# ── Truco de caché de capas Docker ──────────────────────────────────────────
# Se copia el pom.xml PRIMERO y se descargan las dependencias ANTES
# de copiar el código fuente.
#
# Por qué importa:
#   - Docker cachea cada instrucción como una "capa"
#   - Si el pom.xml no cambia, Docker reutiliza la capa de dependencias
#   - Solo reconstruye desde donde algo cambió
#   - El resultado: si solo cambias código Java (no el pom.xml),
#     Maven no re-descarga las dependencias (ahorra 2-3 minutos)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
# -B = modo batch (sin colores, para logs de CI/CD)
# -q = quiet (menos output)
# go-offline = descarga TODAS las dependencias al cache local de Maven

# Ahora sí copiamos el código fuente
COPY src/ ./src/

# Compilar y empaquetar en un fat JAR (incluye todas las dependencias)
# -DskipTests = no correr tests aquí (los tests van en CI/CD, no en el build de imagen)
RUN mvn clean package -DskipTests -B -q

# ── Extraer capas del fat JAR (Spring Boot Layered JARs) ──────────────────
# Spring Boot 2.3+ soporta JARs en capas ordenadas por frecuencia de cambio:
#   1. dependencies        → librerías externas (cambian poco — Spring, Hibernate, etc.)
#   2. spring-boot-loader  → clases del loader de Spring Boot
#   3. snapshot-dependencies → dependencias snapshot
#   4. application         → tu código (cambia con cada commit)
#
# Por qué es mejor que copiar el fat JAR completo:
#   - Docker reutiliza las capas 1-3 si no cambiaron
#   - Solo reconstruye la capa 4 (application) cuando cambias código
#   - Reduce el tiempo de push a ECR en builds sucesivos
#   - Reduce el tiempo de pull en EC2 cuando haces actualizaciones
RUN java -Djarmode=layertools \
    -jar target/*.jar extract \
    --destination target/extracted



# ───────────────────────────────────────────────────────────────────────────
# STAGE 2 — RUNTIME
#
# Imagen final que se despliega en producción.
# JRE (Java Runtime Environment) = solo lo necesario para EJECUTAR Java.
# Alpine = distribución Linux mínima (~5 MB base), perfecta para contenedores.
# eclipse-temurin:21-jre-alpine ≈ 85 MB base (vs ~600 MB del stage de build)
# ───────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# ── Instalar utilidades mínimas necesarias ────────────────────────────────
RUN apk add --no-cache \
    curl \
    tzdata
# curl    → para el HEALTHCHECK de Docker y diagnósticos
# tzdata  → datos de zona horaria (Colombia = America/Bogota = UTC-5)

# ── Configurar zona horaria ───────────────────────────────────────────────
# Sin esto, los timestamps en los logs y en la base de datos
# aparecerán en UTC en lugar de hora de Colombia
ENV TZ=America/Bogota
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# ── Crear usuario sin privilegios ─────────────────────────────────────────
# Por seguridad, NUNCA correr aplicaciones como root en producción.
# Si hay una vulnerabilidad, el atacante obtendría acceso de root al contenedor.
# Con un usuario limitado, el daño potencial es mucho menor.
RUN addgroup -S tizon && adduser -S tizon -G tizon
USER tizon
# Desde aquí todo corre como el usuario "tizon" (sin privilegios)

# ── Copiar las capas del JAR en orden de menor a mayor cambio ─────────────
# Las capas que cambian MENOS van primero (mejor uso del caché de Docker).
# Las capas que cambian MÁS van al final (solo invalidan su propia capa).
#
# Orden:
#   1. dependencies         → Spring, Hibernate, Ktor, etc. (cambian con versiones)
#   2. spring-boot-loader   → clases del bootloader (cambia con versión Spring Boot)
#   3. snapshot-dependencies → dependencias -SNAPSHOT (cambian poco)
#   4. application          → TU código Java (cambia con cada commit)
COPY --from=build /workspace/target/extracted/dependencies/           ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/    ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/           ./

# ── Variables de entorno ──────────────────────────────────────────────────
# Los secretos (DB_PASS, JWT_SECRET, INTERNAL_API_TOKEN) y las URLs de ambiente
# NO van en la imagen: se inyectan al correr el contenedor (deploy/.env).
# Sin ellos la API no arranca. Ver README.md → Configuración.
ENV PORT=8080

# Variables de optimización de la JVM para contenedores:
# UseContainerSupport → la JVM detecta los límites de memoria del contenedor
#                       en lugar de usar la RAM total del host
# MaxRAMPercentage    → usar máximo el 75% de la RAM asignada al contenedor
#                       (deja margen para el sistema operativo)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# ── Exponer el puerto ─────────────────────────────────────────────────────
# EXPOSE es solo documentación — no abre el puerto.
# El puerto real se mapea con -p 8080:8080 en docker run.
EXPOSE 8080

# ── Health check de Docker ────────────────────────────────────────────────
# Docker verifica periódicamente que el contenedor esté sano.
# Si falla 3 veces seguidas, Docker marca el contenedor como "unhealthy".
# ECS/docker-compose pueden reiniciar automáticamente contenedores unhealthy.
#
# Configuración:
#   --interval=30s    → verificar cada 30 segundos
#   --timeout=10s     → si no responde en 10s, cuenta como fallo
#   --start-period=45s → dar 45s de gracia al arrancar (Spring Boot tarda ~30s)
#   --retries=3       → marcar unhealthy después de 3 fallos consecutivos
HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# ── Comando de arranque ───────────────────────────────────────────────────
# Usa el JarLauncher de Spring Boot en lugar de java -jar tizon-api.jar
# porque estamos usando el formato de JAR en capas (Layered JARs).
#
# $JAVA_OPTS se expande para incluir las opciones de JVM definidas arriba.
# Se pueden agregar opciones adicionales al correr el contenedor:
#   docker run -e JAVA_OPTS="-XX:+UseContainerSupport -Xmx512m" tizon-spring
#
# -Djava.security.egd=file:/dev/./urandom
#   → Acelera la generación de números aleatorios en Linux
#   → Sin esto, la JVM puede bloquear en sistemas con poca entropía
#   → Especialmente importante para generación de tokens JWT
ENTRYPOINT ["sh", "-c", \
    "java $JAVA_OPTS \
     -Djava.security.egd=file:/dev/./urandom \
     org.springframework.boot.loader.launch.JarLauncher"]