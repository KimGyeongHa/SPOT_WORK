FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :sukima-web:build -x test --no-daemon

FROM amazoncorretto:21
WORKDIR /app
RUN mkdir -p /log
COPY --from=builder /app/sukima-web/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
