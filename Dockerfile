# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package
RUN ls target/*.jar

# Etapa 2: ejecutar sin privilegios
FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
USER 10001
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
