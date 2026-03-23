# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
