# Build stage - need JDK for Kotlin/Wasm compilation
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app

# Install Node.js for webpack
RUN apk add --no-cache nodejs npm

# Copy gradle files first for better caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source
COPY shared-contract shared-contract
COPY apps-kmp apps-kmp
COPY build build

# Build production distribution
RUN chmod +x gradlew
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
