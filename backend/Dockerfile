FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S fincontrol && adduser -S fincontrol -G fincontrol
WORKDIR /app
COPY --from=build /workspace/target/fincontrol-api-*.jar app.jar
USER fincontrol
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=3s --start-period=35s --retries=3 CMD wget -q --spider http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
