FROM gradle:9.2.1-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY market-app/build.gradle market-app/
COPY payment-service/build.gradle payment-service/
COPY openapi/ openapi/
RUN gradle :payment-service:dependencies --no-daemon || true
COPY payment-service/src/ payment-service/src/
RUN gradle :payment-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/payment-service/build/libs/payment-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
