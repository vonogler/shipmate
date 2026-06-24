FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace
COPY shipmate-api/gradlew shipmate-api/settings.gradle.kts shipmate-api/build.gradle.kts ./
COPY shipmate-api/gradle ./gradle
COPY shipmate-api/src ./src
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre

WORKDIR /workspace
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /workspace/build/libs/*.jar /workspace/shipmate-api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/workspace/shipmate-api.jar"]
