# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN useradd --system vsp
COPY --from=build --chown=vsp:vsp /src/target/backend-0.0.1-SNAPSHOT.jar app.jar

USER vsp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
