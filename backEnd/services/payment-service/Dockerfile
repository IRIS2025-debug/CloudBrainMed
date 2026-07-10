FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY target/payment-service-0.0.1-SNAPSHOT-exec.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
