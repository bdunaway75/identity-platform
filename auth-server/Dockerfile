# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build/auth-server
ENV MAVEN_OPTS="-Xmx2g"

COPY auth-server/pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY auth-server/src src

RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package \
    && cp "$(ls target/*.war | grep -v '\.original$')" /tmp/auth-server.war

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --system --create-home --home-dir /app spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

COPY --from=build /tmp/auth-server.war /app/auth-server.war

USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/auth-server.war"]
