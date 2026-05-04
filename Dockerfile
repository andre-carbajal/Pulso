FROM ghcr.io/graalvm/native-image-community:25 AS builder

RUN microdnf install -y curl ca-certificates findutils && microdnf clean all

WORKDIR /build

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/
RUN ./gradlew nativeCompile --no-daemon -x test

FROM ubuntu:22.04 AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /build/build/native/nativeCompile/pulso .

RUN useradd -r -s /bin/false appuser
USER appuser

EXPOSE 8080

ENTRYPOINT ["./pulso"]
