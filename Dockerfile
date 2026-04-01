# Stage 1: Build
# Use the official Gradle image which bundles Gradle + JDK 21.
# This avoids requiring a pre-committed gradlew script and gradle-wrapper.jar.
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# Copy dependency descriptors first for optimal layer caching.
# Changes to source code will not bust the dependency download cache.
COPY build.gradle settings.gradle ./

# Pre-fetch dependencies in a separate layer so they are cached across builds.
RUN gradle dependencies --no-daemon 2>/dev/null || true

# Copy application source and build the fat jar (skip tests for the image build).
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
# Use a minimal JRE image to keep the production image small and secure.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as non-root user for security hardening.
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
