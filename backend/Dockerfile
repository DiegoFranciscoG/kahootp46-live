# syntax=docker/dockerfile:1

FROM eclipse-temurin:21.0.12.1_1-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline
COPY src/ src/
# Tests run in CI (they need Docker for Testcontainers); the image build only packages.
RUN ./mvnw -B -ntp -q -DskipTests package \
    && java -Djarmode=tools -jar target/kahootp46-live-*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21.0.12.1_1-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
