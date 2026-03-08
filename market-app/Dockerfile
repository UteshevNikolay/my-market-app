FROM gradle:9.2.1-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY market-app/build.gradle market-app/
COPY payment-service/build.gradle payment-service/
COPY openapi/ openapi/
RUN gradle :market-app:dependencies --no-daemon || true
COPY market-app/src/ market-app/src/
RUN gradle :market-app:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/market-app/build/libs/market-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
