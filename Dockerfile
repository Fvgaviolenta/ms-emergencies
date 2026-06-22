# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S catastrofescl && adduser -S catastrofescl -G catastrofescl

COPY --from=build /build/target/ms-emergencies-*.jar app.jar

USER catastrofescl

EXPOSE 8082

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
