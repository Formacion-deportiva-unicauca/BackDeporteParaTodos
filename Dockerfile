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
# Salud: la imagen lleva su propia verificacion (aplicacion + base de datos). Usa curl, que ya trae la base eclipse-temurin: este
# RUN hace fallar el build si una version futura de la base lo quitara (no se instala nada). El endpoint vive en el puerto de
# gestion 8083, solo en loopback y sin publicar (ver application.properties). start-period=60s: con 1 CPU y 1 GB la JVM tarda
# 33-56 s en arrancar (medido); durante ese lapso se sondea cada 3 s (start-interval, igual que la vuelta de desplegar.sh) y los
# fallos no cuentan; ya estable, cada 10 s y 5 fallos seguidos la marcan unhealthy.
RUN command -v curl > /dev/null
USER 10001
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=60s --start-interval=3s \
  CMD curl -fsS --max-time 2 http://127.0.0.1:8083/actuator/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
