# Testes não rodam aqui: exigem Docker (Testcontainers). Execute-os antes do build (ver README).
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
# Diretório de arquivos enviados: somente o usuário da aplicação lê e grava.
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /var/lib/processo-seletivo/storage \
    && chown app:app /var/lib/processo-seletivo/storage \
    && chmod 700 /var/lib/processo-seletivo/storage
WORKDIR /app
COPY --from=build /build/target/processo-seletivo-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
