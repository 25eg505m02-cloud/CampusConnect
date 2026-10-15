FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./pom.xml
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
COPY collections ./collections
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
ENV CAMPUS_CONNECT_SEED_DIRECTORY=/app/collections
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/target/campus-connect-backend-*.jar /app/app.jar
COPY --from=build --chown=app:app /workspace/collections /app/collections

USER app
EXPOSE 8080
VOLUME ["/app/uploads"]
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
