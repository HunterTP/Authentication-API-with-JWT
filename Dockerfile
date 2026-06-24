# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests
RUN keytool -genkeypair -alias selfsigned -keyalg RSA -keysize 2048 \
    -validity 3650 -storetype PKCS12 -keystore keystore.jks \
    -storepass 123456 -keypass 123456 -noprompt \
    -dname "CN=localhost, OU=Dev, O=Authentication-API, L=Unknown, ST=Unknown, C=Unknown"

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    apk add --no-cache curl

COPY --from=builder /app/target/app.jar ./app.jar
COPY --from=builder /app/keystore.jks ./keystore.jks

EXPOSE 8443

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]