# syntax=docker/dockerfile:1
# Single-container image for PaaS deployment (Render): the backend also serves the Angular build,
# so the browser talks to one origin (no CORS, no separate WebSocket proxy).
# For local development use docker-compose.yml (nginx + backend as separate services).

FROM node:24.21.0-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21.0.12.1_1-jdk-alpine AS api
WORKDIR /workspace
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline
COPY backend/src/ src/
COPY --from=web /web/dist/kahootp46-web/browser/ src/main/resources/static/
RUN ./mvnw -B -ntp -q -DskipTests package \
    && java -Djarmode=tools -jar target/kahootp46-live-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21.0.12.1_1-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=api --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=api --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=api --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=api --chown=app:app /workspace/extracted/application/ ./
ENV APP_SERVE_SPA=true
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=90s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:${PORT:-${APP_PORT:-8080}}/actuator/health/readiness" | grep -q '"UP"' || exit 1
# Serial GC and a smaller thread stack keep the footprint inside a 512 MB instance.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70", "-Xss512k", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
