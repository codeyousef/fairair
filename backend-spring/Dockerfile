# Build stage
FROM eclipse-temurin:17-jdk-alpine as builder

# Install required tools
RUN apk add --no-cache curl unzip bash

WORKDIR /app

# Copy build files first
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY gradle/libs.versions.toml gradle/

# Download gradle wrapper jar (gradle 8.11)
RUN curl -fsSL -o gradle/wrapper/gradle-wrapper.jar \
    "https://github.com/gradle/gradle/raw/v8.11.0/gradle/wrapper/gradle-wrapper.jar"

# Copy gradlew and make executable
COPY gradlew .
RUN chmod +x gradlew

# Copy source
COPY shared-contract shared-contract
COPY backend-spring backend-spring

# Build
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
