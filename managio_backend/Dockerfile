# Managio API image. Build from managio_backend: docker build -t managio-api .
# Configuration comes only from environment variables (docs/ENVIRONMENT.md); nothing secret is baked in.

# ── Build ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src

# Dependencies first, so source changes don't re-download them.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI (they need Docker for Testcontainers), not inside the image build.
RUN ./mvnw -B -q package -DskipTests && cp target/managio-api-*.jar app.jar

# ── Run ───────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S managio && adduser -S managio -G managio
COPY --from=build --chown=managio:managio /src/app.jar app.jar
USER managio

ENV PORT=8080
EXPOSE 8080

# Ready means the app has started and can take traffic; hosts like Render can use the same path.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health/readiness" > /dev/null || exit 1

# Leave a quarter of the container's memory to the JVM itself and the OS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
