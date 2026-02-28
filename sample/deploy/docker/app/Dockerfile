ARG GRADLE_IMAGE=gradle:9.1.0-jdk21-alpine
ARG JRE_IMAGE=eclipse-temurin:21-jre-jammy

FROM ${GRADLE_IMAGE} AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

COPY core/ ./core/
COPY feature/ ./feature/
COPY bom/build.gradle.kts ./bom/
COPY sample/build.gradle.kts ./sample/

RUN find core/ -type f ! -name "*.gradle.kts" -delete
RUN find feature/ -type f ! -name "*.gradle.kts" -delete

RUN --mount=type=cache,target=/root/.gradle ./gradlew :sample:dependencies --no-daemon

COPY . .

RUN --mount=type=cache,target=/root/.gradle ./gradlew :sample:jar :sample:copyLibs --no-daemon

FROM ${JRE_IMAGE}
WORKDIR /app

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs && chmod 775 /app/logs

COPY --from=build --chown=appuser:appgroup /app/sample/build/libs/app.jar /app/app.jar
COPY --from=build --chown=appuser:appgroup /app/sample/build/libs/lib /app/lib/
COPY --chown=appuser:appgroup sample/deploy/docker/app/entrypoint.sh entrypoint.sh

RUN chmod +x entrypoint.sh
USER appuser

ARG APP_PORT
EXPOSE ${APP_PORT}

ENTRYPOINT ["./entrypoint.sh"]