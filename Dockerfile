# Dockerfile Multi-Stage para UMAS Core Service
# Java 21 + Spring Boot + Gradle (usa gradlew si existe)

# -------- Stage 1: Build --------
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app

# Copiar todo el contexto (incluye gradlew, gradle/, build.gradle.kts, src/, etc.)
COPY . .

# Ejecutar build con gradle wrapper si existe, si no usar gradle del image
RUN if [ -f ./gradlew ]; then \
      chmod +x ./gradlew && ./gradlew clean bootJar -x test --no-daemon; \
    else \
      gradle clean bootJar -x test --no-daemon; \
    fi

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="CETAD UMAS Team"
LABEL description="UMAS Core Service - Hexagonal Architecture with Kafka and UgCS"
LABEL version="0.0.1-SNAPSHOT"

ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Bogota \
    APP_USER=umas \
    APP_GROUP=umas

# Instalar curl para healthcheck y crear usuario no-root
RUN apk add --no-cache curl \
 && addgroup -S ${APP_GROUP} \
 && adduser -S ${APP_USER} -G ${APP_GROUP}

WORKDIR /app

# Copiar el JAR desde la stage de build y renombrarlo a app.jar
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# Fail early: asegurar que el jar exista en la imagen
RUN [ -f /app/app.jar ]

# Asignar ownership antes de cambiar a user no-root
RUN chown -R ${APP_USER}:${APP_GROUP} /app

# Exponer el puerto que usa tu application.yml (8084)
EXPOSE 8080

# Cambiar a usuario no-root
USER ${APP_USER}

# Healthcheck apuntando al puerto real; usa curl (instalado arriba)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ENTRYPOINT: usar nombre neutro app.jar y JAVA_OPTS
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
