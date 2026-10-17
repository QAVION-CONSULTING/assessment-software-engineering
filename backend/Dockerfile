# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY backend/pom.xml backend/pom.xml
COPY docs/openapi.yaml docs/openapi.yaml
RUN mvn -B -f backend/pom.xml dependency:go-offline

COPY backend/src backend/src
RUN mvn -B -f backend/pom.xml package -DskipTests \
    -Dopenapi.spec=docs/openapi.yaml

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/backend/target/timetracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
