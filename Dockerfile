# --- Build stage ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# --- Run stage ---
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/intranet-qa-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
