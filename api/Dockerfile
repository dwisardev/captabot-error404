# API de CaptaBot – imagen para Docker Compose
# 1. Compilación con Maven
FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# 2. Ejecución solo con el JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 captabot
COPY --from=compilacion /app/target/captabot-api.jar app.jar
USER captabot
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
