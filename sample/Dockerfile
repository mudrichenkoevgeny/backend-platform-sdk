# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :sample:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app
COPY --from=builder /app/sample/build/libs/*-all.jar app.jar

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

