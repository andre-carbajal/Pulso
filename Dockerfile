FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Layer 1: Copy build tooling and dependencies manifest
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Layer 2: Download dependencies (cached separately from source changes)
RUN ./gradlew dependencies --no-daemon

# Layer 3: Copy source and build JAR
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/build/libs/*.jar app.jar

# Set JVM memory constraints
ENV JAVA_OPTS="-Xms128m -Xmx1gm"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S -G appgroup appuser
USER appuser

EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=15s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
