# ─── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/workflow-engine-*.jar app.jar

EXPOSE 8080

# Size the heap from the container's memory limit instead of fixed -Xmx values.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV AI_API_KEY=demo-mode

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/liveness || exit 1

# 'exec' replaces the shell so Java is PID 1 and receives SIGTERM from 'docker stop'
# (otherwise the shell swallows it and the app is killed after the timeout).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
