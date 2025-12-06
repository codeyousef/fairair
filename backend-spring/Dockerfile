# Build stage
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app

# Copy gradle files first for better caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source
COPY shared-contract shared-contract
COPY backend-spring backend-spring

# Build
RUN chmod +x gradlew
RUN ./gradlew :backend-spring:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/backend-spring/build/libs/*.jar app.jar

# Cloud Run sets PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
