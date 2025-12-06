# Build stage - need JDK for Kotlin/Wasm compilation
# Use Debian-based image (not Alpine) for compatibility with Kotlin's Node.js download
FROM eclipse-temurin:17-jdk as builder

# Install Node.js and npm
RUN apt-get update && apt-get install -y nodejs npm curl && rm -rf /var/lib/apt/lists/*

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
COPY apps-kmp apps-kmp

# Build production distribution
RUN ./gradlew :apps-kmp:wasmJsBrowserDistribution --no-daemon

# Runtime stage - nginx to serve static files
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy built app
COPY --from=builder /app/apps-kmp/build/dist/wasmJs/productionExecutable/ .

# Copy nginx config
COPY apps-kmp/nginx.conf /etc/nginx/conf.d/default.conf

# Cloud Run uses port 8080
EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
